package com.floatmaster.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.floatmaster.model.FloatingWindow
import com.floatmaster.service.FloatingWindowManager
import com.floatmaster.ui.theme.FloatMasterTheme

class BubbleView(
    private val context: Context,
    private var window: FloatingWindow,
    private val manager: FloatingWindowManager,
    private val lifecycleOwner: LifecycleOwner
) {
    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val root = FrameLayout(context)
    private val composeView: ComposeView
    private val params: WindowManager.LayoutParams
    private var attached = false

    init {
        val size = (60 * context.resources.displayMetrics.density).toInt()
        params = WindowManager.LayoutParams(
            size, size,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            // snap to right edge initially
            x = context.resources.displayMetrics.widthPixels - size - (8 * context.resources.displayMetrics.density).toInt()
            y = window.geometry.y.coerceIn(0, context.resources.displayMetrics.heightPixels - size)
        }
        composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            if (lifecycleOwner is ViewModelStoreOwner) setViewTreeViewModelStoreOwner(lifecycleOwner)
            if (lifecycleOwner is SavedStateRegistryOwner) setViewTreeSavedStateRegistryOwner(lifecycleOwner)
        }
        root.addView(composeView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        setContent()
        try { wm.addView(root, params); attached = true } catch (e: Exception) { e.printStackTrace() }
    }

    private fun setContent() {
        composeView.setContent {
            FloatMasterTheme {
                BubbleContent(
                    window = window,
                    onTap = { manager.restore(window.id) },
                    onClose = { manager.close(window.id) },
                    onDrag = { dx, dy ->
                        params.x += dx
                        params.y += dy
                        try { wm.updateViewLayout(root, params) } catch (_: Exception) {}
                    }
                )
            }
        }
    }

    fun update(newWindow: FloatingWindow) {
        window = newWindow
        setContent()
    }

    fun destroy() {
        if (!attached) return
        try { wm.removeViewImmediate(root) } catch (_: Exception) { try { wm.removeView(root) } catch (_: Exception) {} }
        attached = false
    }
}

@Composable
private fun BubbleContent(
    window: FloatingWindow,
    onTap: () -> Unit,
    onClose: () -> Unit,
    onDrag: (Int, Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .shadow(8.dp, CircleShape)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable { onTap() }
            .pointerInput(window.id) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x.toInt(), dragAmount.y.toInt())
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(window.type.icon, window.title, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(28.dp))
    }
}
