package com.floatmaster

import android.webkit.WebView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.floatmaster.apps.aichat.AiChatProvider
import com.floatmaster.apps.aichat.AiWebViewSecurity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AiWebViewSettingsTest {
    @Test
    fun dangerous_webview_capabilities_are_disabled() {
        val webView = WebView(ApplicationProvider.getApplicationContext())
        AiWebViewSecurity.configure(webView, AiChatProvider.CHATGPT, desktopMode = true)

        assertTrue(webView.settings.javaScriptEnabled)
        assertTrue(webView.settings.domStorageEnabled)
        assertFalse(webView.settings.allowFileAccess)
        assertFalse(webView.settings.allowContentAccess)
        assertFalse(webView.settings.javaScriptCanOpenWindowsAutomatically)
        assertFalse(webView.settings.supportMultipleWindows())
        webView.destroy()
    }
}
