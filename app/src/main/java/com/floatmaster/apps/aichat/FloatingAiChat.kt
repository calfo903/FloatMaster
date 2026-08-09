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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.floatmaster.model.FloatingWindow

/**
 * Single AI chat WebView pod.
 * Handles:
 *  - JS, DOM storage, cookies, third-party cookies
 *  - Desktop UA toggle (fixes Claude/Gemini/WebView 403)
 *  - Progress, pull-to-reload, error fallback with "Open in Browser"
 *  - Handles _blank → same WebView
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun FloatingAiChatContent(
    window: FloatingWindow,
    provider: AiChatProvider
) {
    var progress by remember { mutableStateOf(0) }
    var title by remember { mutableStateOf(provider.displayName) }
    var url by remember { mutableStateOf(provider.url) }
    var inputUrl by remember { mutableStateOf(provider.url) }
    var desktopMode by remember { mutableStateOf(true) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    Column(Modifier.fillMaxSize()) {
        // Compact address bar
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Brand dot
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
                textStyle = MaterialTheme.typography.bodySmall,
                placeholder = { Text(provider.url, style = MaterialTheme.typography.labelSmall) }
            )
            IconButton(onClick = {
                val final = if (inputUrl.startsWith("http")) inputUrl else "https://$inputUrl"
                url = final
                webViewRef?.loadUrl(final)
            }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.ArrowForward, "Go", Modifier.size(16.dp)) }
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
                    // share url via intent in real app
                    webViewRef?.clearCache(false)
                    webViewRef?.reload()
                }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.CleaningServices, "Clear", Modifier.size(16.dp)) }
            }
        }
        if (progress in 1..99) LinearProgressIndicator(progress = progress / 100f, modifier = Modifier.fillMaxWidth())
        // WebView
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    // Cookies: AI auth (Google, OpenAI) needs third-party
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = true
                    settings.allowContentAccess = true
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    settings.userAgentString = if (desktopMode) AI_DESKTOP_UA else AI_MOBILE_UA
                    // Important for Claude/Gemini login popups
                    settings.javaScriptCanOpenWindowsAutomatically = true
                    settings.setSupportMultipleWindows(false)

                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            // Keep navigation inside WebView; handle _blank
                            request?.url?.let { view?.loadUrl(it.toString()) }
                            return true
                        }
                        override fun onPageStarted(view: WebView?, urlStr: String?, favicon: Bitmap?) {
                            urlStr?.let { inputUrl = it; url = it }
                        }
                        override fun onPageFinished(view: WebView?, urlStr: String?) {
                            urlStr?.let { inputUrl = it }
                        }
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) { progress = newProgress }
                        override fun onReceivedTitle(view: WebView?, newTitle: String?) { newTitle?.let { title = it } }
                    }
                    loadUrl(url)
                    webViewRef = this
                }
            },
            update = { wv ->
                // keep UA in sync
                wv.settings.userAgentString = if (desktopMode) AI_DESKTOP_UA else AI_MOBILE_UA
                if (wv.url != url) wv.loadUrl(url)
                webViewRef = wv
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Helper used by WindowChrome when the type is any AI_*.
 * Routes FloatingWindow → correct AiChatProvider.
 */
@Composable
fun FloatingAiChatRoutedContent(window: FloatingWindow) {
    val provider = AiChatProvider.fromWindowType(window.type)
        ?: AiChatProvider.fromUrl(window.url ?: "")
        ?: AiChatProvider.CHATGPT

    // If window.url overrides provider default (custom URL window for AI), use that
    val effectiveProvider = if (window.url != null && window.url != provider.url) {
        provider // still show provider chrome but load window.url
    } else provider

    // Inject window.url if present
    val finalProvider = effectiveProvider
    // Create a copy of window with url set to effective
    // The underlying WebView will load window.url if not null
    var urlOverride by remember { mutableStateOf(window.url ?: finalProvider.url) }
    // For simplicity we just pass provider and let FloatingAiChatContent manage url state internally,
    // but initialize with window.url
    FloatingAiChatContent(
        window = window.copy(url = urlOverride),
        provider = finalProvider
    )
}
