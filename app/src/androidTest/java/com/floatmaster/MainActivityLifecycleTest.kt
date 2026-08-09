package com.floatmaster

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityLifecycleTest {
    @get:Rule
    val activityRule = ActivityTestRule(MainActivity::class.java, true, false)

    @Test
    fun recreate_preserves_a_valid_activity_lifecycle() {
        activityRule.launchActivity(null)
        activityRule.activity.runOnUiThread { activityRule.activity.recreate() }
        activityRule.activityRuleWaitForResume()
        assertEquals(androidx.lifecycle.Lifecycle.State.RESUMED, activityRule.activity.lifecycle.currentState)
    }
}

private fun ActivityTestRule<MainActivity>.activityRuleWaitForResume() {
    androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync()
}
