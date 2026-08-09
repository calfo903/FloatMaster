package com.floatmaster

import android.content.Context
import android.util.DisplayMetrics
import com.floatmaster.model.WindowType
import com.floatmaster.service.FloatingWindowManager
import com.floatmaster.util.Result
import com.floatmaster.util.WindowId
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FloatingWindowManagerTest {
    private fun manager(): FloatingWindowManager {
        val context = mockk<Context>(relaxed = true)
        val metrics = DisplayMetrics().apply { widthPixels = 1080; heightPixels = 2400; density = 1f }
        every { context.resources.displayMetrics } returns metrics
        return FloatingWindowManager(context)
    }

    @Test
    fun `9th burst is rejected atomically`() {
        val manager = manager()
        repeat(8) { assertTrue(manager.create(WindowType.AI_CHATGPT) is Result.Success) }
        val ninth = manager.create(WindowType.AI_CHATGPT)
        assertTrue(ninth is Result.Failure)
        assertEquals("RATE_LIMITED", (ninth as Result.Failure).error.code)
    }

    @Test
    fun `unsafe URL schemes are rejected`() {
        val manager = manager()
        val javascript = manager.create(WindowType.BROWSER, url = "javascript:alert(1)")
        val http = manager.create(WindowType.BROWSER, url = "http://example.com")
        assertEquals("SECURITY_BLOCKED", (javascript as Result.Failure).error.code)
        assertEquals("SECURITY_BLOCKED", (http as Result.Failure).error.code)
    }

    @Test
    fun `AI host spoofing is rejected while exact host is accepted`() {
        val manager = manager()
        val spoof = manager.create(WindowType.AI_CHATGPT, url = "https://chatgpt.com.attacker.example/")
        val exact = manager.create(WindowType.AI_CHATGPT, url = "https://chatgpt.com/")
        assertTrue(spoof is Result.Failure)
        assertTrue(exact is Result.Success)
    }

    @Test
    fun `geometry is clamped to the display`() {
        val manager = manager()
        val created = manager.create(WindowType.NOTES)
        val createdWindow = (created as Result.Success).value
        val originalGeometry = createdWindow.geometry
        manager.updateGeometry(createdWindow.id, originalGeometry.copy(x = -10000, y = -10000, width = 99999, height = 99999, alpha = 9f))
        val geometry = manager.getWindow(createdWindow.id)?.geometry
        assertNotNull(geometry)
        assertTrue(geometry?.x ?: -1 >= 0 && geometry?.y ?: -1 >= 0)
        assertTrue((geometry?.width ?: 0) <= 1080 && (geometry?.height ?: 0) <= 2400)
        assertTrue((geometry?.alpha ?: 0f) in 0.3f..1f)
    }

    @Test
    fun `pin and z-order changes are atomic`() {
        val manager = manager()
        val first = (manager.create(WindowType.NOTES) as Result.Success).value
        val second = (manager.create(WindowType.CLOCK) as Result.Success).value
        manager.togglePinned(first.id)
        val pinned = manager.getWindow(first.id)
        assertTrue(pinned?.isPinned == true)
        assertTrue((pinned?.zIndex ?: 0) > second.zIndex)
    }

    @Test
    fun `window IDs remain strongly typed`() {
        val id = WindowId.generate()
        assertNotNull(id.value)
        assertTrue(id.value.toString().isNotBlank())
    }
}
