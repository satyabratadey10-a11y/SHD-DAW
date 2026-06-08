package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

@Composable
fun PianoRollEditor(
    viewModel: DawStateViewModel,
    modifier: Modifier = Modifier
) {
    val clips by viewModel.clips.collectAsState()
    val activeClip = clips.firstOrNull() ?: return
    val notes = activeClip.notes
    val engineTick by viewModel.engineTick.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val snapMode by viewModel.snapMode.collectAsState()

    var isSelectionMode by remember { mutableStateOf(false) }
    var marqueeRect by remember { mutableStateOf<Rect?>(null) }
    
    val TICKS_PER_BAR = 1920f
    val PIXELS_PER_BAR = 400f
    val pixelsPerTick = PIXELS_PER_BAR / TICKS_PER_BAR
    val NOTE_HEIGHT = 24.dp
    
    val density = LocalDensity.current
    val noteHeightPx = with(density) { NOTE_HEIGHT.toPx() }
    val handleRadiusPx = with(density) { 8.dp.toPx() }

    var viewportScrollX by remember { mutableStateOf(0f) }
    var viewportScrollY by remember { mutableStateOf(60f * noteHeightPx) } // Start around middle C

    val gridWidth = 4 * PIXELS_PER_BAR // 4 bars for demo
    val gridHeight = 128 * noteHeightPx

    // Playhead auto-scroll
    val playheadX = engineTick * pixelsPerTick
    LaunchedEffect(playheadX, isPlaying) {
        if (isPlaying) {
            val viewWidth = 1000f // Approximate width, ideally use onGloballyPositioned
            if (playheadX > viewportScrollX + viewWidth * 0.8f) {
                viewportScrollX = max(0f, playheadX - viewWidth * 0.2f)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF1E1E24))) {
        
        // Toolbar
        Row(modifier = Modifier.fillMaxWidth().height(48.dp).background(Color(0xFF2B2B36)).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { isSelectionMode = !isSelectionMode },
                colors = ButtonDefaults.buttonColors(containerColor = if (isSelectionMode) Color(0xFF4B8EEB) else Color.DarkGray)) {
                Text("Select Mode: ${if (isSelectionMode) "ON" else "OFF"}")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(onClick = { viewModel.togglePlay() }) {
                Text(if (isPlaying) "Stop" else "Play")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text("Snap: ${snapMode.name}", color = Color.White)
        }

        // Piano Roll Grid & Notes Area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 48.dp)
                .pointerInput(isSelectionMode) {
                    if (isSelectionMode) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val x = offset.x + viewportScrollX
                                val y = offset.y + viewportScrollY
                                marqueeRect = Rect(x, y, x, y)
                            },
                            onDrag = { change, dragAmount ->
                                marqueeRect?.let { rect ->
                                    val currentX = rect.right + dragAmount.x
                                    val currentY = rect.bottom + dragAmount.y
                                    marqueeRect = Rect(rect.left, rect.top, currentX, currentY)
                                }
                            },
                            onDragEnd = {
                                marqueeRect?.let { rect ->
                                    val top = min(rect.top, rect.bottom)
                                    val bottom = max(rect.top, rect.bottom)
                                    val left = min(rect.left, rect.right)
                                    val right = max(rect.left, rect.right)
                                    
                                    val minNote = 127 - (bottom / noteHeightPx).toInt()
                                    val maxNote = 127 - (top / noteHeightPx).toInt()
                                    val minTick = (left / pixelsPerTick).toInt()
                                    val maxTick = (right / pixelsPerTick).toInt()
                                    
                                    viewModel.selectAllNotesInMarquee(minTick, maxTick, minNote, maxNote)
                                }
                                marqueeRect = null
                            },
                            onDragCancel = { marqueeRect = null }
                        )
                    } else {
                        detectTapGestures(
                            onTap = { offset ->
                                val x = offset.x + viewportScrollX
                                val y = offset.y + viewportScrollY
                                val clickTick = (x / pixelsPerTick).toInt()
                                val clickNote = 127 - (y / noteHeightPx).toInt()
                                viewModel.addNote(clickNote, clickTick)
                            }
                        )
                    }
                }
        ) {
            // Background Grid Layer 
            Canvas(modifier = Modifier.fillMaxSize()) {
                val startBar = (viewportScrollX / PIXELS_PER_BAR).toInt()
                val visibleBars = (size.width / PIXELS_PER_BAR).toInt() + 2
                
                // Draw horizontal rules
                for (i in 0 until 128) {
                    val y = i * noteHeightPx - viewportScrollY
                    if (y in -noteHeightPx..size.height) {
                        val isBlackKey = isBlackKey(127 - i)
                        drawRect(
                            color = if (isBlackKey) Color(0xFF24242C) else Color(0xFF2E2E38),
                            topLeft = Offset(0f, y),
                            size = Size(size.width, noteHeightPx)
                        )
                        drawLine(
                            color = Color(0xFF18181D),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1f
                        )
                    }
                }
                
                // Draw vertical rules
                for (b in 0..visibleBars) {
                    val bar = startBar + b
                    val x = bar * PIXELS_PER_BAR - viewportScrollX
                    if (x in 0f..size.width) {
                        drawLine(
                            color = Color(0xFF4A4A58),
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = 2f
                        )
                    }
                    // Quarter note lines
                    for (q in 1..3) {
                        val qx = x + q * (PIXELS_PER_BAR / 4)
                        if (qx in 0f..size.width) {
                            drawLine(
                                color = Color(0xFF383842),
                                start = Offset(qx, 0f),
                                end = Offset(qx, size.height),
                                strokeWidth = 1f
                            )
                        }
                    }
                }
            }

            // Notes Drawing Layer
            Canvas(modifier = Modifier.fillMaxSize()) {
                notes.forEach { note ->
                    val x = note.startTick * pixelsPerTick - viewportScrollX
                    val y = (127 - note.noteValue) * noteHeightPx - viewportScrollY
                    val width = note.durationTicks * pixelsPerTick
                    
                    if (x + width > 0 && x < size.width && y > -noteHeightPx && y < size.height) {
                        val color = if (note.isSelected) Color(0xFFFF4081) else Color(0xFF00E5FF)
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(x, y + 2f), // Small padding
                            size = Size(width, noteHeightPx - 4f),
                            cornerRadius = CornerRadius(4.dp.toPx())
                        )
                        if (note.isSelected) {
                            // Draw boundary
                            drawRoundRect(
                                color = Color.White,
                                topLeft = Offset(x, y + 2f),
                                size = Size(width, noteHeightPx - 4f),
                                cornerRadius = CornerRadius(4.dp.toPx()),
                                style = Stroke(width = 2f)
                            )
                            // Draw Right Handle (visual)
                            drawCircle(
                                color = Color.White,
                                radius = handleRadiusPx,
                                center = Offset(x + width, y + noteHeightPx / 2f)
                            )
                        }
                    }
                }

                // Marquee Selection Box Layer
                marqueeRect?.let { rect ->
                    val screenLeft = min(rect.left, rect.right) - viewportScrollX
                    val screenTop = min(rect.top, rect.bottom) - viewportScrollY
                    val screenRight = max(rect.left, rect.right) - viewportScrollX
                    val screenBottom = max(rect.top, rect.bottom) - viewportScrollY
                    
                    val w = screenRight - screenLeft
                    val h = screenBottom - screenTop
                    
                    if (w > 0 && h > 0) {
                        drawRect(
                            color = Color.White.copy(alpha = 0.2f),
                            topLeft = Offset(screenLeft, screenTop),
                            size = Size(w, h)
                        )
                        drawRect(
                            color = Color.White,
                            topLeft = Offset(screenLeft, screenTop),
                            size = Size(w, h),
                            style = Stroke(width = 2f)
                        )
                    }
                }
            }

            // Interactive Overlays
            var selectedBoundsContext by remember { mutableStateOf<Rect?>(null) }
            
            // Interaction nodes for handles
            notes.filter { it.isSelected }.forEach { note ->
                val x = note.startTick * pixelsPerTick - viewportScrollX
                val y = (127 - note.noteValue) * noteHeightPx - viewportScrollY
                val width = note.durationTicks * pixelsPerTick

                if (x + width > 0 && x < size.width && y > -noteHeightPx && y < size.height) {
                    // Update bounding box for contextual menu
                    LaunchedEffect(note) {
                        if (selectedBoundsContext == null) {
                            selectedBoundsContext = Rect(x, y, x + width, y + noteHeightPx)
                        } else {
                            val cur = selectedBoundsContext!!
                            selectedBoundsContext = Rect(
                                min(cur.left, x), min(cur.top, y),
                                max(cur.right, x + width), max(cur.bottom, y + noteHeightPx)
                            )
                        }
                    }

                    // Touch target for modifying duration via right handle
                    Box(
                        modifier = Modifier
                            .offset { IntOffset((x + width - handleRadiusPx*2).toInt(), (y + noteHeightPx/2f - handleRadiusPx*2).toInt()) }
                            .size((handleRadiusPx*4).dp / density.density)
                            .pointerInput(note.id) {
                                detectDragGestures { change, dragAmount ->
                                    val deltaTicks = (dragAmount.x / pixelsPerTick).toInt()
                                    viewModel.resizeSelectedNotesDelta(deltaTicks)
                                }
                            }
                    )
                }
            }
            
            if (notes.none { it.isSelected }) {
                selectedBoundsContext = null
            } else {
                selectedBoundsContext?.let { bounds ->
                    // Floating Contextual Toolbar
                    Row(
                        modifier = Modifier
                            .offset { IntOffset(bounds.left.toInt(), (bounds.top - 50.dp.toPx()).toInt()) }
                            .background(Color.White, RoundedCornerShape(16.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ContextButton("Copy") { viewModel.copySelectedNotes() }
                        ContextButton("Delete") { viewModel.deleteSelectedNotes() }
                        ContextButton("Snap") { viewModel.snapSelectedNotes() }
                        ContextButton("More...") { /* More Actions */ }
                    }
                }
            }

            // Playhead Layer (Isolated)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val px = playheadX - viewportScrollX
                if (px in 0f..size.width) {
                    drawLine(
                        color = Color(0xFF64B5F6),
                        start = Offset(px, 0f),
                        end = Offset(px, size.height),
                        strokeWidth = 3f
                    )
                    // Playhead Cap (Inverted Triangle)
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(px - 12f, 0f)
                        lineTo(px + 12f, 0f)
                        lineTo(px, 20f)
                        close()
                    }
                    drawPath(path = path, color = Color(0xFF64B5F6))
                }
            }
        }
    }
}

@Composable
fun ContextButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .clip(CircleShape)
            .background(Color(0xFFEEEEEE))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text, color = Color.Black, fontSize = 12.sp)
    }
}

fun isBlackKey(note: Int): Boolean {
    val octaveNote = note % 12
    return octaveNote == 1 || octaveNote == 3 || octaveNote == 6 || octaveNote == 8 || octaveNote == 10
}
