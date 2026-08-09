package com.floatmaster.apps.browser

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Environment
import android.view.View
import android.webkit.*
import android.webkit.URLUtil
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.floatmaster.apps.aichat.AI_DESKTOP_UA
import com.floatmaster.data.BrowserHistoryRepository
import com.floatmaster.model.FloatingWindow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// WHY: KDoc + data class for tab — immutable id, mutable url/title for WebView callbacks
data class BrowserTab(val id: String, var url: String, var title: String = "New Tab", var favicon: Bitmap? = null)

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun FloatingBrowserContent(window: FloatingWindow) {
    val context = LocalContext.current
    val historyRepo = remember { BrowserHistoryRepository(context) }
    val scope = rememberCoroutineScope()
    val clipboard = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    var tabs by remember { mutableStateOf(listOf(BrowserTab("1", window.url ?: "https://www.google.com"))) }
    var activeTabId by remember { mutableStateOf(tabs.first().id) }
    var inputUrl by remember { mutableStateOf(tabs.first().url) }
    var progress by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var desktopMode by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var showTabsSheet by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var history by remember { mutableStateOf(emptyList<com.floatmaster.data.HistoryEntry>()) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    var showFind by remember { mutableStateOf(false) }
    var darkMode by remember { mutableStateOf(false) }
    var sslWarning by remember { mutableStateOf<Pair<SslErrorHandler, SslError>?>(null) }

    // WHY: file upload — WebChromeClient needs ValueCallback
    var fileChooserCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val fileChooserLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        fileChooserCallback?.onReceiveValue(if (uri != null) arrayOf(uri) else arrayOf())
        fileChooserCallback = null
    }

    val activeTab = tabs.find { it.id == activeTabId } ?: tabs.first()

    // WHY: observe history for sheet
    LaunchedEffect(showHistory) {
        if (showHistory) historyRepo.getHistory().let { if (it is com.floatmaster.util.Result.Success) history = it.value }
    }

    Column(Modifier.fillMaxSize()) {
        // === Address bar + controls ===
        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { webViewRef?.goBack() }, enabled = canGoBack, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.ArrowBack, "Back") }
            IconButton(onClick = { webViewRef?.goForward() }, enabled = canGoForward, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.ArrowForward, "Forward") }
            if (isLoading) {
                IconButton(onClick = { webViewRef?.stopLoading() }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Close, "Stop") }
            } else {
                IconButton(onClick = { webViewRef?.reload() }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Refresh, "Reload") }
            }
            OutlinedTextField(
                value = inputUrl,
                onValueChange = { inputUrl = it },
                modifier = Modifier.weight(1f).height(44.dp),
                singleLine = true,
                placeholder = { Text("Search or enter URL", style = MaterialTheme.typography.bodySmall) },
                leadingIcon = { Icon(Icons.Default.Language, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline) },
                trailingIcon = {
                    Row {
                        if (inputUrl.isNotEmpty()) IconButton(onClick = { inputUrl = "" }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Clear, null, Modifier.size(14.dp)) }
                        IconButton(onClick = {
                            val url = if (inputUrl.contains(".")) {
                                if (inputUrl.startsWith("http")) inputUrl else "https://$inputUrl"
                            } else "https://www.google.com/search?q=${Uri.encode(inputUrl)}"
                            activeTab.url = url
                            webViewRef?.loadUrl(url)
                        }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Search, null, Modifier.size(16.dp)) }
                    }
                },
                shape = MaterialTheme.shapes.extraLarge
            )
            IconButton(onClick = { showTabsSheet = true }, modifier = Modifier.size(36.dp)) {
                BadgedBox(badge = { Badge { Text("${tabs.size}") } }) { Icon(Icons.Default.Tab, "Tabs") }
            }
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.MoreVert, "Menu") }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text("Share") }, onClick = {
                        showMenu = false
                        val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, webViewRef?.url ?: inputUrl) }
                        context.startActivity(Intent.createChooser(send, "Share page"))
                    }, leadingIcon = { Icon(Icons.Default.Share, null) })
                    DropdownMenuItem(text = { Text("Copy URL") }, onClick = {
                        showMenu = false
                        clipboard.setPrimaryClip(ClipData.newPlainText("URL", webViewRef?.url ?: inputUrl))
                        Toast.makeText(context, "URL copied", Toast.LENGTH_SHORT).show()
                    }, leadingIcon = { Icon(Icons.Default.ContentCopy, null) })
                    DropdownMenuItem(text = { Text("Open in browser") }, onClick = {
                        showMenu = false
                        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webViewRef?.url ?: inputUrl)).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) } catch (_: Exception) {}
                    }, leadingIcon = { Icon(Icons.Default.OpenInNew, null) })
                    DropdownMenuItem(text = { Text(if (showFind) "Hide Find" else "Find in page") }, onClick = { showMenu = false; showFind = !showFind }, leadingIcon = { Icon(Icons.Default.Search, null) })
                    DropdownMenuItem(text = { Text(if (desktopMode) "Mobile site" else "Desktop site") }, onClick = {
                        showMenu = false
                        desktopMode = !desktopMode
                        webViewRef?.let { wv ->
                            wv.settings.userAgentString = if (desktopMode) AI_DESKTOP_UA else null
                            wv.settings.useWideViewPort = true
                            wv.settings.loadWithOverviewMode = true
                            wv.reload()
                        }
                    }, leadingIcon = { Icon(if (desktopMode) Icons.Default.PhoneAndroid else Icons.Default.Computer, null) })
                    DropdownMenuItem(text = { Text(if (darkMode) "Light mode" else "Dark mode") }, onClick = {
                        showMenu = false
                        darkMode = !darkMode // WHY: WebView forceDark on Q+
                        webViewRef?.let { wv ->
                            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                                @Suppress("DEPRECATION")
                                WebSettingsCompat.setForceDark(wv.settings, if (darkMode) WebSettingsCompat.FORCE_DARK_ON else WebSettingsCompat.FORCE_DARK_OFF)
                            }
                            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)) {
                                WebSettingsCompat.setForceDarkStrategy(wv.settings, WebSettingsCompat.DARK_STRATEGY_WEB_THEME_DARKENING_ONLY)
                            }
                        }
                    }, leadingIcon = { Icon(Icons.Default.DarkMode, null) })
                    DropdownMenuItem(text = { Text("Zoom in") }, onClick = { showMenu = false; webViewRef?.zoomIn() }, leadingIcon = { Icon(Icons.Default.ZoomIn, null) })
                    DropdownMenuItem(text = { Text("Zoom out") }, onClick = { showMenu = false; webViewRef?.zoomOut() }, leadingIcon = { Icon(Icons.Default.ZoomOut, null) })
                    DropdownMenuItem(text = { Text("History") }, onClick = { showMenu = false; showHistory = true }, leadingIcon = { Icon(Icons.Default.History, null) })
                    DropdownMenuItem(text = { Text("Clear cache") }, onClick = {
                        showMenu = false
                        webViewRef?.clearCache(true)
                        CookieManager.getInstance().removeAllCookies(null)
                        CookieManager.getInstance().flush()
                        Toast.makeText(context, "Cache & cookies cleared", Toast.LENGTH_SHORT).show()
                    }, leadingIcon = { Icon(Icons.Default.CleaningServices, null) })
                }
            }
        }

        // Find in page bar
        if (showFind) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = findQuery,
                    onValueChange = {
                        findQuery = it
                        if (it.isNotEmpty()) webViewRef?.findAllAsync(it) else webViewRef?.clearMatches()
                    },
                    modifier = Modifier.weight(1f).height(40.dp),
                    placeholder = { Text("Find in page", style = MaterialTheme.typography.labelSmall) },
                    singleLine = true
                )
                IconButton(onClick = { webViewRef?.findNext(false) }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.KeyboardArrowUp, null, Modifier.size(16.dp)) }
                IconButton(onClick = { webViewRef?.findNext(true) }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.KeyboardArrowDown, null, Modifier.size(16.dp)) }
                IconButton(onClick = { showFind = false; webViewRef?.clearMatches(); findQuery = "" }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) }
            }
        }

        // Chips row: History + Desktop status + Zoom
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(selected = desktopMode, onClick = {
                desktopMode = !desktopMode
                webViewRef?.let { wv ->
                    wv.settings.userAgentString = if (desktopMode) AI_DESKTOP_UA else null
                    wv.reload()
                }
            }, label = { Text(if (desktopMode) "Desktop ✓" else "Desktop", style = MaterialTheme.typography.labelSmall) }, leadingIcon = { Icon(Icons.Default.Computer, null, Modifier.size(14.dp)) })
            AssistChip(onClick = { webViewRef?.zoomIn() }, label = { Text("+", style = MaterialTheme.typography.labelSmall) }, leadingIcon = { Icon(Icons.Default.ZoomIn, null, Modifier.size(14.dp)) }, modifier = Modifier.height(32.dp))
            AssistChip(onClick = { webViewRef?.zoomOut() }, label = { Text("−", style = MaterialTheme.typography.labelSmall) }, leadingIcon = { Icon(Icons.Default.ZoomOut, null, Modifier.size(14.dp)) }, modifier = Modifier.height(32.dp))
            Spacer(Modifier.weight(1f))
            Text("${tabs.size} tabs · ${if (isLoading) "Loading…" else activeTab.title.take(14)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        if (isLoading) LinearProgressIndicator(progress = { if (progress in 1..99) progress / 100f else 0f }, modifier = Modifier.fillMaxWidth())

        // Tabs row (horizontal)
        LazyRow(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(tabs, key = { it.id }) { tab ->
                FilterChip(
                    selected = tab.id == activeTabId,
                    onClick = {
                        activeTabId = tab.id
                        inputUrl = tab.url
                    },
                    label = { Row(verticalAlignment = Alignment.CenterVertically) {
                        tab.favicon?.let { bm ->
                            // WHY: favicon in chip — when minimized to bubble, show same favicon
                            androidx.compose.foundation.Image(
                                bitmap = bm.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.size(12.dp).clip(CircleShape)
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(tab.title.take(12), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    } },
                    trailingIcon = {
                        if (tabs.size > 1) IconButton(onClick = {
                            val wasActive = tab.id == activeTabId
                            tabs = tabs.filterNot { it.id == tab.id }
                            if (wasActive) {
                                activeTabId = tabs.first().id
                                inputUrl = tabs.first().url
                            }
                        }, modifier = Modifier.size(16.dp)) { Icon(Icons.Default.Close, null, Modifier.size(10.dp)) }
                    }
                )
            }
            item {
                IconButton(onClick = {
                    val newId = (tabs.maxOfOrNull { it.id.toIntOrNull() ?: 0 } ?: 0 + 1).toString()
                    val nt = BrowserTab(newId, "https://www.google.com")
                    tabs = tabs + nt
                    activeTabId = newId
                    inputUrl = nt.url
                }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Add, "New tab") }
            }
        }

        // WebView — hardware accelerated
        Box(Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        // WHY: hardware acceleration for smooth scroll/60fps
                        setLayerType(View.LAYER_TYPE_HARDWARE, null)
                        // Cookie + cache
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        settings.javaScriptEnabled = true // WHY: required for modern sites
                        settings.domStorageEnabled = true // WHY: localStorage
                        settings.allowFileAccess = false // WHY: least privilege, but allow file chooser via onShowFileChooser
                        settings.allowContentAccess = true // WHY: needed for file upload content://
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        settings.builtInZoomControls = true // WHY: pinch zoom
                        settings.displayZoomControls = false // WHY: hide ugly buttons, use our +/− chips
                        settings.setSupportZoom(true)
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW // WHY: MITM
                        settings.safeBrowsingEnabled = true // WHY: Safe Browsing
                        settings.javaScriptCanOpenWindowsAutomatically = true
                        settings.setSupportMultipleWindows(false)
                        settings.cacheMode = WebSettings.LOAD_DEFAULT
                        // WHY: dark mode via WebViewFeature — no flash
                        if (darkMode && WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                            WebSettingsCompat.setForceDark(settings, WebSettingsCompat.FORCE_DARK_ON)
                        }
                        if (desktopMode) settings.userAgentString = AI_DESKTOP_UA

                        // Downloads
                        setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                            try {
                                val req = DownloadManager.Request(Uri.parse(url)).apply {
                                    setMimeType(mimetype)
                                    addRequestHeader("User-Agent", userAgent)
                                    addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url) ?: "")
                                    setDescription("Downloading…")
                                    setTitle(URLUtil.guessFileName(url, contentDisposition, mimetype))
                                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                    setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimetype))
                                    setAllowedOverMetered(true)
                                    setAllowedOverRoaming(true)
                                }
                                val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                dm.enqueue(req)
                                Toast.makeText(context, "Downloading…", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                // WHY: fallback to external browser
                                try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) } catch (_: Exception) {}
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isLoading = true
                                url?.let { inputUrl = it; activeTab.url = it }
                                favicon?.let { activeTab.favicon = it }
                            }
                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                progress = 100
                                url?.let {
                                    inputUrl = it
                                    canGoBack = view?.canGoBack() ?: false
                                    canGoForward = view?.canGoForward() ?: false
                                    scope.launch { historyRepo.add(it, view?.title ?: it) }
                                }
                            }
                            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                                // WHY: Never ignore silently — show warning dialog
                                if (handler != null && error != null) sslWarning = handler to error
                                else handler?.cancel()
                            }
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                // WHY: handle tel: mailto: intent:
                                val url = request?.url?.toString() ?: return false
                                if (url.startsWith("tel:") || url.startsWith("mailto:") || url.startsWith("intent:")) {
                                    try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }); return true } catch (_: Exception) { return false }
                                }
                                return false
                            }
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                progress = newProgress
                                isLoading = newProgress in 1..99
                                canGoBack = view?.canGoBack() ?: false
                                canGoForward = view?.canGoForward() ?: false
                            }
                            override fun onReceivedTitle(view: WebView?, title: String?) { title?.let { activeTab.title = it } }
                            override fun onReceivedIcon(view: WebView?, icon: Bitmap?) { icon?.let { activeTab.favicon = it } }
                            // WHY: file upload <input type=file>
                            override fun onShowFileChooser(webView: WebView?, filePathCallback: ValueCallback<Array<Uri>>?, fileChooserParams: FileChooserParams?): Boolean {
                                fileChooserCallback?.onReceiveValue(null)
                                fileChooserCallback = filePathCallback
                                try {
                                    if (fileChooserParams?.acceptTypes?.any { it.contains("image") } == true) fileChooserLauncher.launch("image/*")
                                    else fileChooserLauncher.launch("*/*")
                                } catch (_: Exception) { fileChooserCallback = null; return false }
                                return true
                            }
                        }
                        // WHY: find in page uses WebView API
                        loadUrl(activeTab.url)
                        webViewRef = this
                    }
                },
                update = { wv ->
                    val desiredUA = if (desktopMode) AI_DESKTOP_UA else null
                    if (wv.settings.userAgentString != desiredUA && desktopMode) wv.settings.userAgentString = desiredUA
                    if (!desktopMode && wv.settings.userAgentString == AI_DESKTOP_UA) wv.settings.userAgentString = null
                    if (wv.url != activeTab.url) wv.loadUrl(activeTab.url)
                    webViewRef = wv
                    canGoBack = wv.canGoBack()
                    canGoForward = wv.canGoForward()
                },
                modifier = Modifier.fillMaxSize()
            )

            // Zoom overlay buttons (pinch already works)
            Column(Modifier.align(Alignment.BottomEnd).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallFloatingActionButton(onClick = { webViewRef?.zoomIn() }, containerColor = MaterialTheme.colorScheme.surfaceVariant) { Icon(Icons.Default.ZoomIn, null, Modifier.size(16.dp)) }
                SmallFloatingActionButton(onClick = { webViewRef?.zoomOut() }, containerColor = MaterialTheme.colorScheme.surfaceVariant) { Icon(Icons.Default.ZoomOut, null, Modifier.size(16.dp)) }
            }
        }
    }

    // SSL warning dialog
    sslWarning?.let { (handler, error) ->
        AlertDialog(
            onDismissRequest = { handler.cancel(); sslWarning = null },
            title = { Text("SSL Certificate Warning") },
            text = { Text("This site’s certificate is not trusted:\n${error.primaryError} at ${error.url}\n\nProceed only if you trust it.") },
            confirmButton = { TextButton(onClick = { handler.proceed(); sslWarning = null }) { Text("Continue") } },
            dismissButton = { TextButton(onClick = { handler.cancel(); sslWarning = null }) { Text("Back") } }
        )
    }

    // Tabs bottom sheet (full switcher)
    if (showTabsSheet) {
        ModalBottomSheet(onDismissRequest = { showTabsSheet = false }) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("${tabs.size} Tabs", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = {
                            val nid = (tabs.maxOfOrNull { it.id.toIntOrNull() ?: 0 } ?: 0 + 1).toString()
                            val nt = BrowserTab(nid, "https://www.google.com")
                            tabs = tabs + nt; activeTabId = nid; inputUrl = nt.url; showTabsSheet = false
                        }) { Icon(Icons.Default.Add, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("New") }
                        IconButton(onClick = { showTabsSheet = false }) { Icon(Icons.Default.Close, null) }
                    }
                }
                Spacer(Modifier.height(12.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 400.dp)) {
                    items(tabs, key = { it.id }) { tab ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            tonalElevation = if (tab.id == activeTabId) 4.dp else 1.dp,
                            color = if (tab.id == activeTabId) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth().clickable { activeTabId = tab.id; inputUrl = tab.url; showTabsSheet = false }
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                tab.favicon?.let { bm -> androidx.compose.foundation.Image(bitmap = bm.asImageBitmap(), contentDescription = null, modifier = Modifier.size(20.dp).clip(CircleShape)) }
                                    ?: Icon(Icons.Default.Language, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(tab.title.ifBlank { "New Tab" }.take(32), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(tab.url, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                IconButton(onClick = {
                                    if (tabs.size == 1) {
                                        tabs = listOf(BrowserTab("1", "https://www.google.com"))
                                        activeTabId = "1"; inputUrl = "https://www.google.com"
                                    } else {
                                        tabs = tabs.filterNot { it.id == tab.id }
                                        if (activeTabId == tab.id) { activeTabId = tabs.first().id; inputUrl = tabs.first().url }
                                    }
                                }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // History bottom sheet (persistent)
    if (showHistory) {
        ModalBottomSheet(onDismissRequest = { showHistory = false }) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("History", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { scope.launch { historyRepo.clear(); history = emptyList() } }) { Icon(Icons.Default.DeleteSweep, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Clear") }
                        IconButton(onClick = { showHistory = false }) { Icon(Icons.Default.Close, null) }
                    }
                }
                Text("${history.size} entries · persistent · cap 200", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.height(12.dp))
                if (history.isEmpty()) {
                    Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        Text("No history yet — browse to build it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                } else {
                    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 400.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(history, key = { it.url + it.timestamp }) { entry ->
                            Surface(shape = RoundedCornerShape(12.dp), tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth().clickable {
                                activeTab.url = entry.url; inputUrl = entry.url; webViewRef?.loadUrl(entry.url); showHistory = false
                            }) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Language, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(entry.title.take(40), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(entry.url, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(entry.timestamp)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                    }
                                    IconButton(onClick = { scope.launch { historyRepo.delete(entry.url); history = history.filterNot { it.url == entry.url } } }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Delete, null, Modifier.size(16.dp)) }
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

// WHY: extension to convert Bitmap to ImageBitmap
// WHY: use androidx.compose.ui.graphics.asImageBitmap extension
