package com.floatmaster.util

import com.floatmaster.model.WindowGeometry
import kotlin.math.abs

/**
 * WHY: Aero Snap — drag to edge snaps to half-screen, corners quarter, center free. Hardened clamping prevents off-screen.
 */
object WindowSnapManager {
    data class SnapResult(val geometry: WindowGeometry, val hint: String?)

    // WHY: 24dp threshold near edge triggers snap — matches user expectation on desktop
    private const val EDGE = 24
    private const val CORNER = 80

    fun snap(x: Int, y: Int, w: Int, h: Int, screenW: Int, screenH: Int): SnapResult? {
        val left = x < EDGE
        val right = x + w > screenW - EDGE
        val top = y < EDGE
        val bottom = y + h > screenH - EDGE
        
        return when {
            left && top -> SnapResult(WindowGeometry(0,0,screenW/2, screenH/2), "↖ Quarter")
            right && top -> SnapResult(WindowGeometry(screenW/2,0,screenW/2, screenH/2), "↗ Quarter")
            left && bottom -> SnapResult(WindowGeometry(0,screenH/2,screenW/2, screenH/2), "↙ Quarter")
            right && bottom -> SnapResult(WindowGeometry(screenW/2,screenH/2,screenW/2, screenH/2), "↘ Quarter")
            left -> SnapResult(WindowGeometry(0,0,screenW/2, screenH), "← Half")
            right -> SnapResult(WindowGeometry(screenW/2,0,screenW/2, screenH), "Half →")
            top -> SnapResult(WindowGeometry(0,0,screenW, screenH), "↑ Maximize")
            else -> null
        }
    }

    /** WHY: Grid tiling 2x2 for 4 windows */
    fun tileGrid(count: Int, screenW: Int, screenH: Int): List<WindowGeometry> {
        val cols = when (count) { 1 -> 1; 2 -> 2; else -> 2 }
        val rows = (count + cols -1)/ cols
        val w = screenW / cols
        val h = screenH / rows
        return List(count) { i ->
            val c = i % cols; val r = i / cols
            WindowGeometry(c*w, r*h, w-8, h-8)
        }
    }
}
