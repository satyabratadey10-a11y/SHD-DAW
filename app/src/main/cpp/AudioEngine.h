#pragma once
#include <oboe/Oboe.h>
#include "Mixer.h"
#include "WavWriter.h"
#include <atomic>

class AudioEngine : public oboe::AudioStreamDataCallback {
public:
    static AudioEngine& getInstance();

    void start();
    void stop();
    void setBPM(int bpm);
    void triggerSample(int pluginId);
    void setPluginParameter(int pluginId, int paramId, float value);
    void startRecording(const std::string& filePath);
    void stopRecording();

    oboe::DataCallbackResult onAudioReady(oboe::AudioStream *oboeStream, void *audioData, int32_t numFrames) override;

private:
    AudioEngine();
    ~AudioEngine() = default;

    std::shared_ptr<oboe::AudioStream> mStream;
    Mixer mMixer;
    WavWriter mWavWriter;
    std::atomic<bool> mIsPlaying{false};
    std::atomic<int> mBpm{120};
    int mSampleRate{48000};
};
