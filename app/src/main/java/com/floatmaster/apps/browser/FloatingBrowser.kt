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
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.floatmaster.apps.aichat.AI_DESKTOP_UA
import com.floatmaster.data.BrowserHistoryRepository
import com.floatmaster.model.FloatingWindow
import com.floatmaster.util.ScreenshotHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** WHY: Only web-safe schemes may reach the general browser's main frame. */
object BrowserUrlPolicy {
    fun isWebUrl(raw: String?): Boolean {
        val uri = runCatching { Uri.parse(raw?.trim().orEmpty()) }.getOrNull() ?: return false
        return uri.scheme?.lowercase() in setOf("https", "http") && !uri.host.isNullOrBlank()
    }

    fun normalizeAddress(raw: String): String {
        val input = raw.trim()
        if (isWebUrl(input)) return input
        if (input.contains(".") && !input.contains(" ")) return "https://$input"
        return "https://www.google.com/search?q=${Uri.encode(input)}"
    }
}

data class BrowserTab(val id: String, var url: String, var title: String = "New Tab", var favicon: Bitmap? = null)

data class DownloadItem(val id: Long, val title: String, val status: Int, val bytes: Long, val total: Long, val uri: String?)

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun FloatingBrowserContent(window: FloatingWindow) {
    val context = LocalContext.current
    val historyRepo = remember { BrowserHistoryRepository(context) }
    val scope = rememberCoroutineScope()
    val clipboard = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    var tabs by remember { mutableStateOf(listOf(BrowserTab("1", if (BrowserUrlPolicy.isWebUrl(window.url)) window.url.orEmpty() else "https://www.google.com"))) }
    var activeTabId by remember { mutableStateOf("1") }
    var inputUrl by remember { mutableStateOf(tabs.first().url) }
    var progress by remember { mutableIntStateOf(0) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var desktopMode by remember { mutableStateOf(false) }
    var darkMode by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var history by remember { mutableStateOf(emptyList<com.floatmaster.data.HistoryEntry>()) }
    var showMenu by remember { mutableStateOf(false) }
    var sslWarning by remember { mutableStateOf<Pair<SslErrorHandler, SslError>?>(null) }
    var fileChooserCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val fileChooserLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        fileChooserCallback?.onReceiveValue(uri?.let(::arrayOf))
        fileChooserCallback = null
    }

    val activeTab = tabs.firstOrNull { it.id == activeTabId } ?: tabs.first()
    val loadAddress: (String) -> Unit = { raw ->
        val normalized = BrowserUrlPolicy.normalizeAddress(raw)
        if (BrowserUrlPolicy.isWebUrl(normalized)) {
            inputUrl = normalized
            activeTab.url = normalized
            webViewRef?.loadUrl(normalized)
        }
    }

    LaunchedEffect(showHistory) {
        if (showHistory) {
            history = historyRepo.getHistory().getOrNull().orEmpty()
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { webViewRef?.goBack() }, enabled = webViewRef?.canGoBack() == true, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.ArrowBack, "Back") }
            IconButton(onClick = { webViewRef?.goForward() }, enabled = webViewRef?.canGoForward() == true, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.ArrowForward, "Forward") }
            IconButton(onClick = { webViewRef?.reload() }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Refresh, "Reload") }
            OutlinedTextField(value = inputUrl, onValueChange = { inputUrl = it }, modifier = Modifier.weight(1f).height(44.dp), singleLine = true, placeholder = { Text("Search or enter URL") }, shape = MaterialTheme.shapes.extraLarge)
            IconButton(onClick = { loadAddress(inputUrl) }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Search, "Go") }
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.MoreVert, "Menu") }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text("History") }, onClick = { showMenu = false; showHistory = true }, leadingIcon = { Icon(Icons.Default.History, null) })
                    DropdownMenuItem(text = { Text("Share URL") }, onClick = {
                        showMenu = false
                        val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, webViewRef?.url ?: inputUrl) }
                        context.startActivity(Intent.createChooser(send, "Share page"))
                    }, leadingIcon = { Icon(Icons.Default.Share, null) })
                    DropdownMenuItem(text = { Text("Copy URL") }, onClick = {
                        showMenu = false
                        clipboard.setPrimaryClip(ClipData.newPlainText("URL", webViewRef?.url ?: inputUrl))
                        Toast.makeText(context, "URL copied", Toast.LENGTH_SHORT).show()
                    }, leadingIcon = { Icon(Icons.Default.ContentCopy, null) })
                    DropdownMenuItem(text = { Text("Open externally") }, onClick = {
                        showMenu = false
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webViewRef?.url ?: inputUrl)).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) }
                    }, leadingIcon = { Icon(Icons.Default.OpenInNew, null) })
                    DropdownMenuItem(text = { Text(if (desktopMode) "Mobile site" else "Desktop site") }, onClick = {
                        showMenu = false
                        desktopMode = !desktopMode
                        webViewRef?.let { it.settings.userAgentString = if (desktopMode) AI_DESKTOP_UA else WebSettings.getDefaultUserAgent(context); it.reload() }
                    }, leadingIcon = { Icon(if (desktopMode) Icons.Default.PhoneAndroid else Icons.Default.Computer, null) })
                    DropdownMenuItem(text = { Text(if (darkMode) "Light mode" else "Dark mode") }, onClick = {
                        showMenu = false
                        darkMode = !darkMode
                        webViewRef?.let { webView ->
                            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                                @Suppress("DEPRECATION") WebSettingsCompat.setForceDark(webView.settings, if (darkMode) WebSettingsCompat.FORCE_DARK_ON else WebSettingsCompat.FORCE_DARK_OFF)
                            }
                        }
                    }, leadingIcon = { Icon(Icons.Default.DarkMode, null) })
                    DropdownMenuItem(text = { Text("Zoom in") }, onClick = { showMenu = false; webViewRef?.zoomIn() }, leadingIcon = { Icon(Icons.Default.ZoomIn, null) })
                    DropdownMenuItem(text = { Text("Zoom out") }, onClick = { showMenu = false; webViewRef?.zoomOut() }, leadingIcon = { Icon(Icons.Default.ZoomOut, null) })
                    DropdownMenuItem(text = { Text("Save as PDF") }, onClick = { showMenu = false; webViewRef?.let { ScreenshotHelper.saveAsPdf(context, it, activeTab.title) } }, leadingIcon = { Icon(Icons.Default.PictureAsPdf, null) })
                    DropdownMenuItem(text = { Text("Share screenshot") }, onClick = { showMenu = false; webViewRef?.let { ScreenshotHelper.shareScreenshot(context, it) } }, leadingIcon = { Icon(Icons.Default.Screenshot, null) })
                    DropdownMenuItem(text = { Text("Clear browsing data") }, onClick = {
                        showMenu = false
                        webViewRef?.clearHistory()
                        webViewRef?.clearCache(true)
                        CookieManager.getInstance().removeAllCookies(null)
                        CookieManager.getInstance().flush()
                    }, leadingIcon = { Icon(Icons.Default.CleaningServices, null) })
                }
            }
        }

        if (progress in 1..99) LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())

        LazyRow(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(tabs, key = { it.id }) { tab ->
                FilterChip(
                    selected = tab.id == activeTabId,
                    onClick = { activeTabId = tab.id; inputUrl = tab.url; webViewRef?.loadUrl(tab.url) },
                    label = { Text(tab.title.take(14), style = MaterialTheme.typography.labelSmall) },
                    trailingIcon = if (tabs.size > 1) ({ IconButton(onClick = {
                        val remaining = tabs.filterNot { it.id == tab.id }
                        tabs = remaining
                        if (tab.id == activeTabId) { activeTabId = remaining.first().id; inputUrl = remaining.first().url; webViewRef?.loadUrl(remaining.first().url) }
                    }, modifier = Modifier.size(18.dp)) { Icon(Icons.Default.Close, null, Modifier.size(10.dp)) } }) else null
                )
            }
            item {
                IconButton(onClick = {
                    val next = ((tabs.mapNotNull { it.id.toIntOrNull() }.maxOrNull() ?: 0) + 1).toString()
                    val tab = BrowserTab(next, "https://www.google.com")
                    tabs = tabs + tab
                    activeTabId = next
                    inputUrl = tab.url
                    webViewRef?.loadUrl(tab.url)
                }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Add, "New tab") }
            }
        }

        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    setLayerType(View.LAYER_TYPE_HARDWARE, null)
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = true
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    settings.setSupportZoom(true)
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    settings.safeBrowsingEnabled = true
                    settings.javaScriptCanOpenWindowsAutomatically = false
                    settings.setSupportMultipleWindows(false)
                    settings.mediaPlaybackRequiresUserGesture = true
                    settings.userAgentString = if (desktopMode) AI_DESKTOP_UA else WebSettings.getDefaultUserAgent(ctx)

                    setDownloadListener { url, userAgent, contentDisposition, mimetype, _ ->
                        runCatching {
                            val fileName = URLUtil.guessFileName(url, contentDisposition, mimetype)
                            val request = DownloadManager.Request(Uri.parse(url)).apply {
                                setMimeType(mimetype)
                                addRequestHeader("User-Agent", userAgent)
                                setTitle(fileName)
                                setDescription("FloatMaster download")
                                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                            }
                            (context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
                        }.onFailure { Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show() }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? = super.shouldInterceptRequest(view, request)

                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val target = request?.url?.toString() ?: return true
                            if (request?.isForMainFrame != true) return false
                            if (BrowserUrlPolicy.isWebUrl(target)) {
                                view?.loadUrl(target)
                                return true
                            }
                            if (target.startsWith("tel:") || target.startsWith("mailto:")) {
                                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(target)).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) }
                                return true
                            }
                            view?.stopLoading()
                            return true
                        }

                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            if (!BrowserUrlPolicy.isWebUrl(url)) { view?.stopLoading(); return }
                            inputUrl = url.orEmpty()
                            activeTab.url = url.orEmpty()
                            favicon?.let { activeTab.favicon = it }
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            if (!BrowserUrlPolicy.isWebUrl(url)) return
                            progress = 100
                            inputUrl = url.orEmpty()
                            scope.launch { historyRepo.add(url.orEmpty(), view?.title.orEmpty().ifBlank { url.orEmpty() }) }
                        }

                        override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                            // WHY: Never allow users to bypass certificate validation inside the embedded browser.
                            handler?.cancel()
                            Toast.makeText(context, "Secure connection failed", Toast.LENGTH_SHORT).show()
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) { progress = newProgress }
                        override fun onReceivedTitle(view: WebView?, title: String?) { activeTab.title = title?.take(80).orEmpty().ifBlank { "New Tab" } }
                        override fun onReceivedIcon(view: WebView?, icon: Bitmap?) { icon?.let { activeTab.favicon = it } }
                        override fun onShowFileChooser(webView: WebView?, callback: ValueCallback<Array<Uri>>?, params: FileChooserParams?): Boolean {
                            fileChooserCallback?.onReceiveValue(null)
                            fileChooserCallback = callback
                            runCatching { fileChooserLauncher.launch("*/*") }.onFailure { fileChooserCallback = null; callback?.onReceiveValue(null) }
                            return true
                        }
                    }
                    loadUrl(activeTab.url)
                    webViewRef = this
                }
            },
            update = { webView ->
                val desired = if (desktopMode) AI_DESKTOP_UA else WebSettings.getDefaultUserAgent(context)
                if (webView.settings.userAgentString != desired) webView.settings.userAgentString = desired
                if (BrowserUrlPolicy.isWebUrl(activeTab.url) && webView.url != activeTab.url) webView.loadUrl(activeTab.url)
                webViewRef = webView
            },
            modifier = Modifier.fillMaxSize()
        )
    }

    sslWarning?.let { (handler, _) -> handler.cancel(); sslWarning = null }

    if (showHistory) {
        ModalBottomSheet(onDismissRequest = { showHistory = false }) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("History", style = MaterialTheme.typography.titleMedium)
                    Row {
                        TextButton(onClick = { scope.launch { historyRepo.clear(); history = emptyList() } }) { Text("Clear") }
                        IconButton(onClick = { showHistory = false }) { Icon(Icons.Default.Close, null) }
                    }
                }
                if (history.isEmpty()) {
                    Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) { Text("No history yet") }
                } else {
                    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                        items(history, key = { it.url + it.timestamp }) { entry ->
                            ListItem(
                                headlineContent = { Text(entry.title.take(40), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                supportingContent = { Text(entry.url, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                leadingContent = { Icon(Icons.Default.Language, null) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}
