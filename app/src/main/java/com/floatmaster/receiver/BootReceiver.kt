package com.floatmaster.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.floatmaster.data.SessionRepository
import com.floatmaster.service.FloatingService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * WHY: Reboot/package-update recovery is explicit and state-driven; a dead/no-session app never starts an FGS.
 * The receiver only restores a session that FloatMaster previously persisted and only when overlay access remains granted.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject lateinit var sessionRepository: SessionRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED)) return
        if (!Settings.canDrawOverlays(context)) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (!sessionRepository.hasSavedSession()) return@launch
                val serviceIntent = Intent(context, FloatingService::class.java).apply {
                    action = FloatingService.ACTION_RESTORE
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } catch (_: SecurityException) {
                // WHY: OEM/OS policy can reject an FGS start; never crash the broadcast process.
            } catch (_: IllegalStateException) {
                // WHY: Background-start restrictions are expected on some OEM builds.
            } finally {
                pendingResult.finish()
            }
        }
    }
}
