package com.floatmaster.permission

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

object BatteryOptimizationHelper {

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun requestIgnoreBatteryOptimizations(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            // fallback to settings screen
            try {
                context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
            } catch (_: Exception) {}
        }
    }

    /** Deep link to OEM auto-start / background management screens */
    fun openOemAutoStartSettings(context: Context): Boolean {
        val intents = listOf(
            // Xiaomi
            Intent().apply { component = ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity") },
            Intent().apply { component = ComponentName("com.miui.securitycenter", "com.miui.securitycenter/com.miui.permcenter.autostart.AutoStartManagementActivity") },
            // Oppo
            Intent().apply { component = ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity") },
            Intent().apply { component = ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity") },
            // Vivo
            Intent().apply { component = ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity") },
            // Huawei
            Intent().apply { component = ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity") },
            Intent().apply { component = ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity") },
            // Samsung
            Intent().apply { component = ComponentName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity") },
            // OnePlus
            Intent().apply { component = ComponentName("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity") },
        )
        for (intent in intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return true
            } catch (_: Exception) { continue }
        }
        return false
    }

    fun getOemName(): String = Build.MANUFACTURER.lowercase().replaceFirstChar { it.uppercase() }

    fun getOemInstructions(): String = when (Build.MANUFACTURER.lowercase()) {
        "xiaomi" -> "MIUI: Settings → Apps → Manage apps → FloatMaster → Autostart → Enable. Then Battery saver → No restrictions."
        "oppo", "realme", "oneplus" -> "ColorOS/OxygenOS: Settings → Battery → App autostart → Enable FloatMaster. Also allow background activity."
        "vivo" -> "Funtouch OS: Settings → Battery → Background power consumption → FloatMaster → Allow."
        "huawei", "honor" -> "EMUI: Settings → Battery → App launch → FloatMaster → Manage manually → Enable all toggles."
        "samsung" -> "One UI: Settings → Battery → Background usage limits → Never sleeping apps → Add FloatMaster."
        else -> "Settings → Battery → Battery optimization → All apps → FloatMaster → Don't optimize."
    }
}
