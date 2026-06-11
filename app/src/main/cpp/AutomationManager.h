#ifndef AUTOMATION_MANAGER_H
#define AUTOMATION_MANAGER_H

#include <vector>
#include <cstdint>

struct AutomationNode {
    int64_t targetTick;
    float normalizedValue; // 0.0f to 1.0f
    int curveType;        // 0 = Linear, 1 = Bezier/Spline, 2 = Hold
};

class AutomationTrack {
public:
    std::vector<AutomationNode> nodes;
    
    // Calculate interpolated value for current tick
    float getValueAtTick(int64_t currentTick) const;
    
    void addOrUpdateNode(int64_t targetTick, float value, int curveType);
    void removeNode(int64_t targetTick);
};

class AutomationManager {
public:
    static AutomationManager& getInstance() {
        static AutomationManager instance;
        return instance;
    }

    AutomationTrack& getTrack(int targetParameterId) {
        // Just return a single global track for now or a map
        // For simplicity, we just have one track in this prototype
        return track;
    }

    void process(int64_t currentTick);
    
    // Callback or direct setting of destination parameter could be added here
    float currentInterpolatedValue = 0.0f;
    
private:
    AutomationManager() = default;
    AutomationTrack track;
};

#endif // AUTOMATION_MANAGER_H
