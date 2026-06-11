#include <jni.h>
#include <string.h>
#include "AutomationManager.h"
#include "Mixer.h"
#include "AudioEngine.h"

extern "C" JNIEXPORT void JNICALL
Java_com_example_ui_NativeAudioInterface_startRecording(JNIEnv* env, jobject thiz, jstring path) {
    const char *pathChars = env->GetStringUTFChars(path, 0);
    AudioEngine::getInstance().startRecording(pathChars);
    env->ReleaseStringUTFChars(path, pathChars);
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_ui_NativeAudioInterface_stopRecording(JNIEnv* env, jobject thiz) {
    AudioEngine::getInstance().stopRecording();
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_ui_NativeAudioInterface_addAutomationNode(JNIEnv* env, jobject thiz, jint paramId, jlong targetTick, jfloat value, jint curveType) {
    AutomationManager::getInstance().getTrack(paramId).addOrUpdateNode(targetTick, value, curveType);
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_ui_NativeAudioInterface_removeAutomationNode(JNIEnv* env, jobject thiz, jint paramId, jlong targetTick) {
    AutomationManager::getInstance().getTrack(paramId).removeNode(targetTick);
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_ui_NativeAudioInterface_resetEngine(JNIEnv* env, jobject thiz) {
    // Reset underlying engine queues and state
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_ui_NativeAudioInterface_togglePlayback(JNIEnv* env, jobject thiz, jboolean is_playing) {
    // Toggle audio callbacks and stream processing
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_ui_NativeAudioInterface_updateNoteVelocity(JNIEnv* env, jobject thiz, jstring noteId, jfloat velocity) {
    // update note velocity in the engine
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_ui_NativeAudioInterface_getVisualSamples(JNIEnv* env, jobject thiz, jfloatArray targetArray) {
    jfloat* data = (jfloat*)env->GetPrimitiveArrayCritical(targetArray, 0);
    if (data == nullptr) return;
    
    // Copy the circular buffer to the target array
    memcpy(data, Mixer::getInstance().mVisualBuffer, 256 * sizeof(float));
    
    env->ReleasePrimitiveArrayCritical(targetArray, data, 0);
}
