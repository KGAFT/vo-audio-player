//There must be dsd player, with ability to play dsd without pcm conversion
//But asio is hard af, also needed UAC2 for linux/android

use std::{io, ptr};
use std::fs::File;
use std::io::{Read, Seek, SeekFrom};
use byteorder::{LittleEndian, ReadBytesExt};

extern crate alsa_sys as alsa;

#[derive(Debug)]
struct DSDFormat {
    sampling_rate: u32,
    num_channels: u32,
    total_samples: u64,
    is_lsb_first: bool,
}

trait DSDReader {
    fn open(&mut self, format: &mut DSDFormat) -> io::Result<()>;
    fn read(&mut self, data: &mut [&mut [u8]], bytes_per_channel: usize) -> io::Result<usize>;
    fn seek_percent(&mut self, percent: f64) -> io::Result<()>;
    fn seek_samples(&mut self, sample_index: u64) -> io::Result<()>;
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
    data_start: u64, // <---- new: start offset of "data" chunk content
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

        // --- DSD chunk ---
        self.file.read_exact(&mut ident)?;
        if &ident != b"DSD " {
            return Err(io::Error::new(io::ErrorKind::InvalidData, "not DSF"));
        }
        let dsd_size = self.file.read_u64::<LittleEndian>()?;
        self.file.seek(SeekFrom::Current(dsd_size as i64 - 12))?;

        // --- fmt chunk ---
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
        let _channel_type = self.file.read_u32::<LittleEndian>()?;
        let channels = self.file.read_u32::<LittleEndian>()?;
        format.num_channels = channels;
        self.ch = channels as usize;

        let sampling_freq = self.file.read_u32::<LittleEndian>()?;
        format.sampling_rate = sampling_freq;
        let bits_per_sample = self.file.read_u32::<LittleEndian>()?;
        format.is_lsb_first = bits_per_sample == 1;

        let sample_count = self.file.read_u64::<LittleEndian>()?;
        format.total_samples = sample_count;
        self.total_samples = sample_count;

        let block_size = self.file.read_u32::<LittleEndian>()? as usize;
        self.blocksize = block_size;

        self.file.seek(SeekFrom::Current(fmt_size as i64 - 48))?;

        // --- data chunk ---
        self.file.read_exact(&mut ident)?;
        if &ident != b"data" {
            return Err(io::Error::new(io::ErrorKind::InvalidData, "data chunk missing"));
        }
        let _data_size = self.file.read_u64::<LittleEndian>()?;

        // mark data start
        self.data_start = self.file.seek(SeekFrom::Current(0))?;

        // allocate buffer
        self.buf.resize(self.blocksize * self.ch, 0);

