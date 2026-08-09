package com.floatmaster.ui.screens

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.floatmaster.apps.aichat.AiChatProvider
import com.floatmaster.apps.url.UrlCreatorDialog
import com.floatmaster.model.MiniAppCatalog
import com.floatmaster.model.WindowType
import com.floatmaster.permission.OverlayPermissionHandler
import com.floatmaster.service.FloatingService
import com.floatmaster.service.FloatingWindowManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(manager: FloatingWindowManager) {
    val context = LocalContext.current
    val windows by manager.windows.collectAsState()
    var showUrlDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FloatMaster") },
                actions = {
                    BadgedBox(badge = { if (windows.isNotEmpty()) Badge { Text("${windows.size}") } }) {
                        IconButton(onClick = { selectedTab = 1 }) { Icon(Icons.Default.Layers, null) }
                    }
                    IconButton(onClick = { manager.closeAll() }) { Icon(Icons.Default.Close, "Close all") }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (!OverlayPermissionHandler.hasPermission(context)) {
                        OverlayPermissionHandler.requestPermission(context)
                    } else {
                        FloatingService.start(context)
                    }
                },
                icon = { Icon(Icons.Default.Bolt, null) },
                text = { Text("Launch Dock") }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Mini Apps") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Active (${windows.size})") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Settings") })
            }
            when (selectedTab) {
                0 -> MiniAppsGrid(manager, onUrlClick = { showUrlDialog = true })
                1 -> ActiveWindowsList(manager)
                2 -> SettingsTab(manager)
            }
        }
    }
    if (showUrlDialog) UrlCreatorDialog(manager) { showUrlDialog = false }
}

