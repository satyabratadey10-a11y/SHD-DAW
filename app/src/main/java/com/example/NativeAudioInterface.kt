package com.example

object NativeAudioInterface {
    init {
        try {
            System.loadLibrary("native-lib")
        } catch (e: UnsatisfiedLinkError) {
            e.printStackTrace()
        }
    }
    
    external fun startEngine()
    external fun stopEngine()
    external fun setBPM(bpm: Int)
    external fun triggerSample(pluginId: Int)
    external fun setPluginParameter(pluginId: Int, paramId: Int, value: Float)
    external fun startRecording(filePath: String)
    external fun stopRecording()
}
