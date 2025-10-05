use byteorder::{LittleEndian, ReadBytesExt};
use std::env;
use std::fs::File;
use std::io::{self, Read, Seek, SeekFrom};
use std::mem;
use std::ptr;
use std::slice;
use std::sync::{
    atomic::{AtomicBool, Ordering},
    Arc, Mutex,
};
use std::thread;
use std::time::Duration;

extern crate alsa_sys as alsa;

#[derive(Debug, Clone)]
struct DSDFormat {
    sampling_rate: u32,
    num_channels: u32,
    total_samples: u64,
    is_lsb_first: bool,
}

trait DSDReader {
    fn open(&mut self, format: &mut DSDFormat) -> io::Result<()>;
    /// Read up to `bytes_per_channel` bytes into each channel buffer.
    /// Returns number of bytes actually read per channel.
    fn read(&mut self, data: &mut [&mut [u8]], bytes_per_channel: usize) -> io::Result<usize>;
}

struct DSFReader {
    file: File,
    buf: Vec<u8>,
    ch: usize,
    blocksize: usize,
    filled: usize,
    pos: usize,
    total_samples: u64,
    read_samples: u64,
    data_start: u64, // byte offset in file where "data" begins
}

impl DSFReader {
    fn new(path: &str) -> io::Result<Self> {
        let file = File::open(path)?;
        Ok(Self {
            file,
            buf: Vec::new(),
            ch: 0,
            blocksize: 0,
            filled: 0,
            pos: 0,
            total_samples: 0,
            read_samples: 0,
            data_start: 0,
        })
    }
}

impl DSDReader for DSFReader {
    fn open(&mut self, format: &mut DSDFormat) -> io::Result<()> {
        let mut ident = [0u8; 4];
        // read "DSD " chunk id
        self.file.read_exact(&mut ident)?;
        if &ident != b"DSD " {
            return Err(io::Error::new(io::ErrorKind::InvalidData, "not DSF"));
        }
        let _size = self.file.read_u64::<LittleEndian>()?; // size
        // seek to fmt chunk (mimic original)
        let to_skip = _size as i64 - 12;
        self.file.seek(SeekFrom::Current(to_skip))?;

        self.file.read_exact(&mut ident)?;
        if &ident != b"fmt " {
            return Err(io::Error::new(io::ErrorKind::InvalidData, "fmt chunk missing"));
        }
        let fmt_size = self.file.read_u64::<LittleEndian>()?;
        let format_version = self.file.read_u32::<LittleEndian>()?;
        if format_version != 1 {
            return Err(io::Error::new(io::ErrorKind::InvalidData, "unsupported format version"));
        }
        let format_id = self.file.read_u32::<LittleEndian>()?;
        if format_id != 0 {
            return Err(io::Error::new(io::ErrorKind::InvalidData, "unsupported format id"));
        }
        let channel_type = self.file.read_u32::<LittleEndian>()?;
        if channel_type != 2 {
            return Err(io::Error::new(io::ErrorKind::InvalidData, "unsupported channel type"));
        }
        let channels = self.file.read_u32::<LittleEndian>()?;
        // set channels even if not 2; caller will check stereo
        format.num_channels = channels;
        self.ch = channels as usize;

        let sampling_freq = self.file.read_u32::<LittleEndian>()?;
        if sampling_freq != 2_822_400 && sampling_freq != 5_644_800 {
            return Err(io::Error::new(io::ErrorKind::InvalidData, "unsupported sampling frequency"));
        }
        format.sampling_rate = sampling_freq;

        let bits_per_sample = self.file.read_u32::<LittleEndian>()?;
        if bits_per_sample != 1 && bits_per_sample != 8 {
            return Err(io::Error::new(io::ErrorKind::InvalidData, "unsupported bits per sample"));
        }
        format.is_lsb_first = bits_per_sample == 1;

        let sample_count = self.file.read_u64::<LittleEndian>()?;
        format.total_samples = sample_count;
        self.total_samples = sample_count;

        let block_size = self.file.read_u32::<LittleEndian>()? as usize;
        self.blocksize = block_size;

        // seek to end of fmt chunk like original
        let seek_rel = fmt_size as i64 - 48;
        self.file.seek(SeekFrom::Current(seek_rel))?;

        // find "data" chunk
        self.file.read_exact(&mut ident)?;
        if &ident != b"data" {
            return Err(io::Error::new(io::ErrorKind::InvalidData, "data chunk missing"));
        }
        let _data_size = self.file.read_u64::<LittleEndian>()?;

        // current file pos is beginning of the data region
        self.data_start = self.file.seek(SeekFrom::Current(0))?;

        // prepare buffer: blocksize * num_channels
        self.buf.resize(self.blocksize * self.ch, 0);

        // start at beginning
        self.filled = 0;
        self.pos = 0;
        self.read_samples = 0;

        Ok(())
    }

