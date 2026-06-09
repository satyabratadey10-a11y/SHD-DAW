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
}
