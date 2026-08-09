package com.floatmaster.model

enum class WindowState {
    NORMAL,       // visible floating
    MINIMIZED,    // collapsed to title bar only
    BUBBLE,       // edge bubble (60dp circle)
    MAXIMIZED,    // fills safe area
    CLOSED
}

enum class BubblePosition { LEFT, RIGHT }

data class WindowGeometry(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val alpha: Float = 1f,
    val showBorder: Boolean = true
)

data class SnapTarget(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)