    fn read(&mut self, data: &mut [&mut [u8]], bytes_per_channel: usize) -> io::Result<usize> {
        let mut read_bytes = 0usize;
        let mut want = bytes_per_channel;
        while want > 0 {
            if self.pos == self.filled {
                // read the interleaved block of blocksize * ch
                let to_read = self.blocksize * self.ch;
                self.buf.resize(to_read, 0);
                let n = self.file.read(&mut self.buf)?;
                if n == 0 {
                    return Ok(read_bytes);
                }
                // the original code computed _filled = gcount() / _ch
                self.filled = n / self.ch;
                self.pos = 0;
            }
            let available = self.filled - self.pos;
            let size = if want < available { want } else { available };
            for i in 0..self.ch {
                let src_offset = self.blocksize * i + self.pos;
                let src = &self.buf[src_offset..src_offset + size];
                let dst = &mut data[i][read_bytes..read_bytes + size];
                dst.copy_from_slice(src);
            }
            self.pos += size;
            want -= size;
            read_bytes += size;
        }

        // update read_samples (in bits)
        self.read_samples = self.read_samples.saturating_add((read_bytes as u64) * 8);
        if self.read_samples > self.total_samples {
            let overflow = self.read_samples - self.total_samples;
            let reduce_bytes = (overflow / 8) as usize;
            read_bytes = read_bytes.saturating_sub(reduce_bytes);
            self.read_samples = self.total_samples;
        }
        Ok(read_bytes)
    }
}

static BIT_REVERSE_TABLE: [u8; 256] = [
    0x00,0x80,0x40,0xc0,0x20,0xa0,0x60,0xe0,0x10,0x90,0x50,0xd0,0x30,0xb0,0x70,0xf0,
    0x08,0x88,0x48,0xc8,0x28,0xa8,0x68,0xe8,0x18,0x98,0x58,0xd8,0x38,0xb8,0x78,0xf8,
    0x04,0x84,0x44,0xc4,0x24,0xa4,0x64,0xe4,0x14,0x94,0x54,0xd4,0x34,0xb4,0x74,0xf4,
    0x0c,0x8c,0x4c,0xcc,0x2c,0xac,0x6c,0xec,0x1c,0x9c,0x5c,0xdc,0x3c,0xbc,0x7c,0xfc,
    0x02,0x82,0x42,0xc2,0x22,0xa2,0x62,0xe2,0x12,0x92,0x52,0xd2,0x32,0xb2,0x72,0xf2,
    0x0a,0x8a,0x4a,0xca,0x2a,0xaa,0x6a,0xea,0x1a,0x9a,0x5a,0xda,0x3a,0xba,0x7a,0xfa,
    0x06,0x86,0x46,0xc6,0x26,0xa6,0x66,0xe6,0x16,0x96,0x56,0xd6,0x36,0xb6,0x76,0xf6,
    0x0e,0x8e,0x4e,0xce,0x2e,0xae,0x6e,0xee,0x1e,0x9e,0x5e,0xde,0x3e,0xbe,0x7e,0xfe,
    0x01,0x81,0x41,0xc1,0x21,0xa1,0x61,0xe1,0x11,0x91,0x51,0xd1,0x31,0xb1,0x71,0xf1,
    0x09,0x89,0x49,0xc9,0x29,0xa9,0x69,0xe9,0x19,0x99,0x59,0xd9,0x39,0xb9,0x79,0xf9,
    0x05,0x85,0x45,0xc5,0x25,0xa5,0x65,0xe5,0x15,0x95,0x55,0xd5,0x35,0xb5,0x75,0xf5,
    0x0d,0x8d,0x4d,0xcd,0x2d,0xad,0x6d,0xed,0x1d,0x9d,0x5d,0xdd,0x3d,0xbd,0x7d,0xfd,
    0x03,0x83,0x43,0xc3,0x23,0xa3,0x63,0xe3,0x13,0x93,0x53,0xd3,0x33,0xb3,0x73,0xf3,
    0x0b,0x8b,0x4b,0xcb,0x2b,0xab,0x6b,0xeb,0x1b,0x9b,0x5b,0xdb,0x3b,0xbb,0x7b,0xfb,
    0x07,0x87,0x47,0xc7,0x27,0xa7,0x67,0xe7,0x17,0x97,0x57,0xd7,0x37,0xb7,0x77,0xf7,
    0x0f,0x8f,0x4f,0xcf,0x2f,0xaf,0x6f,0xef,0x1f,0x9f,0x5f,0xdf,0x3f,0xbf,0x7f,0xff,
];

