package com.floatmaster

import android.content.Context
import android.util.DisplayMetrics
import com.floatmaster.model.WindowType
import com.floatmaster.service.FloatingWindowManager
import com.floatmaster.util.Result
import io.mockk.every
import io.mockk.mockk
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import org.junit.Assert.assertTrue
import org.junit.Test

class FloatingWindowManagerConcurrencyTest {
    @Test
    fun concurrent_create_calls_cannot_bypass_burst_limit() {
        val context = mockk<Context>(relaxed = true)
        val metrics = DisplayMetrics().apply { widthPixels = 1080; heightPixels = 2400; density = 1f }
        every { context.resources.displayMetrics } returns metrics
        val manager = FloatingWindowManager(context)
        val pool = Executors.newFixedThreadPool(16)
        try {
            val results = pool.invokeAll(List(32) { Callable { manager.create(WindowType.NOTES) } })
            val successes = results.count { it.get() is Result.Success }
            assertTrue("atomic rate limiter allowed $successes successful creates", successes <= 8)
        } finally {
            pool.shutdownNow()
        }
    }
}
