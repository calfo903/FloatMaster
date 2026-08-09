package com.floatmaster.apps.widget

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.floatmaster.model.FloatingWindow

/**
 * Embeds a home-screen widget inside a floating window.
 * Uses AppWidgetHost. Requires user to pick widget via AppWidgetHost.startListening() + picker intent.
 *
 * Flow:
 *  1. AppWidgetHost.allocateAppWidgetId()
 *  2. ACTION_APPWIDGET_PICK intent
 *  3. On result, AppWidgetHost.createView() → add to floating container
 *
 * This Composable is a placeholder that explains the flow and launches the picker.
 */
@Composable
fun WidgetPickerContent(window: FloatingWindow) {
    val context = LocalContext.current
    var widgetId by remember { mutableStateOf(window.appWidgetId) }

    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.Widgets, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Text("Floating Widget", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Turn any home screen widget into a floating window. " +
                    "Tap below to pick a widget (Clock, Weather, Calendar, etc.). " +
                    "The widget will live inside this floating container.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            // In MainActivity, launch:
            // val host = AppWidgetHost(context, 1024)
            // val id = host.allocateAppWidgetId()
            // val pick = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply { putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id) }
            // startActivityForResult(pick, REQUEST_PICK_WIDGET)
            // Then create FloatingWindow(type=WIDGET, appWidgetId=id)
        }) {
            Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Pick Widget")
        }
        if (widgetId != null) {
            Spacer(Modifier.height(12.dp))
            Text("Widget ID: $widgetId", style = MaterialTheme.typography.labelSmall)
            Text("HostView would be embedded here via AndroidView + AppWidgetHostView", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}

/**
 * Helper to create a host view for a given appWidgetId — called inside FloatingWindowContainer when type==WIDGET.
 *
 * fun createHostView(context: Context, appWidgetId: Int): AppWidgetHostView {
 *   val manager = AppWidgetManager.getInstance(context)
 *   val host = AppWidgetHost(context, 0xF00D)
 *   host.startListening()
 *   val info = manager.getAppWidgetInfo(appWidgetId)
 *   return host.createView(context, appWidgetId, info).apply {
 *     setAppWidget(appWidgetId, info)
 *   }
 * }
 */
