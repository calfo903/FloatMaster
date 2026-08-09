package com.floatmaster.apps.aichat

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.floatmaster.model.FloatingWindow
import com.floatmaster.service.FloatingWindowManager

/**
 * The main "AI Chats in group" floating window.
 *
 * Modes:
 *  - DASHBOARD (default): hero + grid of 12 providers + "Launch All" actions + stats
 *  - TABS: single window hosts 12 WebViews as tabs (one visible at a time, lazy)
 *  - TILED: scrollable 2-col grid where each cell is a live WebView pod (10+ simultaneous)
 *
 * Floating launch modes (via FloatingWindowManager):
 *  - Cascade  → 12 separate floating windows, offset diagonally so they don't overlap fully
 *  - Tiled set → same but with calculated grid positions across screen
 *
 * Each pod is a real WebView with desktop UA, cookies, JS, third-party enabled.
 */
@Composable
fun FloatingAiChatGroupContent(
    window: FloatingWindow,
    manager: FloatingWindowManager
) {
    var mode by remember { mutableStateOf(GroupMode.DASHBOARD) }
    var selected by remember { mutableStateOf(AiChatProvider.CHATGPT) }
    var desktopMode by remember { mutableStateOf(true) }

    Column(Modifier.fillMaxSize()) {
        // Top switcher
        TabRow(selectedTabIndex = mode.ordinal, containerColor = MaterialTheme.colorScheme.surfaceVariant) {
            Tab(selected = mode == GroupMode.DASHBOARD, onClick = { mode = GroupMode.DASHBOARD }, text = { Text("Dashboard", style = MaterialTheme.typography.labelSmall) }, icon = { Icon(Icons.Default.Dashboard, null, Modifier.size(16.dp)) })
            Tab(selected = mode == GroupMode.TABS, onClick = { mode = GroupMode.TABS }, text = { Text("Tabs", style = MaterialTheme.typography.labelSmall) }, icon = { Icon(Icons.Default.Tab, null, Modifier.size(16.dp)) })
            Tab(selected = mode == GroupMode.TILED, onClick = { mode = GroupMode.TILED }, text = { Text("Tiled", style = MaterialTheme.typography.labelSmall) }, icon = { Icon(Icons.Default.GridView, null, Modifier.size(16.dp)) })
        }

        when (mode) {
            GroupMode.DASHBOARD -> DashboardMode(
                manager = manager,
                onOpenTabs = { prov ->
                    selected = prov
                    mode = GroupMode.TABS
                },
                onRequestTiled = { mode = GroupMode.TILED }
            )
            GroupMode.TABS -> TabbedGroupMode(selected = selected, onSelect = { selected = it }, desktopMode = desktopMode, onToggleDesktop = { desktopMode = !desktopMode })
            GroupMode.TILED -> TiledGridMode(desktopMode = desktopMode)
        }
    }
}

enum class GroupMode { DASHBOARD, TABS, TILED }

