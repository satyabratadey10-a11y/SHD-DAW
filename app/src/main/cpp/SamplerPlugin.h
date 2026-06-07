#pragma once
#include "AudioPlugin.h"
#include <vector>

class SamplerPlugin : public AudioPlugin {
public:
    SamplerPlugin();
    void prepareToPlay(int sampleRate, int maxExpectedFrames) override;
    void processBlock(float* outBuffer, int numFrames) override;
    void setParameter(int paramId, float value) override;
    float getParameter(int paramId) override;

    void trigger();

private:
    std::vector<float> mSampleData;
    std::atomic<int> mReadPointer{-1}; 
    std::atomic<float> mGain{1.0f};
};
