#include "AudioEngine.h"
#include <android/log.h>
#include <fstream>
#include "Mixer.h"

#define TAG "AudioEngine"

AudioEngine::AudioEngine() {
    oboe::AudioStreamBuilder builder;
    builder.setDirection(oboe::Direction::Input)
           ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
           ->setSharingMode(oboe::SharingMode::Exclusive)
           ->setFormat(oboe::AudioFormat::Float)
           ->setChannelCount(1)
           ->setDataCallback(this);
           
    oboe::Result result = builder.openStream(mInputStream);
    if (result == oboe::Result::OK) {
        mInputStream->requestStart();
    } else {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "Failed to create input stream");
    }
}

AudioEngine::~AudioEngine() {
    if (mInputStream) {
        mInputStream->requestStop();
        mInputStream->close();
    }
}

void AudioEngine::startRecording(const std::string& tempWavPath) {
    mTempWavPath = tempWavPath;
    mRecordingBuffer.clear();
    mIsRecording = true;
}

void AudioEngine::stopRecording() {
    mIsRecording = false;

    // Save to WAV file
    if (!mTempWavPath.empty() && mRecordingBuffer.size() > 0) {
        std::ofstream wavFile(mTempWavPath, std::ios::binary);
        if (wavFile.is_open()) {
            int byteRate = 48000 * 1 * sizeof(float);
            int dataSize = mRecordingBuffer.size() * sizeof(float);

            wavFile.write("RIFF", 4);
            int chunkSize = 36 + dataSize;
            wavFile.write(reinterpret_cast<const char*>(&chunkSize), 4);
            wavFile.write("WAVE", 4);
            wavFile.write("fmt ", 4);
            int subchunk1Size = 16;
            wavFile.write(reinterpret_cast<const char*>(&subchunk1Size), 4);
            short audioFormat = 3; // IEEE float
            wavFile.write(reinterpret_cast<const char*>(&audioFormat), 2);
            short numChannels = 1;
            wavFile.write(reinterpret_cast<const char*>(&numChannels), 2);
            int sampleRate = 48000;
            wavFile.write(reinterpret_cast<const char*>(&sampleRate), 4);
            wavFile.write(reinterpret_cast<const char*>(&byteRate), 4);
            short blockAlign = numChannels * sizeof(float);
            wavFile.write(reinterpret_cast<const char*>(&blockAlign), 2);
            short bitsPerSample = 32;
            wavFile.write(reinterpret_cast<const char*>(&bitsPerSample), 2);
            wavFile.write("data", 4);
            wavFile.write(reinterpret_cast<const char*>(&dataSize), 4);

            wavFile.write(reinterpret_cast<const char*>(mRecordingBuffer.data()), dataSize);
            wavFile.close();
        }
    }
}

oboe::DataCallbackResult AudioEngine::onAudioReady(oboe::AudioStream *audioStream, void *audioData, int32_t numFrames) {
    if (audioStream->getDirection() == oboe::Direction::Input) {
        float* floatData = static_cast<float*>(audioData);
        float maxPeak = 0.0f;
        for (int32_t i = 0; i < numFrames * audioStream->getChannelCount(); ++i) {
            float s = floatData[i];
            if (mIsRecording) {
                mRecordingBuffer.push_back(s);
            }
            float absVal = std::abs(s);
            if (absVal > maxPeak) maxPeak = absVal;
        }

        int writeIdx = Mixer::getInstance().mVisualWriteIndex.load(std::memory_order_relaxed);
        Mixer::getInstance().mVisualBuffer[writeIdx] = maxPeak;
        Mixer::getInstance().mVisualWriteIndex.store((writeIdx + 1) % 256, std::memory_order_relaxed);

    }
    return oboe::DataCallbackResult::Continue;
}
