package com.floatmaster

import com.floatmaster.apps.aichat.AiChatProvider
import com.floatmaster.model.WindowType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiProviderSecurityTest {
    @Test
    fun `all 12 providers have unique exact HTTPS hosts`() {
        assertEquals(12, AiChatProvider.all.size)
        assertEquals(12, AiChatProvider.all.map { it.host }.toSet().size)
        assertTrue(AiChatProvider.all.all { it.url.startsWith("https://") && it.host.isNotBlank() })
    }

    @Test
    fun `provider lookup rejects hostname suffix spoofing`() {
        assertEquals(AiChatProvider.CHATGPT, AiChatProvider.fromUrl("https://chatgpt.com/"))
        assertNull(AiChatProvider.fromUrl("https://chatgpt.com.attacker.example/"))
        assertNull(AiChatProvider.fromUrl("https://evil.example/?next=https://chatgpt.com/"))
    }

    @Test
    fun `AI URL policy rejects non-HTTPS and cross-host navigation`() {
        assertFalse(AiChatProvider.isAllowedUrl("http://chatgpt.com/", AiChatProvider.CHATGPT))
        assertFalse(AiChatProvider.isAllowedUrl("https://evil.example/", AiChatProvider.CHATGPT))
        assertTrue(AiChatProvider.isAllowedUrl("https://chatgpt.com/some/path", AiChatProvider.CHATGPT))
    }

    @Test
    fun `all AI window types map to a provider`() {
        val aiTypes = WindowType.entries.filter { it.name.startsWith("AI_") && it != WindowType.AI_GROUP }
        assertEquals(12, aiTypes.size)
        assertTrue(aiTypes.all { AiChatProvider.fromWindowType(it) != null })
    }
}
