package com.floatmaster.util

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.taskbar
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.core.view.WindowInsetsCompat
import androidx.window.layout.WindowMetricsCalculator
import com.floatmaster.model.WindowGeometry

/**
 * WHY: 12L+ tablets show system Taskbar (48-80dp) at bottom. Overlays with FLAG_LAYOUT_NO_LIMITS would hide behind it.
 * This helper makes every floating window Taskbar-aware + enables freeform + ActivityEmbedding on large screens.
 */
object TaskbarIntegration {

    /**
     * WHY: Taskbar is part of navigationBars insets on 12L. Heuristic: bottom inset 48..96px + width >600dp → real taskbar, not gesture nav.
     * Called from FloatingWindowManager.defaultGeometry and FloatingDock positioning.
     */
    fun getTaskbarHeightPx(activity: Activity): Int {
        return try {
            val metrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(activity)
            val insets = metrics.windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val isTaskbar = insets.bottom in 48..120 && metrics.bounds.width() > 600
            if (isTaskbar) insets.bottom else 0
        } catch (_: Exception) { 0 } // WHY: never crash if WindowMetrics unavailable on OEM
    }

    /** WHY: Compose helper to pad HomeScreen bottom above taskbar */
    @Composable
    fun taskbarPadding(): Dp {
        return try {
            val density = LocalDensity.current
            val bottomPx = WindowInsets.taskbar.getBottom(density)
            with(density) { bottomPx.toDp() }
        } catch (_: Exception) { androidx.compose.ui.unit.dp(0) }
    }

    /**
     * WHY: Clamp geometry so pod never sits under taskbar. Call after defaultGeometry and onDrag.
     */
    fun insetForTaskbar(geometry: WindowGeometry, taskbarPx: Int, screenH: Int): WindowGeometry {
        if (taskbarPx == 0) return geometry
        val maxY = screenH - geometry.height - taskbarPx - 8 // WHY: 8dp gap above bar
        return if (geometry.y > maxY) geometry.copy(y = maxY.coerceAtLeast(0)) else geometry
    }

    /**
     * WHY: Freeform launch for 12L tablets — AppLauncher uses this to request WINDOWING_MODE_FREEFORM.
     * Phones gracefully ignore the hint; tablets show two freeform windows side-by-side with our overlay.
     */
    fun freeformOptions(): android.app.ActivityOptions? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                android.app.ActivityOptions.makeBasic().apply {
                    setLaunchWindowingMode(android.app.ActivityOptions.WINDOWING_MODE_FREEFORM)
                    // WHY: Bounds are set by system; we just hint freeform
                }
            } else null
        } catch (_: Exception) { null } // WHY: OEM may throw
    }
}
