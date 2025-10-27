//
// Created by kgaft on 21.10.2025.
//
// dsd_asio_player.cpp
#include "asio.h"
#include <asiodrivers.h>
#include <windows.h>
#include <stdio.h>
#include <fstream>
#include <vector>
#include <string>
#include <mutex>
#include <atomic>
#include <algorithm> // for std::reverse
#include <cstdint>

class DsdPlayer;

void dsd_player_stop(DsdPlayer* player) ;

extern AsioDrivers* asioDrivers;
bool loadAsioDriver(char *name);

const uint8_t BIT_REVERSE_TABLE[256] = {
    0x00, 0x80, 0x40, 0xc0, 0x20, 0xa0, 0x60, 0xe0, 0x10, 0x90, 0x50, 0xd0, 0x30, 0xb0, 0x70, 0xf0,
    0x08, 0x88, 0x48, 0xc8, 0x28, 0xa8, 0x68, 0xe8, 0x18, 0x98, 0x58, 0xd8, 0x38, 0xb8, 0x78, 0xf8,
    0x04, 0x84, 0x44, 0xc4, 0x24, 0xa4, 0x64, 0xe4, 0x14, 0x94, 0x54, 0xd4, 0x34, 0xb4, 0x74, 0xf4,
    0x0c, 0x8c, 0x4c, 0xcc, 0x2c, 0xac, 0x6c, 0xec, 0x1c, 0x9c, 0x5c, 0xdc, 0x3c, 0xbc, 0x7c, 0xfc,
    0x02, 0x82, 0x42, 0xc2, 0x22, 0xa2, 0x62, 0xe2, 0x12, 0x92, 0x52, 0xd2, 0x32, 0xb2, 0x72, 0xf2,
    0x0a, 0x8a, 0x4a, 0xca, 0x2a, 0xaa, 0x6a, 0xea, 0x1a, 0x9a, 0x5a, 0xda, 0x3a, 0xba, 0x7a, 0xfa,
    0x06, 0x86, 0x46, 0xc6, 0x26, 0xa6, 0x66, 0xe6, 0x16, 0x96, 0x56, 0xd6, 0x36, 0xb6, 0x76, 0xf6,
    0x0e, 0x8e, 0x4e, 0xce, 0x2e, 0xae, 0x6e, 0xee, 0x1e, 0x9e, 0x5e, 0xde, 0x3e, 0xbe, 0x7e, 0xfe,
    0x01, 0x81, 0x41, 0xc1, 0x21, 0xa1, 0x61, 0xe1, 0x11, 0x91, 0x51, 0xd1, 0x31, 0xb1, 0x71, 0xf1,
    0x09, 0x89, 0x49, 0xc9, 0x29, 0xa9, 0x69, 0xe9, 0x19, 0x99, 0x59, 0xd9, 0x39, 0xb9, 0x79, 0xf9,
    0x05, 0x85, 0x45, 0xc5, 0x25, 0xa5, 0x65, 0xe5, 0x15, 0x95, 0x55, 0xd5, 0x35, 0xb5, 0x75, 0xf5,
    0x0d, 0x8d, 0x4d, 0xcd, 0x2d, 0xad, 0x6d, 0xed, 0x1d, 0x9d, 0x5d, 0xdd, 0x3d, 0xbd, 0x7d, 0xfd,
    0x03, 0x83, 0x43, 0xc3, 0x23, 0xa3, 0x63, 0xe3, 0x13, 0x93, 0x53, 0xd3, 0x33, 0xb3, 0x73, 0xf3,
    0x0b, 0x8b, 0x4b, 0xcb, 0x2b, 0xab, 0x6b, 0xeb, 0x1b, 0x9b, 0x5b, 0xdb, 0x3b, 0xbb, 0x7b, 0xfb,
    0x07, 0x87, 0x47, 0xc7, 0x27, 0xa7, 0x67, 0xe7, 0x17, 0x97, 0x57, 0xd7, 0x37, 0xb7, 0x77, 0xf7,
    0x0f, 0x8f, 0x4f, 0xcf, 0x2f, 0xaf, 0x6f, 0xef, 0x1f, 0x9f, 0x5f, 0xdf, 0x3f, 0xbf, 0x7f, 0xff,
};

