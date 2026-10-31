package com.tagshot.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TagCustomizerTest {

    @Test
    fun testTagShape_allEntriesHaveValidNames() {
        assertEquals(6, TagShape.entries.size)
        TagShape.entries.forEach { shape ->
            assertTrue(shape.displayName.isNotBlank())
            assertTrue(shape.description.isNotBlank())
            assertNotNull(shape.toComposeShape())
        }
    }

    @Test
    fun testTagShape_fromNameOrDefault_fallbackWorks() {
        assertEquals(TagShape.PILL, TagShape.fromNameOrDefault("PILL"))
        assertEquals(TagShape.CHAMFERED_CYBER, TagShape.fromNameOrDefault("chamfered_cyber"))
        assertEquals(TagShape.SQUIRCLE, TagShape.fromNameOrDefault("INVALID_UNKNOWN"))
        assertEquals(TagShape.SQUIRCLE, TagShape.fromNameOrDefault(null))
    }

    @Test
    fun testTagPosition_allEntries() {
        assertEquals(4, TagPosition.entries.size)
        assertEquals(TagPosition.BOTTOM_LEFT, TagPosition.fromNameOrDefault("BOTTOM_LEFT"))
        assertEquals(TagPosition.TOP_RIGHT, TagPosition.fromNameOrDefault("TOP_RIGHT"))
        assertEquals(TagPosition.BOTTOM_LEFT, TagPosition.fromNameOrDefault(null))
    }

    @Test
    fun testTagPalettePreset_colorsDefined() {
        assertEquals(8, TagPalettePreset.entries.size)
        TagPalettePreset.entries.forEach { preset ->
            assertTrue(preset.displayName.isNotBlank())
            assertNotNull(preset.primaryColor)
            assertNotNull(preset.secondaryColor)
            assertNotNull(preset.backgroundColor)
            assertNotNull(preset.textColor)
        }
    }

    @Test
    fun testTagAnimation_allEntries() {
        assertEquals(4, TagAnimation.entries.size)
        assertEquals(TagAnimation.NONE, TagAnimation.fromNameOrDefault("NONE"))
        assertEquals(TagAnimation.PULSE_GLOW, TagAnimation.fromNameOrDefault("PULSE_GLOW"))
        assertEquals(TagAnimation.SHIMMER_SWEEP, TagAnimation.fromNameOrDefault("SHIMMER_SWEEP"))
        assertEquals(TagAnimation.FLOAT_DRIFT, TagAnimation.fromNameOrDefault("FLOAT_DRIFT"))
        assertEquals(TagAnimation.NONE, TagAnimation.fromNameOrDefault("unknown"))
    }
}
