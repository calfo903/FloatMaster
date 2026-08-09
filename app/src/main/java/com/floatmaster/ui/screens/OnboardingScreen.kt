package com.floatmaster.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.floatmaster.permission.BatteryOptimizationHelper
import com.floatmaster.permission.OverlayPermissionHandler

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    var step by remember { mutableIntStateOf(0) }
    val hasOverlay = remember(step) { OverlayPermissionHandler.hasPermission(context) }

    Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.ViewInAr, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Text("Welcome to FloatMaster", style = MaterialTheme.typography.headlineSmall)
        Text("Floating multitasking, reimagined", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(24.dp))

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Step 1 • Display over other apps", style = MaterialTheme.typography.titleSmall)
                Text("FloatMaster needs overlay permission to show the floating windows you create while you use other apps.", style = MaterialTheme.typography.bodySmall)
                if (hasOverlay) {
                    AssistChip(onClick = {}, label = { Text("Granted ✓") }, leadingIcon = { Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary) })
                } else {
                    Button(onClick = { OverlayPermissionHandler.requestPermission(context) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Layers, null); Spacer(Modifier.width(8.dp)); Text("Grant Overlay Permission")
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Step 2 • OEM background settings", style = MaterialTheme.typography.titleSmall)
                Text("Some manufacturers aggressively stop background processes. If windows stop after the screen locks, use your device's app-startup settings.", style = MaterialTheme.typography.bodySmall)
                Text(BatteryOptimizationHelper.getOemInstructions(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                OutlinedButton(onClick = { BatteryOptimizationHelper.openOemAutoStartSettings(context) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Settings, null); Spacer(Modifier.width(8.dp)); Text("Open OEM auto-start settings")
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Step 3 • Notifications", style = MaterialTheme.typography.titleSmall)
                Text("FloatMaster uses a low-importance ongoing notification while floating windows are active. It provides Stop and Close All controls.", style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(Modifier.height(24.dp))
        Button(onClick = onComplete, enabled = hasOverlay, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text(if (hasOverlay) "Start Floating ✨" else "Grant overlay to continue")
        }
        if (!hasOverlay) Text("Floating windows require overlay permission.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onComplete) { Text("Skip for now") }
    }
}
