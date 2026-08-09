package com.floatmaster

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FloatMasterApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FloatMasterContext.initialize(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val serviceChannel = NotificationChannel(CHANNEL_SERVICE, "FloatMaster Service", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Keeps user-requested floating windows active"
                setShowBadge(false)
            }
            val timerChannel = NotificationChannel(CHANNEL_TIMER, "Timers & Alarms", NotificationManager.IMPORTANCE_HIGH)
            val clipboardChannel = NotificationChannel(CHANNEL_CLIPBOARD, "Clipboard", NotificationManager.IMPORTANCE_LOW)
            manager.createNotificationChannels(listOf(serviceChannel, timerChannel, clipboardChannel))
        }
    }

    companion object {
        const val CHANNEL_SERVICE = "floatmaster_service"
        const val CHANNEL_TIMER = "floatmaster_timer"
        const val CHANNEL_CLIPBOARD = "floatmaster_clipboard"
    }
}