struct DSDFormat {
    uint32_t sampling_rate;
    uint32_t num_channels;
    uint64_t total_samples;
    bool is_lsb_first;
    DSDFormat() : sampling_rate(0), num_channels(0), total_samples(0), is_lsb_first(false) {}
};

class DSDReader {
public:
    virtual ~DSDReader() {}
    virtual bool open(DSDFormat* format) = 0;
    virtual size_t read(uint8_t** data, size_t bytes_per_channel) = 0;
    virtual bool seek_percent(double percent) = 0;
    virtual bool seek_samples(uint64_t sample_index) = 0;
    virtual uint64_t get_position_frames() const = 0;
    virtual double get_position_percent() const = 0;
};

class DSFReader : public DSDReader {
private:
    std::ifstream file;
    std::vector<uint8_t> buf;
    size_t ch;
    size_t blocksize;
    size_t filled;
    size_t pos;
    uint64_t total_samples;
    uint64_t read_samples;
    uint64_t data_start;

public:
    DSFReader(const std::string& path) : ch(0), blocksize(0), filled(0), pos(0), total_samples(0), read_samples(0), data_start(0) {
        file.open(path, std::ios::binary);
        if (!file) {
            throw std::runtime_error("Failed to open file");
        }
    }

    bool open(DSDFormat* format) override {
        uint8_t ident[4];
        file.read((char*)ident, 4);
        if (std::string((char*)ident, 4) != "DSD ") {
            return false;
        }
        uint64_t dsd_size = read_le_u64();
        file.seekg(dsd_size - 12, std::ios::cur);

        file.read((char*)ident, 4);
        if (std::string((char*)ident, 4) != "fmt ") {
            return false;
        }
        uint64_t fmt_size = read_le_u64();
        uint32_t format_version = read_le_u32();
        if (format_version != 1) {
            return false;
        }
        uint32_t format_id = read_le_u32();
        if (format_id != 0) {
            return false;
        }
        uint32_t channel_type = read_le_u32();
        uint32_t channels = read_le_u32();
        format->num_channels = channels;
        ch = channels;

        uint32_t sampling_freq = read_le_u32();
        format->sampling_rate = sampling_freq;
        uint32_t bits_per_sample = read_le_u32();
        format->is_lsb_first = (bits_per_sample == 1);

        uint64_t sample_count = read_le_u64();
        format->total_samples = sample_count;
        total_samples = sample_count;

        blocksize = read_le_u32();

        file.seekg(fmt_size - 48, std::ios::cur);

        file.read((char*)ident, 4);
        if (std::string((char*)ident, 4) != "data") {
            return false;
        }
        uint64_t data_size = read_le_u64();

        data_start = file.tellg();

        buf.resize(blocksize * ch);
        return true;
    }

    size_t read(uint8_t** data, size_t bytes_per_channel) override {
        size_t read_bytes = 0;
        size_t want = bytes_per_channel;

        while (want > 0) {
            if (pos == filled) {
                size_t to_read = blocksize * ch;
                buf.resize(to_read);
                file.read((char*)buf.data(), to_read);
                size_t n = file.gcount();
                if (n == 0) {
                    return read_bytes;
                }
                filled = n / ch;
                pos = 0;
            }

            size_t available = filled - pos;
            size_t size = std::min(available, want);
            for (size_t i = 0; i < ch; ++i) {
                uint8_t* src = buf.data() + blocksize * i + pos;
                uint8_t* dst = data[i] + read_bytes;
                std::copy(src, src + size, dst);
            }

            pos += size;
            want -= size;
            read_bytes += size;
        }

        read_samples += read_bytes * 8;
        return read_bytes;
    }

    bool seek_percent(double percent) override {
        if (percent < 0.0 || percent > 1.0) return false;
        uint64_t target_sample = static_cast<uint64_t>(total_samples * percent);
        return seek_samples(target_sample);
    }

    bool seek_samples(uint64_t sample_index) override {
        uint64_t total_bits = sample_index * ch;
        uint64_t total_bytes = total_bits / 8;
        uint64_t aligned_bytes = (total_bytes / (blocksize * ch)) * (blocksize * ch);

        file.seekg(data_start + aligned_bytes);
        read_samples = aligned_bytes * 8;
        pos = 0;
        filled = 0;
        return true;
    }

