package com.floatmaster.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.floatmaster.apps.aichat.AiChatProvider
import com.floatmaster.model.MiniAppCatalog
import com.floatmaster.model.WindowType
import com.floatmaster.service.FloatingWindowManager
import com.floatmaster.util.TaskbarIntegration
import com.floatmaster.ui.theme.FloatMasterTheme

/**
 * Always-on-top floating dock / quick launch strip.
 * Updated: AI Chats is the hero pill (12 pods), plus 6 mini-apps + New URL.
 */
class FloatingDock(
    private val context: Context,
    private val manager: FloatingWindowManager,
    private val lifecycleOwner: LifecycleOwner
) {
    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val root = FrameLayout(context)
    private val composeView: ComposeView
    private val params: WindowManager.LayoutParams
    private var attached = false

    init {
        val dm = context.resources.displayMetrics
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (dm.widthPixels * 0.5 - 160 * dm.density).toInt().coerceAtLeast(0)
            y = dm.heightPixels - (100 * dm.density).toInt() - 48 // WHY: leave room for 12L taskbar (48-80dp); snap helper insets further at runtime
        }
        composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            if (lifecycleOwner is ViewModelStoreOwner) setViewTreeViewModelStoreOwner(lifecycleOwner)
            if (lifecycleOwner is SavedStateRegistryOwner) setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent { FloatMasterTheme { DockContent(manager) } }
        }
        root.addView(composeView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT))
        try { wm.addView(root, params); attached = true } catch (e: Exception) { e.printStackTrace() }
    }

    fun destroy() {
        if (!attached) return
        try { wm.removeViewImmediate(root) } catch (_: Exception) { try { wm.removeView(root) } catch (_: Exception) {} }
        attached = false
    }
}

@Composable
private fun DockContent(manager: FloatingWindowManager) {
    var expanded by remember { mutableStateOf(true) }

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shadowElevation = 12.dp,
        tonalElevation = 4.dp,
        modifier = Modifier.shadow(12.dp, RoundedCornerShape(28.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(40.dp)) {
                Icon(if (expanded) Icons.Default.ExpandMore else Icons.Default.Apps, "Toggle", tint = MaterialTheme.colorScheme.primary)
            }
            if (expanded) {
                // Hero AI Group pill
                Box(
                    modifier = Modifier
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { manager.create(WindowType.AI_GROUP) }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SmartToy, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("AI ×12", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelMedium)
                    }
                }
                Spacer(Modifier.width(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.widthIn(max = 220.dp)) {
                    items(MiniAppCatalog.all.take(6).filterNot { it.type == WindowType.AI_GROUP }) { app ->
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable { manager.create(app.type) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(app.type.icon, app.title, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                        }
                    }
                    // one-tap ChatGPT/Claude/Gemini shortcuts in dock overflow
                    item {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .clickable { manager.create(WindowType.AI_CHATGPT) },
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Default.Chat, "GPT", tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp)) }
                    }
                }
                Divider(modifier = Modifier.width(1.dp).height(32.dp).padding(horizontal = 4.dp))
                IconButton(onClick = { manager.create(WindowType.URL_WINDOW, url = "https://google.com") }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Add, "New URL", tint = MaterialTheme.colorScheme.primary)
                }
            } else {
                Text("FloatMaster", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(end = 8.dp))
            }
        }
    }
}
