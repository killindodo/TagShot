package com.tagshot.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.SavedStateHandle
import com.tagshot.app.ui.FakePhotoRepository
import com.tagshot.app.ui.PhotoViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccentPaletteTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testAllSixPalettesHaveExactHexCodes() {
        val sapphire = AccentPalette.TITANIUM_SAPPHIRE
        assertEquals(Color(0xFF38BDF8), sapphire.primaryColor)
        assertEquals(Color(0xFF0284C7), sapphire.secondaryColor)
        assertEquals(Color(0xFF0C4A6E), sapphire.containerColor)

        val amber = AccentPalette.CUPERTINO_AMBER
        assertEquals(Color(0xFFF59E0B), amber.primaryColor)
        assertEquals(Color(0xFFD97706), amber.secondaryColor)
        assertEquals(Color(0xFF78350F), amber.containerColor)

        val emerald = AccentPalette.CYBER_EMERALD
        assertEquals(Color(0xFF10B981), emerald.primaryColor)
        assertEquals(Color(0xFF059669), emerald.secondaryColor)
        assertEquals(Color(0xFF064E3B), emerald.containerColor)

        val amethyst = AccentPalette.MIDNIGHT_AMETHYST
        assertEquals(Color(0xFFA855F7), amethyst.primaryColor)
        assertEquals(Color(0xFF9333EA), amethyst.secondaryColor)
        assertEquals(Color(0xFF581C87), amethyst.containerColor)

        val crimson = AccentPalette.CRIMSON_DUSK
        assertEquals(Color(0xFFF43F5E), crimson.primaryColor)
        assertEquals(Color(0xFFE11D48), crimson.secondaryColor)
        assertEquals(Color(0xFF881337), crimson.containerColor)

        val obsidian = AccentPalette.OBSIDIAN_MONO
        assertEquals(Color(0xFFE2E8F0), obsidian.primaryColor)
        assertEquals(Color(0xFF94A3B8), obsidian.secondaryColor)
        assertEquals(Color(0xFF334155), obsidian.containerColor)
    }

    @Test
    fun testFromNameOrDefaultFallback() {
        assertEquals(AccentPalette.TITANIUM_SAPPHIRE, AccentPalette.fromNameOrDefault(null))
        assertEquals(AccentPalette.TITANIUM_SAPPHIRE, AccentPalette.fromNameOrDefault(""))
        assertEquals(AccentPalette.TITANIUM_SAPPHIRE, AccentPalette.fromNameOrDefault("NON_EXISTENT"))
        assertEquals(AccentPalette.CRIMSON_DUSK, AccentPalette.fromNameOrDefault("CRIMSON_DUSK"))
        assertEquals(AccentPalette.CYBER_EMERALD, AccentPalette.fromNameOrDefault("cyber_emerald"))
    }

    @Test
    fun testColorSchemesGeneratedForDarkAndLight() {
        for (palette in AccentPalette.entries) {
            val dark = buildDarkColorScheme(palette)
            val light = buildLightColorScheme(palette)

            assertNotNull(dark.primary)
            assertNotNull(light.primary)
            assertEquals(palette.darkScheme.primary, dark.primary)
            assertEquals(palette.lightScheme.primary, light.primary)
        }
    }

    @Test
    fun testPhotoWallTokens() {
        assertNotNull(PhotoWallTokens.BorderlessRadius)
        assertNotNull(PhotoWallTokens.RoundedCellRadius)
        assertNotNull(PhotoWallTokens.WallMicroGap)
        assertNotNull(PhotoWallTokens.WallSeamlessGap)
        assertNotNull(PhotoWallTokens.StandardGridGap)
    }

    @Test
    fun testViewModelAccentPaletteSelection() {
        val repo = FakePhotoRepository()
        val handle = SavedStateHandle()
        val viewModel = PhotoViewModel(repo, handle, testDispatcher)

        assertEquals(AccentPalette.TITANIUM_SAPPHIRE.name, viewModel.accentPaletteFlow.value)

        viewModel.setAccentPalette(AccentPalette.CYBER_EMERALD)
        assertEquals(AccentPalette.CYBER_EMERALD.name, viewModel.accentPaletteFlow.value)

        viewModel.setAccentPalette(AccentPalette.OBSIDIAN_MONO)
        assertEquals(AccentPalette.OBSIDIAN_MONO.name, viewModel.accentPaletteFlow.value)
    }
}
