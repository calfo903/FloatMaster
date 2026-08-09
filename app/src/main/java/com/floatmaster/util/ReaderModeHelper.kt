package com.floatmaster.util

import android.webkit.WebView

/**
 * WHY: Reader mode for 380dp floating window — strips nav/ads, enlarges text, ideal for 1-hand reading.
 */
object ReaderModeHelper {
    var isReaderActive = false

    fun toggle(webView: WebView) {
        if (!isReaderActive) {
            webView.evaluateJavascript(com.floatmaster.data.AdBlocker.READER_JS, null)
            isReaderActive = true
        } else {
            webView.reload() // WHY: reload restores original
            isReaderActive = false
        }
    }

    fun reset() { isReaderActive = false }
}
