package com.floatmaster.apps.launcher

import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.floatmaster.model.FloatingWindow
import com.floatmaster.model.WindowType
import com.floatmaster.service.FloatingWindowManager
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.text.style.TextOverflow
import com.floatmaster.util.TaskbarIntegration
import com.google.accompanist.drawablepainter.rememberDrawablePainter

@Composable
fun AppLauncherContent(window: FloatingWindow) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var packageManager = context.packageManager
    // Use queries-safe approach: launcher intent
    val apps = remember {
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        packageManager.queryIntentActivities(intent, 0).map { it.activityInfo }.sortedBy { it.loadLabel(packageManager).toString().lowercase() }
    }
    val filtered = remember(query, apps) {
        if (query.isBlank()) apps else apps.filter { it.loadLabel(packageManager).toString().contains(query, true) || it.packageName.contains(query, true) }
    }

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            placeholder = { Text("Search apps") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = { if (query.isNotBlank()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Clear, null) } },
            singleLine = true
        )
        Divider()
        LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered) { info ->
                val label = info.loadLabel(packageManager).toString()
                val icon = info.loadIcon(packageManager)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            // Try to launch as floating window where possible
                            // Strategy 1: ActivityView / freeform (Android 12L+)
                            // Strategy 2: Standard launch with NEW_TASK + try to pin via WindowManager
                            // Fallback: regular launch — still useful as quick launcher inside overlay
                            try {
                                val launch = packageManager.getLaunchIntentForPackage(info.packageName)?.apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)
                                }
                                val opts = TaskbarIntegration.freeformOptions() // WHY: request freeform on 12L tablets
                                if (launch != null) {
                                    try {
                                        val bundle = opts?.toBundle()
                                        if (bundle != null) context.startActivity(launch, bundle) else context.startActivity(launch)
                                    } catch (_: Exception) { context.startActivity(launch) }
                                }
                            } catch (e: Exception) { e.printStackTrace() }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(painter = rememberDrawablePainter(icon), contentDescription = label, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(4.dp))
                    Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
        Text("${filtered.size} apps", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(8.dp))
    }
}
