#include "Mixer.h"
#include <algorithm>

Mixer::Mixer() {
    for (int i = 0; i < NUM_CHANNELS; ++i) {
        mSamplers.push_back(std::make_unique<SamplerPlugin>());
        mVolumes.push_back(std::make_unique<VolumePlugin>());
    }
}

void Mixer::prepareToPlay(int sampleRate, int maxExpectedFrames) {
    mMixBuffer.resize(maxExpectedFrames * 2, 0.0f);
    for (int i = 0; i < NUM_CHANNELS; ++i) {
        mSamplers[i]->prepareToPlay(sampleRate, maxExpectedFrames);
        mVolumes[i]->prepareToPlay(sampleRate, maxExpectedFrames);
    }
}

void Mixer::processBlock(float* outBuffer, int numFrames) {
    if (mMixBuffer.size() < (size_t)(numFrames * 2)) {
        mMixBuffer.resize(numFrames * 2, 0.0f);
    }
    std::fill_n(outBuffer, numFrames * 2, 0.0f);

    for (int i = 0; i < NUM_CHANNELS; ++i) {
        std::fill(mMixBuffer.begin(), mMixBuffer.end(), 0.0f);
        mSamplers[i]->processBlock(mMixBuffer.data(), numFrames);
        mVolumes[i]->processBlock(mMixBuffer.data(), numFrames);
        
        for (int j = 0; j < numFrames * 2; ++j) {
            outBuffer[j] += mMixBuffer[j];
        }
    }
    
    for (int j = 0; j < numFrames * 2; ++j) {
        if (outBuffer[j] > 1.0f) outBuffer[j] = 1.0f;
        else if (outBuffer[j] < -1.0f) outBuffer[j] = -1.0f;
    }
}

SamplerPlugin* Mixer::getSampler(int id) {
    if (id >= 0 && id < NUM_CHANNELS) return mSamplers[id].get();
    return nullptr;
}

VolumePlugin* Mixer::getVolume(int id) {
    if (id >= 0 && id < NUM_CHANNELS) return mVolumes[id].get();
    return nullptr;
}
