package com.floatmaster.apps.clipboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import com.floatmaster.data.ClipboardRepository
import com.floatmaster.model.FloatingWindow
import kotlinx.coroutines.delay

@Composable
fun FloatingClipboardContent(window: FloatingWindow) {
    val context = LocalContext.current
    val repo = remember { ClipboardRepository(context) }
    var items by remember { mutableStateOf(repo.getAll()) }
    var autoCapture by remember { mutableStateOf(true) }

    // Poll clipboard every 1.5s when auto-capture on
    LaunchedEffect(autoCapture) {
        while (autoCapture) {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = cm.primaryClip?.getItemAt(0)?.text?.toString()
            if (!clip.isNullOrBlank()) repo.addIfNew(clip)
            items = repo.getAll()
            delay(1500)
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = autoCapture, onCheckedChange = { autoCapture = it })
                Spacer(Modifier.width(8.dp))
                Text("Auto-capture", style = MaterialTheme.typography.labelMedium)
            }
            IconButton(onClick = { repo.clear(); items = emptyList() }) { Icon(Icons.Default.DeleteSweep, "Clear") }
        }
        Divider()
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No clipboard history yet\nCopy something to capture it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline) }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items) { entry ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(entry.text.take(300), style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(6.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(entry.timeAgo(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (entry.isPinned) Icon(Icons.Default.PushPin, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                    IconButton(onClick = {
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cm.setPrimaryClip(ClipData.newPlainText("FloatMaster", entry.text))
                                    }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.ContentCopy, "Copy", Modifier.size(16.dp)) }
                                    IconButton(onClick = { repo.togglePin(entry.id); items = repo.getAll() }, modifier = Modifier.size(32.dp)) {
                                        Icon(if (entry.isPinned) Icons.Default.Star else Icons.Default.StarBorder, "Pin", Modifier.size(16.dp))
                                    }
                                    IconButton(onClick = { repo.delete(entry.id); items = repo.getAll() }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Delete, "Delete", Modifier.size(16.dp)) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
