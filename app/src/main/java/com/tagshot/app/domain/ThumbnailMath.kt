package com.tagshot.app.domain

object ThumbnailMath {

    /**
     * Calculates scaled thumbnail dimensions per Section 5.7.
     *
     * @param w Oriented image width (must be >= 1).
     * @param h Oriented image height (must be >= 1).
     * @param maxEdge Maximum allowed edge length (default [THUMB_MAX_EDGE] = 512).
     * @return [Pair] of (outWidth, outHeight).
     * @throws IllegalArgumentException if [w] <= 0, [h] <= 0, or [maxEdge] <= 0.
     */
    fun calculate(w: Int, h: Int, maxEdge: Int = THUMB_MAX_EDGE): Pair<Int, Int> {
        require(w > 0 && h > 0) { "Dimensions must be positive: w=$w, h=$h" }
        require(maxEdge > 0) { "maxEdge must be positive: maxEdge=$maxEdge" }

        val lL = maxOf(w, h).toLong()
        val tL = maxEdge.toLong()

        if (lL <= tL) {
            // Never upscale
            return Pair(w, h)
        }

        val wL = w.toLong()
        val hL = h.toLong()

        val outW = maxOf(1L, (wL * tL + lL / 2L) / lL).toInt()
        val outH = maxOf(1L, (hL * tL + lL / 2L) / lL).toInt()

        return Pair(outW, outH)
    }

    /**
     * Computes the power-of-2 inSampleSize for bitmap decoding per Section 5.7:
     * start s = 1; while L / (2 * s) >= targetEdge: s *= 2.
     * Ensures the decoded long edge is >= targetEdge so only downscaling remains.
     *
     * @throws IllegalArgumentException if [w] <= 0, [h] <= 0, or [targetEdge] <= 0.
     */
    fun computeInSampleSize(w: Int, h: Int, targetEdge: Int = THUMB_MAX_EDGE): Int {
        require(w > 0 && h > 0) { "Dimensions must be positive: w=$w, h=$h" }
        require(targetEdge > 0) { "targetEdge must be positive: targetEdge=$targetEdge" }

        val l = maxOf(w, h)
        var s = 1
        while (l / (2 * s) >= targetEdge) {
            s *= 2
        }
        return s
    }
}
