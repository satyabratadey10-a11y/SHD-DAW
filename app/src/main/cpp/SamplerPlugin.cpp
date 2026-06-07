#include "SamplerPlugin.h"
#include <cmath>

SamplerPlugin::SamplerPlugin() {
    mSampleData.resize(48000, 0.0f);
    for(size_t i=0; i<mSampleData.size(); ++i) {
        float time = (float)i / 48000.0f;
        float env = exp(-time * 10.0f);
        mSampleData[i] = sin(2 * 3.14159f * 60.0f * time) * env;
    }
    mReadPointer.store(-1);
}

void SamplerPlugin::prepareToPlay(int sampleRate, int maxExpectedFrames) {
}

void SamplerPlugin::trigger() {
    mReadPointer.store(0);
}

void SamplerPlugin::processBlock(float* outBuffer, int numFrames) {
    int pos = mReadPointer.load();
    if (pos < 0) return;
    
    float gain = mGain.load();
    for (int i=0; i<numFrames; ++i) {
        float sample = 0.0f;
        if (pos < (int)mSampleData.size()) {
            sample = mSampleData[pos++] * gain;
        } else {
            pos = -1;
        }
        
        outBuffer[i*2] += sample;
        outBuffer[i*2+1] += sample;
        
        if (pos < 0) break;
    }
    mReadPointer.store(pos);
}

void SamplerPlugin::setParameter(int paramId, float value) {
    if (paramId == 0) mGain.store(value);
}

float SamplerPlugin::getParameter(int paramId) {
    if (paramId == 0) return mGain.load();
    return 0.0f;
}
