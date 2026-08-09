package com.floatmaster.util

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager
import com.floatmaster.model.WindowGeometry

fun Context.screenSize(): Pair<Int, Int> {
    val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val dm = DisplayMetrics()
    @Suppress("DEPRECATION") wm.defaultDisplay.getMetrics(dm)
    return dm.widthPixels to dm.heightPixels
}

fun clampGeometry(geo: WindowGeometry, screenW: Int, screenH: Int): WindowGeometry {
    val w = geo.width.coerceIn(200, screenW)
    val h = geo.height.coerceIn(160, screenH)
    val x = geo.x.coerceIn(-w + 60, screenW - 60)
    val y = geo.y.coerceIn(0, screenH - 60)
    return geo.copy(width = w, height = h, x = x, y = y)
}

fun WindowGeometry.centeredOn(screenW: Int, screenH: Int): WindowGeometry {
    return copy(x = (screenW - width) / 2, y = (screenH - height) / 2)
}
