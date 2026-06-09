#include <jni.h>

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
