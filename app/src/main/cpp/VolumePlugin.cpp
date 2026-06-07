#include "VolumePlugin.h"

VolumePlugin::VolumePlugin() {}

void VolumePlugin::prepareToPlay(int sampleRate, int maxExpectedFrames) {}

void VolumePlugin::processBlock(float* outBuffer, int numFrames) {
    if (mMute.load()) {
        for (int i=0; i<numFrames*2; ++i) outBuffer[i] = 0.0f;
        return;
    }

    float gain = mGain.load();
    float pan = mPan.load();
    float leftGain = gain * (1.0f - pan) * 2.0f;
    float rightGain = gain * pan * 2.0f;
    
    for (int i=0; i<numFrames; ++i) {
        outBuffer[i*2] *= leftGain;
        outBuffer[i*2+1] *= rightGain;
    }
}

void VolumePlugin::setParameter(int paramId, float value) {
    if (paramId == 0) mGain.store(value);
    else if (paramId == 1) mPan.store(value);
    else if (paramId == 2) mMute.store(value > 0.5f);
}

float VolumePlugin::getParameter(int paramId) {
    if (paramId == 0) return mGain.load();
    if (paramId == 1) return mPan.load();
    if (paramId == 2) return mMute.load() ? 1.0f : 0.0f;
    return 0.0f;
}
