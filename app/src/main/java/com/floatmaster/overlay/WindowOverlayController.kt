package com.floatmaster.overlay

import android.view.View
import android.view.WindowManager

/**
 * Owns the imperative WindowManager boundary for one overlay view.
 * WHY: WindowManager throws runtime exceptions for stale/detached views; contain those failures here instead of leaking them into Compose/state code.
 */
class WindowOverlayController(
    private val windowManager: WindowManager
) {
    var isAttached: Boolean = false
        private set

    fun add(root: View, params: WindowManager.LayoutParams): Boolean {
        if (isAttached) return true
        return runCatching {
            windowManager.addView(root, params)
            isAttached = true
            true
        }.getOrDefault(false)
    }

    fun update(root: View, params: WindowManager.LayoutParams): Boolean {
        if (!isAttached) return false
        return runCatching {
            windowManager.updateViewLayout(root, params)
            true
        }.getOrDefault(false)
    }

    fun reattach(root: View, params: WindowManager.LayoutParams): Boolean {
        remove(root)
        return add(root, params)
    }

    fun remove(root: View) {
        if (!isAttached) return
        runCatching { windowManager.removeViewImmediate(root) }
            .recoverCatching { windowManager.removeView(root) }
        isAttached = false
    }
}
