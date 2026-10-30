package com.tagshot.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets

class TagColorTest {

    @Test
    fun testFnv1a32TestVectors() {
        // Official FNV-1a test vectors per Section 5.6 and Section 11:
        // "" -> 0x811C9DC5
        // "a" -> 0xE40C292C
        // "foobar" -> 0xBF9CF968
        val emptyHash = TagColor.fnv1a32("".toByteArray(StandardCharsets.UTF_8))
        assertEquals(0x811C9DC5.toInt(), emptyHash)

        val aHash = TagColor.fnv1a32("a".toByteArray(StandardCharsets.UTF_8))
        assertEquals(0xE40C292C.toInt(), aHash)

        val foobarHash = TagColor.fnv1a32("foobar".toByteArray(StandardCharsets.UTF_8))
        assertEquals(0xBF9CF968.toInt(), foobarHash)
    }

    @Test
    fun testHueRangeAndDeterminism() {
        val tags = listOf("nature", "receipt", "travel", "summer 2024", "📷")
        for (tag in tags) {
            val hue1 = TagColor.hue(tag)
            val hue2 = TagColor.hue(tag)
            assertEquals("Hue must be deterministic", hue1, hue2)
            assertTrue("Hue must be in 0..359, but was $hue1", hue1 in 0..359)
        }
    }

    @Test
    fun testContrastRatioForAll360HuesInBothThemes() {
        // Section 11 requirement:
        // "contrast >= 4.5 for all 360 hues in both themes."
        for (h in 0 until 360) {
            // Light theme test
            val (rLight, gLight, bLight) = TagColor.hslToSrgb(h, TagColor.SATURATION, TagColor.LIGHTNESS_LIGHT)
            val lLight = TagColor.relativeLuminance(rLight, gLight, bLight)
            val textColorLight = TagColor.textColorForBackground(rLight, gLight, bLight)
            val chosenLuminanceLight = if (textColorLight == TagColor.BLACK_ARGB) 0.0 else 1.0
            val contrastLight = TagColor.contrastRatio(lLight, chosenLuminanceLight)
            assertTrue(
                "Light theme contrast must be >= 4.5 for hue $h (was $contrastLight)",
                contrastLight >= 4.5
            )

            // Dark theme test
            val (rDark, gDark, bDark) = TagColor.hslToSrgb(h, TagColor.SATURATION, TagColor.LIGHTNESS_DARK)
            val lDark = TagColor.relativeLuminance(rDark, gDark, bDark)
            val textColorDark = TagColor.textColorForBackground(rDark, gDark, bDark)
            val chosenLuminanceDark = if (textColorDark == TagColor.BLACK_ARGB) 0.0 else 1.0
            val contrastDark = TagColor.contrastRatio(lDark, chosenLuminanceDark)
            assertTrue(
                "Dark theme contrast must be >= 4.5 for hue $h (was $contrastDark)",
                contrastDark >= 4.5
            )
        }
    }

    @Test
    fun testGetColorSchemeProducesValidPackings() {
        val schemeLight = TagColor.getColorScheme("vacation", isDarkTheme = false)
        assertTrue(schemeLight.hue in 0..359)
        assertTrue(schemeLight.red in 0..255)
        assertTrue(schemeLight.green in 0..255)
        assertTrue(schemeLight.blue in 0..255)
        assertTrue(schemeLight.textArgb == TagColor.BLACK_ARGB || schemeLight.textArgb == TagColor.WHITE_ARGB)

        val schemeDark = TagColor.getColorScheme("vacation", isDarkTheme = true)
        assertEquals(schemeLight.hue, schemeDark.hue)
    }
}
