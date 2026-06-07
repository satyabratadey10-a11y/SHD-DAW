package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun PianoRollGrid(onNoteTriggered: (Int) -> Unit, modifier: Modifier = Modifier) {
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val rows = 8
                    val cellHeight = size.height / rows
                    val row = (offset.y / cellHeight).toInt()
                    val channel = row % 4
                    onNoteTriggered(channel)
                }
            }
    ) {
        val rows = 8
        val cols = 16
        val cellWidth = size.width / cols
        val cellHeight = size.height / rows

        for (i in 0..rows) {
            drawLine(
                color = gridColor,
                start = Offset(0f, i * cellHeight),
                end = Offset(size.width, i * cellHeight),
                strokeWidth = 2f
            )
        }
        for (i in 0..cols) {
            drawLine(
                color = gridColor,
                start = Offset(i * cellWidth, 0f),
                end = Offset(i * cellWidth, size.height),
                strokeWidth = 2f
            )
        }
    }
}
