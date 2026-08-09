package com.floatmaster.apps.quicksettings

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings
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

@Composable
fun FloatingQuickSettingsContent(window: FloatingWindow) {
    val context = LocalContext.current
    val audio = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    var brightness by remember { mutableStateOf(getBrightness(context)) }
    var volume by remember { mutableStateOf(audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()) }
    val maxVol = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat()
    var brightnessAuto by remember { mutableStateOf(Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, 0) == 1) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Quick Settings", style = MaterialTheme.typography.titleMedium)
        // Brightness
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Brightness6, null); Spacer(Modifier.width(8.dp)); Text("Brightness") ; Spacer(Modifier.weight(1f)); Switch(checked = brightnessAuto, onCheckedChange = { checked ->
                    brightnessAuto = checked
                    Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, if (checked) 1 else 0)
                }) ; Text("Auto", style = MaterialTheme.typography.labelSmall) }
                Slider(value = brightness.toFloat(), onValueChange = {
                    brightness = it.toInt()
                    if (!brightnessAuto) setBrightness(context, brightness)
                }, valueRange = 10f..255f, enabled = !brightnessAuto)
                Text("${(brightness / 255f * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
        }
        // Volume
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.VolumeUp, null); Spacer(Modifier.width(8.dp)); Text("Media Volume") }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Slider(value = volume, onValueChange = {
                        volume = it
                        audio.setStreamVolume(AudioManager.STREAM_MUSIC, it.toInt(), 0)
                    }, valueRange = 0f..maxVol, modifier = Modifier.weight(1f))
                    Text("${volume.toInt()}/${maxVol.toInt()}", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        // Toggles
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickTile(Icons.Default.Wifi, "Wi-Fi") { context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) }
            QuickTile(Icons.Default.Bluetooth, "Bluetooth") { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) }
            QuickTile(Icons.Default.ScreenRotation, "Auto-rotate") { context.startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickTile(Icons.Default.DarkMode, "Dark theme") { context.startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) }
            QuickTile(Icons.Default.FlashlightOn, "Flashlight") {
                // Requires camera permission — placeholder
            }
            QuickTile(Icons.Default.AirplanemodeActive, "Airplane") { context.startActivity(Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) }
        }
        Text("Some toggles open system settings due to Android 10+ restrictions.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun RowScope.QuickTile(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.weight(1f).height(72.dp)) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, Modifier.size(24.dp)); Spacer(Modifier.height(4.dp)); Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

private fun getBrightness(ctx: Context): Int = try { Settings.System.getInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS) } catch (_: Exception) { 128 }
private fun setBrightness(ctx: Context, v: Int) {
    try {
        if (Settings.System.canWrite(ctx)) Settings.System.putInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS, v)
        // Also update window brightness if possible
    } catch (_: Exception) {}
}
