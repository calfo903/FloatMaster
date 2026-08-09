package com.floatmaster.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.floatmaster.service.FloatingService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in listOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED, "android.intent.action.PACKAGE_REPLACED")) {
            // Only restart if user had windows open or dock enabled — check preference
            // For demo, don't auto-start without user consent to avoid Play Store policy strike
            // Instead, post a notification offering to restore
            // Uncomment to auto-restore:
            // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(Intent(context, FloatingService::class.java).apply { action = FloatingService.ACTION_SHOW })
            // else context.startService(Intent(context, FloatingService::class.java).apply { action = FloatingService.ACTION_SHOW })
        }
    }
}
