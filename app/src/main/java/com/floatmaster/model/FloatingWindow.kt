package com.floatmaster.model

import com.floatmaster.util.WindowId
import java.time.Instant

/**
 * Core domain entity for a floating window.
 * WHY: KDoc on every public class — quality bar; value class WindowId prevents ID mixing.
 */
data class FloatingWindow(
    val id: WindowId = WindowId.generate(), // WHY: UUID value class, not String — type-safe, no collision
    val type: WindowType,
    val title: String = type.title,
    val url: String? = null, // WHY: nullable explicit + validated length
    val packageName: String? = null,
    val appWidgetId: Int? = null,
    val state: WindowState = WindowState.NORMAL,
    val geometry: WindowGeometry,
    val isPinned: Boolean = false,
    val isLocked: Boolean = false,
    val createdAt: Instant = Instant.now(), // WHY: Instant not Long — timezone-safe, testable
    val lastFocusedAt: Instant = Instant.now(),
    @Transient var zIndex: Int = 0
) {
    val isMinimized get() = state == WindowState.MINIMIZED
    val isBubble get() = state == WindowState.BUBBLE
    val isMaximized get() = state == WindowState.MAXIMIZED

    companion object {
        /** WHY: Factory validates invariants at boundary — prevents 0-size or overlong URLs */
        fun create(
            type: WindowType,
            geometry: WindowGeometry,
            title: String? = null,
            url: String? = null,
            packageName: String? = null
        ): FloatingWindow {
            require(geometry.width in 200..2560 && geometry.height in 160..3840) // WHY: clamp prevents invisible windows
            url?.let { require(it.length <= 2048) { "URL too long" } } // WHY: guard DoS via huge URL
            return FloatingWindow(
                type = type,
                title = (title ?: type.title).take(80), // WHY: truncation prevents UI overflow
                url = url?.take(2048),
                packageName = packageName?.take(256),
                geometry = geometry
            )
        }
    }
}

/** WHY: Favorites use value class + Instant */
data class WindowFavorite(
    val id: WindowId = WindowId.generate(),
    val type: WindowType,
    val title: String,
    val url: String?,
    val packageName: String?,
    val createdAt: Instant = Instant.now()
)

data class WindowHistoryEntry(
    val windowId: WindowId,
    val type: WindowType,
    val title: String,
    val openedAt: Instant,
    val closedAt: Instant? = null
)
