package com.floatmaster.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import com.floatmaster.FloatMasterApp
import com.floatmaster.MainActivity
import com.floatmaster.R
import com.floatmaster.model.WindowState
import com.floatmaster.overlay.BubbleView
import com.floatmaster.overlay.FloatingDock
import com.floatmaster.overlay.FloatingWindowContainer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

@AndroidEntryPoint
class FloatingService : Service(), ViewModelStoreOwner, SavedStateRegistryOwner {

    @Inject lateinit var windowManager: FloatingWindowManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // We need to be a LifecycleOwner so ComposeView can work inside WindowManager
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore = ViewModelStore()
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    // Maps windowId -> container / bubble
    private val containers = mutableMapOf<String, FloatingWindowContainer>()
    private val bubbles = mutableMapOf<String, BubbleView>()
    private var dock: FloatingDock? = null

    override fun onCreate() {
        super.onCreate()
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        startForegroundNotification()
        observeWindows()
        ensureDock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CLOSE_ALL -> {
                windowManager.closeAll()
                // keep service for dock unless user explicitly stops
            }
            ACTION_STOP -> {
                windowManager.closeAll()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TOGGLE_DOCK -> {
                if (dock == null) ensureDock() else removeDock()
            }
        }
        // Promote to foreground again (Android 14 needs explicit type)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(
                        NOTIF_ID, buildNotification(),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                } else {
                    startForeground(NOTIF_ID, buildNotification())
                }
            } else {
                startForeground(NOTIF_ID, buildNotification())
            }
        } catch (_: Exception) {}
        return START_STICKY
    }

    private fun startForegroundNotification() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIF_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(NOTIF_ID, buildNotification())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val closeAllIntent = PendingIntent.getService(
            this, 1, Intent(this, FloatingService::class.java).apply { action = ACTION_CLOSE_ALL },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 2, Intent(this, FloatingService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val count = windowManager.allWindows().size
        return NotificationCompat.Builder(this, FloatMasterApp.CHANNEL_SERVICE)
            .setSmallIcon(R.drawable.ic_notification) // vector fallback needed
            .setContentTitle("FloatMaster • $count window(s)")
            .setContentText("Tap to open manager • Floating windows active")
            .setOngoing(true)
            .setContentIntent(openIntent)
            .addAction(R.drawable.ic_close, "Close all", closeAllIntent)
            .addAction(R.drawable.ic_stop, "Stop", stopIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun observeWindows() {
        serviceScope.launch {
            windowManager.windows.collectLatest { windows ->
                // Update notification
                try {
                    val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
                    nm.notify(NOTIF_ID, buildNotification())
                } catch (_: Exception) {}

                val ids = windows.map { it.id }.toSet()
                // Remove containers/bubbles for closed windows
                (containers.keys - ids).forEach { id ->
                    containers[id]?.destroy()
                    containers.remove(id)
                }
                (bubbles.keys - ids).forEach { id ->
                    bubbles[id]?.destroy()
                    bubbles.remove(id)
                }
                // Create or update remaining
                windows.forEach { win ->
                    when (win.state) {
                        WindowState.BUBBLE -> {
                            // ensure bubble, remove container
                            containers[win.id]?.destroy()
                            containers.remove(win.id)
                            val b = bubbles.getOrPut(win.id) {
                                BubbleView(this@FloatingService, win, windowManager, this@FloatingService)
                            }
                            b.update(win)
                        }
                        WindowState.CLOSED -> { /* handled */ }
                        else -> {
                            bubbles[win.id]?.destroy()
                            bubbles.remove(win.id)
                            val c = containers.getOrPut(win.id) {
                                FloatingWindowContainer(this@FloatingService, win, windowManager, this@FloatingService)
                            }
                            c.update(win)
                        }
                    }
                }
            }
        }
    }

    private fun ensureDock() {
        if (dock != null) return
        try {
            dock = FloatingDock(this, windowManager, this)
        } catch (_: Exception) {}
    }

    private fun removeDock() {
        dock?.destroy()
        dock = null
    }

    override fun onDestroy() {
        serviceScope.cancel()
        containers.values.forEach { it.destroy() }
        containers.clear()
        bubbles.values.forEach { it.destroy() }
        bubbles.clear()
        removeDock()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        viewModelStore.clear()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIF_ID = 1001
        const val ACTION_SHOW = "floatmaster.SHOW"
        const val ACTION_CLOSE_ALL = "floatmaster.CLOSE_ALL"
        const val ACTION_STOP = "floatmaster.STOP"
        const val ACTION_TOGGLE_DOCK = "floatmaster.TOGGLE_DOCK"

        fun start(context: android.content.Context) {
            val i = Intent(context, FloatingService::class.java).apply { action = ACTION_SHOW }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(i)
            else context.startService(i)
        }
        fun stop(context: android.content.Context) {
            context.startService(Intent(context, FloatingService::class.java).apply { action = ACTION_STOP })
        }
    }
}
