package com.floatmaster

import com.floatmaster.util.WindowSnapManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class OverlayInteractionTest {
    @Test
    fun `left edge drag snaps to half screen`() {
        val result = WindowSnapManager.snap(5, 400, 400, 400, 1080, 2400)
        assertNotNull(result)
        assertEquals(0, result?.geometry?.x)
        assertEquals(540, result?.geometry?.width)
    }

    @Test
    fun `corner drag snaps to quarter screen`() {
        val result = WindowSnapManager.snap(2, 3, 400, 400, 1080, 2400)
        assertNotNull(result)
        assertEquals(540, result?.geometry?.width)
        assertEquals(1200, result?.geometry?.height)
    }

    @Test
    fun `center drag does not snap`() {
        assertNull(WindowSnapManager.snap(300, 700, 400, 400, 1080, 2400))
    }

    @Test
    fun `grid tiling stays inside display`() {
        val tiles = WindowSnapManager.tileGrid(4, 1080, 2400)
        assertEquals(4, tiles.size)
        tiles.forEach { tile ->
            assert(tile.x >= 0 && tile.y >= 0)
            assert(tile.x + tile.width <= 1080)
            assert(tile.y + tile.height <= 2400)
        }
    }
}
