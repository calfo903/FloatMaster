package com.floatmaster.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import com.floatmaster.model.FloatingWindow
import com.floatmaster.model.WindowGeometry
import com.floatmaster.model.WindowState
import com.floatmaster.model.WindowType
import com.floatmaster.util.AppError
import com.floatmaster.util.Result
import com.floatmaster.util.WindowId
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class FloatingWindowManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val lock = Any()
    private val _windows = MutableStateFlow<List<FloatingWindow>>(emptyList())
    val windows: StateFlow<List<FloatingWindow>> = _windows.asStateFlow()
    private var zCounter = 0
    private var lastCreateMs = 0L
    private var burstCount = 0

    private companion object {
        const val MAX_BURST = 8
        const val BURST_WINDOW_MS = 2_000L
        const val MAX_TOTAL_WINDOWS = 20
        const val MIN_WIDTH = 220
        const val MIN_HEIGHT = 180
        const val MAX_WIDTH = 2_560
        const val MAX_HEIGHT = 3_840
    }

    fun defaultGeometry(type: WindowType): WindowGeometry {
        val dm = context.resources.displayMetrics
        val density = dm.density
        val w = (type.defaultWidthDp * density).roundToInt().coerceIn(MIN_WIDTH, MAX_WIDTH).coerceAtMost((dm.widthPixels * 0.92f).roundToInt())
        val h = (type.defaultHeightDp * density).roundToInt().coerceIn(MIN_HEIGHT, MAX_HEIGHT).coerceAtMost((dm.heightPixels * 0.78f).roundToInt())
        val count = synchronized(lock) { _windows.value.size }
        val offset = (24 * density * (count % 5)).roundToInt()
        return sanitizeGeometry(WindowGeometry((dm.widthPixels - w) / 2 + offset, (dm.heightPixels - h) / 2 + offset - (80 * density).roundToInt(), w, h))
    }

    fun create(type: WindowType, title: String? = null, url: String? = null, packageName: String? = null, geometry: WindowGeometry? = null): Result<FloatingWindow> = synchronized(lock) {
        validateUrl(type, url)?.let { return Result.Failure(it) }
        val now = SystemClock.elapsedRealtime()
        if (now - lastCreateMs > BURST_WINDOW_MS) burstCount = 0
        lastCreateMs = now
        burstCount += 1
        if (burstCount > MAX_BURST) return Result.Failure(AppError.RateLimited())
        if (_windows.value.size >= MAX_TOTAL_WINDOWS) return Result.Failure(AppError.RateLimited("Maximum $MAX_TOTAL_WINDOWS windows reached"))
        return try {
            val win = FloatingWindow.create(
                type = type,
                geometry = sanitizeGeometry(geometry ?: defaultGeometry(type)),
                title = title?.trim()?.take(80),
                url = url?.trim()?.take(2048),
                packageName = packageName?.trim()?.take(256)
            ).copy(zIndex = ++zCounter)
            _windows.value = _windows.value + win
            ensureServiceRunning()
            Result.Success(win)
        } catch (e: IllegalArgumentException) {
            Result.Failure(AppError.Validation(null, e.message ?: "Invalid window"))
        } catch (_: Exception) {
            Result.Failure(AppError.Internal())
        }
    }

    fun restoreSession(saved: List<FloatingWindow>): Result<Int> = synchronized(lock) {
        val restored = mutableListOf<FloatingWindow>()
        for (window in saved.take(MAX_TOTAL_WINDOWS)) {
            val validationError = validateUrl(window.type, window.url)
            if (validationError != null) return Result.Failure(validationError)
            runCatching {
                window.copy(
                    title = window.title.take(80),
                    url = window.url?.trim()?.take(2048),
                    packageName = window.packageName?.trim()?.take(256),
                    geometry = sanitizeGeometry(window.geometry),
                    zIndex = ++zCounter
                )
            }.getOrNull()?.let(restored::add)
        }
        _windows.value = restored
        Result.Success(restored.size)
    }

    fun close(id: WindowId): Result<Unit> = synchronized(lock) {
        if (_windows.value.none { it.id == id }) return Result.Failure(AppError.NotFound("Window not found"))
        _windows.value = _windows.value.filterNot { it.id == id }
        Result.Success(Unit)
    }

    fun closeAll(): Result<Unit> = synchronized(lock) {
        _windows.value = emptyList(); burstCount = 0; lastCreateMs = 0L; Result.Success(Unit)
    }

    fun updateGeometry(id: WindowId, geometry: WindowGeometry): Result<Unit> = synchronized(lock) {
        if (_windows.value.none { it.id == id }) return Result.Failure(AppError.NotFound("Window not found"))
        _windows.value = _windows.value.map { if (it.id == id) it.copy(geometry = sanitizeGeometry(geometry)) else it }
        Result.Success(Unit)
    }

    fun updateState(id: WindowId, state: WindowState): Result<Unit> = synchronized(lock) {
        if (_windows.value.none { it.id == id }) return Result.Failure(AppError.NotFound("Window not found"))
        _windows.value = _windows.value.map { if (it.id == id) it.copy(state = state) else it }; Result.Success(Unit)
    }

    fun setAlpha(id: WindowId, alpha: Float): Result<Unit> = synchronized(lock) {
        if (alpha !in 0.3f..1f) return Result.Failure(AppError.Validation("alpha", "0.3..1.0"))
        val window = _windows.value.find { it.id == id } ?: return Result.Failure(AppError.NotFound("Window not found"))
        _windows.value = _windows.value.map { if (it.id == id) it.copy(geometry = window.geometry.copy(alpha = alpha)) else it }; Result.Success(Unit)
    }

    fun toggleBorder(id: WindowId): Result<Unit> = synchronized(lock) {
        val window = _windows.value.find { it.id == id } ?: return Result.Failure(AppError.NotFound("Window not found"))
        _windows.value = _windows.value.map { if (it.id == id) it.copy(geometry = window.geometry.copy(showBorder = !window.geometry.showBorder)) else it }; Result.Success(Unit)
    }

    fun togglePinned(id: WindowId): Result<Unit> = synchronized(lock) {
        val window = _windows.value.find { it.id == id } ?: return Result.Failure(AppError.NotFound("Window not found"))
        _windows.value = _windows.value.map { if (it.id == id) it.copy(isPinned = !window.isPinned, zIndex = if (!window.isPinned) ++zCounter else it.zIndex) else it }.sortedBy { it.zIndex }
        Result.Success(Unit)
    }

    fun bringToFront(id: WindowId): Result<Unit> = synchronized(lock) {
        if (_windows.value.none { it.id == id }) return Result.Failure(AppError.NotFound("Window not found"))
        _windows.value = _windows.value.map { if (it.id == id) it.copy(zIndex = ++zCounter, lastFocusedAt = java.time.Instant.now()) else it }.sortedBy { it.zIndex }; Result.Success(Unit)
    }

    fun sendToBack(id: WindowId): Result<Unit> = synchronized(lock) {
        if (_windows.value.none { it.id == id }) return Result.Failure(AppError.NotFound("Window not found"))
        val minZ = (_windows.value.minOfOrNull { it.zIndex } ?: 0) - 1
        _windows.value = _windows.value.map { if (it.id == id) it.copy(zIndex = minZ) else it }.sortedBy { it.zIndex }; Result.Success(Unit)
    }

    fun minimize(id: WindowId) = updateState(id, WindowState.MINIMIZED)
    fun bubble(id: WindowId) = updateState(id, WindowState.BUBBLE)

    fun maximize(id: WindowId): Result<Unit> = synchronized(lock) {
        val window = _windows.value.find { it.id == id } ?: return Result.Failure(AppError.NotFound("Window not found"))
        if (window.isMaximized) {
            _windows.value = _windows.value.map { if (it.id == id) it.copy(state = WindowState.NORMAL) else it }
        } else {
            val dm = context.resources.displayMetrics
            _windows.value = _windows.value.map { if (it.id == id) it.copy(state = WindowState.MAXIMIZED, geometry = WindowGeometry(0, 0, dm.widthPixels, dm.heightPixels, window.geometry.alpha)) else it }
        }
        Result.Success(Unit)
    }

    fun restore(id: WindowId) = updateState(id, WindowState.NORMAL)
    fun getWindow(id: WindowId): FloatingWindow? = synchronized(lock) { _windows.value.find { it.id == id } }
    fun allWindows(): List<FloatingWindow> = synchronized(lock) { _windows.value.toList() }

    private fun validateUrl(type: WindowType, rawUrl: String?): AppError.Security? {
        if (rawUrl.isNullOrBlank()) return null
        if (rawUrl.length > 2_048) return AppError.Security("URL too long")
        val uri = runCatching { Uri.parse(rawUrl.trim()) }.getOrNull() ?: return AppError.Security("Invalid URL")
        if (uri.scheme?.lowercase() != "https") return AppError.Security("Only HTTPS URLs are allowed")
        if (type.name.startsWith("AI_")) {
            val allowedHost = com.floatmaster.apps.aichat.AiChatProvider.fromWindowType(type)?.host ?: return AppError.Security("Unknown AI provider")
            if (uri.host?.lowercase() != allowedHost) return AppError.Security("AI provider host is not allowlisted")
        }
        return null
    }

    private fun sanitizeGeometry(geometry: WindowGeometry): WindowGeometry {
        val dm = context.resources.displayMetrics
        val width = geometry.width.coerceIn(MIN_WIDTH, MAX_WIDTH).coerceAtMost(dm.widthPixels.coerceAtLeast(MIN_WIDTH))
        val height = geometry.height.coerceIn(MIN_HEIGHT, MAX_HEIGHT).coerceAtMost(dm.heightPixels.coerceAtLeast(MIN_HEIGHT))
        return geometry.copy(
            x = geometry.x.coerceIn(0, (dm.widthPixels - width).coerceAtLeast(0)),
            y = geometry.y.coerceIn(0, (dm.heightPixels - height).coerceAtLeast(0)),
            width = width,
            height = height,
            alpha = geometry.alpha.coerceIn(0.3f, 1f)
        )
    }

    private fun ensureServiceRunning() {
        val intent = Intent(context, FloatingService::class.java).apply { action = FloatingService.ACTION_SHOW }
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) context.startForegroundService(intent) else context.startService(intent)
        } catch (_: SecurityException) {
            // WHY: FGS start policy failures must not crash state mutation.
        } catch (_: IllegalStateException) {
            // WHY: Background start restrictions are expected on modern Android/OEM builds.
        }
    }
}
