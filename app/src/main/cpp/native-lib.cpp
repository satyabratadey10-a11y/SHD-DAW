#include <jni.h>
#include "AudioEngine.h"

extern "C"
JNIEXPORT void JNICALL
Java_com_example_NativeAudioInterface_startEngine(JNIEnv *env, jobject thiz) {
    AudioEngine::getInstance().start();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_NativeAudioInterface_stopEngine(JNIEnv *env, jobject thiz) {
    AudioEngine::getInstance().stop();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_NativeAudioInterface_setBPM(JNIEnv *env, jobject thiz, jint bpm) {
    AudioEngine::getInstance().setBPM(bpm);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_NativeAudioInterface_triggerSample(JNIEnv *env, jobject thiz, jint plugin_id) {
    AudioEngine::getInstance().triggerSample(plugin_id);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_NativeAudioInterface_setPluginParameter(JNIEnv *env, jobject thiz, jint plugin_id, jint param_id, jfloat value) {
    AudioEngine::getInstance().setPluginParameter(plugin_id, param_id, value);
}
