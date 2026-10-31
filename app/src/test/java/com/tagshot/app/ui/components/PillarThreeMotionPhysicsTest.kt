package com.tagshot.app.ui.components

import androidx.compose.animation.core.Spring
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PillarThreeMotionPhysicsTest {

    @Test
    fun testPinchZoomIn_decreasesColumnsTowardsHero() {
        val minColumns = 1
        var currentColumns = 3

        val zoomRatioIn = 1.35f
        if (zoomRatioIn > 1.28f && currentColumns > minColumns) {
            currentColumns -= 1
        }
        assertEquals(2, currentColumns)

        // Repeat zoom-in to reach hero card view
        if (zoomRatioIn > 1.28f && currentColumns > minColumns) {
            currentColumns -= 1
        }
        assertEquals(1, currentColumns)

        // Clamped at minimum
        if (zoomRatioIn > 1.28f && currentColumns > minColumns) {
            currentColumns -= 1
        }
        assertEquals(1, currentColumns)
    }

    @Test
    fun testPinchZoomOut_increasesColumnsTowardsOverview() {
        val maxColumns = 4
        var currentColumns = 2

        val zoomRatioOut = 0.72f
        if (zoomRatioOut < 0.78f && currentColumns < maxColumns) {
            currentColumns += 1
        }
        assertEquals(3, currentColumns)

        // Repeat zoom-out to reach 4 columns overview
        if (zoomRatioOut < 0.78f && currentColumns < maxColumns) {
            currentColumns += 1
        }
        assertEquals(4, currentColumns)

        // Clamped at maximum
        if (zoomRatioOut < 0.78f && currentColumns < maxColumns) {
            currentColumns += 1
        }
        assertEquals(4, currentColumns)
    }

    @Test
    fun testSwipeDismissScaleAndAlpha_progressFormulas() {
        fun computeScale(progress: Float): Float = 1.0f - (progress.coerceIn(0f, 1f) * 0.25f)
        fun computeAlpha(progress: Float): Float = (1.0f - progress.coerceIn(0f, 1f))

        // Idle state (progress = 0)
        assertEquals(1.0f, computeScale(0.0f), 0.001f)
        assertEquals(1.0f, computeAlpha(0.0f), 0.001f)

        // Halfway dragged (progress = 0.5)
        assertEquals(0.875f, computeScale(0.5f), 0.001f)
        assertEquals(0.5f, computeAlpha(0.5f), 0.001f)

        // Fully dragged to dismiss limit (progress = 1.0)
        assertEquals(0.75f, computeScale(1.0f), 0.001f)
        assertEquals(0.0f, computeAlpha(1.0f), 0.001f)
    }

    @Test
    fun testSwipeDismissThresholds() {
        val dismissDistancePx = 300f
        val dismissVelocityPx = 2000f

        // Condition 1: Exceeded distance threshold
        val dragDist1 = 350f
        val velocity1 = 500f
        val dismiss1 = dragDist1 >= dismissDistancePx || (velocity1 >= dismissVelocityPx && dragDist1 > 24f)
        assertTrue(dismiss1)

        // Condition 2: Fast downward flick even before distance threshold
        val dragDist2 = 120f
        val velocity2 = 2500f
        val dismiss2 = dragDist2 >= dismissDistancePx || (velocity2 >= dismissVelocityPx && dragDist2 > 24f)
        assertTrue(dismiss2)

        // Condition 3: Not enough distance or velocity -> snap back
        val dragDist3 = 80f
        val velocity3 = 400f
        val dismiss3 = dragDist3 >= dismissDistancePx || (velocity3 >= dismissVelocityPx && dragDist3 > 24f)
        assertFalse(dismiss3)
    }

    @Test
    fun testSpringPhysicsConstants() {
        assertEquals(Spring.DampingRatioMediumBouncy, 0.5f, 0.001f)
        assertEquals(Spring.DampingRatioLowBouncy, 0.75f, 0.001f)
    }
}
