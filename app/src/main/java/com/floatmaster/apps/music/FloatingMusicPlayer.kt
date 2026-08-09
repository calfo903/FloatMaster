package com.floatmaster.apps.music

import android.content.Context
import android.media.AudioManager
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
fun FloatingMusicPlayerContent(window: FloatingWindow) {
    val context = LocalContext.current
    val audio = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    var volume by remember { mutableStateOf(audio.getStreamVolume(AudioManager.STREAM_MUSIC)) }
    val maxVol = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    var isMuted by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.MusicNote, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Text("Music Controls", style = MaterialTheme.typography.titleMedium)
        Text("Controls any app playing audio via system media session", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(24.dp))
        // Media controls via dispatching media key events (works for most players)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { dispatchMediaKey(audio, android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS) }, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.SkipPrevious, "Prev", Modifier.size(32.dp)) }
            FilledIconButton(onClick = { dispatchMediaKey(audio, android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) }, modifier = Modifier.size(64.dp)) { Icon(Icons.Default.PlayArrow, "Play/Pause", Modifier.size(36.dp)) }
            IconButton(onClick = { dispatchMediaKey(audio, android.view.KeyEvent.KEYCODE_MEDIA_NEXT) }, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.SkipNext, "Next", Modifier.size(32.dp)) }
        }
        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = {
                isMuted = !isMuted
                audio.isStreamMute(AudioManager.STREAM_MUSIC)
                // modern: adjustStreamVolume with ADJUST_MUTE
                if (isMuted) audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0)
                else audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0)
            }) { Icon(if (isMuted || volume == 0) Icons.Default.VolumeOff else Icons.Default.VolumeUp, null) }
            Slider(value = volume.toFloat(), onValueChange = {
                volume = it.toInt()
                audio.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0)
            }, valueRange = 0f..maxVol.toFloat(), modifier = Modifier.weight(1f))
            Text("$volume/$maxVol", style = MaterialTheme.typography.labelSmall)
        }
        Spacer(Modifier.height(12.dp))
        Text("Tip: Enable notification access to see track info (MediaSession).", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
    }
}

private fun dispatchMediaKey(audio: AudioManager, keyCode: Int) {
    val eventDown = android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, keyCode)
    val eventUp = android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, keyCode)
    try { audio.dispatchMediaKeyEvent(eventDown); audio.dispatchMediaKeyEvent(eventUp) } catch (_: Exception) {}
}
