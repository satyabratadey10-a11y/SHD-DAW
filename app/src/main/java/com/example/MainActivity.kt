package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.DawStateViewModel
import com.example.ui.HorizontalPlaylist
import com.example.ui.ProjectBrowserScreen
import com.example.ui.PianoRollEditor
import com.example.ui.LandscapeMixer
import com.example.ui.DawView

import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.widget.Toast
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, "Microphone permission is required to record audio", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                        val viewModel: DawStateViewModel = viewModel()
                        val currentView by viewModel.currentView.collectAsState()
                        
                        when (currentView) {
                            DawView.PIANO_ROLL -> PianoRollEditor(viewModel = viewModel)
                            DawView.PLAYLIST -> HorizontalPlaylist(viewModel = viewModel)
                            DawView.MIXER -> LandscapeMixer(viewModel = viewModel)
                            else -> ProjectBrowserScreen(viewModel = viewModel)
                        }
                }
            }
        }
    }
}
