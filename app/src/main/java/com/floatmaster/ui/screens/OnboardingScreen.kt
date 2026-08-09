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
    var step by remember { mutableStateOf(0) }
    val hasOverlay = remember(step) { OverlayPermissionHandler.hasPermission(context) }
    val isIgnoringBattery = remember(step) { BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context) }

    Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.ViewInAr, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Text("Welcome to FloatMaster", style = MaterialTheme.typography.headlineSmall)
        Text("Floating multitasking, reimagined", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(24.dp))

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Step 1 • Display over other apps", style = MaterialTheme.typography.titleSmall)
                Text("FloatMaster needs overlay permission to show floating windows on top of any app.", style = MaterialTheme.typography.bodySmall)
                if (hasOverlay) {
                    AssistChip(onClick = {}, label = { Text("Granted ✓") }, leadingIcon = { Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary) })
                } else {
                    Button(onClick = { OverlayPermissionHandler.requestPermission(context) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Layers, null); Spacer(Modifier.width(8.dp)); Text("Grant Overlay Permission")
                    }
                    Text("On the next screen: Find FloatMaster → Allow display over other apps", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Step 2 • Stay alive in background (recommended)", style = MaterialTheme.typography.titleSmall)
                Text("Android and OEM skins kill background apps. Whitelist FloatMaster so windows stay alive.", style = MaterialTheme.typography.bodySmall)
                Text(BatteryOptimizationHelper.getOemInstructions(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                if (isIgnoringBattery) {
                    AssistChip(onClick = {}, label = { Text("Whitelisted ✓") }, leadingIcon = { Icon(Icons.Default.CheckCircle, null) })
                } else {
                    OutlinedButton(onClick = { BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(context) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.BatterySaver, null); Spacer(Modifier.width(8.dp)); Text("Disable Battery Optimization")
                    }
                    TextButton(onClick = { BatteryOptimizationHelper.openOemAutoStartSettings(context) }) { Text("Open ${BatteryOptimizationHelper.getOemName()} auto-start settings") }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Step 3 • Optional: Accessibility (advanced)", style = MaterialTheme.typography.titleSmall)
                Text("Enables auto-minimize, gestures, and app-specific pinning. Completely optional.", style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = {
                    try { context.startActivity(android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) }) } catch (_: Exception) {}
                }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Accessibility, null); Spacer(Modifier.width(8.dp)); Text("Open Accessibility Settings")
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onComplete,
            enabled = hasOverlay,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text(if (hasOverlay) "Start Floating ✨" else "Grant overlay to continue") }
        if (!hasOverlay) Text("You can still explore the app, but floating windows require the overlay permission.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onComplete) { Text("Skip for now") }
    }
}