struct DSDPlayer {
    reader: Arc<Mutex<DSFReader>>,
    format: DSDFormat,
    is_paused: Arc<AtomicBool>,
    stop_flag: Arc<AtomicBool>,
    thread_handle: Option<thread::JoinHandle<()>>,
}

impl DSDPlayer {
    fn new(reader: DSFReader, format: DSDFormat) -> Self {
        Self {
            reader: Arc::new(Mutex::new(reader)),
            format,
            is_paused: Arc::new(AtomicBool::new(false)),
            stop_flag: Arc::new(AtomicBool::new(false)),
            thread_handle: None,
        }
    }

    /// spawn playback thread (if not already running)
    fn play(&mut self) {
        if self.thread_handle.is_some() {
            // already running - just resume
            self.is_paused.store(false, Ordering::SeqCst);
            return;
        }

        let reader = self.reader.clone();
        let format = self.format.clone();
        let is_paused = self.is_paused.clone();
        let stop_flag = self.stop_flag.clone();

        let handle = thread::spawn(move || unsafe {
            let mut err: i32 = 0;
            let mut playback_handle: *mut alsa::snd_pcm_t = ptr::null_mut();
            let mut hw_params: *mut alsa::snd_pcm_hw_params_t = ptr::null_mut();

            // buffer sizes (same as original)
            let mut alsa_buffer_size = 8192 * 4usize;
            let blocksize = alsa_buffer_size / format.num_channels as usize;

            // per-channel work buffers
            let mut work0 = vec![0u8; alsa_buffer_size >> 1];
            let mut work1 = vec![0u8; alsa_buffer_size >> 1];
            let mut work_slices: [&mut [u8]; 2] = [work0.as_mut_slice(), work1.as_mut_slice()];

            let mut pcm = vec![0u8; alsa_buffer_size];

            // open device (hw:2,0 as in original)
            let device = std::ffi::CString::new("hw:2,0").unwrap();
            err = alsa::snd_pcm_open(&mut playback_handle, device.as_ptr(), alsa::SND_PCM_STREAM_PLAYBACK, 0);
            if err < 0 {
                eprintln!("cannot open audio device: {}", err);
                return;
            }

            if (alsa::snd_pcm_hw_params_malloc)(&mut hw_params) < 0 {
                eprintln!("cannot allocate hardware parameter structure");
                if !playback_handle.is_null() {
                    alsa::snd_pcm_close(playback_handle);
                }
                return;
            }
            if alsa::snd_pcm_hw_params_any(playback_handle, hw_params) < 0 {
                eprintln!("cannot initialize hardware parameter structure");
                goto_close(playback_handle, hw_params);
                return;
            }
            if alsa::snd_pcm_hw_params_set_access(playback_handle, hw_params, alsa::SND_PCM_ACCESS_RW_INTERLEAVED) < 0 {
                eprintln!("cannot set access type");
                goto_close(playback_handle, hw_params);
                return;
            }
            // set rate to sampling_rate / 8 / 4 (matching original)
            let rate = (format.sampling_rate / 8 / 4) as u32;
            if alsa::snd_pcm_hw_params_set_rate(playback_handle, hw_params, rate, 0) < 0 {
                eprintln!("cannot set sample rate");
                goto_close(playback_handle, hw_params);
                return;
            }
            if alsa::snd_pcm_hw_params_set_channels(playback_handle, hw_params, format.num_channels as u32) < 0 {
                eprintln!("cannot set channel count");
                goto_close(playback_handle, hw_params);
                return;
            }
            if alsa::snd_pcm_hw_params_set_format(playback_handle, hw_params, alsa::SND_PCM_FORMAT_DSD_U32_BE) < 0 {
                eprintln!("cannot set sample format");
                goto_close(playback_handle, hw_params);
                return;
            }

            let mut frames: alsa::snd_pcm_uframes_t = (alsa_buffer_size / format.num_channels as usize / 4) as alsa::snd_pcm_uframes_t;
            let mut dir: i32 = 0;
            alsa::snd_pcm_hw_params_set_period_size_near(playback_handle, hw_params, &mut frames, &mut dir);
            if alsa::snd_pcm_hw_params(playback_handle, hw_params) < 0 {
                eprintln!("cannot set parameters");
                goto_close(playback_handle, hw_params);
                return;
            }
            alsa::snd_pcm_hw_params_free(hw_params);
            if alsa::snd_pcm_prepare(playback_handle) < 0 {
                eprintln!("cannot prepare audio interface for use");
                goto_close(playback_handle, ptr::null_mut());
                return;
            }

            // playback loop
            loop {
                if stop_flag.load(Ordering::SeqCst) {
                    break;
                }

                if is_paused.load(Ordering::SeqCst) {
                    // while paused, sleep to avoid busy-loop and to avoid underruns
                    thread::sleep(Duration::from_millis(50));
                    continue;
                }

                // Read from DSFReader
                let bytes = {
                    let mut rdr = reader.lock().unwrap();
                    match rdr.read(&mut work_slices, alsa_buffer_size / format.num_channels as usize) {
                        Ok(b) => b,
                        Err(_) => {
                            eprintln!("read error");
                            break;
                        }
                    }
                };

                if bytes == 0 {
                    // EOF
                    break;
                }

                // Convert/interleave to pcm buffer (DSD_U32_BE layout expected by ALSA)
                let mut i = 0usize;
                let l = &work_slices[0];
                let r = &work_slices[1];

                if format.is_lsb_first {
                    // bit reverse per byte
                    let mut j = 0usize;
                    while j + 3 < bytes {
                        pcm[i + 0] = BIT_REVERSE_TABLE[l[j + 0] as usize];
                        pcm[i + 1] = BIT_REVERSE_TABLE[l[j + 1] as usize];
                        pcm[i + 2] = BIT_REVERSE_TABLE[l[j + 2] as usize];
                        pcm[i + 3] = BIT_REVERSE_TABLE[l[j + 3] as usize];

                        pcm[i + 4] = BIT_REVERSE_TABLE[r[j + 0] as usize];
                        pcm[i + 5] = BIT_REVERSE_TABLE[r[j + 1] as usize];
                        pcm[i + 6] = BIT_REVERSE_TABLE[r[j + 2] as usize];
                        pcm[i + 7] = BIT_REVERSE_TABLE[r[j + 3] as usize];

                        i += 8;
                        j += 4;
                    }
                } else {
                    let mut j = 0usize;
                    while j + 3 < bytes {
                        pcm[i + 0] = l[j + 0];
                        pcm[i + 1] = l[j + 1];
                        pcm[i + 2] = l[j + 2];
                        pcm[i + 3] = l[j + 3];

                        pcm[i + 4] = r[j + 0];
                        pcm[i + 5] = r[j + 1];
                        pcm[i + 6] = r[j + 2];
                        pcm[i + 7] = r[j + 3];

                        i += 8;
                        j += 4;
                    }
                }

                // write frames: bytes / 4 (32-bit samples)
                let write_frames = (bytes / 4) as i64;
                let ptr_pcm = pcm.as_ptr() as *const std::ffi::c_void;
                let written = alsa::snd_pcm_writei(playback_handle, ptr_pcm, write_frames as alsa::snd_pcm_uframes_t);
                if written != write_frames as i64 {
                    eprintln!("write to audio interface failed: wrote {} expected {}", written, write_frames);
                    break;
                }
            }

            if !playback_handle.is_null() {
                alsa::snd_pcm_close(playback_handle);
            }
            println!("Playback thread exiting.");
        });

        self.thread_handle = Some(handle);
        // ensure not paused
        self.is_paused.store(false, Ordering::SeqCst);
    }

