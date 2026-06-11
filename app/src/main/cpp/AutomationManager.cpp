#include "AutomationManager.h"
#include <algorithm>
#include <cmath>

void AutomationTrack::addOrUpdateNode(int64_t targetTick, float value, int curveType) {
    for (auto& node : nodes) {
        if (node.targetTick == targetTick) {
            node.normalizedValue = value;
            node.curveType = curveType;
            return;
        }
    }
    nodes.push_back({targetTick, value, curveType});
    std::sort(nodes.begin(), nodes.end(), [](const AutomationNode& a, const AutomationNode& b) {
        return a.targetTick < b.targetTick;
    });
}

void AutomationTrack::removeNode(int64_t targetTick) {
    nodes.erase(std::remove_if(nodes.begin(), nodes.end(), [targetTick](const AutomationNode& n) {
        return n.targetTick == targetTick;
    }), nodes.end());
}

float AutomationTrack::getValueAtTick(int64_t currentTick) const {
    if (nodes.empty()) return 1.0f; // Default value if no automation
    
    if (currentTick <= nodes.front().targetTick) {
        return nodes.front().normalizedValue;
    }
    if (currentTick >= nodes.back().targetTick) {
        return nodes.back().normalizedValue;
    }
    
    // Find the two bounding nodes
    for (size_t i = 0; i < nodes.size() - 1; ++i) {
        const auto& n1 = nodes[i];
        const auto& n2 = nodes[i + 1];
        
        if (currentTick >= n1.targetTick && currentTick < n2.targetTick) {
            int64_t tickSpan = n2.targetTick - n1.targetTick;
            if (tickSpan == 0) return n2.normalizedValue;
            
            float t = static_cast<float>(currentTick - n1.targetTick) / tickSpan;
            
            if (n1.curveType == 0) { // Linear
                return n1.normalizedValue + (n2.normalizedValue - n1.normalizedValue) * t;
            } else if (n1.curveType == 1) { // Cubic Bezier simplified (Ease In-Out)
                float tBezier = t * t * (3.0f - 2.0f * t); // SmoothStep interpolation
                return n1.normalizedValue + (n2.normalizedValue - n1.normalizedValue) * tBezier;
            } else if (n1.curveType == 2) { // Hold
                return n1.normalizedValue;
            } else {
                return n1.normalizedValue;
            }
        }
    }
    
    return 1.0f;
}

void AutomationManager::process(int64_t currentTick) {
    currentInterpolatedValue = getTrack(0).getValueAtTick(currentTick);
    // In a real DSP engine, this currentInterpolatedValue would be applied directly 
    // to the Synth parameters (e.g. Master Volume, Filter Cutoff) immediately.
}
