package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import org.json.JSONObject
import org.json.JSONArray

@Composable
fun ProjectBrowserScreen(
    viewModel: DawStateViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var projects by remember { mutableStateOf<List<File>>(emptyList()) }

    // Load available projects
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val dir = context.getExternalFilesDir(null)
            val files = dir?.listFiles { file -> file.name.endsWith(".msp") }
            projects = files?.toList() ?: emptyList()
        }
    }

    Column(modifier = modifier.fillMaxSize().background(Color(0xFF1E1E24)).padding(16.dp)) {
        Text("Project Browser", style = MaterialTheme.typography.titleLarge, color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { viewModel.createNewProject() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("New Project")
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(projects) { file ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    onClick = {
                        coroutineScope.launch {
                            viewModel.loadProject(file)
                        }
                    }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(file.name, style = MaterialTheme.typography.titleMedium)
                        Text("Size: ${file.length() / 1024} KB", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
