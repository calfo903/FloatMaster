package com.floatmaster

import com.floatmaster.apps.aichat.AiChatProvider
import com.floatmaster.apps.aichat.AiWebViewSecurity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiWebViewSecurityTest {
    @Test
    fun `main-frame policy blocks cross-origin navigation`() {
        assertTrue(AiWebViewSecurity.isAllowedMainFrameNavigation("https://chatgpt.com/", AiChatProvider.CHATGPT))
        assertFalse(AiWebViewSecurity.isAllowedMainFrameNavigation("https://evil.example/", AiChatProvider.CHATGPT))
        assertFalse(AiWebViewSecurity.isAllowedMainFrameNavigation("javascript:alert(1)", AiChatProvider.CHATGPT))
        assertFalse(AiWebViewSecurity.isAllowedMainFrameNavigation(null, AiChatProvider.CHATGPT))
    }
}