    fn pause(&self) {
        self.is_paused.store(true, Ordering::SeqCst);
    }

    fn resume(&self) {
        self.is_paused.store(false, Ordering::SeqCst);
    }

    /// request stop and join thread
    fn stop(&mut self) {
        self.stop_flag.store(true, Ordering::SeqCst);
        if let Some(handle) = self.thread_handle.take() {
            let _ = handle.join();
        }
    }

    /// Seek to `seconds` (DSD sample domain). This adjusts DSFReader's file pointer and read state.
    /// Because DSD is 1-bit per sample per channel, bytes_per_channel = sample_index / 8.
    fn seek(&mut self, seconds: f64) -> io::Result<()> {
        // compute bit-sample index
        let sample_index = (seconds * (self.format.sampling_rate as f64)) as u64;
        // bytes per channel (each 8 DSD samples == 1 byte)
        let byte_offset_chan = (sample_index / 8) as u64;

        // align to block boundary for DSFReader
        let mut rdr = self.reader.lock().unwrap();

        let block = rdr.blocksize as u64;
        let ch = rdr.ch as u64;
        if block == 0 {
            return Err(io::Error::new(io::ErrorKind::Other, "invalid blocksize"));
        }

        let nblocks = byte_offset_chan / block;
        let remainder = (byte_offset_chan % block) as usize;

        let file_pos = rdr.data_start
            .checked_add(nblocks * (block * ch))
            .ok_or_else(|| io::Error::new(io::ErrorKind::Other, "seek overflow"))?;

        rdr.file.seek(SeekFrom::Start(file_pos))?;
        // reset buffer state: so next read will fill buffer from this multi-channel block,
        // and pos will start at 'remainder' inside per-channel block
        rdr.filled = 0;
        rdr.pos = remainder;
        // set read_samples to reflect actual DSD bits read so far
        rdr.read_samples = byte_offset_chan.saturating_mul(8);

        Ok(())
    }
}

