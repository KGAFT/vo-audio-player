
#include "asio.h"
#include <asiodrivers.h>
#include <windows.h>
#include <stdio.h>

extern AsioDrivers* asioDrivers;
bool loadAsioDriver(char *name);

// Define constants
#define TEST_RUN_TIME 10.0  // Duration in seconds
#define DSD_SAMPLE_RATE 2822400.0  // DSD64 (2.8224 MHz)

// Global structures
struct DriverInfo {
    ASIOBool stopped;
    long inputBuffers;
    long outputBuffers;
    ASIOBufferInfo bufferInfos[32];  // Max channels
    ASIOChannelInfo channelInfos[32];
    long preferredSize;
    ASIOSampleRate sampleRate;
    ASIOCallbacks callbacks;
    ASIOBool postOutput;
} asioDriverInfo;

long processedSamples = 0;

// Callback functions
ASIOTime* bufferSwitchTimeInfo(ASIOTime* timeInfo, long index, ASIOBool processNow) {
    long buffSize = asioDriverInfo.preferredSize;

    // Process output buffers only
    for (int i = 0; i < asioDriverInfo.inputBuffers + asioDriverInfo.outputBuffers; i++) {
        if (asioDriverInfo.bufferInfos[i].isInput == ASIOFalse) {
            switch (asioDriverInfo.channelInfos[i].type) {
                case ASIOSTDSDInt8LSB1:
                case ASIOSTDSDInt8MSB1:
                case ASIOSTDSDInt8NER8:
                    // Fill with silence (zeros); replace with real DSD data copy
                    memset(asioDriverInfo.bufferInfos[i].buffers[index], 0, buffSize);
                    break;
                // Add other formats if needed (e.g., PCM for fallback)
                default:
                    break;
            }
        }
    }

    // Signal output ready if driver supports it
    if (asioDriverInfo.postOutput) {
        ASIOOutputReady();
    }

    // Update processed samples
    processedSamples += buffSize;

    // Stop after test duration
    double smpRate = 0;
    memcpy(&smpRate, &asioDriverInfo.sampleRate, sizeof(ASIOSampleRate));
    if (processedSamples >= (long)(smpRate * TEST_RUN_TIME)) {
        asioDriverInfo.stopped = true;
    }

    return 0L;
}

void bufferSwitch(long index, ASIOBool processNow) {
    // Delegate to time-info version (simple case)
    ASIOTime timeInfo = {0};
    bufferSwitchTimeInfo(&timeInfo, index, processNow);
}

// Other callbacks (stubbed)
void sampleRateChanged(ASIOSampleRate sRate) {}
long asioMessages(long selector, long value, void* message, double* opt) { return 0; }

// Init function
long init_asio_static_data(DriverInfo* asioDriverInfo) {
    // Switch to DSD format
    ASIOIoFormat dsdFormat = { kASIODSDFormat };
    if (ASIOFuture(kAsioSetIoFormat, &dsdFormat) != ASE_SUCCESS) {
        printf("Failed to set DSD format\n");
        return -1;
    }

    // Get channels
    long maxInputChannels = 0, maxOutputChannels = 0;
    if (ASIOGetChannels(&maxInputChannels, &maxOutputChannels) != ASE_OK) {
        return -2;
    }
    asioDriverInfo->inputBuffers = 0;  // No input for playback
    asioDriverInfo->outputBuffers = 2;  // Stereo output

    // Get buffer size
    long minSize, maxSize, preferredSize, granularity;
    if (ASIOGetBufferSize(&minSize, &maxSize, &preferredSize, &granularity) != ASE_OK) {
        return -3;
    }
    asioDriverInfo->preferredSize = preferredSize;
    double srcSmp = DSD_SAMPLE_RATE;
    ASIOSampleRate sampleRate = ASIOSampleRate{};
    memcpy(&sampleRate, &srcSmp, sizeof(ASIOSampleRate));
    // Check and set sample rate
    if (ASIOCanSampleRate(sampleRate) != ASE_OK) {
        printf("Driver does not support DSD sample rate\n");
        return -4;
    }
    if (ASIOSetSampleRate(sampleRate) != ASE_OK) {
        printf("Failed to set sample rate\n");
        return -5;
    }
    if (ASIOGetSampleRate(&asioDriverInfo->sampleRate) != ASE_OK) {
        return -6;
    }
    printf("Sample rate set to: %f\n", asioDriverInfo->sampleRate);

    return 0;
}

// Main function
int main(int argc, char* argv[]) {
    if (argc < 2) {
        printf("Usage: %s <ASIO_driver_name>\n", argv[0]);
        return -1;
    }
    if (!loadAsioDriver(argv[1])) {
        printf("Failed to load ASIO driver\n");
    }
    // Load driver
    /*
    AsioDrivers drivers;
    if (!drivers.loadDriver(argv[1])) {
        printf("Failed to load driver: %s\n", argv[1]);
        return -1;
    }
    */

    // Init ASIO
    ASIODriverInfo driverInfo = {0};
    if (ASIOInit(&driverInfo) != ASE_OK) {
        printf("ASIOInit error\n");
        return -1;
    }

    // Init static data
    memset(&asioDriverInfo, 0, sizeof(asioDriverInfo));
    if (init_asio_static_data(&asioDriverInfo) < 0) {
        ASIOExit();
        return -1;
    }

    // Setup callbacks
    asioDriverInfo.callbacks.bufferSwitch = &bufferSwitch;
    asioDriverInfo.callbacks.bufferSwitchTimeInfo = &bufferSwitchTimeInfo;
    asioDriverInfo.callbacks.sampleRateDidChange = &sampleRateChanged;
    asioDriverInfo.callbacks.asioMessage = &asioMessages;

    // Create buffers
    int i;
    for (i = 0; i < asioDriverInfo.outputBuffers; i++) {
        asioDriverInfo.bufferInfos[i].isInput = ASIOFalse;
        asioDriverInfo.bufferInfos[i].channelNum = i;
        asioDriverInfo.bufferInfos[i].buffers[0] = asioDriverInfo.bufferInfos[i].buffers[1] = 0;
    }
    if (ASIOCreateBuffers(asioDriverInfo.bufferInfos, asioDriverInfo.outputBuffers, asioDriverInfo.preferredSize, &asioDriverInfo.callbacks) != ASE_OK) {
        printf("ASIOCreateBuffers error\n");
        ASIOExit();
        return -1;
    }

    // Get channel info
    for (i = 0; i < asioDriverInfo.outputBuffers; i++) {
        asioDriverInfo.channelInfos[i].channel = asioDriverInfo.bufferInfos[i].channelNum;
        asioDriverInfo.channelInfos[i].isInput = ASIOFalse;
        ASIOGetChannelInfo(&asioDriverInfo.channelInfos[i]);
    }

    // Check postOutput support
    unsigned long a = 0;
    ASIOControlPanel();  // Optional: Open driver control panel
   // ASIOFuture(kAsioCanOutputMeter, &a);
 //   asioDriverInfo.postOutput = (a != 0);

    // Start playback
    if (ASIOStart() != ASE_OK) {
        printf("ASIOStart error\n");
        ASIODisposeBuffers();
        ASIOExit();
        return -1;
    }

    printf("Playback started. Press Enter to stop...\n");
    getchar();  // Wait for user input or use timer

    // Stop and cleanup
    ASIOStop();
    ASIODisposeBuffers();
    ASIOExit();
    //drivers.removeCurrentDriver();

    printf("Playback stopped.\n");
    return 0;
}