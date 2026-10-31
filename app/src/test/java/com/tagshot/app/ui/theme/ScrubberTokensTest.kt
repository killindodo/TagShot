package com.tagshot.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScrubberTokensTest {

    @Test
    fun testScrubberStyle_allEntries() {
        assertEquals(4, ScrubberStyle.entries.size)
        assertEquals(ScrubberStyle.CUPERTINO_CAPSULE, ScrubberStyle.fromNameOrDefault("CUPERTINO_CAPSULE"))
        assertEquals(ScrubberStyle.CYBER_NEON_BAR, ScrubberStyle.fromNameOrDefault("cyber_neon_bar"))
        assertEquals(ScrubberStyle.RETRO_WINDOWS, ScrubberStyle.fromNameOrDefault("RETRO_WINDOWS"))
        assertEquals(ScrubberStyle.MINIMAL_NOTCH, ScrubberStyle.fromNameOrDefault("MINIMAL_NOTCH"))
        assertEquals(ScrubberStyle.CUPERTINO_CAPSULE, ScrubberStyle.fromNameOrDefault("invalid"))
    }

    @Test
    fun testScrubberWidth_allEntries() {
        assertEquals(3, ScrubberWidth.entries.size)
        assertEquals(ScrubberWidth.ULTRA_THIN, ScrubberWidth.fromNameOrDefault("ULTRA_THIN"))
        assertEquals(ScrubberWidth.STANDARD, ScrubberWidth.fromNameOrDefault("STANDARD"))
        assertEquals(ScrubberWidth.ACCESSIBLE_BOLD, ScrubberWidth.fromNameOrDefault("ACCESSIBLE_BOLD"))
        assertEquals(ScrubberWidth.STANDARD, ScrubberWidth.fromNameOrDefault(null))
    }

    @Test
    fun testScrubberBubbleStyle_allEntries() {
        assertEquals(4, ScrubberBubbleStyle.entries.size)
        assertEquals(ScrubberBubbleStyle.FROSTED_GLASS_LENS, ScrubberBubbleStyle.fromNameOrDefault("FROSTED_GLASS_LENS"))
        assertEquals(ScrubberBubbleStyle.WINDOWS_95_TOOLTIP, ScrubberBubbleStyle.fromNameOrDefault("windows_95_tooltip"))
        assertEquals(ScrubberBubbleStyle.CYBER_HUD, ScrubberBubbleStyle.fromNameOrDefault("CYBER_HUD"))
        assertEquals(ScrubberBubbleStyle.MATERIAL_PILL, ScrubberBubbleStyle.fromNameOrDefault("MATERIAL_PILL"))
        assertEquals(ScrubberBubbleStyle.FROSTED_GLASS_LENS, ScrubberBubbleStyle.fromNameOrDefault("invalid"))
    }
}
