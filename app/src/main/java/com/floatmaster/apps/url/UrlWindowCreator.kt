package com.floatmaster.apps.url

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.floatmaster.model.WindowType
import com.floatmaster.service.FloatingWindowManager

@Composable
fun UrlCreatorDialog(manager: FloatingWindowManager, onDismiss: () -> Unit) {
    var url by remember { mutableStateOf("https://") }
    var title by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create floating window from URL") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("URL") }, placeholder = { Text("https://example.com") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Text("Any URL can be opened as a resizable floating browser window. Supports web apps, docs, dashboards.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("https://google.com", "https://notion.so", "https://youtube.com").forEach { quick ->
                        SuggestionChip(onClick = { url = quick }, label = { Text(quick.removePrefix("https://").take(10)) })
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val finalUrl = if (url.startsWith("http")) url else "https://$url"
                manager.create(WindowType.URL_WINDOW, title = title.ifBlank { finalUrl }, url = finalUrl)
                onDismiss()
            }, enabled = url.isNotBlank() && url.contains(".")) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
