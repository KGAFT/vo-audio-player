use std::fs::File;
use std::io;
use std::io::{Read, Seek, SeekFrom};
use byteorder::{BigEndian, LittleEndian, ReadBytesExt};

#[derive(Debug, Copy, Clone, Eq, PartialEq)]
pub struct DSDFormat {
    pub sampling_rate: u32,
    pub num_channels: u32,
    pub total_samples: u64,
    pub is_lsb_first: bool,
}

impl DSDFormat {
    pub fn is_alsa_update_need(&self, other: &Self) -> bool {
        return self.sampling_rate != other.sampling_rate || self.num_channels != other.num_channels;
    }
}

pub fn open_dsd_auto(path: &str, format: &mut DSDFormat) -> io::Result<Box<dyn DSDReader>> {
    let mut file = File::open(path)?;

    let mut ident = [0u8; 4];
    file.read_exact(&mut ident)?;
    file.seek(SeekFrom::Start(0))?; // rewind for the reader itself

    match &ident {
        b"DSD " => {
            // DSF file
            let mut reader = DSFReader::new(path)?;
            reader.open(format)?;
            Ok(Box::new(reader))
        }
        b"FRM8" => {
            // DFF file
            let mut reader = DFFReader::new(path)?;
            reader.set_planar(false);
            reader.open(format)?;
            Ok(Box::new(reader))
        }
        _ => Err(io::Error::new(io::ErrorKind::InvalidData, "unknown DSD format")),
    }
}

pub trait DSDReader {
    fn open(&mut self, format: &mut DSDFormat) -> io::Result<()>;
    fn read(&mut self, data: &mut [&mut [u8]], bytes_per_channel: usize) -> io::Result<usize>;
    fn seek_percent(&mut self, percent: f64) -> io::Result<()>;
    fn seek_samples(&mut self, sample_index: u64) -> io::Result<()>;
    fn get_position_frames(&self) -> u64;
    fn get_position_percent(&self) -> f64;
}

