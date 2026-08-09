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
import com.floatmaster.data.SessionRepository
import com.floatmaster.model.WindowState
import com.floatmaster.overlay.BubbleView
import com.floatmaster.overlay.FloatingDock
import com.floatmaster.overlay.FloatingWindowContainer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FloatingService : Service(), ViewModelStoreOwner, SavedStateRegistryOwner {

    @Inject lateinit var windowManager: FloatingWindowManager
    @Inject lateinit var sessionRepository: SessionRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var restorationComplete = false

    // WHY: ComposeView needs a real lifecycle/saved-state owner even though the host is a Service.
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore = ViewModelStore()
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

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
        observePersistence()
        ensureDock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_RESTORE -> restorePersistedSession()
            ACTION_CLOSE_ALL -> {
                restorationComplete = true
                windowManager.closeAll()
                serviceScope.launch(Dispatchers.IO) { sessionRepository.clear() }
            }
            ACTION_STOP -> {
                restorationComplete = true
                windowManager.closeAll()
                serviceScope.launch(Dispatchers.IO) { sessionRepository.clear() }
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TOGGLE_DOCK -> {
                restorationComplete = true
                if (dock == null) ensureDock() else removeDock()
            }
            null -> restorePersistedSession() // WHY: START_STICKY process recreation has no original Intent.
            else -> restorationComplete = true
        }

        promoteToForeground()
        return START_STICKY
    }

    private fun restorePersistedSession() {
        if (restorationComplete) return
        serviceScope.launch(Dispatchers.IO) {
            val saved = sessionRepository.restore()
            windowManager.restoreSession(saved)
            restorationComplete = true
        }
    }

    private fun promoteToForeground() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIF_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(NOTIF_ID, buildNotification())
            }
        } catch (_: SecurityException) {
            // WHY: OS/OEM can reject promotion; the service must not crash the process.
        } catch (_: IllegalStateException) {
            // WHY: Background-start policy failures must be contained.
        }
    }

    private fun startForegroundNotification() = promoteToForeground()

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
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("FloatMaster • $count window(s)")
            .setContentText("Floating windows are active")
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
                runCatching {
                    val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
                    nm.notify(NOTIF_ID, buildNotification())
                }

                val ids = windows.map { it.id.toString() }.toSet()
                (containers.keys - ids).forEach { id ->
                    containers.remove(id)?.destroy()
                }
                (bubbles.keys - ids).forEach { id ->
                    bubbles.remove(id)?.destroy()
                }

                windows.forEach { win ->
                    val id = win.id.toString()
                    when (win.state) {
                        WindowState.BUBBLE -> {
                            containers.remove(id)?.destroy()
                            val bubble = bubbles.getOrPut(id) {
                                BubbleView(this@FloatingService, win, windowManager, this@FloatingService)
                            }
                            bubble.update(win)
                        }
                        WindowState.CLOSED -> Unit
                        else -> {
                            bubbles.remove(id)?.destroy()
                            val container = containers.getOrPut(id) {
                                FloatingWindowContainer(this@FloatingService, win, windowManager, this@FloatingService)
                            }
                            container.update(win)
                        }
                    }
                }
            }
        }
    }

    private fun observePersistence() {
        serviceScope.launch(Dispatchers.IO) {
            windowManager.windows
                .debounce(500)
                .collectLatest { windows ->
                    if (restorationComplete) sessionRepository.save(windows)
                }
        }
    }

    private fun ensureDock() {
        if (dock != null) return
        dock = runCatching { FloatingDock(this, windowManager, this) }.getOrNull()
    }

    private fun removeDock() {
        dock?.destroy()
        dock = null
    }

    override fun onDestroy() {
        containers.values.forEach { it.destroy() }
        containers.clear()
        bubbles.values.forEach { it.destroy() }
        bubbles.clear()
        removeDock()
        serviceScope.cancel()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        viewModelStore.clear()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIF_ID = 1001
        const val ACTION_SHOW = "floatmaster.SHOW"
        const val ACTION_RESTORE = "floatmaster.RESTORE"
        const val ACTION_CLOSE_ALL = "floatmaster.CLOSE_ALL"
        const val ACTION_STOP = "floatmaster.STOP"
        const val ACTION_TOGGLE_DOCK = "floatmaster.TOGGLE_DOCK"

        fun start(context: android.content.Context) {
            val intent = Intent(context, FloatingService::class.java).apply { action = ACTION_SHOW }
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
                else context.startService(intent)
            }
        }

        fun stop(context: android.content.Context) {
            runCatching {
                context.startService(Intent(context, FloatingService::class.java).apply { action = ACTION_STOP })
            }
        }
    }
}
