package com.floatmaster.apps.youtube

import android.graphics.Rect
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.floatmaster.model.FloatingWindow
import com.floatmaster.util.PipHelper

private val YOUTUBE_HOSTS = setOf("www.youtube.com", "youtube.com", "m.youtube.com", "www.youtube-nocookie.com")

@Composable
fun FloatingYouTubeContent(window: FloatingWindow) {
    val context = LocalContext.current
    var query by remember { mutableStateOf(window.url?.let { extractVideoId(it) }.orEmpty()) }
    var input by remember { mutableStateOf(if (query.isNotBlank()) "https://youtu.be/$query" else "") }
    var videoId by remember { mutableStateOf(query) }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f).height(48.dp), placeholder = { Text("YouTube URL or search", style = MaterialTheme.typography.bodySmall) }, singleLine = true)
            Spacer(Modifier.width(8.dp))
            Button(onClick = { videoId = extractVideoId(input).orEmpty().ifBlank { input.trim().take(200) } }) { Text("Load") }
            FilledTonalButton(onClick = {
                if (context is androidx.activity.ComponentActivity) PipHelper.enterPipForVideo(context, Rect(0, 0, 380, 214), isPlaying = true)
            }) { Icon(Icons.Default.PictureInPicture, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("PiP", style = MaterialTheme.typography.labelSmall) }
        }
        HorizontalDivider()

        if (videoId.isBlank()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Default.PlayCircle, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(8.dp))
                    Text("Paste a YouTube link", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            val embedUrl = if (videoId.matches(Regex("[A-Za-z0-9_-]{11}"))) {
                "https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&playsinline=1"
            } else {
                "https://www.youtube.com/results?search_query=${Uri.encode(videoId)}"
            }
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.safeBrowsingEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        settings.mediaPlaybackRequiresUserGesture = true
                        settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                        settings.javaScriptCanOpenWindowsAutomatically = false
                        settings.setSupportMultipleWindows(false)
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                if (request == null || !request.isForMainFrame) return false
                                val host = request.url.host?.lowercase()
                                return if (host in YOUTUBE_HOSTS && request.url.scheme == "https") {
                                    view?.loadUrl(request.url.toString())
                                    true
                                } else {
                                    view?.stopLoading()
                                    true
                                }
                            }
                        }
                        webChromeClient = WebChromeClient()
                        loadUrl(embedUrl)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

private fun extractVideoId(url: String): String? = Regex("""(?:v=|youtu\.be/|embed/)([A-Za-z0-9_-]{11})""").find(url)?.groupValues?.get(1)
