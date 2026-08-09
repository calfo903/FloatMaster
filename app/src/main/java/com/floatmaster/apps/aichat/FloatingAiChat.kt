package com.floatmaster.apps.aichat

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.floatmaster.model.FloatingWindow

/**
 * Single AI chat WebView pod.
 * WHY: AI pods are zero-trust WebViews: HTTPS only, exact provider host allowlist, no file/content access,
 * no JavaScript bridges, no mixed content, Safe Browsing enabled, and no arbitrary popup windows.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun FloatingAiChatContent(window: FloatingWindow, provider: AiChatProvider) {
    val initialUrl = remember(window.url, provider) {
        window.url?.takeIf { AiChatProvider.isAllowedUrl(it, provider) } ?: provider.url
    }
    var progress by remember { mutableStateOf(0) }
    var title by remember { mutableStateOf(provider.displayName) }
    var url by remember { mutableStateOf(initialUrl) }
    var inputUrl by remember { mutableStateOf(initialUrl) }
    var desktopMode by remember { mutableStateOf(true) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    fun safeLoad(candidate: String): Boolean {
        if (!AiChatProvider.isAllowedUrl(candidate, provider)) return false
        url = candidate
        inputUrl = candidate
        webViewRef?.loadUrl(candidate)
        return true
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(provider.icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = inputUrl,
                onValueChange = { inputUrl = it },
                modifier = Modifier.weight(1f).height(42.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall
            )
            IconButton(onClick = { safeLoad(inputUrl.trim()) }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.ArrowForward, "Go", Modifier.size(16.dp))
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { webViewRef?.goBack() }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.ArrowBack, null, Modifier.size(16.dp)) }
                IconButton(onClick = { webViewRef?.goForward() }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.ArrowForward, null, Modifier.size(16.dp)) }
                IconButton(onClick = { webViewRef?.reload() }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Refresh, null, Modifier.size(16.dp)) }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(
                    selected = desktopMode,
                    onClick = {
                        desktopMode = !desktopMode
                        webViewRef?.settings?.userAgentString = if (desktopMode) AI_DESKTOP_UA else AI_MOBILE_UA
                        webViewRef?.reload()
                    },
                    label = { Text(if (desktopMode) "Desktop" else "Mobile", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = { Icon(if (desktopMode) Icons.Default.Computer else Icons.Default.PhoneAndroid, null, Modifier.size(14.dp)) }
                )
                IconButton(onClick = {
                    webViewRef?.clearCache(false)
                    webViewRef?.reload()
                }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.CleaningServices, "Clear", Modifier.size(16.dp)) }
            }
        }
        if (progress in 1..99) LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())

        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    settings.mediaPlaybackRequiresUserGesture = true
                    settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    settings.userAgentString = if (desktopMode) AI_DESKTOP_UA else AI_MOBILE_UA
                    settings.safeBrowsingEnabled = true
                    settings.javaScriptCanOpenWindowsAutomatically = false
                    settings.setSupportMultipleWindows(false)

                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val target = request?.url?.toString() ?: return true
                            if (!request.isForMainFrame) return false
                            if (!AiChatProvider.isAllowedUrl(target, provider)) {
                                view?.stopLoading()
                                return true
                            }
                            view?.loadUrl(target)
                            return true
                        }

                        override fun onPageStarted(view: WebView?, urlStr: String?, favicon: Bitmap?) {
                            if (urlStr == null || !AiChatProvider.isAllowedUrl(urlStr, provider)) {
                                view?.stopLoading()
                                view?.loadUrl(provider.url)
                                return
                            }
                            inputUrl = urlStr
                            url = urlStr
                        }

                        override fun onPageFinished(view: WebView?, urlStr: String?) {
                            if (urlStr != null && AiChatProvider.isAllowedUrl(urlStr, provider)) {
                                inputUrl = urlStr
                                url = urlStr
                            }
                        }
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) { progress = newProgress }
                        override fun onReceivedTitle(view: WebView?, newTitle: String?) { newTitle?.let { title = it } }
                    }
                    loadUrl(initialUrl)
                    webViewRef = this
                }
            },
            update = { webView ->
                webView.settings.userAgentString = if (desktopMode) AI_DESKTOP_UA else AI_MOBILE_UA
                if (webView.url != url && AiChatProvider.isAllowedUrl(url, provider)) webView.loadUrl(url)
                webViewRef = webView
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun FloatingAiChatRoutedContent(window: FloatingWindow) {
    val provider = AiChatProvider.fromWindowType(window.type)
        ?: AiChatProvider.fromUrl(window.url.orEmpty())
        ?: AiChatProvider.CHATGPT
    FloatingAiChatContent(window = window, provider = provider)
}
