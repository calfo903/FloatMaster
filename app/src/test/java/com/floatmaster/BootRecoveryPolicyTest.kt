package com.floatmaster

import android.content.Intent
import com.floatmaster.receiver.BootRecoveryPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BootRecoveryPolicyTest {
    @Test
    fun `boot restores only with overlay permission and saved session`() {
        assertTrue(BootRecoveryPolicy.shouldRestore(Intent.ACTION_BOOT_COMPLETED, true, true))
        assertTrue(BootRecoveryPolicy.shouldRestore(Intent.ACTION_MY_PACKAGE_REPLACED, true, true))
        assertFalse(BootRecoveryPolicy.shouldRestore(Intent.ACTION_BOOT_COMPLETED, false, true))
        assertFalse(BootRecoveryPolicy.shouldRestore(Intent.ACTION_BOOT_COMPLETED, true, false))
        assertFalse(BootRecoveryPolicy.shouldRestore(Intent.ACTION_SCREEN_OFF, true, true))
    }
}
