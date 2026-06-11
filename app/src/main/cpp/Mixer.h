#ifndef MIXER_H
#define MIXER_H

#include "AutomationManager.h"
#include <cstdint>
#include <atomic>

class Mixer {
public:
    static Mixer& getInstance() {
        static Mixer instance;
        return instance;
    }

    void processBlock(float* outputBuffer, int numFrames, int64_t startTick);
    
    float mVisualBuffer[256];
    std::atomic<int> mVisualWriteIndex{0};

private:
    Mixer() {
        for (int i = 0; i < 256; ++i) mVisualBuffer[i] = 0.0f;
    }
};

#endif // MIXER_H