@Composable
private fun MiniAppsGrid(manager: FloatingWindowManager, onUrlClick: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Hero AI Group
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(44.dp)) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { Icon(Icons.Default.SmartToy, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp)) }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("AI Chats · 12 Floating Pods", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("ChatGPT · Claude · Gemini · Perplexity · Grok · DeepSeek · +6 more", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                        }
                        Badge(containerColor = MaterialTheme.colorScheme.primary) { Text("NEW", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelSmall) }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Group of 10+ AI chats using WebView (iframe-ready). Each pod keeps its own login/cookies, draggable, resizable, minimizable to bubble. Launch them all at once or tab them inside one window.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { manager.create(WindowType.AI_GROUP) },
                            modifier = Modifier.weight(1f)
                        ) { Icon(Icons.Default.Groups, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Open AI Group") }
                        FilledTonalButton(
                            onClick = {
                                // One-tap cascade: open 10 as separate windows
                                AiChatProvider.all.take(10).forEachIndexed { idx, prov ->
                                    val geo = manager.defaultGeometry(prov.windowType).let { base ->
                                        base.copy(x = base.x + (idx % 3) * 32, y = base.y + (idx / 3) * 40)
                                    }
                                    manager.create(type = prov.windowType, title = prov.displayName, url = prov.url, geometry = geo)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) { Icon(Icons.Default.AutoAwesome, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Launch 10 Now") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { manager.create(WindowType.AI_CHATGPT) }, modifier = Modifier.weight(1f)) { Text("ChatGPT", style = MaterialTheme.typography.labelSmall) }
                        OutlinedButton(onClick = { manager.create(WindowType.AI_CLAUDE) }, modifier = Modifier.weight(1f)) { Text("Claude", style = MaterialTheme.typography.labelSmall) }
                        OutlinedButton(onClick = { manager.create(WindowType.AI_GEMINI) }, modifier = Modifier.weight(1f)) { Text("Gemini", style = MaterialTheme.typography.labelSmall) }
                    }
                }
            }
        }
        // Quick AI strip
        item {
            Text("Tap to float single AI · or open the Group for 12 at once", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                items(AiChatProvider.all) { prov ->
                    ElevatedCard(
                        onClick = { manager.create(type = prov.windowType, title = prov.displayName, url = prov.url) },
                        modifier = Modifier.width(96.dp).height(92.dp)
                    ) {
                        Column(Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Box(Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                                Icon(prov.icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(prov.displayName, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                            Text(prov.shortId, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TipsAndUpdates, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("How to use AI Group", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text("Inside AI Group: Dashboard → “Launch All 12 Cascade” = 12 draggable windows. Tabs → single window with 12 tabs. Tiled → 2-col grid of live pods (tap Load to save RAM).", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onUrlClick, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Link, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("New URL Window") }
                FilledTonalButton(onClick = { manager.create(WindowType.APP_LAUNCHER) }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Apps, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("All Apps") }
            }
        }
        item {
            Text("All mini-apps", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
        // Grid of non-AI mini apps
        item {
            // We put them in a column of rows manually because LazyVerticalGrid inside LazyColumn needs fixed height
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniAppCatalog.all.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        row.forEach { app ->
                            ElevatedCard(
                                onClick = {
                                    if (app.type == WindowType.URL_WINDOW) onUrlClick() else manager.create(app.type)
                                },
                                modifier = Modifier.weight(1f).height(110.dp)
                            ) {
                                Column(Modifier.fillMaxSize().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                    Box(Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                                        Icon(app.type.icon, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text(app.title, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                                    Text(app.description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, maxLines = 1)
                                }
                            }
                        }
                        // pad row to 3 columns
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun ActiveWindowsList(manager: FloatingWindowManager) {
    val windows by manager.windows.collectAsState()
    if (windows.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Icon(Icons.Default.LayersClear, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.height(12.dp))
                Text("No active floating windows", style = MaterialTheme.typography.titleMedium)
                Text("Open AI Group → Launch 10, or any mini-app. Each window can be minimized to bubble, pinned, or made transparent.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(windows.reversed(), key = { it.id }) { w ->
            ElevatedCard(Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text(w.title) },
                    supportingContent = { Text("${w.type.name} • ${w.state.name} • ${w.geometry.width}×${w.geometry.height}") },
                    leadingContent = {
                        Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                            Icon(w.type.icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    },
                    trailingContent = {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = { manager.bringToFront(w.id) }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.FlipToFront, null, Modifier.size(18.dp)) }
                            IconButton(onClick = { manager.bubble(w.id) }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Circle, null, Modifier.size(18.dp)) }
                            IconButton(onClick = { manager.close(w.id) }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Close, null, Modifier.size(18.dp)) }
                        }
                    }
                )
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SuggestionChip(onClick = { manager.minimize(w.id) }, label = { Text("Minimize") })
                    SuggestionChip(onClick = { manager.maximize(w.id) }, label = { Text(if (w.isMaximized) "Restore" else "Maximize") })
                    SuggestionChip(onClick = { manager.toggleBorder(w.id) }, label = { Text("Border") })
                }
            }
        }
    }
}

@Composable
private fun SettingsTab(manager: FloatingWindowManager) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("AI Group Settings", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("WebView: JS, DOM storage, third-party cookies enabled. Desktop UA by default (fixes Claude/Gemini). Toggle per-pod Mobile/Desktop in its toolbar.", style = MaterialTheme.typography.bodySmall)
                Text("Tip: If an AI shows “Browser not supported”, toggle to Desktop and reload. Logs you in via Google/OpenAI cookies preserved per pod.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
        }
        Text("Appearance", style = MaterialTheme.typography.titleSmall)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Dark mode")
                    Switch(checked = false, onCheckedChange = {})
                }
                Divider()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Snap to edge")
                    Switch(checked = true, onCheckedChange = {})
                }
            }
        }
        Text("Overlay controls", style = MaterialTheme.typography.titleSmall)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { FloatingService.start(context) }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text("Start Floating Service") }
                OutlinedButton(onClick = { FloatingService.stop(context) }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Stop, null); Spacer(Modifier.width(8.dp)); Text("Stop All & Dismiss") }
                OutlinedButton(onClick = { OverlayPermissionHandler.requestPermission(context) }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Settings, null); Spacer(Modifier.width(8.dp)); Text("Overlay Permission Settings") }
            }
        }
        Text("Gestures", style = MaterialTheme.typography.titleSmall)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("• Drag title bar to move", style = MaterialTheme.typography.bodySmall)
                Text("• Drag corner to resize", style = MaterialTheme.typography.bodySmall)
                Text("• Tap bubble to restore", style = MaterialTheme.typography.bodySmall)
                Text("• Long-press title for menu", style = MaterialTheme.typography.bodySmall)
                Text("• Slider in menu adjusts transparency", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
