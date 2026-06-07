#include "AudioEngine.h"
#include <android/log.h>

#define TAG "AudioEngine"

AudioEngine& AudioEngine::getInstance() {
    static AudioEngine instance;
    return instance;
}

AudioEngine::AudioEngine() {
}

void AudioEngine::start() {
    if (mIsPlaying.load()) return;

    oboe::AudioStreamBuilder builder;
    builder.setDirection(oboe::Direction::Output)
           ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
           ->setSharingMode(oboe::SharingMode::Exclusive)
           ->setFormat(oboe::AudioFormat::Float)
           ->setChannelCount(oboe::ChannelCount::Stereo)
           ->setDataCallback(this);

    oboe::Result result = builder.openStream(mStream);
    if (result == oboe::Result::OK) {
        mSampleRate = mStream->getSampleRate();
        mMixer.prepareToPlay(mSampleRate, mStream->getFramesPerBurst());
        mStream->requestStart();
        mIsPlaying.store(true);
        __android_log_print(ANDROID_LOG_INFO, TAG, "Stream started with sample rate %d", mSampleRate);
    } else {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "Failed to open stream");
    }
}

void AudioEngine::stop() {
    if (!mIsPlaying.load()) return;
    if (mStream) {
        mStream->requestStop();
        mStream->close();
        mStream.reset();
    }
    mIsPlaying.store(false);
}

void AudioEngine::setBPM(int bpm) {
    mBpm.store(bpm);
}

void AudioEngine::triggerSample(int pluginId) {
    if (auto* sampler = mMixer.getSampler(pluginId)) {
        sampler->trigger();
    }
}

void AudioEngine::setPluginParameter(int pluginId, int paramId, float value) {
    if (auto* vol = mMixer.getVolume(pluginId)) {
        vol->setParameter(paramId, value);
    }
}

oboe::DataCallbackResult AudioEngine::onAudioReady(oboe::AudioStream *oboeStream, void *audioData, int32_t numFrames) {
    float* floatData = static_cast<float*>(audioData);
    mMixer.processBlock(floatData, numFrames);
    return oboe::DataCallbackResult::Continue;
}