    uint64_t get_position_frames() const override {
        if (ch == 0) return 0;
        return read_samples / ch;
    }

    double get_position_percent() const override {
        if (total_samples == 0) return 0.0;
        uint64_t frames = get_position_frames();
        return std::min(static_cast<double>(frames) / total_samples, 1.0);
    }

private:
    uint32_t read_le_u32() {
        uint32_t val;
        file.read((char*)&val, 4);
        return val;
    }

    uint64_t read_le_u64() {
        uint64_t val;
        file.read((char*)&val, 8);
        return val;
    }
};

class DFFReader : public DSDReader {
private:
    std::ifstream file;
    std::vector<uint8_t> buf;
    size_t ch;
    size_t block_frames;
    size_t filled_frames;
    size_t pos_frames;
    uint64_t total_frames;
    uint64_t read_frames;
    uint64_t data_start;

public:
    DFFReader(const std::string& path) : ch(0), block_frames(4096), filled_frames(0), pos_frames(0), total_frames(0), read_frames(0), data_start(0) {
        file.open(path, std::ios::binary);
        if (!file) {
            throw std::runtime_error("Failed to open file");
        }
    }

    bool open(DSDFormat* format) override {
        uint8_t id[4];
        file.read((char*)id, 4);
        if (std::string((char*)id, 4) != "FRM8") {
            return false;
        }
        uint64_t frm8_size = read_be_u64();

        file.read((char*)id, 4);
        if (std::string((char*)id, 4) != "DSD ") {
            return false;
        }

        bool found_dsd = false;
        uint64_t dsd_chunk_size = 0;
        uint32_t sample_rate_hz = 0;
        uint16_t channels = 0;
        format->is_lsb_first = false;

        while (true) {
            file.read((char*)id, 4);
            if (file.eof()) break;
            uint64_t chunk_size = read_be_u64();
            std::streampos chunk_payload_start = file.tellg();

            std::string chunk_id((char*)id, 4);
            if (chunk_id == "PROP") {
                uint8_t prop_id[4];
                file.read((char*)prop_id, 4);
                if (std::string((char*)prop_id, 4) == "SND ") {
                    std::streampos prop_end = chunk_payload_start + static_cast<std::streamoff>(chunk_size);
                    while (file.tellg() < prop_end) {
                        uint8_t sub_id[4];
                        file.read((char*)sub_id, 4);
                        uint64_t sub_size = read_be_u64();
                        std::streampos sub_payload_start = file.tellg();

                        std::string sub_id_str((char*)sub_id, 4);
                        if (sub_id_str == "FS  ") {
                            if (sub_size >= 4) {
                                sample_rate_hz = read_be_u32();
                            }
                        } else if (sub_id_str == "CHNL") {
                            if (sub_size >= 2) {
                                channels = read_be_u16();
                            }
                        } else if (sub_id_str == "CMPR") {
                            if (sub_size >= 4) {
                                uint8_t cmp[4];
                                file.read((char*)cmp, 4);
                                if (std::string((char*)cmp, 4) != "DSD ") {
                                    return false;
                                }
                            }
                        }
                        uint64_t padded = (sub_size + 1) & ~1ULL;
                        file.seekg(sub_payload_start + static_cast<std::streamoff>(padded));
                    }
                } else {
                    uint64_t padded = (chunk_size + 1) & ~1ULL;
                    file.seekg(chunk_payload_start + static_cast<std::streamoff>(padded));
                }
            } else if (chunk_id == "DSD ") {
                found_dsd = true;
                dsd_chunk_size = chunk_size;
                data_start = file.tellg();
                break;
            } else {
                uint64_t padded = (chunk_size + 1) & ~1ULL;
                file.seekg(chunk_payload_start + static_cast<std::streamoff>(padded));
            }
        }

        if (!found_dsd) return false;
        if (channels == 0 || sample_rate_hz == 0) return false;

        format->num_channels = channels;
        ch = channels;
        format->sampling_rate = sample_rate_hz;

        total_frames = dsd_chunk_size / ch;
        format->total_samples = total_frames;
        total_frames = total_frames;

        buf.resize(block_frames * ch);
        return true;
    }

