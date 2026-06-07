#pragma once
#include <atomic>

class AudioPlugin {
public:
    virtual ~AudioPlugin() = default;
    virtual void prepareToPlay(int sampleRate, int maxExpectedFrames) = 0;
    virtual void processBlock(float* outBuffer, int numFrames) = 0;
    virtual void setParameter(int paramId, float value) = 0;
    virtual float getParameter(int paramId) = 0;
};
