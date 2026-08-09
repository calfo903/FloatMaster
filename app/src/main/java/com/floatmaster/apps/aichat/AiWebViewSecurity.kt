package com.floatmaster.apps.aichat

import android.webkit.CookieManager
import android.webkit.WebView

/**
 * Security configuration shared by every AI WebView.
 * WHY: Keep dangerous WebView defaults in one auditable place and make the policy directly testable.
 */
object AiWebViewSecurity {
    fun configure(webView: WebView, provider: AiChatProvider, desktopMode: Boolean) {
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = true
            displayZoomControls = false
            mediaPlaybackRequiresUserGesture = true
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
            safeBrowsingEnabled = true
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            userAgentString = if (desktopMode) AI_DESKTOP_UA else AI_MOBILE_UA
        }
    }

    fun isAllowedMainFrameNavigation(url: String?, provider: AiChatProvider): Boolean =
        url != null && AiChatProvider.isAllowedUrl(url, provider)
}
