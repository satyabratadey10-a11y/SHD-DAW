package com.example.ui

import androidx.lifecycle.ViewModel
import com.example.NativeAudioInterface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DawStateViewModel : ViewModel() {
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _bpm = MutableStateFlow(120)
    val bpm: StateFlow<Int> = _bpm.asStateFlow()

    private val _volumes = MutableStateFlow(listOf(1.0f, 1.0f, 1.0f, 1.0f))
    val volumes: StateFlow<List<Float>> = _volumes.asStateFlow()

    private val _mutes = MutableStateFlow(listOf(false, false, false, false))
    val mutes: StateFlow<List<Boolean>> = _mutes.asStateFlow()

    fun togglePlay() {
        if (_isPlaying.value) {
            NativeAudioInterface.stopEngine()
        } else {
            NativeAudioInterface.startEngine()
        }
        _isPlaying.value = !_isPlaying.value
    }

    fun setBpm(newBpm: Int) {
        _bpm.value = newBpm
        NativeAudioInterface.setBPM(newBpm)
    }

    fun triggerSample(index: Int) {
        NativeAudioInterface.triggerSample(index)
    }

    fun setVolume(index: Int, volume: Float) {
        val newVolumes = _volumes.value.toMutableList()
        newVolumes[index] = volume
        _volumes.value = newVolumes
        NativeAudioInterface.setPluginParameter(index, 0, volume)
    }

    fun toggleMute(index: Int) {
        val newMutes = _mutes.value.toMutableList()
        val isMuted = !newMutes[index]
        newMutes[index] = isMuted
        _mutes.value = newMutes
        NativeAudioInterface.setPluginParameter(index, 2, if (isMuted) 1.0f else 0.0f)
    }
}
