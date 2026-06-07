#pragma once
#include "SamplerPlugin.h"
#include "VolumePlugin.h"
#include <vector>
#include <memory>

class Mixer {
public:
    Mixer();
    void prepareToPlay(int sampleRate, int maxExpectedFrames);
    void processBlock(float* outBuffer, int numFrames);
    
    SamplerPlugin* getSampler(int id);
    VolumePlugin* getVolume(int id);

private:
    static const int NUM_CHANNELS = 4;
    std::vector<std::unique_ptr<SamplerPlugin>> mSamplers;
    std::vector<std::unique_ptr<VolumePlugin>> mVolumes;
    std::vector<float> mMixBuffer;
};
