package com.tagshot.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.math.abs

class ThumbnailMathTest {

    @Test
    fun testTableFromSpec() {
        val testCases = listOf(
            Triple(4000, 3000, Pair(512, 384)),
            Triple(3000, 4000, Pair(384, 512)),
            Triple(512, 512, Pair(512, 512)),
            Triple(100, 100, Pair(100, 100)),
            Triple(1, 5000, Pair(1, 512)),
            Triple(5000, 1, Pair(512, 1))
        )

        for ((w, h, expected) in testCases) {
            val result = ThumbnailMath.calculate(w, h, maxEdge = 512)
            assertEquals("Thumbnail calculation mismatch for ($w, $h)", expected, result)
        }
    }

    @Test
    fun testSection57PropertiesForLargeRange() {
        // Section 11: "section 5.7 properties for L ∈ 1..20000."
        val targetEdge = 512

        for (l in 1..20000) {
            // Test square image
            val (sqW, sqH) = ThumbnailMath.calculate(l, l, targetEdge)
            val expectedSqEdge = if (l <= targetEdge) l else targetEdge
            assertEquals(expectedSqEdge, sqW)
            assertEquals(expectedSqEdge, sqH)

            // Test aspect ratio 4:3
            val w = l
            val h = maxOf(1, (l * 3) / 4)
            val (outW, outH) = ThumbnailMath.calculate(w, h, targetEdge)

            // Invariant 1: max(outW, outH) == expectedEdge
            val maxOut = maxOf(outW, outH)
            val expectedMax = if (l <= targetEdge) l else targetEdge
            assertEquals("Long edge mismatch for ($w, $h)", expectedMax, maxOut)

            // Invariant 2: |outW * H - outH * W| <= max(W, H)
            val aspectError = abs(outW.toLong() * h.toLong() - outH.toLong() * w.toLong())
            val bound = maxOf(w, h).toLong()
            assertTrue("Aspect error exceeded for ($w, $h): $aspectError > $bound", aspectError <= bound)

            // InSampleSize property test
            val s = ThumbnailMath.computeInSampleSize(w, h, targetEdge)
            assertTrue("inSampleSize must be >= 1", s >= 1)
            assertTrue("inSampleSize must be power of 2", (s and (s - 1)) == 0)
            if (l >= targetEdge) {
                val decodedLongEdge = l / s
                assertTrue("Decoded edge must be >= targetEdge so only downscaling remains: $decodedLongEdge >= $targetEdge", decodedLongEdge >= targetEdge)
            }
        }
    }

    @Test
    fun testInvalidDimensionsThrowTypedError() {
        val invalidInputs = listOf(
            Pair(0, 100),
            Pair(100, 0),
            Pair(-1, 500),
            Pair(500, -10),
            Pair(0, 0)
        )

        for ((w, h) in invalidInputs) {
            try {
                ThumbnailMath.calculate(w, h)
                fail("Expected IllegalArgumentException for ($w, $h)")
            } catch (e: IllegalArgumentException) {
                // Expected
            }

            try {
                ThumbnailMath.computeInSampleSize(w, h)
                fail("Expected IllegalArgumentException for inSampleSize ($w, $h)")
            } catch (e: IllegalArgumentException) {
                // Expected
            }
        }
    }
}
