#pragma once
#include "AudioPlugin.h"

class VolumePlugin : public AudioPlugin {
public:
    VolumePlugin();
    void prepareToPlay(int sampleRate, int maxExpectedFrames) override;
    void processBlock(float* outBuffer, int numFrames) override;
    void setParameter(int paramId, float value) override;
    float getParameter(int paramId) override;

private:
    std::atomic<float> mGain{1.0f};
    std::atomic<float> mPan{0.5f};
    std::atomic<bool> mMute{false};
};
