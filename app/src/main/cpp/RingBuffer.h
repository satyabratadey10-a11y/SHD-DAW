#pragma once
#include <atomic>
#include <vector>
#include <algorithm>

class RingBuffer {
public:
    RingBuffer(size_t capacity) : mBuffer(capacity), mCapacity(capacity) {
        mWriteIndex.store(0);
        mReadIndex.store(0);
    }

    size_t write(const float* data, size_t count) {
        size_t writeIndex = mWriteIndex.load(std::memory_order_relaxed);
        size_t readIndex = mReadIndex.load(std::memory_order_acquire);
        
        size_t available = mCapacity - 1 - (writeIndex - readIndex + mCapacity) % mCapacity;
        if (count > available) count = available;
        if (count == 0) return 0;
        
        size_t firstPart = std::min(count, mCapacity - writeIndex);
        std::copy(data, data + firstPart, mBuffer.data() + writeIndex);
        if (firstPart < count) {
            std::copy(data + firstPart, data + count, mBuffer.data());
        }
        
        mWriteIndex.store((writeIndex + count) % mCapacity, std::memory_order_release);
        return count;
    }
    
    size_t read(float* data, size_t count) {
        size_t writeIndex = mWriteIndex.load(std::memory_order_acquire);
        size_t readIndex = mReadIndex.load(std::memory_order_relaxed);
        
        size_t available = (writeIndex - readIndex + mCapacity) % mCapacity;
        if (count > available) count = available;
        if (count == 0) return 0;
        
        size_t firstPart = std::min(count, mCapacity - readIndex);
        std::copy(mBuffer.data() + readIndex, mBuffer.data() + readIndex + firstPart, data);
        if (firstPart < count) {
            std::copy(mBuffer.data(), mBuffer.data() + (count - firstPart), data + firstPart);
        }
        
        mReadIndex.store((readIndex + count) % mCapacity, std::memory_order_release);
        return count;
    }
    
    size_t availableRead() const {
        size_t writeIndex = mWriteIndex.load(std::memory_order_acquire);
        size_t readIndex = mReadIndex.load(std::memory_order_relaxed);
        return (writeIndex - readIndex + mCapacity) % mCapacity;
    }

private:
    std::vector<float> mBuffer;
    size_t mCapacity;
    std::atomic<size_t> mWriteIndex;
    std::atomic<size_t> mReadIndex;
};
