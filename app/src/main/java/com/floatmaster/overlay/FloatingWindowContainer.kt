package com.floatmaster.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.compose.runtime.*
import com.floatmaster.model.FloatingWindow
import com.floatmaster.model.WindowState
import com.floatmaster.service.FloatingWindowManager
import com.floatmaster.util.WindowSnapManager
import com.floatmaster.ui.theme.FloatMasterTheme
import androidx.compose.foundation.layout.*

/**
 * The WindowManager container for a single floating window.
 * Hosts a ComposeView inside a FrameLayout, and owns the LayoutParams.
 *
 * Handles:
 *  - alpha / border / maximized geometry
 *  - focus toggle (FLAG_NOT_FOCUSABLE dance)
 *  - bring-to-front via re-add
 */
class FloatingWindowContainer(
    private val context: Context,
    private var window: FloatingWindow,
    private val manager: FloatingWindowManager,
    private val lifecycleOwner: LifecycleOwner
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val root = FrameLayout(context)
    private var composeView: ComposeView
    private var params: WindowManager.LayoutParams
    private var isAttached = false

    // Keep previous geometry for maximize restore
    private var preMaximizeGeometry = window.geometry

    init {
        params = createParams(window)
        composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            if (lifecycleOwner is ViewModelStoreOwner) setViewTreeViewModelStoreOwner(lifecycleOwner)
            if (lifecycleOwner is SavedStateRegistryOwner) setViewTreeSavedStateRegistryOwner(lifecycleOwner)
        }
        root.addView(composeView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        setContent()
        addToWindow()
    }

    private fun createParams(win: FloatingWindow): WindowManager.LayoutParams {
        val dm = context.resources.displayMetrics
        val isMaximized = win.state == WindowState.MAXIMIZED

        val w = if (isMaximized) dm.widthPixels else win.geometry.width
        val h = if (isMaximized) {
            // leave room for status/nav
            dm.heightPixels - (24 * dm.density).toInt()
        } else if (win.state == WindowState.MINIMIZED) {
            (56 * dm.density).toInt() // title bar only
        } else win.geometry.height

        val x = if (isMaximized) 0 else win.geometry.x
        val y = if (isMaximized) 0 else win.geometry.y

        return WindowManager.LayoutParams(
            w, h,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = x
            this.y = y
            alpha = win.geometry.alpha
            // Start not focusable — tap title bar to focus
            flags = flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            title = "FloatMaster-${win.id.take(6)}"
        }
    }

    private fun setContent() {
        composeView.setContent {
            FloatMasterTheme {
                WindowChrome(
                    window = window,
                    manager = manager,
                    onDrag = { dx, dy -> onDrag(dx, dy) },
                    onResize = { dw, dh -> onResize(dw, dh) },
                    onClose = { manager.close(window.id) },
                    onMinimize = { manager.minimize(window.id) },
                    onBubble = { manager.bubble(window.id) },
                    onMaximize = {
                        if (window.isMaximized) manager.restore(window.id) else manager.maximize(window.id)
                    },
                    onBringToFront = { manager.bringToFront(window.id) },
                    onSendToBack = { manager.sendToBack(window.id) },
                    onToggleBorder = { manager.toggleBorder(window.id) },
                    onAlphaChange = { manager.setAlpha(window.id, it) },
                    onFocusRequest = { requestFocus() },
                    onOutsideTap = { clearFocus() }
                )
            }
        }
    }

    private fun addToWindow() {
        if (isAttached) return
        try {
            windowManager.addView(root, params)
            isAttached = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun update(newWindow: FloatingWindow) {
        val wasMaximized = window.isMaximized
        val nowMaximized = newWindow.state == WindowState.MAXIMIZED
        window = newWindow

        // Handle maximize geometry save/restore
        if (!wasMaximized && nowMaximized) {
            preMaximizeGeometry = window.geometry
        }

        // Rebuild params if size/state/alpha changed
        val newParams = createParams(newWindow)
        // Preserve focus flag if we were focused
        val hadFocus = (params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE) == 0
        if (hadFocus) newParams.flags = newParams.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        // Border handled inside WindowChrome via compose state

        val zChanged = newWindow.zIndex != window.zIndex
        // Bring to front = remove + add again if needed
        if (zChanged && isAttached) {
            try {
                windowManager.removeViewImmediate(root)
                isAttached = false
            } catch (_: Exception) {}
            params = newParams
            addToWindow()
            // re-set compose content to refresh z
            setContent()
            return
        }

        params = newParams
        if (isAttached) {
            try {
                windowManager.updateViewLayout(root, params)
            } catch (_: Exception) {}
            // Recompose with new window
            setContent()
        }
    }

    private fun onDrag(dx: Int, dy: Int) {
        if (window.isLocked || window.isMaximized) return
        params.x += dx
        params.y += dy
        // WHY: Aero Snap — if near edge, snap to half/quarter
        try {
            val dm = context.resources.displayMetrics
            val snap = WindowSnapManager.snap(params.x, params.y, params.width, params.height, dm.widthPixels, dm.heightPixels)
            if (snap != null && (Math.abs(params.x) < 40 || Math.abs(params.y) < 40)) {
                // WHY: threshold 40px prevents accidental snap while dragging slowly
                // Apply snap preview: update params to snapped geometry
                params.x = snap.geometry.x
                params.y = snap.geometry.y
                params.width = snap.geometry.width
                params.height = snap.geometry.height
            }
        } catch (_: Exception) {}
        try { windowManager.updateViewLayout(root, params) } catch (_: Exception) {}
        manager.updateGeometry(window.id, window.geometry.copy(x = params.x, y = params.y, width = params.width, height = params.height))
    }

    private fun onResize(dw: Int, dh: Int) {
        if (window.isLocked || window.isMaximized) return
        val minW = (220 * context.resources.displayMetrics.density).toInt()
        val minH = (180 * context.resources.displayMetrics.density).toInt()
        params.width = (params.width + dw).coerceAtLeast(minW)
        params.height = (params.height + dh).coerceAtLeast(minH)
        try { windowManager.updateViewLayout(root, params) } catch (_: Exception) {}
        manager.updateGeometry(window.id, window.geometry.copy(width = params.width, height = params.height))
    }

    private fun requestFocus() {
        params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        try { windowManager.updateViewLayout(root, params) } catch (_: Exception) {}
        root.requestFocus()
        composeView.requestFocus()
        manager.bringToFront(window.id)
    }

    private fun clearFocus() {
        params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        try { windowManager.updateViewLayout(root, params) } catch (_: Exception) {}
        root.clearFocus()
    }

    fun destroy() {
        if (!isAttached) return
        try {
            windowManager.removeViewImmediate(root)
        } catch (_: Exception) {
            try { windowManager.removeView(root) } catch (_: Exception) {}
        }
        isAttached = false
    }
}
