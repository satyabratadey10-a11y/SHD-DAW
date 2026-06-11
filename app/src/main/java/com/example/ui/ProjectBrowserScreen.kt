package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import androidx.compose.ui.platform.LocalContext

@Composable
fun ClayCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp)
            .clickable(onClick = onClick)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cornerRadius = 16.dp.toPx()
            
            // Wide merged outer drop shadow
            drawIntoCanvas { canvas ->
                val paint = Paint().asFrameworkPaint()
                paint.color = Color.Black.copy(alpha = 0.15f).toArgb()
                paint.maskFilter = android.graphics.BlurMaskFilter(16.dp.toPx(), android.graphics.BlurMaskFilter.Blur.NORMAL)
                canvas.nativeCanvas.drawRoundRect(
                    android.graphics.RectF(0f, 8.dp.toPx(), size.width, size.height + 8.dp.toPx()),
                    cornerRadius, cornerRadius,
                    paint
                )
            }

            // Sharp dark shadow for local ambient occlusion
            drawIntoCanvas { canvas ->
                val paint = Paint().asFrameworkPaint()
                paint.color = Color.Black.copy(alpha = 0.25f).toArgb()
                paint.maskFilter = android.graphics.BlurMaskFilter(4.dp.toPx(), android.graphics.BlurMaskFilter.Blur.NORMAL)
                canvas.nativeCanvas.drawRoundRect(
                    android.graphics.RectF(0f, 2.dp.toPx(), size.width, size.height + 2.dp.toPx()),
                    cornerRadius, cornerRadius,
                    paint
                )
            }

            // Main Card Body
            drawRoundRect(
                color = backgroundColor,
                size = size,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius)
            )

            // Inner Specular Highlight
            drawIntoCanvas { canvas ->
                val path = Path().apply {
                    addRoundRect(androidx.compose.ui.geometry.RoundRect(
                        left = 0f, top = 0f, right = size.width, bottom = size.height,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius)
                    ))
                }
                
                canvas.save()
                canvas.nativeCanvas.clipPath(android.graphics.Path().apply {
                    addRoundRect(
                        android.graphics.RectF(0f, 0f, size.width, size.height),
                        cornerRadius, cornerRadius,
                        android.graphics.Path.Direction.CW
                    )
                })
                
                val highlightPaint = Paint().asFrameworkPaint()
                highlightPaint.color = Color.White.copy(alpha = 0.5f).toArgb()
                highlightPaint.maskFilter = android.graphics.BlurMaskFilter(6.dp.toPx(), android.graphics.BlurMaskFilter.Blur.NORMAL)
                highlightPaint.style = android.graphics.Paint.Style.STROKE
                highlightPaint.strokeWidth = 6.dp.toPx()

                canvas.nativeCanvas.drawPath(
                    android.graphics.Path().apply {
                        addRoundRect(
                            android.graphics.RectF(-2.dp.toPx(), -2.dp.toPx(), size.width, size.height),
                            cornerRadius, cornerRadius,
                            android.graphics.Path.Direction.CW
                        )
                    },
                    highlightPaint
                )
                
                canvas.restore()
            }
        }
        Box(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun ProjectBrowserScreen(
    viewModel: DawStateViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var projects by remember { mutableStateOf<List<File>>(emptyList()) }
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("PROJECTS", "STORE", "FILES")
    
    val snackbarHostState = remember { SnackbarHostState() }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val tempZip = File(context.cacheDir, "imported.zip")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(tempZip).use { output ->
                            input.copyTo(output)
                        }
                    }
                    viewModel.importCustomPack(context, tempZip)
                    tempZip.delete()
                    snackbarHostState.showSnackbar("Successfully registered Pack: new samples loaded.")
                } catch (e: Exception) {
                    e.printStackTrace()
                    snackbarHostState.showSnackbar("Import failed.")
                }
            }
        }
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 0 || selectedTab == 2) {
            withContext(Dispatchers.IO) {
                val dir = context.getExternalFilesDir(null)
                val files = dir?.listFiles { file -> file.name.endsWith(".msp") }
                projects = files?.toList() ?: emptyList()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFE5E5EB),
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(24.dp)) {
            Text("Browser", style = MaterialTheme.typography.headlineMedium, color = Color(0xFF33333A))
            Spacer(modifier = Modifier.height(16.dp))

            TabRow(selectedTabIndex = selectedTab, containerColor = Color.Transparent, contentColor = Color(0xFF90C2E7)) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, style = MaterialTheme.typography.titleMedium, color = if (selectedTab == index) Color(0xFF33333A) else Color(0xFF666670)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            when (selectedTab) {
                0 -> { // PROJECTS
                    ClayCard(
                        backgroundColor = Color(0xFF90C2E7), // Matte sky blue
                        onClick = { viewModel.createNewProject() }
                    ) {
                        Text("Create New Project", style = MaterialTheme.typography.titleMedium, color = Color.White, modifier = Modifier.align(androidx.compose.ui.Alignment.Center))
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Recent Projects", style = MaterialTheme.typography.titleMedium, color = Color(0xFF666670))
                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(projects) { file ->
                            ClayCard(
                                backgroundColor = Color(0xFF646B7D), // Deep warm slate
                                onClick = {
                                    coroutineScope.launch {
                                        viewModel.loadProject(file)
                                    }
                                }
                            ) {
                                Column {
                                    Text(file.name, style = MaterialTheme.typography.titleMedium, color = Color.White)
                                    Text("Size: ${file.length() / 1024} KB", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
                                }
                            }
                        }
                    }
                }
                1 -> { // STORE
                    val downloadStates by viewModel.downloadStates.collectAsState()
                    val expansionPacks = listOf(
                        Triple("Phonk Drum Kit Vol. 1", "24.5 MB", "https://example.com/phonk.zip"),
                        Triple("Vintage Synth Patches", "12.1 MB", "https://example.com/vintage.zip"),
                        Triple("Jumpstyle Bass Pack", "34.2 MB", "https://example.com/jumpstyle.zip")
                    )

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(expansionPacks) { pack ->
                            val url = pack.third
                            val downloadState = downloadStates[url]

                            ClayCard(
                                backgroundColor = Color(0xFFC7B1E6), // Soft lavender
                                onClick = { }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().align(androidx.compose.ui.Alignment.Center),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("📦 ${pack.first}", style = MaterialTheme.typography.titleMedium, color = Color.White)
                                        Text("Size: ${pack.second}", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
                                        
                                        if (downloadState?.isDownloading == true) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            LinearProgressIndicator(
                                                progress = { downloadState.progress },
                                                modifier = Modifier.fillMaxWidth().height(4.dp),
                                                color = Color.White,
                                                trackColor = Color.White.copy(alpha = 0.3f),
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    
                                    val buttonText = when {
                                        downloadState?.isInstalled == true -> "Installed"
                                        downloadState?.isExtracting == true -> "Extracting..."
                                        downloadState?.isDownloading == true -> "${(downloadState.progress * 100).toInt()}%"
                                        else -> "Get"
                                    }

                                    Button(
                                        onClick = {
                                            if (downloadState == null || (!downloadState.isDownloading && !downloadState.isExtracting && !downloadState.isInstalled)) {
                                                viewModel.downloadAssetPack(context, url, pack.first)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                                        enabled = (downloadState == null)
                                    ) {
                                        Text(buttonText, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> { // FILES
                    ClayCard(
                        backgroundColor = Color(0xFFF4A261), // Soft clay orange
                        onClick = { importLauncher.launch("application/zip") }
                    ) {
                        Text("Import Custom Pack (.zip)", style = MaterialTheme.typography.titleMedium, color = Color.White, modifier = Modifier.align(androidx.compose.ui.Alignment.Center))
                    }
                }
            }
        }
    }
}
