package com.floatmaster

import com.floatmaster.apps.browser.BrowserUrlPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserSecurityTest {
    @Test
    fun `browser accepts only HTTP(S) web URLs`() {
        assertTrue(BrowserUrlPolicy.isWebUrl("https://example.com/path"))
        assertTrue(BrowserUrlPolicy.isWebUrl("http://example.com"))
        assertFalse(BrowserUrlPolicy.isWebUrl("javascript:alert(1)"))
        assertFalse(BrowserUrlPolicy.isWebUrl("data:text/html,<script>alert(1)</script>"))
        assertFalse(BrowserUrlPolicy.isWebUrl("file:///data/data/com.floatmaster/foo"))
        assertFalse(BrowserUrlPolicy.isWebUrl("content://com.example.provider/item"))
    }

    @Test
    fun `address normalization never emits dangerous schemes`() {
        assertEquals("https://example.com", BrowserUrlPolicy.normalizeAddress("example.com"))
        assertTrue(BrowserUrlPolicy.normalizeAddress("javascript:alert(1)").startsWith("https://www.google.com/search?q="))
    }
}
