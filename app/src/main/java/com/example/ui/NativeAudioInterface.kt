package com.example.ui

object NativeAudioInterface {
    init {
        try {
            System.loadLibrary("soniccore")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    external fun resetEngine()
    external fun togglePlayback(isPlaying: Boolean)
    external fun updateNoteVelocity(noteId: String, velocity: Float)
    external fun addAutomationNode(paramId: Int, targetTick: Long, value: Float, curveType: Int)
    external fun removeAutomationNode(paramId: Int, targetTick: Long)
    external fun getVisualSamples(targetArray: FloatArray)
    external fun startRecording(tempWavPath: String)
    external fun stopRecording()
}
