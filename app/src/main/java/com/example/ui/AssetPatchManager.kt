package com.example.ui

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

data class AssetPack(
    val name: String,
    val sampleCount: Int,
    val presetCount: Int
)

class AssetPatchManager(private val context: Context) {
    private val _installedPacks = MutableStateFlow<List<AssetPack>>(emptyList())
    val installedPacks = _installedPacks.asStateFlow()

    init {
        refreshPacks()
    }

    suspend fun extractZipPack(zipFile: File, packName: String): AssetPack = withContext(Dispatchers.IO) {
        var sampleCount = 0
        var presetCount = 0

        val soundsDir = File(context.getExternalFilesDir(null), "sounds")
        val samplesDir = File(soundsDir, "samples").apply { mkdirs() }
        val presetsDir = File(soundsDir, "presets").apply { mkdirs() }

        zipFile.inputStream().use { fileInput ->
            ZipInputStream(fileInput).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val fileName = entry.name.lowercase()
                        val isSample = fileName.endsWith(".wav") || fileName.endsWith(".mp3")
                        val isPreset = fileName.endsWith(".json")
                        
                        if (isSample || isPreset) {
                            val targetDir = if (isSample) samplesDir else presetsDir
                            val targetFile = File(targetDir, File(entry.name).name)
                            
                            FileOutputStream(targetFile).use { fos ->
                                zis.copyTo(fos)
                            }
                            
                            if (isSample) sampleCount++
                            if (isPreset) presetCount++
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }
        
        val pack = AssetPack(packName, sampleCount, presetCount)
        refreshPacks()
        pack
    }
    
    fun refreshPacks() {
        // In a real implementation you would scan the directories here and group by pack metadata
        // For simplicity, we just keep this logic stubbed out or read from a manifest file if we had one.
    }
}
