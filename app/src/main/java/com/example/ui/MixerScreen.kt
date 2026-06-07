package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp

@Composable
fun VerticalSlider(value: Float, onValueChange: (Float) -> Unit) {
    Box(
        modifier = Modifier
            .width(48.dp)
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1.5f,
            modifier = Modifier
                .width(200.dp)
                .height(48.dp)
                .rotate(-90f)
        )
    }
}

@Composable
fun MixerScreen(viewModel: DawStateViewModel) {
    val volumes by viewModel.volumes.collectAsState()
    val mutes by viewModel.mutes.collectAsState()
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        volumes.forEachIndexed { index, volume ->
            val isMuted = mutes[index]
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("CH ${index + 1}")
                Spacer(modifier = Modifier.height(8.dp))
                VerticalSlider(
                    value = volume,
                    onValueChange = { viewModel.setVolume(index, it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.toggleMute(index) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isMuted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("M")
                }
                Spacer(modifier = Modifier.height(4.dp))
                Button(onClick = { viewModel.triggerSample(index) }) {
                    Text("Hit")
                }
            }
        }
    }
}
