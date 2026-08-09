package com.floatmaster.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.floatmaster.model.FloatingWindow
import com.floatmaster.model.WindowState
import com.floatmaster.service.FloatingWindowManager
import com.floatmaster.ui.theme.FloatMasterTheme
import com.floatmaster.util.WindowSnapManager

/**
 * Compose host for one floating overlay.
 * WHY: WindowOverlayController owns the exception-prone WindowManager boundary; Compose state owns UI recomposition.
 */
class FloatingWindowContainer(
    private val context: Context,
    initialWindow: FloatingWindow,
    private val manager: FloatingWindowManager,
    lifecycleOwner: LifecycleOwner
) {
    private val overlayController = WindowOverlayController(
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    )
    private val root = FrameLayout(context)
    private val composeView: ComposeView
    private var window by mutableStateOf(initialWindow)
    private var params: WindowManager.LayoutParams = createParams(initialWindow)

    init {
        composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            if (lifecycleOwner is ViewModelStoreOwner) setViewTreeViewModelStoreOwner(lifecycleOwner)
            if (lifecycleOwner is SavedStateRegistryOwner) setViewTreeSavedStateRegistryOwner(lifecycleOwner)
        }
        root.addView(composeView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        setContent()
        overlayController.add(root, params)
    }

    private fun createParams(win: FloatingWindow): WindowManager.LayoutParams {
        val dm = context.resources.displayMetrics
        val density = dm.density
        val maximized = win.state == WindowState.MAXIMIZED
        val width = if (maximized) dm.widthPixels else win.geometry.width
        val height = if (maximized) (dm.heightPixels - (24 * density).toInt()).coerceAtLeast(180)
        else if (win.state == WindowState.MINIMIZED) (56 * density).toInt()
        else win.geometry.height
        val x = if (maximized) 0 else win.geometry.x
        val y = if (maximized) 0 else win.geometry.y

        return WindowManager.LayoutParams(
            width,
            height,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = x
            this.y = y
            alpha = win.geometry.alpha.coerceIn(0.3f, 1f)
            title = "FloatMaster-${win.id.toString().take(6)}"
        }
    }

    private fun setContent() {
        composeView.setContent {
            FloatMasterTheme {
                WindowChrome(
                    window = window,
                    manager = manager,
                    onDrag = ::onDrag,
                    onResize = ::onResize,
                    onClose = { manager.close(window.id) },
                    onMinimize = { manager.minimize(window.id) },
                    onBubble = { manager.bubble(window.id) },
                    onMaximize = { manager.maximize(window.id) },
                    onBringToFront = { manager.bringToFront(window.id) },
                    onSendToBack = { manager.sendToBack(window.id) },
                    onToggleBorder = { manager.toggleBorder(window.id) },
                    onAlphaChange = { manager.setAlpha(window.id, it) },
                    onFocusRequest = ::requestFocus,
                    onOutsideTap = ::clearFocus
                )
            }
        }
    }

    fun update(newWindow: FloatingWindow) {
        val zChanged = newWindow.zIndex != window.zIndex
        window = newWindow
        val newParams = createParams(newWindow)
        val hadFocus = (params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE) == 0
        if (hadFocus) newParams.flags = newParams.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        params = newParams

        if (zChanged && overlayController.isAttached) {
            overlayController.reattach(root, params)
        } else {
            overlayController.update(root, params)
        }
    }

    private fun onDrag(dx: Int, dy: Int) {
        if (window.isLocked || window.isMaximized) return
        params.x += dx
        params.y += dy

        runCatching {
            val dm = context.resources.displayMetrics
            val snap = WindowSnapManager.snap(params.x, params.y, params.width, params.height, dm.widthPixels, dm.heightPixels)
            if (snap != null && (kotlin.math.abs(params.x) < 40 || kotlin.math.abs(params.y) < 40)) {
                params.x = snap.geometry.x
                params.y = snap.geometry.y
                params.width = snap.geometry.width
                params.height = snap.geometry.height
            }
        }

        overlayController.update(root, params)
        manager.updateGeometry(
            window.id,
            window.geometry.copy(x = params.x, y = params.y, width = params.width, height = params.height)
        )
    }

    private fun onResize(dw: Int, dh: Int) {
        if (window.isLocked || window.isMaximized) return
        val density = context.resources.displayMetrics.density
        params.width = (params.width + dw).coerceAtLeast((220 * density).toInt())
        params.height = (params.height + dh).coerceAtLeast((180 * density).toInt())
        overlayController.update(root, params)
        manager.updateGeometry(window.id, window.geometry.copy(width = params.width, height = params.height))
    }

    private fun requestFocus() {
        params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        overlayController.update(root, params)
        root.requestFocus()
        composeView.requestFocus()
        manager.bringToFront(window.id)
    }

    private fun clearFocus() {
        params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        overlayController.update(root, params)
        root.clearFocus()
    }

    fun destroy() {
        overlayController.remove(root)
    }
}
