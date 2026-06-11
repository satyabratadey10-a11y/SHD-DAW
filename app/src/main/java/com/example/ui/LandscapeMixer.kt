package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LandscapeMixer(
    viewModel: DawStateViewModel,
    modifier: Modifier = Modifier
) {
    val automationTracks by viewModel.automationTracks.collectAsState()
    
    Row(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF2A2A2E))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        automationTracks.forEach { track ->
            ChannelStrip(name = track.name)
        }
        // Mock a couple of more channel strips
        ChannelStrip(name = "Synth 1")
        ChannelStrip(name = "Drums")
    }
}

@Composable
fun ChannelStrip(name: String) {
    var volume by remember { mutableStateOf(0.7f) }
    var pan by remember { mutableStateOf(0.5f) }
    var levelLeft by remember { mutableStateOf(0.0f) }
    var levelRight by remember { mutableStateOf(0.0f) }

    // Dummy VU meter logic
    LaunchedEffect(volume) {
        levelLeft = volume * (if (pan < 0.5f) 1f else 1f - (pan - 0.5f) * 2f)
        levelRight = volume * (if (pan > 0.5f) 1f else pan * 2f)
    }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(100.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF333333), Color(0xFF1A1A1A))
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Pan Knob
        PanKnob(
            value = pan,
            onValueChange = { pan = it.coerceIn(0f, 1f) }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // VU Meters
        Row(
            modifier = Modifier.height(100.dp).fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            VuMeter(level = levelLeft, modifier = Modifier.weight(1f).padding(end = 2.dp))
            VuMeter(level = levelRight, modifier = Modifier.weight(1f).padding(start = 2.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        // Fader
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            SkeuomorphicFader(
                value = volume,
                onValueChange = { volume = it.coerceIn(0f, 1f) }
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(text = name, color = Color(0xFFB0B0B0), style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun PanKnob(value: Float, onValueChange: (Float) -> Unit) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .pointerInput(Unit) {
                var dragY = 0f
                detectDragGestures(
                    onDragStart = { dragY = 0f },
                    onDrag = { change, dragAmount ->
                        dragY -= dragAmount.y
                        val delta = dragY * 0.005f
                        onValueChange(value + delta)
                        dragY = 0f
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.width / 2 - 4.dp.toPx()
            
            // Outer Ring / Shadow
            drawCircle(
                color = Color.Black.copy(alpha = 0.5f),
                radius = radius + 2.dp.toPx(),
                center = center.copy(y = center.y + 2.dp.toPx())
            )
            
            // Brushed metal base
            val sweepGradient = Brush.sweepGradient(
                colors = listOf(
                    Color(0xFF606065), Color(0xFF909095), Color(0xFF606065),
                    Color(0xFF808085), Color(0xFF505055), Color(0xFF909095),
                    Color(0xFF606065)
                ),
                center = center
            )
            drawCircle(
                brush = sweepGradient,
                radius = radius,
                center = center
            )
            
            // Inner gradient
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF505055), Color(0xFF303035)),
                    center = center,
                    radius = radius * 0.8f
                ),
                radius = radius * 0.8f,
                center = center
            )
            
            // Indicator Dot
            val angle = -135f + (value * 270f)
            val angleRad = Math.toRadians(angle.toDouble())
            val dotRadius = radius * 0.6f
            val dotCenter = Offset(
                x = center.x + (dotRadius * sin(angleRad)).toFloat(),
                y = center.y - (dotRadius * cos(angleRad)).toFloat() // minus because y goes down
            )
            
            // Emissive glow behind dot
            drawIntoCanvas { canvas ->
                val paint = Paint().asFrameworkPaint()
                paint.color = Color(0xFF00FFCC).toArgb()
                paint.maskFilter = android.graphics.BlurMaskFilter(4.dp.toPx(), android.graphics.BlurMaskFilter.Blur.NORMAL)
                canvas.nativeCanvas.drawCircle(dotCenter.x, dotCenter.y, 3.dp.toPx(), paint)
            }
            
            drawCircle(
                color = Color(0xFF00FFCC), // Neon dot
                radius = 2.dp.toPx(),
                center = dotCenter
            )
        }
    }
}

@Composable
fun VuMeter(level: Float, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val segmentCount = 12
        val segmentHeight = size.height / segmentCount
        val gap = 2.dp.toPx()
        
        drawRect(color = Color(0xFF111111), size = size) // Tinted glass background
        
        for (i in 0 until segmentCount) {
            val segmentValue = 1f - (i.toFloat() / segmentCount)
            val isActive = level >= segmentValue
            
            val color = when {
                segmentValue > 0.8f -> if (isActive) Color(0xFFFF3333) else Color(0xFF441111) // Red peak
                segmentValue > 0.6f -> if (isActive) Color(0xFFFFCC00) else Color(0xFF443300) // Yellow warning
                else -> if (isActive) Color(0xFF00FF66) else Color(0xFF004411) // Green normal
            }
            
            val top = i * segmentHeight + gap / 2
            val h = segmentHeight - gap
            
            if (isActive) {
                // Emissive Blur Layer
                drawIntoCanvas { canvas ->
                    val paint = Paint().asFrameworkPaint()
                    paint.color = color.toArgb()
                    paint.maskFilter = android.graphics.BlurMaskFilter(3.dp.toPx(), android.graphics.BlurMaskFilter.Blur.NORMAL)
                    canvas.nativeCanvas.drawRect(
                        0f, top, size.width, top + h,
                        paint
                    )
                }
            }
            
            drawRect(
                color = color,
                topLeft = Offset(0f, top),
                size = Size(size.width, h)
            )
        }
        
        // Glass Overlay
        val glassGradient = Brush.linearGradient(
            colors = listOf(Color.White.copy(alpha = 0.1f), Color.White.copy(alpha = 0.0f)),
            start = Offset(0f, 0f),
            end = Offset(size.width, size.height)
        )
        drawRect(brush = glassGradient, size = size)
    }
}

@Composable
fun SkeuomorphicFader(value: Float, onValueChange: (Float) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                var dragY = 0f
                detectDragGestures(
                    onDragStart = { dragY = 0f },
                    onDrag = { change, dragAmount ->
                        dragY -= dragAmount.y
                        val height = size.height.toFloat()
                        // 1.0 is at top, 0.0 is at bottom
                        val delta = dragY / height
                        onValueChange(value + delta)
                        dragY = 0f
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val faderWidth = 12.dp.toPx()
            val trackX = size.width / 2 - faderWidth / 2
            
            // The Fader Track (Groove)
            // Recessed metallic channel
            val grooveGradient = Brush.horizontalGradient(
                colors = listOf(Color(0xFF222222), Color(0xFF0A0A0A), Color(0xFF050505), Color(0xFF333333)),
                startX = trackX,
                endX = trackX + faderWidth
            )
            drawRect(
                brush = grooveGradient,
                topLeft = Offset(trackX, 0f),
                size = Size(faderWidth, size.height)
            )
            
            // Inner track drop shadow (top / bottom edges)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.8f))
                ),
                topLeft = Offset(trackX, 0f),
                size = Size(faderWidth, size.height)
            )

            // The Fader Knob (Cap)
            val capHeight = 40.dp.toPx()
            val capWidth = 32.dp.toPx()
            val minCenterY = capHeight / 2
            val maxCenterY = size.height - capHeight / 2
            val range = maxCenterY - minCenterY
            
            val centerY = maxCenterY - (value * range)
            val capTopLeft = Offset(size.width / 2 - capWidth / 2, centerY - capHeight / 2)
            
            // Cap Drop Shadow
            drawIntoCanvas { canvas ->
                val paint = Paint().asFrameworkPaint()
                paint.color = Color.Black.copy(alpha = 0.7f).toArgb()
                paint.maskFilter = android.graphics.BlurMaskFilter(8.dp.toPx(), android.graphics.BlurMaskFilter.Blur.NORMAL)
                canvas.nativeCanvas.drawRoundRect(
                    android.graphics.RectF(capTopLeft.x, capTopLeft.y + 10.dp.toPx(), capTopLeft.x + capWidth, capTopLeft.y + capHeight + 10.dp.toPx()),
                    4.dp.toPx(), 4.dp.toPx(),
                    paint
                )
            }
            
            // Cap Base (Brushed metal block)
            val capGradient = Brush.verticalGradient(
                colors = listOf(Color(0xFF909095), Color(0xFFB0B0B5), Color(0xFF606065)),
                startY = capTopLeft.y,
                endY = capTopLeft.y + capHeight
            )
            drawRoundRect(
                brush = capGradient,
                topLeft = capTopLeft,
                size = Size(capWidth, capHeight),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
            
            // Horizontal ribbing lines
            val ribbingCount = 5
            val ribGap = (capHeight * 0.4f) / ribbingCount
            val startRibY = centerY - (ribGap * (ribbingCount / 2f))
            for (i in 0 until ribbingCount) {
                val ry = startRibY + (i * ribGap)
                // Dark crevice
                drawLine(
                    color = Color(0xFF303030),
                    start = Offset(capTopLeft.x + 4.dp.toPx(), ry),
                    end = Offset(capTopLeft.x + capWidth - 4.dp.toPx(), ry),
                    strokeWidth = 1.dp.toPx()
                )
                // Highlight
                drawLine(
                    color = Color.White.copy(alpha = 0.5f),
                    start = Offset(capTopLeft.x + 4.dp.toPx(), ry + 1.dp.toPx()),
                    end = Offset(capTopLeft.x + capWidth - 4.dp.toPx(), ry + 1.dp.toPx()),
                    strokeWidth = 0.5.dp.toPx()
                )
            }
            
            // Illuminated Center Indicator Line
            val indicatorWidth = capWidth * 0.8f
            val indicatorX = size.width / 2 - indicatorWidth / 2
            
            // Emissive glow
            drawIntoCanvas { canvas ->
                val paint = Paint().asFrameworkPaint()
                paint.color = Color.White.toArgb()
                paint.maskFilter = android.graphics.BlurMaskFilter(2.dp.toPx(), android.graphics.BlurMaskFilter.Blur.NORMAL)
                canvas.drawRect(
                    indicatorX, centerY - 1.dp.toPx(), indicatorX + indicatorWidth, centerY + 1.dp.toPx(),
                    Paint().apply { asFrameworkPaint().set(paint) }
                )
            }
            // Core bright line
            drawRect(
                color = Color.White,
                topLeft = Offset(indicatorX, centerY - 0.5.dp.toPx()),
                size = Size(indicatorWidth, 1.dp.toPx())
            )
        }
    }
}
