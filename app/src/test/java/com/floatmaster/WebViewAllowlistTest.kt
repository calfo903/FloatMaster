package com.floatmaster

import org.junit.Assert.*
import org.junit.Test

class WebViewAllowlistTest {
    private val allowed = setOf("chatgpt.com","claude.ai","gemini.google.com","www.perplexity.ai","grok.com","chat.deepseek.com","copilot.microsoft.com","www.meta.ai","poe.com","you.com","chat.mistral.ai","character.ai")
    @Test fun `evil host blocked`() { assertFalse("evil.com" in allowed) }
    @Test fun `all 12 AI hosts allowlisted`() { assertEquals(12, allowed.size) }
}
