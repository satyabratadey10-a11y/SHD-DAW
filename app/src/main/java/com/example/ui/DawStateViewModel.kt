package com.example.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.roundToInt

data class MidiNote(
    val id: String = java.util.UUID.randomUUID().toString(),
    val startTick: Int,
    val durationTicks: Int,
    val noteValue: Int,
    val velocity: Float = 1.0f,
    val isSelected: Boolean = false
)

data class Clip(
    val id: String = java.util.UUID.randomUUID().toString(),
    val trackId: String,
    val name: String,
    val startTick: Int,
    val durationTicks: Int,
    val notes: List<MidiNote> = emptyList()
)

enum class SnapMode(val ticks: Int) {
    BAR(1920),
    HALF(960),
    QUARTER(480),
    EIGHTH(240),
    SIXTEENTH(120),
    OFF(1)
}

class DawStateViewModel : ViewModel() {
    private val _clips = MutableStateFlow<List<Clip>>(
        listOf(Clip(trackId = "track1", name = "Pattern 1", startTick = 0, durationTicks = 1920 * 4,
            notes = listOf(
                MidiNote(startTick = 0, durationTicks = 480, noteValue = 60),
                MidiNote(startTick = 480, durationTicks = 480, noteValue = 64),
                MidiNote(startTick = 960, durationTicks = 480, noteValue = 67),
                MidiNote(startTick = 1440, durationTicks = 480, noteValue = 72)
            )))
    )
    val clips = _clips.asStateFlow()

    private val _engineTick = MutableStateFlow(0)
    val engineTick = _engineTick.asStateFlow()

    private val _snapMode = MutableStateFlow(SnapMode.QUARTER)
    val snapMode = _snapMode.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _clipboardNotes = MutableStateFlow<List<MidiNote>>(emptyList())

    fun togglePlay() {
        _isPlaying.value = !_isPlaying.value
    }

    fun setSnapMode(mode: SnapMode) {
        _snapMode.value = mode
    }

    fun updateEngineTick(tick: Int) {
        _engineTick.value = tick
    }

    private fun getActiveClip(): Clip? {
        return _clips.value.firstOrNull() // For demo purposes we just use the first clip
    }

    private inline fun mutateActiveClip(transform: (Clip) -> Clip) {
        _clips.update { currentClips ->
            if (currentClips.isEmpty()) return@update currentClips
            val newClips = currentClips.toMutableList()
            newClips[0] = transform(newClips[0])
            newClips
        }
    }

    fun updateNoteSelection(selectionMap: Map<String, Boolean>) {
        mutateActiveClip { clip ->
            clip.copy(notes = clip.notes.map { note ->
                selectionMap[note.id]?.let { isSelected ->
                    note.copy(isSelected = isSelected)
                } ?: note
            })
        }
    }

    fun selectAllNotesInMarquee(minTick: Int, maxTick: Int, minNote: Int, maxNote: Int) {
        mutateActiveClip { clip ->
            clip.copy(notes = clip.notes.map { note ->
                val noteEndTick = note.startTick + note.durationTicks
                val intersectsHorizontally = (note.startTick < maxTick && noteEndTick > minTick)
                val intersectsVertically = (note.noteValue in minNote..maxNote)
                
                note.copy(isSelected = (intersectsHorizontally && intersectsVertically))
            })
        }
    }

    fun deselectAllNotes() {
        mutateActiveClip { clip ->
            clip.copy(notes = clip.notes.map { it.copy(isSelected = false) })
        }
    }

    fun selectNoteExclusively(noteId: String) {
        mutateActiveClip { clip ->
            clip.copy(notes = clip.notes.map { it.copy(isSelected = (it.id == noteId)) })
        }
    }

    fun toggleNoteSelection(noteId: String) {
        mutateActiveClip { clip ->
            clip.copy(notes = clip.notes.map { 
                if (it.id == noteId) it.copy(isSelected = !it.isSelected) else it 
            })
        }
    }
    
    fun applyNoteDelta(deltaTicks: Int, deltaNoteValue: Int) {
        val snapStep = if (snapMode.value != SnapMode.OFF) snapMode.value.ticks else 1
        mutateActiveClip { clip ->
            clip.copy(notes = clip.notes.map { note ->
                if (note.isSelected) {
                    val rawStart = note.startTick + deltaTicks
                    val rawNote = note.noteValue + deltaNoteValue
                    
                    val snappedStart = ((rawStart.toFloat() / snapStep).roundToInt() * snapStep).coerceAtLeast(0)
                    val targetNoteValue = rawNote.coerceIn(0, 127)
                    
                    note.copy(startTick = snappedStart, noteValue = targetNoteValue)
                } else note
            })
        }
    }

    fun setNoteDuration(noteId: String, newDurationRaw: Int) {
        val snapStep = if (snapMode.value != SnapMode.OFF) snapMode.value.ticks else 1
        val snappedDuration = ((newDurationRaw.toFloat() / snapStep).roundToInt() * snapStep).coerceAtLeast(snapStep)
        
        mutateActiveClip { clip ->
            clip.copy(notes = clip.notes.map { note ->
                if (note.id == noteId || note.isSelected) {
                    note.copy(durationTicks = snappedDuration)
                } else note
            })
        }
    }

    fun resizeSelectedNotesDelta(deltaTicks: Int) {
        val snapStep = if (snapMode.value != SnapMode.OFF) snapMode.value.ticks else 1
        mutateActiveClip { clip ->
            clip.copy(notes = clip.notes.map { note ->
                if (note.isSelected) {
                    val rawDuration = note.durationTicks + deltaTicks
                    val snappedDuration = ((rawDuration.toFloat() / snapStep).roundToInt() * snapStep).coerceAtLeast(snapStep)
                    note.copy(durationTicks = snappedDuration)
                } else note
            })
        }
    }

    // Contextual Action Menu Commands
    fun copySelectedNotes() {
        val clip = getActiveClip() ?: return
        _clipboardNotes.value = clip.notes.filter { it.isSelected }.map { it.copy() }
    }

    fun deleteSelectedNotes() {
        mutateActiveClip { clip ->
            clip.copy(notes = clip.notes.filter { !it.isSelected })
        }
    }

    fun snapSelectedNotes() {
        val snapStep = if (snapMode.value != SnapMode.OFF) snapMode.value.ticks else return
        mutateActiveClip { clip ->
            clip.copy(notes = clip.notes.map { note ->
                if (note.isSelected) {
                    val snappedStart = ((note.startTick.toFloat() / snapStep).roundToInt() * snapStep).coerceAtLeast(0)
                    note.copy(startTick = snappedStart)
                } else note
            })
        }
    }

    fun addNote(noteValue: Int, rawStartTick: Int) {
        val snapStep = if (snapMode.value != SnapMode.OFF) snapMode.value.ticks else 1
        val snappedStart = ((rawStartTick.toFloat() / snapStep).roundToInt() * snapStep).coerceAtLeast(0)
        
        val newNote = MidiNote(
            startTick = snappedStart,
            durationTicks = snapStep.coerceAtLeast(120),
            noteValue = noteValue,
            isSelected = true
        )
        mutateActiveClip { clip ->
            clip.copy(notes = clip.notes + newNote)
        }
    }
}