    size_t read(uint8_t** data, size_t bytes_per_channel) override {
        if (ch == 0) return 0;

        size_t written = 0;
        while (written < bytes_per_channel) {
            if (pos_frames == filled_frames) {
                size_t frames_to_read = std::min(bytes_per_channel - written, block_frames);
                size_t bytes_to_read = frames_to_read * ch;
                buf.resize(bytes_to_read);
                file.read((char*)buf.data(), bytes_to_read);
                size_t n = file.gcount();
                if (n == 0) return written;
                filled_frames = n / ch;
                pos_frames = 0;
            }

            size_t available_frames = filled_frames - pos_frames;
            size_t take_frames = std::min(available_frames, bytes_per_channel - written);

            for (size_t ch_idx = 0; ch_idx < ch; ++ch_idx) {
                uint8_t* dst = data[ch_idx] + written;
                size_t src_offset = pos_frames * ch + ch_idx;
                for (size_t f = 0; f < take_frames; ++f) {
                    dst[f] = buf[src_offset];
                    src_offset += ch;
                }
            }

            pos_frames += take_frames;
            written += take_frames;
            read_frames += take_frames;
        }
        return written;
    }

    bool seek_percent(double percent) override {
        if (percent < 0.0 || percent > 1.0) return false;
        uint64_t target_frame = static_cast<uint64_t>(total_frames * percent);
        return seek_samples(target_frame);
    }

    bool seek_samples(uint64_t sample_index) override {
        uint64_t byte_offset = sample_index * ch;
        file.seekg(data_start + byte_offset);
        read_frames = sample_index;
        pos_frames = 0;
        filled_frames = 0;
        return true;
    }

    uint64_t get_position_frames() const override {
        return read_frames;
    }

    double get_position_percent() const override {
        if (total_frames == 0) return 0.0;
        return std::min(static_cast<double>(get_position_frames()) / total_frames, 1.0);
    }

private:
    uint32_t read_be_u32() {
        uint32_t val;
        file.read((char*)&val, 4);
        return _byteswap_ulong(val); // Windows specific, or implement swap
    }

    uint64_t read_be_u64() {
        uint64_t val;
        file.read((char*)&val, 8);
        return _byteswap_uint64(val); // Windows
    }

    uint16_t read_be_u16() {
        uint16_t val;
        file.read((char*)&val, 2);
        return _byteswap_ushort(val);
    }
};

DSDReader* open_dsd_auto(const std::string& path, DSDFormat* format) {
    std::ifstream f(path, std::ios::binary);
    uint8_t ident[4];
    f.read((char*)ident, 4);
    f.close();

    std::string id((char*)ident, 4);
    if (id == "DSD ") {
        DSFReader* reader = new DSFReader(path);
        if (reader->open(format)) return reader;
        delete reader;
    } else if (id == "FRM8") {
        DFFReader* reader = new DFFReader(path);
        if (reader->open(format)) return reader;
        delete reader;
    }
    return nullptr;
}

// ASIO part
struct DriverInfo {
    ASIOBool stopped;
    long inputBuffers;
    long outputBuffers;
    ASIOBufferInfo bufferInfos[32];
    ASIOChannelInfo channelInfos[32];
    long preferredSize;
    ASIOSampleRate sampleRate;
    ASIOCallbacks callbacks;
    ASIOBool postOutput;
};

class DsdPlayer {
public:
    static inline DsdPlayer* instance = nullptr;

public:
    std::string driver_name;
    ASIODriverInfo asio_driver_info;
    DriverInfo driver_info;
    DSDReader* reader;
    std::mutex reader_mutex;
    std::atomic<bool> paused;
    std::atomic<bool> stopped;
    std::atomic<bool> is_playing;
    DSDFormat format;
    bool need_reverse;
    bool post_output_supported;

    DsdPlayer(const char* name) : reader(nullptr), paused(true), stopped(true), is_playing(false), need_reverse(false), post_output_supported(false) {
        driver_name = name;
        memset(&driver_info, 0, sizeof(driver_info));
    }

    ~DsdPlayer() {
        dsd_player_stop(this);
        if (reader) delete reader;
        ASIOExit();
        asioDrivers->removeCurrentDriver();
    }

