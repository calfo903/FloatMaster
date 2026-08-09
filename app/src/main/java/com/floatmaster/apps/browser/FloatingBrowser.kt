package com.floatmaster.apps.browser

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.floatmaster.apps.aichat.AI_DESKTOP_UA
import com.floatmaster.data.BrowserHistoryRepository
import com.floatmaster.model.FloatingWindow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class BrowserTab(val id: String, var url: String, var title: String = "New Tab")

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun FloatingBrowserContent(window: FloatingWindow) {
    val context = LocalContext.current
    val historyRepo = remember { BrowserHistoryRepository(context) }
    val scope = rememberCoroutineScope()

    var tabs by remember { mutableStateOf(listOf(BrowserTab("1", window.url ?: "https://www.google.com"))) }
    var activeTabId by remember { mutableStateOf(tabs.first().id) }
    var inputUrl by remember { mutableStateOf(tabs.first().url) }
    var progress by remember { mutableStateOf(0) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var desktopMode by remember { mutableStateOf(false) } // WHY: Desktop UA toggle — fixes sites that force mobile
    var showHistory by remember { mutableStateOf(false) }
    var history by remember { mutableStateOf(emptyList<com.floatmaster.data.HistoryEntry>()) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }

    val activeTab = tabs.find { it.id == activeTabId } ?: tabs.first()

    // WHY: Observe persistent history reactively
    LaunchedEffect(showHistory) {
        if (showHistory) {
            historyRepo.getHistory().let { res ->
                if (res is com.floatmaster.util.Result.Success) history = res.value
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        // Address bar
        Row(Modifier.fillMaxWidth().padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { webViewRef?.goBack() }, modifier = Modifier.size(36.dp), enabled = canGoBack) { Icon(Icons.Default.ArrowBack, "Back") }
            IconButton(onClick = { webViewRef?.goForward() }, modifier = Modifier.size(36.dp), enabled = canGoForward) { Icon(Icons.Default.ArrowForward, "Forward") }
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

        // Toolbar: History + Desktop + tabs count
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                FilledTonalButton(onClick = { showHistory = true }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp), modifier = Modifier.height(32.dp)) {
                    Icon(Icons.Default.History, null, Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)); Text("History", style = MaterialTheme.typography.labelSmall)
                }
                FilterChip(
                    selected = desktopMode,
                    onClick = {
                        desktopMode = !desktopMode // WHY: Toggle UA + viewport
                        webViewRef?.let { wv ->
                            wv.settings.userAgentString = if (desktopMode) AI_DESKTOP_UA else null // WHY: null restores default mobile UA
                            wv.settings.useWideViewPort = desktopMode || true // WHY: desktop needs wide viewport
                            wv.settings.loadWithOverviewMode = true
                            wv.reload() // WHY: UA only applies on reload
                        }
                    },
                    label = { Text(if (desktopMode) "Desktop ✓" else "Mobile", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = { Icon(if (desktopMode) Icons.Default.Computer else Icons.Default.PhoneAndroid, null, Modifier.size(14.dp)) }
                )
            }
            Text("${tabs.size} tabs · ${if (desktopMode) "Desktop" else "Mobile"}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }

        if (progress in 1..99) LinearProgressIndicator(progress = progress / 100f, modifier = Modifier.fillMaxWidth())

        // Tabs row
        LazyRow(Modifier.fillMaxWidth().padding(horizontal = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(tabs) { tab ->
                FilterChip(
                    selected = tab.id == activeTabId,
                    onClick = { activeTabId = tab.id; inputUrl = tab.url },
                    label = { Text(tab.title.take(16), style = MaterialTheme.typography.labelSmall) },
                    trailingIcon = {
                        if (tabs.size > 1) IconButton(onClick = {
                            tabs = tabs.filterNot { it.id == tab.id }
                            if (activeTabId == tab.id) {
                                activeTabId = tabs.first().id
                                inputUrl = tabs.first().url
                            }
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
                    settings.safeBrowsingEnabled = true // WHY: Safe Browsing
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = false // WHY: least privilege
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW // WHY: MITM
                    if (desktopMode) settings.userAgentString = AI_DESKTOP_UA
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            url?.let { inputUrl = it; activeTab.url = it }
                            canGoBack = view?.canGoBack() ?: false
                            canGoForward = view?.canGoForward() ?: false
                        }
                        override fun onPageFinished(view: WebView?, url: String?) {
                            url?.let {
                                inputUrl = it
                                canGoBack = view?.canGoBack() ?: false
                                canGoForward = view?.canGoForward() ?: false
                                // WHY: Persist history — survives window close; capped 200, deduped 5s
                                scope.launch { historyRepo.add(it, view?.title ?: it) }
                            }
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
                // WHY: Apply desktop UA change immediately
                val desiredUA = if (desktopMode) AI_DESKTOP_UA else null
                if (wv.settings.userAgentString != desiredUA) {
                    // WebView returns default UA string, not null, so check via desktopMode flag
                    wv.settings.userAgentString = desiredUA
                }
                if (wv.url != activeTab.url) wv.loadUrl(activeTab.url)
                webViewRef = wv
                canGoBack = wv.canGoBack()
                canGoForward = wv.canGoForward()
            },
            modifier = Modifier.fillMaxSize()
        )
    }

    // History bottom sheet
    if (showHistory) {
        ModalBottomSheet(onDismissRequest = { showHistory = false }) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("History", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            scope.launch { historyRepo.clear(); history = emptyList() }
                        }) { Icon(Icons.Default.DeleteSweep, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Clear") }
                        IconButton(onClick = { showHistory = false }) { Icon(Icons.Default.Close, null) }
                    }
                }
                Text("${history.size} entries · persistent across restarts · capped 200", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.height(12.dp))
                if (history.isEmpty()) {
                    Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        Text("No history yet — browse to build it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                } else {
                    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 400.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(history, key = { it.url + it.timestamp }) { entry ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                tonalElevation = 1.dp,
                                modifier = Modifier.fillMaxWidth().clickable {
                                    activeTab.url = entry.url
                                    inputUrl = entry.url
                                    webViewRef?.loadUrl(entry.url)
                                    showHistory = false
                                }
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Language, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(entry.title.take(40), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(entry.url, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(entry.timestamp)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                    }
                                    IconButton(onClick = { scope.launch { historyRepo.delete(entry.url); history = history.filterNot { it.url == entry.url } } }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.Delete, null, Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
