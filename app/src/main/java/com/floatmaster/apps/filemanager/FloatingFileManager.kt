package com.floatmaster.apps.filemanager

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.floatmaster.model.FloatingWindow
import java.io.File

@Composable
fun FloatingFileManagerContent(window: FloatingWindow) {
    val context = LocalContext.current
    var currentDir by remember { mutableStateOf(File(context.getExternalFilesDir(null)?.parentFile?.parentFile?.absolutePath ?: "/storage/emulated/0")) }
    // fallback to app-specific dir if not accessible, user can navigate up
    var files by remember { mutableStateOf(listFilesSafe(currentDir)) }
    var path by remember { mutableStateOf(currentDir.absolutePath) }

    fun navigate(dir: File) {
        if (dir.isDirectory && dir.canRead()) {
            currentDir = dir
            path = dir.absolutePath
            files = listFilesSafe(dir)
        }
    }

    Column(Modifier.fillMaxSize()) {
        // Breadcrumb + actions
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { currentDir.parentFile?.let { navigate(it) } }) { Icon(Icons.Default.ArrowUpward, "Up") }
            Text(path.takeLast(32), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            IconButton(onClick = { files = listFilesSafe(currentDir) }) { Icon(Icons.Default.Refresh, "Refresh") }
        }
        Divider()
        if (files.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Empty or no permission\nTry /Download or app storage", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(files.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))) { file ->
                    ListItem(
                        headlineContent = { Text(file.name, maxLines = 1) },
                        supportingContent = { Text(if (file.isDirectory) "${file.listFiles()?.size ?: 0} items" else "${file.length() / 1024} KB", style = MaterialTheme.typography.labelSmall) },
                        leadingContent = {
                            Icon(
                                when {
                                    file.isDirectory -> Icons.Default.Folder
                                    file.name.endsWith(".pdf", true) -> Icons.Default.PictureAsPdf
                                    file.name.endsWith(".jpg", true) || file.name.endsWith(".png", true) -> Icons.Default.Image
                                    file.name.endsWith(".mp4", true) -> Icons.Default.VideoFile
                                    file.name.endsWith(".mp3", true) -> Icons.Default.AudioFile
                                    else -> Icons.Default.Description
                                }, null, tint = if (file.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        },
                        trailingContent = {
                            if (!file.isDirectory) IconButton(onClick = { /* share via FileProvider */ }) { Icon(Icons.Default.Share, null, Modifier.size(18.dp)) }
                        },
                        modifier = Modifier.clickable {
                            if (file.isDirectory) navigate(file) else {
                                // open with system chooser — in real app use FileProvider + Intent.ACTION_VIEW
                            }
                        }
                    )
                    Divider()
                }
            }
        }
    }
}

private fun listFilesSafe(dir: File): List<File> {
    return try { dir.listFiles()?.toList() ?: emptyList() } catch (_: Exception) { emptyList() }
}
