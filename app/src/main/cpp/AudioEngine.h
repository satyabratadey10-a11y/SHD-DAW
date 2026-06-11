#ifndef AUDIOENGINE_H
#define AUDIOENGINE_H

#include <oboe/Oboe.h>
#include <atomic>
#include <vector>
#include <string>

class AudioEngine : public oboe::AudioStreamDataCallback {
public:
    static AudioEngine& getInstance() {
        static AudioEngine instance;
        return instance;
    }

    AudioEngine();
    ~AudioEngine();

    void startRecording(const std::string& tempWavPath);
    void stopRecording();

    oboe::DataCallbackResult onAudioReady(oboe::AudioStream *audioStream, void *audioData, int32_t numFrames) override;

private:
    std::shared_ptr<oboe::AudioStream> mInputStream;
    std::shared_ptr<oboe::AudioStream> mOutputStream;
    
    std::atomic<bool> mIsRecording{false};
    std::vector<float> mRecordingBuffer;
    std::string mTempWavPath;

};

#endif // AUDIOENGINE_H
