#include "WavWriter.h"
#include <cmath>
#include <algorithm>
#include <android/log.h>

#define TAG "WavWriter"

WavWriter::WavWriter() {
    // Capacity for ~10 seconds of stereo at 48kHz (1024 * 1024 floats)
    mRingBuffer = std::make_unique<RingBuffer>(1048576);
}

WavWriter::~WavWriter() {
    stopRecording();
}

bool WavWriter::startRecording(const std::string& filePath, int sampleRate, int channels) {
    if (mIsRecording.load(std::memory_order_acquire)) return false;
    
    mFilePath = filePath;
    mSampleRate = sampleRate;
    mChannels = channels;
    mStopRequested.store(false, std::memory_order_release);
    mIsRecording.store(true, std::memory_order_release);
    
    mThread = std::thread(&WavWriter::workerThread, this);
    __android_log_print(ANDROID_LOG_INFO, TAG, "Started recording to %s", filePath.c_str());
    return true;
}

void WavWriter::stopRecording() {
    if (!mIsRecording.load(std::memory_order_acquire)) return;
    
    mStopRequested.store(true, std::memory_order_release);
    if (mThread.joinable()) {
        mThread.join();
    }
    mIsRecording.store(false, std::memory_order_release);
    __android_log_print(ANDROID_LOG_INFO, TAG, "Stopped recording");
}

void WavWriter::pushAudio(const float* data, size_t numFrames) {
    if (mIsRecording.load(std::memory_order_acquire) && !mStopRequested.load(std::memory_order_acquire)) {
        mRingBuffer->write(data, numFrames * mChannels);
    }
}

void WavWriter::writeWavHeader(std::ofstream& file, uint32_t numFrames, int sampleRate, int channels) {
    uint32_t dataSize = numFrames * channels * sizeof(int16_t);
    uint32_t fileSize = 36 + dataSize;
    
    file.seekp(0, std::ios::beg);
    file.write("RIFF", 4);
    file.write(reinterpret_cast<const char*>(&fileSize), 4);
    file.write("WAVE", 4);
    file.write("fmt ", 4);
    
    uint32_t subchunk1Size = 16;
    file.write(reinterpret_cast<const char*>(&subchunk1Size), 4);
    
    uint16_t audioFormat = 1; // PCM
    file.write(reinterpret_cast<const char*>(&audioFormat), 2);
    file.write(reinterpret_cast<const char*>(&channels), 2);
    file.write(reinterpret_cast<const char*>(&sampleRate), 4);
    
    uint32_t byteRate = sampleRate * channels * sizeof(int16_t);
    file.write(reinterpret_cast<const char*>(&byteRate), 4);
    
    uint16_t blockAlign = channels * sizeof(int16_t);
    file.write(reinterpret_cast<const char*>(&blockAlign), 2);
    
    uint16_t bitsPerSample = 16;
    file.write(reinterpret_cast<const char*>(&bitsPerSample), 2);
    
    file.write("data", 4);
    file.write(reinterpret_cast<const char*>(&dataSize), 4);
}

void WavWriter::workerThread() {
    std::ofstream file(mFilePath, std::ios::binary);
    if (!file.is_open()) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "Failed to open file for recording!");
        return;
    }
    
    // Write placeholder header
    writeWavHeader(file, 0, mSampleRate, mChannels);
    
    std::vector<float> readBuf(8192);
    std::vector<int16_t> writeBuf(8192);
    uint32_t framesRecorded = 0;
    
    while (!mStopRequested.load(std::memory_order_acquire) || mRingBuffer->availableRead() > 0) {
        size_t available = mRingBuffer->availableRead();
        if (available > 0) {
            size_t toRead = std::min(available, readBuf.size());
            size_t readCount = mRingBuffer->read(readBuf.data(), toRead);
            
            for (size_t i = 0; i < readCount; ++i) {
                float sample = readBuf[i];
                if (sample > 1.0f) sample = 1.0f;
                else if (sample < -1.0f) sample = -1.0f;
                writeBuf[i] = static_cast<int16_t>(sample * 32767.0f);
            }
            
            file.write(reinterpret_cast<const char*>(writeBuf.data()), readCount * sizeof(int16_t));
            framesRecorded += (readCount / mChannels);
        } else {
            std::this_thread::sleep_for(std::chrono::milliseconds(5));
        }
    }
    
    // Rewrite header with actual size
    writeWavHeader(file, framesRecorded, mSampleRate, mChannels);
    file.close();
}
