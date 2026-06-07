#pragma once
#include "RingBuffer.h"
#include <string>
#include <thread>
#include <atomic>
#include <fstream>
#include <vector>
#include <memory>

class WavWriter {
public:
    WavWriter();
    ~WavWriter();
    
    bool startRecording(const std::string& filePath, int sampleRate, int channels);
    void stopRecording();
    void pushAudio(const float* data, size_t numFrames);
    
    bool isRecording() const { return mIsRecording.load(std::memory_order_acquire); }

private:
    void workerThread();
    void writeWavHeader(std::ofstream& file, uint32_t numFrames, int sampleRate, int channels);

    std::unique_ptr<RingBuffer> mRingBuffer;
    std::thread mThread;
    std::atomic<bool> mIsRecording{false};
    std::atomic<bool> mStopRequested{false};
    std::string mFilePath;
    int mSampleRate{48000};
    int mChannels{2};
};
