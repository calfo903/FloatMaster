package com.floatmaster.apps.youtube

import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.floatmaster.model.FloatingWindow

@Composable
fun FloatingYouTubeContent(window: FloatingWindow) {
    var query by remember { mutableStateOf(window.url?.let { extractVideoId(it) } ?: "") }
    var input by remember { mutableStateOf(if (query.isNotBlank()) "https://www.youtube.com/watch?v=$query" else "") }
    var videoId by remember { mutableStateOf(query) }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f).height(48.dp),
                placeholder = { Text("YouTube URL or search", style = MaterialTheme.typography.bodySmall) },
                singleLine = true
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                videoId = extractVideoId(input) ?: input
                if (videoId.isBlank()) videoId = input
            }) { Text("Load") }
        }
        Divider()
        if (videoId.isBlank()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Default.PlayCircle, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(8.dp))
                    Text("Paste a YouTube link", style = MaterialTheme.typography.bodyMedium)
                    Text("e.g. https://youtu.be/dQw4w9WgXcQ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Lofi", "News", "Music").forEach { tag ->
                            SuggestionChip(onClick = {
                                input = tag; videoId = tag
                            }, label = { Text(tag) })
                        }
                    }
                }
            }
        } else {
            val embedUrl = if (videoId.length == 11 && !videoId.contains(" ")) {
                "https://www.youtube.com/embed/$videoId?autoplay=1&playsinline=1"
            } else {
                "https://www.youtube.com/results?search_query=$videoId"
            }
            AndroidView(factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                        settings.safeBrowsingEnabled = true // WHY: Safe Browsing
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = false // WHY: least privilege
                    settings.mediaPlaybackRequiresUserGesture = false
                    webViewClient = WebViewClient()
                    webChromeClient = WebChromeClient()
                    loadUrl(embedUrl)
                }
            }, modifier = Modifier.fillMaxSize())
        }
    }
}

private fun extractVideoId(url: String): String? {
    val regex = Regex("""(?:v=|youtu\.be/|embed/)([A-Za-z0-9_-]{11})""")
    return regex.find(url)?.groupValues?.get(1)
}
