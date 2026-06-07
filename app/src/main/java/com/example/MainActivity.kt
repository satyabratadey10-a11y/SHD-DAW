package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.DawStateViewModel
import com.example.ui.MixerScreen
import com.example.ui.PianoRollGrid
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: DawStateViewModel = viewModel()
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = { Text("Mobile DAW") }
                        )
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val isPlaying by viewModel.isPlaying.collectAsState()
                        
                        Button(
                            onClick = { viewModel.togglePlay() },
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(if (isPlaying) "Stop Engine" else "Start Engine")
                        }
                        
                        MixerScreen(viewModel = viewModel)
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Tap grid to trigger samples",
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        PianoRollGrid(
                            onNoteTriggered = { channel ->
                                viewModel.triggerSample(channel)
                            },
                            modifier = Modifier.weight(1f).padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}
