package com.floatmaster

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.floatmaster.data.SessionRepository
import com.floatmaster.model.FloatingWindow
import com.floatmaster.model.WindowGeometry
import com.floatmaster.model.WindowType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SessionRepositoryRecoveryTest {
    @Test
    fun saved_session_survives_repository_recreation() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val first = SessionRepository(context)
        first.clear()

        val window = FloatingWindow.create(
            type = WindowType.NOTES,
            title = "Recovery",
            geometry = WindowGeometry(20, 30, 340, 480),
        )
        first.save(listOf(window))

        val second = SessionRepository(context)
        assertTrue(second.hasSavedSession())
        val restored = second.restore()
        assertEquals(1, restored.size)
        assertEquals(WindowType.NOTES, restored.first().type)
        assertEquals("Recovery", restored.first().title)

        second.clear()
    }
}
