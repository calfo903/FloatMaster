package com.floatmaster.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.*

/**
 * Lightweight watchdog that restarts FloatingService if OEM kills it.
 * Some OEMs (Xiaomi, Oppo) aggressively kill foreground services — this + BootReceiver + notification helps.
 * Android 12+ restricts starting FGS from background, so we only restart when app is in foreground or via alarm.
 */
class KeepAliveService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scope.launch {
            while (isActive) {
                delay(30_000)
                // heartbeat: ensure FloatingService still alive — check via ActivityManager if needed
                // For now, no-op; real implementation would check and restart
            }
        }
        return START_STICKY
    }
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() { scope.cancel(); super.onDestroy() }
}
