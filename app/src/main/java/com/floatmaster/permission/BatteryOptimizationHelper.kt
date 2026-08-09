package com.floatmaster.permission

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * OEM background-start guidance only.
 * WHY: FloatMaster does not request the high-risk battery-optimization exemption permission.
 */
object BatteryOptimizationHelper {
    fun openOemAutoStartSettings(context: Context): Boolean {
        val intents = listOf(
            Intent().apply { component = ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity") },
            Intent().apply { component = ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity") },
            Intent().apply { component = ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity") },
            Intent().apply { component = ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity") },
            Intent().apply { component = ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity") },
            Intent().apply { component = ComponentName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity") },
            Intent().apply { component = ComponentName("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity") }
        )
        for (intent in intents) {
            runCatching {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return true
            }
        }
        return false
    }

    fun getOemName(): String = Build.MANUFACTURER.lowercase().replaceFirstChar { it.uppercase() }

    fun getOemInstructions(): String = when (Build.MANUFACTURER.lowercase()) {
        "xiaomi" -> "MIUI/HyperOS: Settings → Apps → Manage apps → FloatMaster → Autostart → Enable."
        "oppo", "realme", "oneplus" -> "ColorOS/OxygenOS: Settings → Battery → App autostart → Enable FloatMaster."
        "vivo" -> "Funtouch OS: Settings → Battery → Background power consumption → FloatMaster → Allow."
        "huawei", "honor" -> "EMUI: Settings → Battery → App launch → FloatMaster → Manage manually."
        "samsung" -> "One UI: Settings → Battery → Background usage limits → Never sleeping apps → Add FloatMaster."
        else -> "If your device stops background apps aggressively, add FloatMaster to its allowed/auto-start list."
    }
}