pub struct DSFReader {
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
            return Err(io::Error::new(
                io::ErrorKind::InvalidData,
                "fmt chunk missing",
            ));
        }
        let fmt_size = self.file.read_u64::<LittleEndian>()?;
        let format_version = self.file.read_u32::<LittleEndian>()?;
        if format_version != 1 {
            return Err(io::Error::new(
                io::ErrorKind::InvalidData,
                "unsupported format version",
            ));
        }
        let format_id = self.file.read_u32::<LittleEndian>()?;
        if format_id != 0 {
            return Err(io::Error::new(
                io::ErrorKind::InvalidData,
                "unsupported format id",
            ));
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
            return Err(io::Error::new(
                io::ErrorKind::InvalidData,
                "data chunk missing",
            ));
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

        self.read_samples = self.read_samples.saturating_add((read_bytes as u64) * 8);
        Ok(read_bytes)
    }

    fn seek_percent(&mut self, percent: f64) -> io::Result<()> {
        if percent < 0.0 || percent > 1.0 {
            return Err(io::Error::new(
                io::ErrorKind::InvalidInput,
                "percent out of range",
            ));
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

    fn get_position_frames(&self) -> u64 {
        // self.read_samples counts *bits*, so divide by channels and by 1 bit/sample
        // That gives total DSD frames read so far
        if self.ch == 0 {
            return 0;
        }
        self.read_samples / (self.ch as u64)
    }

    fn get_position_percent(&self) -> f64 {
        if self.total_samples == 0 {
            return 0.0;
        }
        let frames = self.get_position_frames();
        (frames as f64 / self.total_samples as f64).min(1.0)
    }
}


pub struct DFFReader {
    file: File,
    buf: Vec<u8>,
    ch: usize,
    blocksize: usize,    // bytes per channel block we read at once
    filled: usize,
    pos: usize,
    total_samples: u64,  // frames per channel
    read_samples: u64,   // bits read in total (like DSFReader)
    data_start: u64,
    data_size: u64,
    planar: bool,        // true => per-channel blocks (channel0 block, channel1 block, ...), false => interleaved
    is_msb_first: bool,  // DFF often MSB-first
}

impl DFFReader {
    pub fn new(path: &str) -> io::Result<Self> {
        let file = File::open(path)?;
        Ok(Self {
            file,
            buf: Vec::new(),
            ch: 0,
            blocksize: 4096, // starting block size; we will use it as read chunk per channel
            filled: 0,
            pos: 0,
            total_samples: 0,
            read_samples: 0,
            data_start: 0,
            data_size: 0,
            planar: true,
            is_msb_first: true,
        })
    }

    /// Switch layout if your file is interleaved
    pub fn set_planar(&mut self, planar: bool) { self.planar = planar; }

    /// Switch bit order manually if autodetection suggests different
    pub fn set_msb_first(&mut self, is_msb_first: bool) { self.is_msb_first = is_msb_first; }
}

impl DSDReader for DFFReader {
    fn open(&mut self, format: &mut DSDFormat) -> io::Result<()> {
        let mut ident = [0u8; 4];

        // FRM8 header
        self.file.read_exact(&mut ident)?;
        if &ident != b"FRM8" {
            return Err(io::Error::new(io::ErrorKind::InvalidData, "not DFF (no FRM8)"));
        }
        // FRM8 size (big endian 64-bit)
        let _frm8_size = self.file.read_u64::<BigEndian>()?;
        // form type should be "DSD "
        self.file.read_exact(&mut ident)?;
        if &ident != b"DSD " {
            return Err(io::Error::new(io::ErrorKind::InvalidData, "not DFF (form != 'DSD ')"));
        }

        // iterate chunks until we find PROP and DSD
        let mut sampling_rate: Option<u32> = None;
        let mut channels: Option<u32> = None;
        let mut data_chunk_size: u64 = 0;
        let mut data_offset: u64 = 0;

        loop {
            // read chunk id (4) and chunk size (be64)
            if let Err(_) = self.file.read_exact(&mut ident) {
                break;
            }
            let chunk_size = match self.file.read_u64::<BigEndian>() {
                Ok(sz) => sz,
                Err(_) => break,
            };

            match &ident {
                b"FVER" => {
                    // version chunk — skip
                    self.file.seek(SeekFrom::Current(chunk_size as i64))?;
                }
                b"PROP" => {
                    // PROP chunk contains SND (sound properties)
                    let prop_start = self.file.seek(SeekFrom::Current(0))?;
                    // next should be "SND "
                    self.file.read_exact(&mut ident)?;
                    if &ident != b"SND " {
                        return Err(io::Error::new(io::ErrorKind::InvalidData, "PROP missing SND"));
                    }
                    // SND chunk size
                    let snd_size = self.file.read_u64::<BigEndian>()?;
                    let prop_end = prop_start + chunk_size;

                    // read sub-chunks inside SND
                    while self.file.seek(SeekFrom::Current(0))? < prop_end {
                        // sub-chunk id and size
                        self.file.read_exact(&mut ident)?;
                        let sub_size = self.file.read_u64::<BigEndian>()?;
                        match &ident {
                            b"FS  " => {
                                // sampling frequency 32-bit big-endian
                                let fs = self.file.read_u32::<BigEndian>()?;
                                sampling_rate = Some(fs);
                            }
                            b"CHNL" => {
                                // channel count is stored as two bytes after an ID string (per spec may vary)
                                // We'll read a u16 (big-endian)
                                // Some DFF variants store more info; here we just read channel count
                                let ch_count = self.file.read_u16::<BigEndian>()?;
                                channels = Some(ch_count as u32);
                                // skip the rest of the sub-chunk if any
                                let skip = sub_size as i64 - 2;
                                if skip > 0 {
                                    self.file.seek(SeekFrom::Current(skip))?;
                                }
                            }
                            b"CMPR" => {
                                // compression id: 4 bytes text (e.g. "DSD " for raw)
                                let mut comp = [0u8; 4];
                                self.file.read_exact(&mut comp)?;
                                if &comp != b"DSD " {
                                    return Err(io::Error::new(io::ErrorKind::InvalidData, "compressed DFF not supported"));
                                }
                                let skip = sub_size as i64 - 4;
                                if skip > 0 {
                                    self.file.seek(SeekFrom::Current(skip))?;
                                }
                            }
                            _ => {
                                // unknown sub-chunk: skip
                                self.file.seek(SeekFrom::Current(sub_size as i64))?;
                            }
                        }
                    }
                    // ensure file cursor at end of PROP
                    self.file.seek(SeekFrom::Start(prop_end))?;
                }
                b"DSD " => {
                    // data chunk: remember offset and size
                    let data_pos = self.file.seek(SeekFrom::Current(0))?;
                    data_offset = data_pos;
                    data_chunk_size = chunk_size;
                    // move file cursor past data chunk (leave positioned at start of data)
                    // do NOT seek past it — we want to start reading from here later
                    break;
                }
                _ => {
                    // skip unknown chunk
                    self.file.seek(SeekFrom::Current(chunk_size as i64))?;
                }
            }
        }

        if data_offset == 0 {
            return Err(io::Error::new(io::ErrorKind::InvalidData, "DFF missing DSD chunk"));
        }

        // fill format
        if let Some(fs) = sampling_rate { format.sampling_rate = fs; } else { format.sampling_rate = 2822400; }
        if let Some(ch) = channels { format.num_channels = ch; } else { format.num_channels = 2; }

        // DFF typically uses MSB-first bit order; keep is_lsb_first false
        format.is_lsb_first = false;
        self.is_msb_first = true;

        // compute total samples (frames per channel)
        // DSD chunk contains raw bytes (all channels combined). total bits = data_chunk_size * 8
        // frames per channel = total_bits / num_channels
        let total_bits = data_chunk_size.saturating_mul(8);
        self.total_samples = if format.num_channels > 0 {
            total_bits / (format.num_channels as u64)
        } else {
            0
        };
        format.total_samples = self.total_samples;

        // store data info
        self.data_start = data_offset;
        self.data_size = data_chunk_size;

        // choose a blocksize: use 4096 bytes per channel as default; you may change
        self.blocksize = 4096;
        // allocate read buffer. If planar: we'll read blocksize * channels
        self.buf.resize(self.blocksize * (self.ch.max(1)), 0);

        // set internal channel count
        self.ch = format.num_channels as usize;

        // position file at start of data
        self.file.seek(SeekFrom::Start(self.data_start))?;
        self.pos = 0;
        self.filled = 0;
        self.read_samples = 0;

        Ok(())
    }

    fn read(&mut self, data: &mut [&mut [u8]], bytes_per_channel: usize) -> io::Result<usize> {
        // The read behaviour depends on `planar`:
        // - planar=true: we read channel0 block then channel1 block ... (per-channel blocks)
        //   To be compatible with your DSFReader semantics, we will fill internal buf with blocksize*ch
        //   and then copy from buf[channel_block + pos .. +size] into per-channel dest buffers.
        // - planar=false: we treat bytes as sample-interleaved: [ch0, ch1, ch0, ch1, ...]
        //
        // This gives you a switch to try both layouts.

        let mut read_bytes = 0usize;
        let mut want = bytes_per_channel;

        if self.planar {
            // we mimic DSFReader behaviour: read channel-blocks into buf then split
            while want > 0 {
                if self.pos == self.filled {
                    // read up to blocksize * channels bytes
                    let to_read = self.blocksize * self.ch;
                    self.buf.resize(to_read, 0);
                    let n = self.file.read(&mut self.buf)?;
                    if n == 0 {
                        return Ok(read_bytes);
                    }
                    // filled means bytes per channel available
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
        } else {
            // interleaved mode: we need to read  (bytes_per_channel * channels) bytes and split them
            // We will read into a temp buffer of size want * ch (or blocksize * ch)
            // To avoid realloc every call, reuse self.buf
            while want > 0 {
                let to_read_total = (want * self.ch).min(self.blocksize * self.ch);
                self.buf.resize(to_read_total, 0);
                let n = self.file.read(&mut self.buf)?;
                if n == 0 {
                    return Ok(read_bytes);
                }
                // n is total bytes read (interleaved), so we have n/ch bytes per channel (maybe truncated)
                let bytes_per_ch_available = n / self.ch;
                for i in 0..self.ch {
                    let mut dst_off = read_bytes;
                    let mut src_off = i;
                    let dst = &mut data[i];
                    // copy bytes_per_ch_available bytes per channel
                    for _ in 0..bytes_per_ch_available {
                        dst[dst_off] = self.buf[src_off];
                        dst_off += 1;
                        src_off += self.ch; // move to next sample for this channel
                    }
                }
                read_bytes += bytes_per_ch_available;
                // continue until want satisfied
                want = want.saturating_sub(bytes_per_ch_available);
            }
        }

        // update read_samples: we count bits (bytes_read * 8)
        self.read_samples = self.read_samples.saturating_add((read_bytes as u64) * 8);
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
        // compute byte position in DSD chunk
        // total bits = sample_index * channels (1 bit per channel per sample)
        let total_bits = sample_index.saturating_mul(self.ch as u64);
        let total_bytes = total_bits / 8;
        // align to block boundary if planar, or to 1 byte if interleaved
        let aligned_bytes = if self.planar {
            // align to blocksize*ch boundary (same logic as DSF)
            ((total_bytes) / ((self.blocksize * self.ch) as u64)) * ((self.blocksize * self.ch) as u64)
        } else {
            // interleaved: align to any byte
            total_bytes
        };
        let offset = self.data_start + aligned_bytes;
        self.file.seek(SeekFrom::Start(offset))?;
        self.read_samples = aligned_bytes * 8;
        self.pos = 0;
        self.filled = 0;
        Ok(())
    }

    fn get_position_frames(&self) -> u64 {
        if self.ch == 0 { return 0; }
        self.read_samples / (self.ch as u64)
    }

    fn get_position_percent(&self) -> f64 {
        if self.total_samples == 0 { return 0.0; }
        (self.get_position_frames() as f64 / self.total_samples as f64).min(1.0)
    }
}