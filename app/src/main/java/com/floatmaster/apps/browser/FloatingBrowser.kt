package com.floatmaster.apps.browser

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.floatmaster.model.FloatingWindow

data class BrowserTab(val id: String, var url: String, var title: String = "New Tab")

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun FloatingBrowserContent(window: FloatingWindow) {
    val context = LocalContext.current
    var tabs by remember { mutableStateOf(listOf(BrowserTab("1", window.url ?: "https://www.google.com"))) }
    var activeTabId by remember { mutableStateOf(tabs.first().id) }
    var inputUrl by remember { mutableStateOf(tabs.first().url) }
    var progress by remember { mutableStateOf(0) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    val activeTab = tabs.find { it.id == activeTabId } ?: tabs.first()

    Column(Modifier.fillMaxSize()) {
        // Address bar
        Row(Modifier.fillMaxWidth().padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { webViewRef?.goBack() }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.ArrowBack, "Back") }
            IconButton(onClick = { webViewRef?.goForward() }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.ArrowForward, "Forward") }
            IconButton(onClick = { webViewRef?.reload() }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Refresh, "Reload") }
            OutlinedTextField(
                value = inputUrl,
                onValueChange = { inputUrl = it },
                modifier = Modifier.weight(1f).height(44.dp),
                singleLine = true,
                placeholder = { Text("Search or URL", style = MaterialTheme.typography.bodySmall) },
                trailingIcon = {
                    IconButton(onClick = {
                        val url = if (inputUrl.contains(".")) {
                            if (inputUrl.startsWith("http")) inputUrl else "https://$inputUrl"
                        } else "https://www.google.com/search?q=$inputUrl"
                        activeTab.url = url
                        webViewRef?.loadUrl(url)
                    }) { Icon(Icons.Default.Search, null, Modifier.size(18.dp)) }
                },
                shape = MaterialTheme.shapes.extraLarge
            )
            IconButton(onClick = {
                val newId = (tabs.size + 1).toString()
                tabs = tabs + BrowserTab(newId, "https://www.google.com")
                activeTabId = newId
                inputUrl = "https://www.google.com"
            }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Add, "New tab") }
        }
        if (progress in 1..99) LinearProgressIndicator(progress = progress / 100f, modifier = Modifier.fillMaxWidth())
        // Tabs
        LazyRow(Modifier.fillMaxWidth().padding(horizontal = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(tabs) { tab ->
                FilterChip(
                    selected = tab.id == activeTabId,
                    onClick = { activeTabId = tab.id; inputUrl = tab.url },
                    label = { Text(tab.title.take(16), style = MaterialTheme.typography.labelSmall) },
                    trailingIcon = {
                        if (tabs.size > 1) IconButton(onClick = {
                            tabs = tabs.filterNot { it.id == tab.id }
                            if (activeTabId == tab.id) activeTabId = tabs.first().id
                        }, modifier = Modifier.size(16.dp)) { Icon(Icons.Default.Close, null, Modifier.size(10.dp)) }
                    }
                )
            }
        }
        // WebView
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = true
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            url?.let { inputUrl = it; activeTab.url = it }
                        }
                        override fun onPageFinished(view: WebView?, url: String?) {
                            url?.let { inputUrl = it }
                        }
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) { progress = newProgress }
                        override fun onReceivedTitle(view: WebView?, title: String?) { title?.let { activeTab.title = it } }
                    }
                    loadUrl(activeTab.url)
                    webViewRef = this
                }
            },
            update = { wv ->
                if (wv.url != activeTab.url) wv.loadUrl(activeTab.url)
                webViewRef = wv
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
