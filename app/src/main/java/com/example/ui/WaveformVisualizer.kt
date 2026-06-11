package com.example.ui

import android.graphics.BlurMaskFilter
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb

@Composable
fun WaveformVisualizer(viewModel: DawStateViewModel, modifier: Modifier = Modifier) {
    val waveformData by viewModel.waveformFlow.collectAsState()

    Spacer(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                if (waveformData.isEmpty()) return@drawBehind

                val maxCanvasHeight = size.height
                val width = size.width
                val barWidth = width / waveformData.size

                drawIntoCanvas { canvas ->
                    // Outer soft glow pass
                    val paintGlow = Paint().asFrameworkPaint().apply {
                        color = Color(0xFFFF00FF).toArgb() // vibrant neon pink
                        maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = barWidth * 0.8f
                        strokeCap = android.graphics.Paint.Cap.ROUND
                    }

                    // Core stroke
                    val paintCore = Paint().asFrameworkPaint().apply {
                        color = Color(0xFF00FFFF).toArgb() // cyan core
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = barWidth * 0.8f
                        strokeCap = android.graphics.Paint.Cap.ROUND
                    }

                    for (i in waveformData.indices) {
                        val sampleValue = waveformData[i].coerceIn(0.0f, 1.0f)
                        val lineHeight = sampleValue * maxCanvasHeight
                        val startX = i * barWidth + (barWidth / 2f)

                        val startY = (maxCanvasHeight - lineHeight) / 2f
                        val stopY = startY + lineHeight

                        if (lineHeight > 0) {
                            canvas.nativeCanvas.drawLine(startX, startY, startX, stopY, paintGlow)
                            canvas.nativeCanvas.drawLine(startX, startY, startX, stopY, paintCore)
                        }
                    }
                }
            }
    )
}
