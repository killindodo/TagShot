package com.tagshot.app.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.ImageLoaderFactory
import com.tagshot.app.TagShotApp
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PillarTwoSmoothnessTest {

    @Test
    fun tagShotApp_implementsImageLoaderFactory() {
        val app = TagShotApp()
        assertTrue("TagShotApp must implement coil.ImageLoaderFactory", app is ImageLoaderFactory)
    }

    @Test
    fun shimmerPlaceholder_whenInvisible_returnsOriginalModifierWithoutOverhead() {
        val originalModifier = Modifier
        val result = originalModifier.shimmerPlaceholder(visible = false)
        // Must return the exact same modifier without composing transitions or drawing overhead
        assertSame("When invisible, shimmerPlaceholder must return unmodified receiver", originalModifier, result)
    }

    @Test
    fun shimmerPlaceholder_customShapeAccepted() {
        val originalModifier = Modifier
        val result = originalModifier.shimmerPlaceholder(visible = false, shape = RoundedCornerShape(16.dp))
        assertSame(originalModifier, result)
    }
}
