package com.floatmaster.apps.clock

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.floatmaster.model.FloatingWindow
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun FloatingClockContent(window: FloatingWindow) {
    var tab by remember { mutableStateOf(0) } // 0 clock 1 stopwatch 2 timer
    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Clock") }, icon = { Icon(Icons.Default.Schedule, null) })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Stopwatch") }, icon = { Icon(Icons.Default.Timer, null) })
            Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Timer") }, icon = { Icon(Icons.Default.HourglassBottom, null) })
        }
        Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            when (tab) {
                0 -> ClockTab()
                1 -> StopwatchTab()
                2 -> TimerTab()
            }
        }
    }
}

@Composable
private fun ClockTab() {
    var now by remember { mutableStateOf(Calendar.getInstance()) }
    LaunchedEffect(Unit) { while (true) { delay(1000); now = Calendar.getInstance() } }
    val fmtTime = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val fmtDate = remember { SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Analog
        val seconds = now.get(Calendar.SECOND)
        val minutes = now.get(Calendar.MINUTE)
        val hours = now.get(Calendar.HOUR)
        Canvas(modifier = Modifier.size(140.dp)) {
            val r = size.minDimension / 2
            val cx = center.x; val cy = center.y
            drawCircle(color = Color(0xFFE7E0EC), radius = r)
            drawCircle(color = Color(0xFF6750A4), radius = r, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f))
            // ticks
            for (i in 0 until 12) {
                val angle = Math.toRadians((i * 30 - 90).toDouble())
                val start = Offset((cx + cos(angle) * (r - 8)).toFloat(), (cy + sin(angle) * (r - 8)).toFloat())
                val end = Offset((cx + cos(angle) * (r - 2)).toFloat(), (cy + sin(angle) * (r - 2)).toFloat())
                drawLine(Color.Gray, start, end, strokeWidth = 3f, cap = StrokeCap.Round)
            }
            fun hand(angleDeg: Float, length: Float, width: Float, color: Color) {
                rotate(angleDeg) {
                    drawLine(color, Offset(cx, cy), Offset(cx, cy - length), strokeWidth = width, cap = StrokeCap.Round)
                }
            }
            hand(hours * 30f + minutes * 0.5f, r * 0.5f, 8f, Color(0xFF1C1B1F))
            hand(minutes * 6f, r * 0.7f, 6f, Color(0xFF49454F))
            hand(seconds * 6f, r * 0.85f, 3f, Color(0xFFBA1A1A))
            drawCircle(Color(0xFF6750A4), radius = 8f, center = Offset(cx, cy))
        }
        Spacer(Modifier.height(16.dp))
        Text(fmtTime.format(now.time), style = MaterialTheme.typography.headlineMedium)
        Text(fmtDate.format(now.time), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun StopwatchTab() {
    var running by remember { mutableStateOf(false) }
    var elapsed by remember { mutableStateOf(0L) }
    var base by remember { mutableStateOf(0L) }
    LaunchedEffect(running) {
        base = System.currentTimeMillis() - elapsed
        while (running) { delay(50); elapsed = System.currentTimeMillis() - base }
    }
    fun fmt(ms: Long): String {
        val s = ms / 1000; val m = s / 60; val h = m / 60
        return String.format("%02d:%02d:%02d.%01d", h, m % 60, s % 60, (ms % 1000) / 100)
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(fmt(elapsed), style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { running = !running }) { Text(if (running) "Pause" else "Start") }
            OutlinedButton(onClick = { running = false; elapsed = 0 }) { Text("Reset") }
        }
    }
}

@Composable
private fun TimerTab() {
    var totalSec by remember { mutableStateOf(300) }
    var remaining by remember { mutableStateOf(totalSec) }
    var running by remember { mutableStateOf(false) }
    LaunchedEffect(running, totalSec) {
        if (running) {
            remaining = totalSec
            while (running && remaining > 0) { delay(1000); remaining-- }
            if (remaining == 0) running = false
        }
    }
    fun fmt(s: Int) = String.format("%02d:%02d", s / 60, s % 60)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(fmt(remaining), style = MaterialTheme.typography.displayMedium, color = if (remaining < 10 && running) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(60, 300, 600, 1800).forEach { sec ->
                FilterChip(selected = totalSec == sec, onClick = { totalSec = sec; remaining = sec; running = false }, label = { Text("${sec/60}m") })
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { running = !running }, enabled = remaining > 0) { Text(if (running) "Pause" else "Start") }
            OutlinedButton(onClick = { running = false; remaining = totalSec }) { Text("Reset") }
        }
        if (!running && remaining == 0 && totalSec > 0) {
            Spacer(Modifier.height(8.dp))
            Text("Time's up!", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleMedium)
        }
    }
}