        Ok(())
    }

    fn read(&mut self, data: &mut [&mut [u8]], bytes_per_channel: usize) -> io::Result<usize> {
        let mut read_bytes = 0usize;
        let mut want = bytes_per_channel;

        while want > 0 {
            if self.pos == self.filled {
                // read next interleaved block
                let to_read = self.blocksize * self.ch;
                self.buf.resize(to_read, 0);
                let n = self.file.read(&mut self.buf)?;
                if n == 0 {
                    return Ok(read_bytes);
                }
                self.filled = n / self.ch;
                self.pos = 0;
            }

            let available = self.filled - self.pos;
            let size = available.min(want);
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

        self.read_samples = self
            .read_samples
            .saturating_add((read_bytes as u64) * 8);
        Ok(read_bytes)
    }

    fn seek_percent(&mut self, percent: f64) -> io::Result<()> {
        if percent < 0.0 || percent > 1.0 {
            return Err(io::Error::new(io::ErrorKind::InvalidInput, "percent out of range"));
        }
        let target_sample = (self.total_samples as f64 * percent) as u64;
        self.seek_samples(target_sample)
    }

    fn seek_samples(&mut self, sample_index: u64) -> io::Result<()> {
        // DSD = 1 bit per sample per channel
        let total_bits = sample_index * self.ch as u64;
        let total_bytes = total_bits / 8;

        // align to nearest block boundary
        let aligned_bytes =
            (total_bytes / (self.blocksize * self.ch) as u64) * (self.blocksize * self.ch) as u64;

        let offset = self.data_start + aligned_bytes;
        self.file.seek(SeekFrom::Start(offset))?;

        self.read_samples = aligned_bytes * 8;
        self.pos = 0;
        self.filled = 0;

        Ok(())
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

struct Buffers{
    buf_left: Vec<u8>,
    buf_right: Vec<u8>,
    block_size: usize,
    alsa_buffer_size: usize
}

impl Buffers {
    fn get_buff_slices(&mut self) -> [&mut [u8]; 2]{
        [self.buf_left.as_mut_slice(), self.buf_right.as_mut_slice()]
    }
}

pub struct DsdPlayer {
    playback_handle: *mut alsa::snd_pcm_t,
    hw_params: *mut alsa::snd_pcm_hw_params_t,
    buffers: Buffers,
}



impl DsdPlayer{
    pub fn new(filename: &str) -> Self{
        let mut reader = DSFReader::new(filename).unwrap();
        let mut format = DSDFormat {
            sampling_rate: 0,
            num_channels: 0,
            total_samples: 0,
            is_lsb_first: false,
        };
        reader.open(&mut format).unwrap();

        println!("DSD Format:");
        println!("  sampling rate: {}", format.sampling_rate);
        println!("  num channels:  {}", format.num_channels);
        println!(
            "  lsb first?:    {}",
            if format.is_lsb_first { "yes" } else { "no" }
        );
        println!("  total samples: {}", format.total_samples);

        if format.num_channels != 2 {
            println!("not stereo");
            std::process::exit(1);
        }

        let mut alsa_buffer_size = 8192 * 4usize;
        let blocksize = alsa_buffer_size / format.num_channels as usize;

        // per-channel buffers
        let mut buf_left = vec![0u8; blocksize];
        let mut buf_right = vec![0u8; blocksize];
        let mut buf_slices: [&mut [u8]; 2] = [buf_left.as_mut_slice(), buf_right.as_mut_slice()];

        unsafe {
            let mut err: i32 = 0;
            let mut playback_handle: *mut alsa::snd_pcm_t = ptr::null_mut();
            let mut hw_params: *mut alsa::snd_pcm_hw_params_t = ptr::null_mut();

            // prepare pcm buffer (interleaved)
            let mut pcm = vec![0u8; alsa_buffer_size];

            // work buffers halves similar to C++ (not strictly necessary because we allocated buf slices)
            let mut work0 = vec![0u8; alsa_buffer_size >> 1];
            let mut work1 = vec![0u8; alsa_buffer_size >> 1];
            let mut work_slices: [&mut [u8]; 2] = [work0.as_mut_slice(), work1.as_mut_slice()];

            // open device (hw:2,0 as in original) - you may want to change the device string
            let device = std::ffi::CString::new("hw:2,0").unwrap();
            err = alsa::snd_pcm_open(
                &mut playback_handle,
                device.as_ptr(),
                alsa::SND_PCM_STREAM_PLAYBACK,
                0,
            );
            if err < 0 {
                eprintln!("cannot open audio device: {}", err);
                return Ok(());
            }

            if (alsa::snd_pcm_hw_params_malloc)(&mut hw_params) < 0 {
                eprintln!("cannot allocate hardware parameter structure");
                if !playback_handle.is_null() {
                    alsa::snd_pcm_close(playback_handle);
                }
                return Ok(());
            }
            if alsa::snd_pcm_hw_params_any(playback_handle, hw_params) < 0 {
                eprintln!("cannot initialize hardware parameter structure");
                goto_close(playback_handle, hw_params);
                return Ok(());
            }
            if alsa::snd_pcm_hw_params_set_access(
                playback_handle,
                hw_params,
                alsa::SND_PCM_ACCESS_RW_INTERLEAVED,
            ) < 0
            {
                eprintln!("cannot set access type");
                goto_close(playback_handle, hw_params);
                return Ok(());
            }

            // set rate to sampling_rate / 8 / 4 as in original (because in DSD->DSD_U32_BE conversion)

            let mut index = 0;

            // main loop: read, convert, write

            reader.seek_percent(0.1).expect("failed to seek");
        }
        Self{}
    }

    fn setup_params(&mut self){
        unsafe{
            if alsa::snd_pcm_hw_params_malloc(&mut self.hw_params) < 0 {
                panic!("cannot allocate hardware parameter structure");
            }
            if alsa::snd_pcm_hw_params_any(self.playback_handle.clone(), self.hw_params.clone()) < 0 {
                panic!("cannot initialize hardware parameter structure");
            }
            if alsa::snd_pcm_hw_params_set_access(
                self.playback_handle.clone(),
                self.hw_params.clone(),
                alsa::SND_PCM_ACCESS_RW_INTERLEAVED,
            ) < 0
            {
                panic!("cannot set access type");
            }
        }
    }

    fn update_hw_params(&mut self, format: &DSDFormat, alsa_buffer_size: usize){
        unsafe{
            let rate = (format.sampling_rate / 8 / 4);
            if alsa::snd_pcm_hw_params_set_rate(self.playback_handle.clone(), self.hw_params.clone(), rate, 0) < 0 {
                panic!("cannot set sample rate");
            }
            if alsa::snd_pcm_hw_params_set_channels(
                self.playback_handle.clone(),
                self.hw_params.clone(),
                format.num_channels as u32,
            ) < 0
            {
                panic!("cannot set channel count");
            }
            // set DSD format constant
            if alsa::snd_pcm_hw_params_set_format(
                self.playback_handle.clone(),
                self.hw_params.clone(),
                alsa::SND_PCM_FORMAT_DSD_U32_BE,
            ) < 0
            {
                panic!("cannot set sample format");
            }

            let mut frames: alsa::snd_pcm_uframes_t =
                (alsa_buffer_size / format.num_channels as usize / 4) as alsa::snd_pcm_uframes_t;
            let mut dir: i32 = 0;
            alsa::snd_pcm_hw_params_set_period_size_near(
                self.playback_handle.clone(),
                self.hw_params.clone(),
                &mut frames,
                &mut dir,
            );
            // set hw params
            if alsa::snd_pcm_hw_params(self.playback_handle.clone(), self.hw_params) < 0 {
                panic!("cannot set parameters");
            }
            if alsa::snd_pcm_prepare(self.playback_handle.clone()) < 0 {
                panic!("cannot prepare audio interface for use");
            }
        }
    }
}