unsafe fn goto_close(playback_handle: *mut alsa::snd_pcm_t, hw_params: *mut alsa::snd_pcm_hw_params_t) {
    if !hw_params.is_null() {
        alsa::snd_pcm_hw_params_free(hw_params);
    }
    if !playback_handle.is_null() {
        alsa::snd_pcm_close(playback_handle);
    }
}

fn main() -> io::Result<()> {
    // Accept filename as first argument, otherwise fallback to your test path
    let args: Vec<String> = env::args().collect();
    let filename = if args.len() >= 2 {
        args[1].as_str()
    } else {
        // fallback path you used earlier
        "/mnt/files2/Music/Dire Straits - Brothers In Arms - 1985(2021),(USA),DSF(tracks),(OC-9XMLf+MF(115)avb+Pio)/A1 So Far Away.dsf"
    };

    let mut reader = DSFReader::new(filename)?;
    let mut format = DSDFormat {
        sampling_rate: 0,
        num_channels: 0,
        total_samples: 0,
        is_lsb_first: false,
    };
    reader.open(&mut format)?;

    println!("DSD Format:");
    println!("  sampling rate: {}", format.sampling_rate);
    println!("  num channels:  {}", format.num_channels);
    println!("  lsb first?:    {}", if format.is_lsb_first { "yes" } else { "no" });
    println!("  total samples: {}", format.total_samples);

    if format.num_channels != 2 {
        println!("not stereo");
        std::process::exit(1);
    }

    let mut player = DSDPlayer::new(reader, format);

    // Start playback
    println!("Starting playback...");
    player.play();

    // demo sequence: play 5s, pause 3s, resume 3s, seek to 30s, play 5s, stop
    /*
    thread::sleep(Duration::from_secs(5));
    println!("Pausing...");
    player.pause();

    thread::sleep(Duration::from_secs(3));
    println!("Resuming...");
    player.resume();


     */
    thread::sleep(Duration::from_secs(3));
    println!("Seeking to 30.0s...");
    if let Err(e) = player.seek(30.0) {
        eprintln!("seek failed: {}", e);
    }

    thread::sleep(Duration::from_secs(5));
    println!("Stopping...");
    player.stop();

    println!("Done.");
    Ok(())
}