    // Callback functions
    static ASIOTime* bufferSwitchTimeInfo(ASIOTime* timeInfo, long index, ASIOBool processNow) {
        DsdPlayer* self = instance;
        long buffSize = self->driver_info.preferredSize;

        std::lock_guard<std::mutex> lock(self->reader_mutex);

        uint8_t* data_buffers[32] = {nullptr};
        for (int i = 0; i < self->driver_info.outputBuffers; i++) {
            if (self->driver_info.bufferInfos[i].isInput == ASIOFalse) {
                uint8_t* buffer = (uint8_t*)self->driver_info.bufferInfos[i].buffers[index];
                data_buffers[i] = buffer;
                if (self->paused.load() || self->stopped.load() || !self->reader) {
                    memset(buffer, 0, buffSize);
                }
            }
        }

        if (!self->paused.load() && !self->stopped.load() && self->reader) {
            size_t bytes_read = self->reader->read(data_buffers, buffSize);
            if (bytes_read < buffSize) {
                for (int i = 0; i < self->driver_info.outputBuffers; i++) {
                    memset(data_buffers[i] + bytes_read, 0, buffSize - bytes_read);
                }
                if (bytes_read == 0) {
                    self->stopped.store(true);
                    self->is_playing.store(false);
                }
            }

            if (self->need_reverse) {
                for (int i = 0; i < self->driver_info.outputBuffers; i++) {
                    for (size_t j = 0; j < buffSize; j++) {
                        data_buffers[i][j] = BIT_REVERSE_TABLE[data_buffers[i][j]];
                    }
                }
            }
        }

        if (self->post_output_supported) {
            ASIOOutputReady();
        }

        return nullptr;
    }

    static void bufferSwitch(long index, ASIOBool processNow) {
        ASIOTime timeInfo = {0};
        bufferSwitchTimeInfo(&timeInfo, index, processNow);
    }

    static void sampleRateChanged(ASIOSampleRate sRate) {}
    static long asioMessages(long selector, long value, void* message, double* opt) { return 0; }

    bool init_asio() {
        if (!loadAsioDriver((char*)driver_name.c_str())) {
            return false;
        }

        if (ASIOInit(&asio_driver_info) != ASE_OK) {
            return false;
        }

        // Get channels
        long maxInputChannels = 0, maxOutputChannels = 0;
        if (ASIOGetChannels(&maxInputChannels, &maxOutputChannels) != ASE_OK) {
            return false;
        }
        driver_info.inputBuffers = 0;
        driver_info.outputBuffers = format.num_channels;

        // Get buffer size
        long minSize, maxSize, preferredSize, granularity;
        if (ASIOGetBufferSize(&minSize, &maxSize, &preferredSize, &granularity) != ASE_OK) {
            return false;
        }
        driver_info.preferredSize = maxSize;

        // Switch to DSD format
        ASIOIoFormat dsdFormat = { kASIODSDFormat };
        if (ASIOFuture(kAsioCanDoIoFormat, &dsdFormat) != ASE_SUCCESS) {
            printf("Driver does not support DSD format\n");
            return false;
        }
        if (ASIOFuture(kAsioSetIoFormat, &dsdFormat) != ASE_SUCCESS) {
            printf("Failed to set DSD format\n");
            return false;
        }
        ASIOIoFormat currentFormat = {0};
        if (ASIOFuture(kAsioGetIoFormat, &currentFormat) != ASE_SUCCESS || currentFormat.FormatType != kASIODSDFormat) {
            printf("Failed to confirm DSD format\n");
            return false;
        }

        // Check and set sample rate
        ASIOSampleRate sr = format.sampling_rate;
        if (ASIOCanSampleRate(sr) != ASE_OK) {
            printf("Driver does not support DSD sample rate\n");
            return false;
        }
        if (ASIOSetSampleRate(sr) != ASE_OK) {
            printf("Failed to set sample rate\n");
            return false;
        }
        if (ASIOGetSampleRate(&driver_info.sampleRate) != ASE_OK) {
            return false;
        }
        printf("Sample rate set to: %f\n", driver_info.sampleRate);
        // Setup callbacks with user_data
        driver_info.callbacks.bufferSwitch = [](long index, ASIOBool processNow) { bufferSwitch(index, processNow); };
        driver_info.callbacks.bufferSwitchTimeInfo = [](ASIOTime* timeInfo, long index, ASIOBool processNow) { return bufferSwitchTimeInfo(timeInfo, index, processNow); };
        driver_info.callbacks.sampleRateDidChange = sampleRateChanged;
        driver_info.callbacks.asioMessage = asioMessages;

        // Create buffers
        for (int i = 0; i < driver_info.outputBuffers; i++) {
            driver_info.bufferInfos[i].isInput = ASIOFalse;
            driver_info.bufferInfos[i].channelNum = i;
            driver_info.bufferInfos[i].buffers[0] = driver_info.bufferInfos[i].buffers[1] = 0;
        }
        ASIOError error = ASIOCreateBuffers(driver_info.bufferInfos, driver_info.outputBuffers, driver_info.preferredSize, &driver_info.callbacks);
        if (error != ASE_OK) {
            printf("Failed to create ASIO buffers\n");
            return false;
        }

        // Get channel info and verify DSD type
        for (int i = 0; i < driver_info.outputBuffers; i++) {
            driver_info.channelInfos[i].channel = driver_info.bufferInfos[i].channelNum;
            driver_info.channelInfos[i].isInput = ASIOFalse;
            ASIOGetChannelInfo(&driver_info.channelInfos[i]);
            printf("Channel %d type: %ld\n", i, driver_info.channelInfos[i].type);
            if (driver_info.channelInfos[i].type < ASIOSTDSDInt8LSB1 || driver_info.channelInfos[i].type > ASIOSTDSDInt8NER8) {
                return false;
            }
        }

        // Determine if reverse needed
        long type = driver_info.channelInfos[0].type;
        if (format.is_lsb_first) {
            need_reverse = (type != ASIOSTDSDInt8LSB1);
        } else {
            need_reverse = (type != ASIOSTDSDInt8MSB1);
        }
        if (type == ASIOSTDSDInt8NER8) {
            // Assume MSB, reverse if lsb
            need_reverse = format.is_lsb_first;
        }

        // Test postOutput
        post_output_supported = (ASIOOutputReady() == ASE_OK);

        return true;
    }
};

