package com.tagshot.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UiDesignSystemTest {

    @Test
    fun testUiDesignSystem_allSixSystemsDefined() {
        assertEquals(6, UiDesignSystem.entries.size)
        val names = UiDesignSystem.entries.map { it.name }
        assertTrue(names.contains("MATERIAL_YOU"))
        assertTrue(names.contains("LIQUID_GLASS"))
        assertTrue(names.contains("HYPER_OS"))
        assertTrue(names.contains("ONE_UI"))
        assertTrue(names.contains("RETRO_WIN95"))
        assertTrue(names.contains("PIXEL_CLEAN"))
    }

    @Test
    fun testUiDesignSystem_shapesGeneratedCorrectly() {
        UiDesignSystem.entries.forEach { system ->
            assertTrue(system.displayName.isNotBlank())
            assertTrue(system.subtitle.isNotBlank())
            assertNotNull(system.cardShape())
            assertNotNull(system.buttonShape())
        }
    }

    @Test
    fun testUiDesignSystem_retroWin95HasZeroRadius() {
        val win95 = UiDesignSystem.RETRO_WIN95
        assertTrue(win95.isRetroBeveled)
        assertEquals(0, win95.cardRadius.value.toInt())
        assertEquals(0, win95.buttonRadius.value.toInt())
    }

    @Test
    fun testUiDesignSystem_fromNameOrDefault() {
        assertEquals(UiDesignSystem.MATERIAL_YOU, UiDesignSystem.fromNameOrDefault("MATERIAL_YOU"))
        assertEquals(UiDesignSystem.HYPER_OS, UiDesignSystem.fromNameOrDefault("hyper_os"))
        assertEquals(UiDesignSystem.LIQUID_GLASS, UiDesignSystem.fromNameOrDefault(null))
        assertEquals(UiDesignSystem.LIQUID_GLASS, UiDesignSystem.fromNameOrDefault("UNKNOWN"))
    }
}
