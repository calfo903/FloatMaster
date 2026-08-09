package com.floatmaster.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.floatmaster.apps.aichat.FloatingAiChatGroupContent
import com.floatmaster.apps.aichat.FloatingAiChatRoutedContent
import com.floatmaster.apps.browser.FloatingBrowserContent
import com.floatmaster.apps.calculator.FloatingCalculatorContent
import com.floatmaster.apps.clipboard.FloatingClipboardContent
import com.floatmaster.apps.clock.FloatingClockContent
import com.floatmaster.apps.document.FloatingDocumentViewerContent
import com.floatmaster.apps.filemanager.FloatingFileManagerContent
import com.floatmaster.apps.launcher.AppLauncherContent
import com.floatmaster.apps.music.FloatingMusicPlayerContent
import com.floatmaster.apps.notes.FloatingNotesContent
import com.floatmaster.apps.quicksettings.FloatingQuickSettingsContent
import com.floatmaster.apps.translator.FloatingTranslatorContent
import com.floatmaster.apps.youtube.FloatingYouTubeContent
import com.floatmaster.model.FloatingWindow
import com.floatmaster.model.WindowType
import com.floatmaster.service.FloatingWindowManager

@Composable
fun WindowChrome(
    window: FloatingWindow,
    manager: FloatingWindowManager,
    onDrag: (Int, Int) -> Unit,
    onResize: (Int, Int) -> Unit,
    onClose: () -> Unit,
    onMinimize: () -> Unit,
    onBubble: () -> Unit,
    onMaximize: () -> Unit,
    onBringToFront: () -> Unit,
    onSendToBack: () -> Unit,
    onToggleBorder: () -> Unit,
    onAlphaChange: (Float) -> Unit,
    onFocusRequest: () -> Unit,
    onOutsideTap: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showTransparency by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)
    val borderModifier = if (window.geometry.showBorder) Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape) else Modifier
    val alpha = window.geometry.alpha

    Surface(
        modifier = Modifier.fillMaxSize().then(borderModifier).clip(shape),
        shape = shape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = if (window.geometry.showBorder) 1f else 0.96f),
        tonalElevation = 6.dp,
        shadowElevation = 12.dp
    ) {
        Column(Modifier.fillMaxSize()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth().height(48.dp).pointerInput(window.id) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.x.toInt(), dragAmount.y.toInt())
                    }
                }
            ) {
                Row(Modifier.fillMaxSize().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DragIndicator, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Icon(window.type.icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(window.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f), maxLines = 1)
                    IconButton(onClick = onMinimize, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Minimize, "Minimize", Modifier.size(16.dp)) }
                    IconButton(onClick = onBubble, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Circle, "Bubble", Modifier.size(14.dp)) }
                    IconButton(onClick = onMaximize, modifier = Modifier.size(32.dp)) {
                        Icon(if (window.isMaximized) Icons.Default.FullscreenExit else Icons.Default.CropSquare, "Maximize", Modifier.size(16.dp))
                    }
                    IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                        Box(Modifier.background(MaterialTheme.colorScheme.error, CircleShape).padding(4.dp)) {
                            Icon(Icons.Default.Close, "Close", tint = Color.White, modifier = Modifier.size(12.dp))
                        }
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.MoreVert, "Menu", Modifier.size(16.dp)) }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(text = { Text("Bring to front") }, onClick = { showMenu = false; onBringToFront() }, leadingIcon = { Icon(Icons.Default.FlipToFront, null) })
                            DropdownMenuItem(text = { Text("Send to back") }, onClick = { showMenu = false; onSendToBack() }, leadingIcon = { Icon(Icons.Default.FlipToBack, null) })
                            DropdownMenuItem(text = { Text(if (window.geometry.showBorder) "Hide border" else "Show border") }, onClick = { showMenu = false; onToggleBorder() }, leadingIcon = { Icon(Icons.Default.BorderOuter, null) })
                            DropdownMenuItem(
                                text = { Text(if (window.isPinned) "Unpin" else "Pin on top") },
                                onClick = { showMenu = false; manager.togglePinned(window.id) },
                                leadingIcon = { Icon(Icons.Default.PushPin, null) }
                            )
                            DropdownMenuItem(text = { Text("Transparency") }, onClick = { showMenu = false; showTransparency = !showTransparency }, leadingIcon = { Icon(Icons.Default.Opacity, null) })
                        }
                    }
                }
            }

            if (showTransparency) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Opacity, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Slider(value = alpha, onValueChange = onAlphaChange, valueRange = 0.3f..1f, modifier = Modifier.weight(1f))
                    Text("${(alpha * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
                }
                HorizontalDivider()
            }

            Box(
                modifier = Modifier.fillMaxSize().pointerInput(window.id) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type == androidx.compose.ui.input.pointer.PointerEventType.Press) onFocusRequest()
                        }
                    }
                }
            ) {
                when (window.type) {
                    WindowType.BROWSER -> FloatingBrowserContent(window)
                    WindowType.NOTES -> FloatingNotesContent(window)
                    WindowType.CALCULATOR -> FloatingCalculatorContent(window)
                    WindowType.DOCUMENT -> FloatingDocumentViewerContent(window)
                    WindowType.FILE_MANAGER -> FloatingFileManagerContent(window)
                    WindowType.CLIPBOARD -> FloatingClipboardContent(window)
                    WindowType.CLOCK -> FloatingClockContent(window)
                    WindowType.YOUTUBE -> FloatingYouTubeContent(window)
                    WindowType.TRANSLATOR -> FloatingTranslatorContent(window)
                    WindowType.MUSIC -> FloatingMusicPlayerContent(window)
                    WindowType.QUICK_SETTINGS -> FloatingQuickSettingsContent(window)
                    WindowType.APP_LAUNCHER -> AppLauncherContent(window)
                    WindowType.URL_WINDOW -> FloatingBrowserContent(window.copy(url = window.url ?: "https://google.com"))
                    WindowType.WIDGET -> AppLauncherContent(window)
                    WindowType.AI_GROUP -> FloatingAiChatGroupContent(window = window, manager = manager)
                    WindowType.AI_CHATGPT, WindowType.AI_CLAUDE, WindowType.AI_GEMINI, WindowType.AI_PERPLEXITY,
                    WindowType.AI_GROK, WindowType.AI_DEEPSEEK, WindowType.AI_COPILOT, WindowType.AI_META,
                    WindowType.AI_POE, WindowType.AI_YOU, WindowType.AI_MISTRAL, WindowType.AI_CHARACTER ->
                        FloatingAiChatRoutedContent(window = window)
                }

                Box(
                    modifier = Modifier.align(Alignment.BottomEnd).size(24.dp).pointerInput(window.id) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onResize(dragAmount.x.toInt(), dragAmount.y.toInt())
                        }
                    },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.OpenInFull, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}