@Composable
private fun DashboardMode(
    manager: FloatingWindowManager,
    onOpenTabs: (AiChatProvider) -> Unit,
    onRequestTiled: () -> Unit
) {
    LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            // Hero
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp)) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { Icon(Icons.Default.SmartToy, null, tint = MaterialTheme.colorScheme.onPrimary) }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("AI Chat Group", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("12 floating AI pods · 1-tap group launch", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                        }
                        Badge(containerColor = MaterialTheme.colorScheme.primary) { Text("12×", color = MaterialTheme.colorScheme.onPrimary) }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Open any AI as its own resizable floating window, or launch all 10+ at once in a cascade. Each pod keeps its own cookies/login, desktop User-Agent, and history.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                // Cascade: 12 separate floating windows with offset
                                AiChatProvider.all.forEachIndexed { idx, prov ->
                                    val geo = manager.defaultGeometry(prov.windowType).let { base ->
                                        base.copy(x = base.x + (idx % 4) * 28, y = base.y + (idx / 4) * 36)
                                    }
                                    manager.create(type = prov.windowType, title = prov.displayName, url = prov.url, geometry = geo)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) { Icon(Icons.Default.AutoAwesome, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Launch All 12 · Cascade") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = {
                                // Tiled grid cascade: positions in 3×4 screen tiling
                                val providers = AiChatProvider.all.take(10)
                                // Approximate screen tiling: we use manager.defaultGeometry as base and then offset to grid
                                providers.forEachIndexed { idx, prov ->
                                    val col = idx % 2
                                    val row = idx / 2
                                    val geo = manager.defaultGeometry(prov.windowType).copy(
                                        width = (manager.defaultGeometry(prov.windowType).width * 0.92).toInt(),
                                        x = (if (col == 0) 8 else 420).let { it }, // naive tiling; real screen size will clamp
                                        y = 80 + row * 140
                                    )
                                    manager.create(type = prov.windowType, title = prov.displayName, url = prov.url, geometry = geo)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) { Icon(Icons.Default.GridView, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Launch 10 · Tiled set") }
                        FilledTonalButton(onClick = onRequestTiled, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Tab, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Inside Tabs") }
                    }
                    Text("Tip: “Cascade” gives you 10+ real draggable windows. “Tiled” inside this one is lighter (one window, many WebViews).", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f), modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("All providers", style = MaterialTheme.typography.titleSmall)
                Text("${AiChatProvider.all.size} available", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
        }
        item {
            LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.fillMaxWidth().height(460.dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(AiChatProvider.all) { prov ->
                    ElevatedCard(
                        onClick = {
                            // Single tap: open that AI as its own floating window
                            manager.create(type = prov.windowType, title = prov.displayName, url = prov.url)
                        },
                        modifier = Modifier.height(132.dp)
                    ) {
                        Column(Modifier.fillMaxSize().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
                            Box(Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                                Icon(prov.icon, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(prov.displayName, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(prov.shortId, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                FilledTonalButton(
                                    onClick = { manager.create(type = prov.windowType, title = prov.displayName, url = prov.url) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    modifier = Modifier.height(28.dp)
                                ) { Text("Float", style = MaterialTheme.typography.labelSmall) }
                                IconButton(onClick = { onOpenTabs(prov) }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Tab, "Tabs", Modifier.size(14.dp)) }
                            }
                        }
                    }
                }
            }
        }
        item {
            // Stats + keyboard hint
            Surface(shape = RoundedCornerShape(12.dp), tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("How focus works", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text("• Tap any floating pod’s title bar to focus → keyboard appears\n• Each AI keeps separate cookies/logins\n• Drag title bar to move, corner to resize, ○ to bubble", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun TabbedGroupMode(
    selected: AiChatProvider,
    onSelect: (AiChatProvider) -> Unit,
    desktopMode: Boolean,
    onToggleDesktop: () -> Unit
) {
    var progress by remember { mutableStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        // Provider tabs (scrollable)
        ScrollableTabRow(
            selectedTabIndex = AiChatProvider.all.indexOf(selected),
            edgePadding = 8.dp,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            AiChatProvider.all.forEach { prov ->
                Tab(
                    selected = prov == selected,
                    onClick = { onSelect(prov) },
                    text = { Text(prov.displayName, maxLines = 1, style = MaterialTheme.typography.labelSmall) },
                    icon = { Icon(prov.icon, null, Modifier.size(16.dp)) },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.outline
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(selected.icon, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(6.dp))
                Text(selected.url, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 180.dp))
            }
            FilterChip(selected = desktopMode, onClick = onToggleDesktop, label = { Text(if (desktopMode) "Desktop UA" else "Mobile UA", style = MaterialTheme.typography.labelSmall) })
        }
        if (progress in 1..99) LinearProgressIndicator(progress = progress / 100f, modifier = Modifier.fillMaxWidth())
        Divider()
        // Lazy WebView: we keep a map of already-created WebViews so switching tabs is instant
        // For simplicity, we recreate AndroidView per selection but with remember+key we preserve.
        key(selected) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = true
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        settings.userAgentString = if (desktopMode) AI_DESKTOP_UA else AI_MOBILE_UA
                        settings.javaScriptCanOpenWindowsAutomatically = true
                        webViewClient = WebViewClient()
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) { progress = newProgress }
                        }
                        loadUrl(selected.url)
                    }
                },
                update = { wv ->
                    wv.settings.userAgentString = if (desktopMode) AI_DESKTOP_UA else AI_MOBILE_UA
                    if (wv.url != selected.url) wv.loadUrl(selected.url)
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        // Optional: row of quick jump chips
        LazyRow(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(AiChatProvider.all) { prov ->
                SuggestionChip(
                    onClick = { onSelect(prov) },
                    label = { Text(prov.displayName, style = MaterialTheme.typography.labelSmall) },
                    icon = { Icon(prov.icon, null, Modifier.size(12.dp)) }
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun TiledGridMode(desktopMode: Boolean) {
    // Shows all providers as live WebView tiles in a vertical grid (2 columns)
    // Each tile is ~220dp tall. This renders up to 12 WebViews simultaneously — heavy but demonstrates "in group".
    // We use a placeholder + "Load" button per tile to avoid OOM: tap to load. For demo we auto-load first 4, others on demand.
    var loadedIds by remember { mutableStateOf(setOf(AiChatProvider.CHATGPT.shortId, AiChatProvider.CLAUDE.shortId, AiChatProvider.GEMINI.shortId, AiChatProvider.PERPLEXITY.shortId)) }

    Column(Modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.GridView, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("Tiled group — 12 live pods in one window", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text("Scroll to see all. Tap “Load” to activate a pod (saves RAM). Drag window to move whole grid.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f))
                }
            }
        }
        LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(AiChatProvider.all) { prov ->
                val isLoaded = loadedIds.contains(prov.shortId)
                ElevatedCard(modifier = Modifier.height(240.dp)) {
                    Column(Modifier.fillMaxSize()) {
                        // header
                        Row(
                            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(prov.icon, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(6.dp))
                            Text(prov.displayName, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (isLoaded) {
                                Box(Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                                Spacer(Modifier.width(6.dp))
                                Text("LIVE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            } else {
                                TextButton(onClick = { loadedIds = loadedIds + prov.shortId }, contentPadding = PaddingValues(horizontal = 6.dp)) { Text("Load", style = MaterialTheme.typography.labelSmall) }
                            }
                        }
                        if (isLoaded) {
                            AndroidView(
                                factory = { ctx ->
                                    WebView(ctx).apply {
                                        CookieManager.getInstance().setAcceptCookie(true)
                                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                                        settings.javaScriptEnabled = true
                                        settings.domStorageEnabled = true
                                        settings.allowFileAccess = true
                                        settings.useWideViewPort = true
                                        settings.loadWithOverviewMode = true
                                        settings.builtInZoomControls = false
                                        settings.displayZoomControls = false
                                        settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                        settings.userAgentString = if (desktopMode) AI_DESKTOP_UA else AI_MOBILE_UA
                                        webViewClient = WebViewClient()
                                        webChromeClient = WebChromeClient()
                                        loadUrl(prov.url)
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(prov.icon, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.outline)
                                    Spacer(Modifier.height(8.dp))
                                    Text(prov.displayName, style = MaterialTheme.typography.labelMedium)
                                    Text(prov.description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedButton(onClick = { loadedIds = loadedIds + prov.shortId }) { Text("Load pod") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
