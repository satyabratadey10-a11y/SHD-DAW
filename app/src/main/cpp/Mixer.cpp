#include "Mixer.h"
#include <cmath>
#include <algorithm>

void Mixer::processBlock(float* outputBuffer, int numFrames, int64_t startTick) {
    float maxPeak = 0.0f;
    int frameCounter = 0;

    // Process audio buffer, per-frame sample level
    for (int i = 0; i < numFrames; ++i) {
        // Calculate the current tick based on the frame index, sample rate, and tempo (simplified)
        // Typically tick increases depending on PPQN and sample rate.
        // For demonstration, we'll increment the tick assuming some ratio or just use startTick
        int64_t currentFrameTick = startTick + (i / 10); // Simplified tick advancement
        
        AutomationManager::getInstance().process(currentFrameTick);
        
        float automationValue = AutomationManager::getInstance().currentInterpolatedValue;
        
        // Output audio generation and apply automation 
        // Example: Master Volume automation
        outputBuffer[i] *= automationValue;
        
        float absVal = std::abs(outputBuffer[i]);
        if (absVal > maxPeak) maxPeak = absVal;
        
        frameCounter++;
        if (frameCounter >= 64) {
            int writeIdx = mVisualWriteIndex.load(std::memory_order_relaxed);
            mVisualBuffer[writeIdx] = maxPeak;
            mVisualWriteIndex.store((writeIdx + 1) % 256, std::memory_order_relaxed);
            maxPeak = 0.0f;
            frameCounter = 0;
        }
    }
}
