package com.floatmaster

import com.floatmaster.model.WindowType
import com.floatmaster.service.FloatingWindowManager
import com.floatmaster.util.Result
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/**
 * WHY: Unit test for burst limit — no Android needed; verifies 8/2s rate-limit (critical path).
 */
class FloatingWindowManagerTest {
    @Test fun `9th burst returns 429`() = runTest {
        val manager = FloatingWindowManager(mockk(relaxed=true))
        repeat(8) { assertTrue(manager.create(WindowType.AI_CHATGPT) is Result.Success) }
        val ninth = manager.create(WindowType.AI_CHATGPT)
        assertTrue(ninth is Result.Failure && (ninth as Result.Failure).error.code == "RATE_LIMITED")
    }

    @Test fun `javascript url blocked 403`() = runTest {
        val manager = FloatingWindowManager(mockk(relaxed=true))
        val res = manager.create(WindowType.BROWSER, url="javascript:alert(1)")
        assertTrue((res as Result.Failure).error.code == "SECURITY_BLOCKED")
    }

    @Test fun `windowId is UUID value class`() {
        val id = com.floatmaster.util.WindowId.generate()
        assertNotNull(id.value)
    }
}
