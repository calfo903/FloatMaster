package com.floatmaster.util

import android.content.Context
import android.webkit.WebView
import java.util.LinkedList

/**
 * WHY: WebView is 40MB each — pool reuses 1 instance for Tabs mode, reduces GC & OOM.
 */
object WebViewPool {
    private val pool = LinkedList<WebView>()
    private const val MAX = 2

    fun acquire(context: Context): WebView = synchronized(pool) {
        if (pool.isNotEmpty()) pool.removeFirst() else WebView(context.applicationContext)
    }

    fun release(wv: WebView) = synchronized(pool) {
        try { wv.stopLoading(); wv.clearHistory(); wv.loadUrl("about:blank") } catch (_: Exception) {}
        if (pool.size < MAX) pool.addLast(wv) else wv.destroy()
    }

    fun onTrimMemory(level: Int) {
        // WHY: onTrimMemory CRITICAL → clear caches
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) {
            synchronized(pool) { pool.forEach { try{ it.clearCache(true)}catch(_:Exception){} }; pool.clear() }
        }
    }
}
