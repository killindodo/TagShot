package com.tagshot.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ZoomMathTest {

    private val cw = 1080f
    private val ch = 1920f
    private val contentW = 4000f
    private val contentH = 3000f

    @Test
    fun testScaleNeverLeavesBounds() {
        val extremeZoomChanges = listOf(0.001f, 0.1f, 0.5f, 1.0f, 2.0f, 5.0f, 10.0f, 100.0f)
        for (zc in extremeZoomChanges) {
            val state = ZoomMath.calculateZoom(
                current = ZoomState(scale = 2f, offsetX = 0f, offsetY = 0f),
                zoomChange = zc,
                centroidX = cw / 2f,
                centroidY = ch / 2f,
                containerWidth = cw,
                containerHeight = ch,
                contentWidth = contentW,
                contentHeight = contentH
            )
            assertTrue("Scale must be >= 1.0f (was ${state.scale})", state.scale >= 1.0f)
            assertTrue("Scale must be <= 8.0f (was ${state.scale})", state.scale <= 8.0f)
        }
    }

    @Test
    fun testOffsetZeroAtScaleOne() {
        val state = ZoomMath.calculateZoom(
            current = ZoomState(scale = 1f, offsetX = 0f, offsetY = 0f),
            zoomChange = 1f,
            panDeltaX = 50f, // pan ignored/clamped away at 1x
            panDeltaY = 50f,
            centroidX = 300f,
            centroidY = 400f,
            containerWidth = cw,
            containerHeight = ch,
            contentWidth = contentW,
            contentHeight = contentH
        )
        assertEquals(1f, state.scale, 0.0001f)
        assertEquals(0f, state.offsetX, 0.0001f)
        assertEquals(0f, state.offsetY, 0.0001f)
    }

    @Test
    fun testClampIdempotent() {
        val scales = listOf(1f, 1.5f, 3f, 5f, 8f)
        val offsets = listOf(-2000f, -500f, 0f, 500f, 2000f)

        for (s in scales) {
            for (ox in offsets) {
                for (oy in offsets) {
                    val (clampedX1, clampedY1) = ZoomMath.clampOffset(
                        scale = s,
                        offsetX = ox,
                        offsetY = oy,
                        containerWidth = cw,
                        containerHeight = ch,
                        contentWidth = contentW,
                        contentHeight = contentH
                    )
                    val (clampedX2, clampedY2) = ZoomMath.clampOffset(
                        scale = s,
                        offsetX = clampedX1,
                        offsetY = clampedY1,
                        containerWidth = cw,
                        containerHeight = ch,
                        contentWidth = contentW,
                        contentHeight = contentH
                    )
                    assertEquals("Clamp X must be idempotent", clampedX1, clampedX2, 0.0001f)
                    assertEquals("Clamp Y must be idempotent", clampedY1, clampedY2, 0.0001f)
                }
            }
        }
    }

    @Test
    fun testFocalInvariance() {
        // Point under centroid before zoom must equal point under centroid after zoom (before clamping)
        val initialScale = 2f
        val initialOffsetX = 100f
        val initialOffsetY = -80f
        val centroidX = 750f
        val centroidY = 1200f
        val zoomFactor = 1.6f

        val cPrimeX = centroidX - cw / 2f
        val cPrimeY = centroidY - ch / 2f

        // Content coordinate under centroid before zoom:
        val contentPointXBefore = (cPrimeX - initialOffsetX) / initialScale
        val contentPointYBefore = (cPrimeY - initialOffsetY) / initialScale

        val newScale = (initialScale * zoomFactor).coerceIn(1f, 8f)
        val (unclampedX, unclampedY) = ZoomMath.calculateFocalOffset(
            scale = initialScale,
            newScale = newScale,
            currentOffsetX = initialOffsetX,
            currentOffsetY = initialOffsetY,
            centroidX = centroidX,
            centroidY = centroidY,
            containerWidth = cw,
            containerHeight = ch
        )

        // Content coordinate under centroid after zoom:
        val contentPointXAfter = (cPrimeX - unclampedX) / newScale
        val contentPointYAfter = (cPrimeY - unclampedY) / newScale

        assertEquals("Content point X must remain invariant under focal zoom", contentPointXBefore, contentPointXAfter, 0.0001f)
        assertEquals("Content point Y must remain invariant under focal zoom", contentPointYBefore, contentPointYAfter, 0.0001f)
    }

    @Test
    fun testDoubleTapTogglesOneToThreeToOne() {
        val initial = ZoomState(scale = 1f, offsetX = 0f, offsetY = 0f)

        // First double tap: 1 -> 3
        val zoomed = ZoomMath.toggleDoubleTap(
            current = initial,
            containerWidth = cw,
            containerHeight = ch,
            contentWidth = contentW,
            contentHeight = contentH,
            tapX = 600f,
            tapY = 800f
        )
        assertEquals(3f, zoomed.scale, 0.0001f)

        // Second double tap: 3 -> 1
        val reset = ZoomMath.toggleDoubleTap(
            current = zoomed,
            containerWidth = cw,
            containerHeight = ch,
            contentWidth = contentW,
            contentHeight = contentH
        )
        assertEquals(1f, reset.scale, 0.0001f)
        assertEquals(0f, reset.offsetX, 0.0001f)
        assertEquals(0f, reset.offsetY, 0.0001f)
    }
}