void dsd_player_stop(DsdPlayer* player) {
    player->stopped.store(true);
    player->is_playing.store(false);
    ASIOStop();
}

DsdPlayer* dsd_player_new(const char* driver_name) {
    if (!DsdPlayer::instance) {
        DsdPlayer::instance = new DsdPlayer(driver_name);;
    }
    return DsdPlayer::instance;
}

void dsd_player_load_track(DsdPlayer* player, const char* filename) {
    dsd_player_stop(player);
    if (player->reader) {
        delete player->reader;
        player->reader = nullptr;
    }
    DSDFormat format;
    player->reader = open_dsd_auto(filename, &format);
    if (!player->reader) {
        printf("Failed to open DSD file\n");
        return;
    }
    if (format.num_channels != 2) {
        printf("Only stereo supported\n");
        delete player->reader;
        player->reader = nullptr;
        return;
    }
    player->format = format;
    player->stopped.store(false);
    player->paused.store(false);
    if (!player->init_asio()) {
        printf("Failed to init ASIO\n");
    }
}

void dsd_player_play(DsdPlayer* player) {
    if (player->reader) {
        player->paused.store(false);
        player->is_playing.store(true);
        if (ASIOStart() != ASE_OK) {
            printf("ASIOStart error\n");
        }
    }
}

void dsd_player_pause(DsdPlayer* player) {
    player->paused.store(true);
    player->is_playing.store(false);
}



void dsd_player_seek(DsdPlayer* player, double percent) {
    std::lock_guard<std::mutex> lock(player->reader_mutex);
    if (player->reader) {
        player->reader->seek_percent(percent);
    }
}

double dsd_player_get_position(DsdPlayer* player) {
    std::lock_guard<std::mutex> lock(player->reader_mutex);
    if (player->reader) {
        return player->reader->get_position_percent();
    }
    return 0.0;
}

void dsd_player_delete(DsdPlayer* player) {
    delete player;
}

int main() {
    auto player = dsd_player_new("FiiO ASIO Driver");
    dsd_player_load_track(player, "F:\\Music\\Nazareth - Hair Of The Dog 1975\\A2. Miss Misery.dsf");
    dsd_player_play(player);
    while (true);
}