package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.roundToInt
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import org.json.JSONObject
import org.json.JSONArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import android.content.Context

import androidx.compose.runtime.mutableStateOf

enum class DawView {
    PLAYLIST, BROWSER, PIANO_ROLL, MIXER
}

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
    val notes: List<MidiNote> = emptyList(),
    val filePath: String? = null
)

data class AutomationNodeModel(
    val id: String = java.util.UUID.randomUUID().toString(),
    val targetTick: Long,
    val value: Float,
    val curveType: Int = 0 
)

data class AutomationTrackModel(
    val id: String = java.util.UUID.randomUUID().toString(),
    val paramId: Int = 0,
    val name: String = "Master Volume",
    val nodes: List<AutomationNodeModel> = emptyList()
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

    private val _automationTracks = MutableStateFlow<List<AutomationTrackModel>>(
        listOf(AutomationTrackModel(name = "Master Volume", paramId = 0))
    )
    val automationTracks = _automationTracks.asStateFlow()

    private val _engineTick = MutableStateFlow(0L)
    val engineTick = _engineTick.asStateFlow()

    private val _snapMode = MutableStateFlow(SnapMode.QUARTER)
    val snapMode = _snapMode.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()

    var armedTrackId = mutableStateOf<String?>(null)

    private val _currentView = MutableStateFlow(DawView.PLAYLIST)
    val currentView = _currentView.asStateFlow()

    private val _waveformFlow = MutableStateFlow<FloatArray>(FloatArray(256))
    val waveformFlow = _waveformFlow.asStateFlow()

    private val visualCache = FloatArray(256)

    private val _clipboardNotes = MutableStateFlow<List<MidiNote>>(emptyList())

    private var assetPatchManager: AssetPatchManager? = null

    init {
        startVisualizerPolling()
    }

    data class PackDownloadState(
        val url: String,
        val progress: Float = 0f,
        val isDownloading: Boolean = false,
        val isExtracting: Boolean = false,
        val isInstalled: Boolean = false
    )

    private val _downloadStates = MutableStateFlow<Map<String, PackDownloadState>>(emptyMap())
    val downloadStates = _downloadStates.asStateFlow()

    fun initAssetManager(context: Context) {
        if (assetPatchManager == null) {
            assetPatchManager = AssetPatchManager(context)
        }
    }

    fun startVisualizerPolling() {
        viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(16) // Equates to a strict ~60 FPS polling interval
                if (isPlaying.value || isRecording.value) {
                    NativeAudioInterface.getVisualSamples(visualCache)
                    _waveformFlow.emit(visualCache.clone())
                }
            }
        }
    }

    suspend fun importCustomPack(context: Context, zipFile: File) {
        initAssetManager(context)
        assetPatchManager?.extractZipPack(zipFile, "Imported Pack")
    }

    fun downloadAssetPack(context: Context, url: String, packName: String) {
        initAssetManager(context)
        
        _downloadStates.update { current ->
            current + (url to PackDownloadState(url = url, isDownloading = true))
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Mock downloading logic
                val tempZip = File(context.cacheDir, "temp_pack.zip")
                val connection = URL(url).openConnection()
                connection.connect()

                val totalLength = connection.contentLength
                var totalBytesRead = 0L
                val buffer = ByteArray(8192)

                connection.getInputStream().use { input ->
                    FileOutputStream(tempZip).use { output ->
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead
                            if (totalLength > 0) {
                                val progress = (totalBytesRead.toFloat() / totalLength.toFloat()).coerceIn(0f, 1f)
                                _downloadStates.update { current ->
                                    current + (url to PackDownloadState(url = url, isDownloading = true, progress = progress))
                                }
                            }
                        }
                    }
                }
                
                _downloadStates.update { current ->
                    current + (url to PackDownloadState(url = url, isExtracting = true))
                }

                assetPatchManager?.extractZipPack(tempZip, packName)
                
                tempZip.delete()
                
                _downloadStates.update { current ->
                    current + (url to PackDownloadState(url = url, isInstalled = true))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Reset state on failure
                _downloadStates.update { current ->
                    current - url
                }
            }
        }
    }

    fun togglePlay() {
        _isPlaying.value = !_isPlaying.value
        NativeAudioInterface.togglePlayback(_isPlaying.value)
    }

    fun toggleRecord(context: Context) {
        val armed = armedTrackId.value
        if (armed == null) return

        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return
        }

        _isRecording.value = !_isRecording.value
        if (_isRecording.value) {
            val file = File(context.cacheDir, "temp_recording.wav")
            NativeAudioInterface.startRecording(file.absolutePath)
            if (!isPlaying.value) togglePlay() // Starts playback visually as well
        } else {
            NativeAudioInterface.stopRecording()
            // Assume we create a clip from the temp wav on the armed track
            val file = File(context.cacheDir, "temp_recording.wav")
            if (file.exists()) {
                val newClip = Clip(
                    id = "audio_${System.currentTimeMillis()}",
                    trackId = armed!!,
                    name = "Recording",
                    startTick = 0, // In real scenario, calculate from play head
                    durationTicks = 1920,
                    filePath = file.absolutePath
                )
                _clips.update { it + newClip }
            }
        }
    }

    fun setNoteVelocity(noteId: String, velocity: Float) {
        val boundedVelocity = velocity.coerceIn(0f, 1f)
        mutateActiveClip { clip ->
            clip.copy(notes = clip.notes.map { note ->
                if (note.id == noteId) {
                    NativeAudioInterface.updateNoteVelocity(note.id, boundedVelocity)
                    note.copy(velocity = boundedVelocity)
                } else note
            })
        }
    }

    fun setView(view: DawView) {
        _currentView.value = view
    }

    fun createNewProject() {
        _clips.value = emptyList()
        NativeAudioInterface.resetEngine()
        setView(DawView.PLAYLIST)
    }

    suspend fun saveProject(file: File) {
        withContext(Dispatchers.IO) {
            val root = JSONObject()
            val clipsArray = JSONArray()
            _clips.value.forEach { clip ->
                val clipObj = JSONObject()
                clipObj.put("id", clip.id)
                clipObj.put("trackId", clip.trackId)
                clipObj.put("name", clip.name)
                clipObj.put("startTick", clip.startTick)
                clipObj.put("durationTicks", clip.durationTicks)

                val notesArray = JSONArray()
                clip.notes.forEach { note ->
                    val noteObj = JSONObject()
                    noteObj.put("id", note.id)
                    noteObj.put("startTick", note.startTick)
                    noteObj.put("durationTicks", note.durationTicks)
                    noteObj.put("noteValue", note.noteValue)
                    noteObj.put("velocity", note.velocity.toDouble())
                    notesArray.put(noteObj)
                }
                clipObj.put("notes", notesArray)
                clipsArray.put(clipObj)
            }
            root.put("clips", clipsArray)
            file.writeText(root.toString())
        }
    }

    suspend fun loadProject(file: File) {
        withContext(Dispatchers.IO) {
            try {
                if (!file.exists()) return@withContext
                val root = JSONObject(file.readText())
                val clipsArray = root.optJSONArray("clips") ?: JSONArray()
                val loadedClips = mutableListOf<Clip>()
                
                for (i in 0 until clipsArray.length()) {
                    val clipObj = clipsArray.getJSONObject(i)
                    val notesArray = clipObj.optJSONArray("notes") ?: JSONArray()
                    val loadedNotes = mutableListOf<MidiNote>()
                    
                    for (j in 0 until notesArray.length()) {
                        val noteObj = notesArray.getJSONObject(j)
                        loadedNotes.add(
                            MidiNote(
                                id = noteObj.getString("id"),
                                startTick = noteObj.getInt("startTick"),
                                durationTicks = noteObj.getInt("durationTicks"),
                                noteValue = noteObj.getInt("noteValue"),
                                velocity = noteObj.optDouble("velocity", 1.0).toFloat(),
                                isSelected = false
                            )
                        )
                    }
                    
                    loadedClips.add(
                        Clip(
                            id = clipObj.getString("id"),
                            trackId = clipObj.getString("trackId"),
                            name = clipObj.getString("name"),
                            startTick = clipObj.getInt("startTick"),
                            durationTicks = clipObj.getInt("durationTicks"),
                            notes = loadedNotes
                        )
                    )
                }
                _clips.value = loadedClips
                NativeAudioInterface.resetEngine()
                setView(DawView.PLAYLIST)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setSnapMode(mode: SnapMode) {
        _snapMode.value = mode
    }

    fun updateEngineTick(tick: Long) {
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

    fun addAutomationNode(trackId: String, targetTick: Long, value: Float, curveType: Int = 1) {
        val boundedValue = value.coerceIn(0f, 1f)
        val snapStep = if (snapMode.value != SnapMode.OFF) snapMode.value.ticks else 1
        val snappedTick = ((targetTick.toFloat() / snapStep).roundToInt() * snapStep).coerceAtLeast(0).toLong()

        _automationTracks.update { tracks ->
            tracks.map { track ->
                if (track.id == trackId) {
                    NativeAudioInterface.addAutomationNode(track.paramId, snappedTick, boundedValue, curveType)
                    val newNode = AutomationNodeModel(targetTick = snappedTick, value = boundedValue, curveType = curveType)
                    track.copy(nodes = (track.nodes + newNode).sortedBy { it.targetTick })
                } else track
            }
        }
    }

    fun updateAutomationNode(trackId: String, nodeId: String, targetTick: Long, value: Float) {
        val boundedValue = value.coerceIn(0f, 1f)
        val snapStep = if (snapMode.value != SnapMode.OFF) snapMode.value.ticks else 1
        val snappedTick = ((targetTick.toFloat() / snapStep).roundToInt() * snapStep).coerceAtLeast(0).toLong()

        _automationTracks.update { tracks ->
            tracks.map { track ->
                if (track.id == trackId) {
                    val matchingNode = track.nodes.find { it.id == nodeId }
                    if (matchingNode != null) {
                        // Remove old node from C++
                        NativeAudioInterface.removeAutomationNode(track.paramId, matchingNode.targetTick)
                        // Add new node position to C++
                        NativeAudioInterface.addAutomationNode(track.paramId, snappedTick, boundedValue, matchingNode.curveType)
                    }

                    track.copy(nodes = track.nodes.map { node ->
                        if (node.id == nodeId) node.copy(targetTick = snappedTick, value = boundedValue) else node
                    }.sortedBy { it.targetTick })
                } else track
            }
        }
    }

    fun removeAutomationNode(trackId: String, nodeId: String) {
        _automationTracks.update { tracks ->
            tracks.map { track ->
                if (track.id == trackId) {
                    val matchingNode = track.nodes.find { it.id == nodeId }
                    if (matchingNode != null) {
                        NativeAudioInterface.removeAutomationNode(track.paramId, matchingNode.targetTick)
                    }
                    track.copy(nodes = track.nodes.filter { it.id != nodeId })
                } else track
            }
        }
    }

    fun setAutomationNodeCurveType(trackId: String, nodeId: String, curveType: Int) {
        _automationTracks.update { tracks ->
            tracks.map { track ->
                if (track.id == trackId) {
                    track.copy(nodes = track.nodes.map { node ->
                        if (node.id == nodeId) {
                            NativeAudioInterface.addAutomationNode(track.paramId, node.targetTick, node.value, curveType)
                            node.copy(curveType = curveType)
                        } else node
                    })
                } else track
            }
        }
    }
}
