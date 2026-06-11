package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Info
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
    val engineTickState = viewModel.engineTick.collectAsState()
    LaunchedEffect(isPlaying) {
        // Auto scroll could be implemented here manually observing the flow if needed
    }

    Column(modifier = modifier.fillMaxSize().background(Color(0xFF1E1E24))) {
        
        // Toolbar
        Row(modifier = Modifier.fillMaxWidth().height(48.dp).background(Color(0xFF2B2B36)).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { isSelectionMode = !isSelectionMode },
                colors = ButtonDefaults.buttonColors(containerColor = if (isSelectionMode) Color(0xFF4B8EEB) else Color.DarkGray)) {
                Text("Select Mode: ${if (isSelectionMode) "ON" else "OFF"}")
            }
            Spacer(modifier = Modifier.width(16.dp))
            IconButton(onClick = { viewModel.togglePlay() }) {
                if (isPlaying) {
                    Canvas(modifier = Modifier.size(24.dp)) {
                        drawRect(Color.White, topLeft = Offset(6.dp.toPx(), 4.dp.toPx()), size = Size(4.dp.toPx(), 16.dp.toPx()))
                        drawRect(Color.White, topLeft = Offset(14.dp.toPx(), 4.dp.toPx()), size = Size(4.dp.toPx(), 16.dp.toPx()))
                    }
                } else {
                    Icon(androidx.compose.material.icons.Icons.Filled.PlayArrow, contentDescription = "Play", tint = Color.White)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text("Snap: ${snapMode.name}", color = Color.White)
        }

        // Piano Roll Area Container
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            
            // Skeuomorphic Piano Keys Rail
            Canvas(modifier = Modifier.width(60.dp).fillMaxHeight().background(Color(0xFF1E1E24))) {
                for (i in 0 until 128) {
                    val y = i * noteHeightPx - viewportScrollY
                    if (y in -noteHeightPx..size.height) {
                        val isBlack = isBlackKey(127 - i)
                        if (!isBlack) {
                            val whiteGradient = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color(0xFFF0F0F0), Color(0xFFCCCCCC)),
                                startY = y, endY = y + noteHeightPx
                            )
                            drawRect(
                                brush = whiteGradient,
                                topLeft = Offset(0f, y),
                                size = Size(size.width, noteHeightPx)
                            )
                            drawLine(
                                color = Color(0xFF888888),
                                start = Offset(0f, y + noteHeightPx),
                                end = Offset(size.width, y + noteHeightPx),
                                strokeWidth = 0.5.dp.toPx()
                            )
                        }
                    }
                }
                // Draw black keys on top
                for (i in 0 until 128) {
                    val y = i * noteHeightPx - viewportScrollY
                    if (y in -noteHeightPx..size.height) {
                        val isBlack = isBlackKey(127 - i)
                        if (isBlack) {
                            val blackGradient = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color(0xFF444444), Color(0xFF111111)),
                                startY = y, endY = y + noteHeightPx
                            )
                            // Shadow onto adjacent white keys
                            drawRect(
                                color = Color.Black.copy(alpha = 0.6f),
                                topLeft = Offset(size.width * 0.6f, y + 4f),
                                size = Size(4.dp.toPx(), noteHeightPx + 2f)
                            )
                            drawRect(
                                brush = blackGradient,
                                topLeft = Offset(0f, y),
                                size = Size(size.width * 0.6f, noteHeightPx)
                            )
                            // Inner gloss highlight for black key
                            drawLine(
                                color = Color.White.copy(alpha = 0.2f),
                                start = Offset(1f, y + 1f),
                                end = Offset(size.width * 0.6f - 1f, y + 1f),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                    }
                }
            }

            // Piano Roll Grid & Notes Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
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
                                
                                val hitNote = notes.firstOrNull { note ->
                                    clickTick in note.startTick..(note.startTick + note.durationTicks) &&
                                    clickNote == note.noteValue
                                }
                                if (hitNote != null) {
                                    viewModel.toggleNoteSelection(hitNote.id)
                                } else {
                                    viewModel.addNote(clickNote, clickTick)
                                }
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
                        // 3D Beveled glass edges
                        val highlightBrush = androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(Color.White.copy(alpha=0.6f), Color.Transparent),
                            start = Offset(x, y+2f), end = Offset(x + width*0.5f, y + 2f + (noteHeightPx-4f)*0.5f)
                        )
                        drawRoundRect(
                            brush = highlightBrush,
                            topLeft = Offset(x+1.5f, y + 3.5f),
                            size = Size(width-3f, noteHeightPx - 7f),
                            cornerRadius = CornerRadius(2.dp.toPx()),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                        val shadowBrush = androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha=0.5f)),
                            start = Offset(x + width*0.5f, y + 2f + (noteHeightPx-4f)*0.5f), end = Offset(x + width, y + noteHeightPx - 2f)
                        )
                        drawRoundRect(
                            brush = shadowBrush,
                            topLeft = Offset(x+1.5f, y + 3.5f),
                            size = Size(width-3f, noteHeightPx - 7f),
                            cornerRadius = CornerRadius(2.dp.toPx()),
                            style = Stroke(width = 1.5.dp.toPx())
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

                if (x + width > 0 && y > -noteHeightPx) {
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
                                var accumulatedDeltaX = 0f
                                detectDragGestures(
                                    onDragStart = { accumulatedDeltaX = 0f },
                                    onDrag = { change, dragAmount ->
                                        // Accumulated delta logic
                                        accumulatedDeltaX += dragAmount.x
                                        if (kotlin.math.abs(accumulatedDeltaX) >= pixelsPerTick / 2f) {
                                            val deltaTicks = (accumulatedDeltaX / pixelsPerTick).toInt()
                                            viewModel.resizeSelectedNotesDelta(deltaTicks)
                                            accumulatedDeltaX -= (deltaTicks * pixelsPerTick)
                                        }
                                    }
                                )
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
            Spacer(modifier = Modifier.fillMaxSize().drawBehind {
                val currentTick = engineTickState.value
                val playheadPx = currentTick * pixelsPerTick - viewportScrollX
                // auto-scroll
                if (isPlaying) {
                    val viewWidth = 1000f 
                    // This is handled in composition ideally, but we isolate playhead
                }

                if (playheadPx in 0f..size.width) {
                    drawLine(
                        color = Color.White,
                        start = Offset(playheadPx, 0f),
                        end = Offset(playheadPx, size.height),
                        strokeWidth = 1.5.dp.toPx()
                    )
                    // Playhead Cap (Inverted Triangle)
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(playheadPx - 12f, 0f)
                        lineTo(playheadPx + 12f, 0f)
                        lineTo(playheadPx, 20f)
                        close()
                    }
                    drawPath(path = path, color = Color.White)
                }
            })
        }
    }

        // Velocity Drawer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(Color(0xFF202028))
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val x = change.position.x + viewportScrollX
                        val y = change.position.y
                        
                        val clickTick = (x / pixelsPerTick).toInt()
                        val heightPx = 100.dp.toPx()
                        val velocity = (1f - (y / heightPx)).coerceIn(0f, 1f)
                        
                        val hitNote = notes.firstOrNull { note ->
                            clickTick in note.startTick..(note.startTick + note.durationTicks)
                        }
                        hitNote?.let {
                            viewModel.setNoteVelocity(it.id, velocity)
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            val x = offset.x + viewportScrollX
                            val y = offset.y
                            val heightPx = 100.dp.toPx()
                            val velocity = (1f - (y / heightPx)).coerceIn(0f, 1f)
                            val clickTick = (x / pixelsPerTick).toInt()
                            val hitNote = notes.firstOrNull { note ->
                                clickTick in note.startTick..(note.startTick + note.durationTicks)
                            }
                            hitNote?.let {
                                viewModel.setNoteVelocity(it.id, velocity)
                            }
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                notes.forEach { note ->
                    val x = note.startTick * pixelsPerTick - viewportScrollX
                    val width = 10.dp.toPx() // Fader bar width
                    
                    if (x + width > 0 && x < size.width) {
                        val barHeight = note.velocity * size.height
                        drawRect(
                            color = if (note.isSelected) Color(0xFFFF4081) else Color(0xFF00E5FF),
                            topLeft = Offset(x, size.height - barHeight),
                            size = Size(width, barHeight)
                        )
                    }
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
