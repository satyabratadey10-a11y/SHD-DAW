package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Composable
fun HorizontalPlaylist(
    viewModel: DawStateViewModel,
    modifier: Modifier = Modifier
) {
    val automationTracks by viewModel.automationTracks.collectAsState()
    val engineTick by viewModel.engineTick.collectAsState()
    
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val armedTrack by viewModel.armedTrackId
    
    val pixelsPerTick = 0.2f
    val viewportScrollX = 0f // Would typically be a state
    
    Column(modifier = modifier.fillMaxSize().background(Color(0xFF1E1E24))) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text("Playlist Arrangement", color = Color.White)
            Spacer(modifier = Modifier.width(16.dp))
            Button(onClick = { viewModel.togglePlay() }) {
                Text(if (isPlaying) "Stop" else "Play")
            }
            Spacer(modifier = Modifier.width(8.dp))
            val context = androidx.compose.ui.platform.LocalContext.current
            Button(onClick = { viewModel.toggleRecord(context) }, colors = ButtonDefaults.buttonColors(containerColor = if (isRecording) Color.Red else Color.DarkGray)) {
                Text("Record")
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text("Audio Track 1", color = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { 
                    if (armedTrack == "track1") viewModel.armedTrackId.value = null 
                    else viewModel.armedTrackId.value = "track1" 
                },
                colors = ButtonDefaults.buttonColors(containerColor = if (armedTrack == "track1") Color.Red else Color.Gray),
                modifier = Modifier.size(32.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("R", color = Color.White)
            }
        }
        
        if (isRecording && armedTrack != null) {
            Box(modifier = Modifier.fillMaxWidth().height(100.dp).padding(16.dp)) {
                WaveformVisualizer(viewModel)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(automationTracks) { track ->
                AutomationTrackLane(
                    track = track,
                    viewModel = viewModel,
                    pixelsPerTick = pixelsPerTick,
                    viewportScrollX = viewportScrollX
                )
            }
        }
    }
}

@Composable
fun AutomationTrackLane(
    track: AutomationTrackModel,
    viewModel: DawStateViewModel,
    pixelsPerTick: Float,
    viewportScrollX: Float
) {
    var showPopupForNodeId by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Text("Auto: ${track.name}", color = Color.LightGray, modifier = Modifier.padding(horizontal = 8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(Color(0xFF282832))
                .pointerInput(track.id) {
                    detectTapGestures(
                        onTap = { offset ->
                            val x = offset.x + viewportScrollX
                            val clickTick = (x / pixelsPerTick).toLong()
                            val heightPx = size.height.toFloat()
                            val value = (1f - (offset.y / heightPx)).coerceIn(0f, 1f)
                            
                            viewModel.addAutomationNode(track.id, clickTick, value, 1)
                        },
                        onLongPress = { offset ->
                            val hitRadius = 24.dp.toPx()
                            val hitNode = track.nodes.find { node ->
                                val nodeX = node.targetTick * pixelsPerTick - viewportScrollX
                                val nodeY = (1f - node.value) * size.height.toFloat()
                                val distSq = (offset.x - nodeX) * (offset.x - nodeX) + (offset.y - nodeY) * (offset.y - nodeY)
                                distSq <= hitRadius * hitRadius
                            }
                            if (hitNode != null) {
                                showPopupForNodeId = hitNode.id
                            }
                        }
                    )
                }
                .pointerInput(track.id) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            // Optional tracking of active drag node, done implicitly by matching drag offset
                        },
                        onDrag = { change, dragAmount ->
                            val hitRadius = 36.dp.toPx()
                            val heightPx = size.height.toFloat()
                            
                            val startPos = change.previousPosition
                            val hitNode = track.nodes.find { node ->
                                val nodeX = node.targetTick * pixelsPerTick - viewportScrollX
                                val nodeY = (1f - node.value) * heightPx
                                val distSq = (startPos.x - nodeX) * (startPos.x - nodeX) + (startPos.y - nodeY) * (startPos.y - nodeY)
                                distSq <= hitRadius * hitRadius
                            }
                            
                            if (hitNode != null) {
                                val newX = change.position.x + viewportScrollX
                                val newY = change.position.y
                                val newTick = (newX / pixelsPerTick).coerceAtLeast(0f).toLong()
                                val newValue = (1f - (newY / heightPx)).coerceIn(0f, 1f)
                                viewModel.updateAutomationNode(track.id, hitNode.id, newTick, newValue)
                            }
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val path = Path()
                
                track.nodes.forEachIndexed { index, node ->
                    val x = node.targetTick * pixelsPerTick - viewportScrollX
                    val y = (1f - node.value) * size.height
                    
                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        val prevNode = track.nodes[index - 1]
                        val prevX = prevNode.targetTick * pixelsPerTick - viewportScrollX
                        val prevY = (1f - prevNode.value) * size.height
                        
                        if (prevNode.curveType == 0) { // Linear
                            path.lineTo(x, y)
                        } else if (prevNode.curveType == 1) { // Smooth (Bezier / spline)
                            // Catmull-Rom or cubic Bezier representation
                            // Here we'll use a simple ease-in-out cubic bezier
                            val cp1x = prevX + (x - prevX) * 0.5f
                            val cp1y = prevY
                            val cp2x = prevX + (x - prevX) * 0.5f
                            val cp2y = y
                            path.cubicTo(cp1x, cp1y, cp2x, cp2y, x, y)
                        } else if (prevNode.curveType == 2) { // Hold
                            path.lineTo(x, prevY)
                            path.lineTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }
                    }
                }
                
                drawPath(
                    path = path,
                    color = Color(0xFF00FF7F),
                    style = Stroke(width = 2.dp.toPx())
                )
                
                track.nodes.forEach { node ->
                    val x = node.targetTick * pixelsPerTick - viewportScrollX
                    val y = (1f - node.value) * size.height
                    drawCircle(
                        color = Color(0xFF00FF7F),
                        radius = 6.dp.toPx(),
                        center = Offset(x, y)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.dp.toPx(),
                        center = Offset(x, y)
                    )
                }
            }
            
            // Popup for node editing
            if (showPopupForNodeId != null) {
                // simple custom popup layout
                Box(modifier = Modifier.align(androidx.compose.ui.Alignment.Center).background(Color.DarkGray).padding(8.dp)) {
                    Column {
                        Text("Edit Curve Type", color = Color.White)
                        Row {
                            Button(onClick = { 
                                viewModel.setAutomationNodeCurveType(track.id, showPopupForNodeId!!, 0)
                                showPopupForNodeId = null 
                            }) { Text("Linear") }
                            Button(onClick = { 
                                viewModel.setAutomationNodeCurveType(track.id, showPopupForNodeId!!, 1)
                                showPopupForNodeId = null 
                            }) { Text("Smooth") }
                            Button(onClick = { 
                                viewModel.setAutomationNodeCurveType(track.id, showPopupForNodeId!!, 2)
                                showPopupForNodeId = null 
                            }) { Text("Hold") }
                            Button(onClick = { 
                                viewModel.removeAutomationNode(track.id, showPopupForNodeId!!)
                                showPopupForNodeId = null 
                            }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Delete") }
                        }
                        Button(onClick = { showPopupForNodeId = null }) { Text("Cancel") }
                    }
                }
            }
        }
    }
}
