package com.tagshot.app.domain

data class ZoomState(
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f
)

object ZoomMath {

    const val MIN_SCALE = 1f
    const val MAX_SCALE = MAX_ZOOM // 8f
    const val DOUBLE_TAP_SCALE = DOUBLE_TAP_ZOOM // 3f

    /**
     * Computes the displayed (fit) dimensions (dw, dh) for content (W, H) inside container (cw, ch):
     * r = min(cw / W, ch / H), dw = W * r, dh = H * r.
     */
    fun fitDimensions(
        contentWidth: Float,
        contentHeight: Float,
        containerWidth: Float,
        containerHeight: Float
    ): Pair<Float, Float> {
        if (contentWidth <= 0f || contentHeight <= 0f || containerWidth <= 0f || containerHeight <= 0f) {
            return Pair(0f, 0f)
        }
        val r = minOf(containerWidth / contentWidth, containerHeight / contentHeight)
        return Pair(contentWidth * r, contentHeight * r)
    }

    /**
     * Clamps offset per Section 5.8:
     * maxX = max(0, (dw * s' - cw) / 2)
     * maxY = max(0, (dh * s' - ch) / 2)
     * offset' = (clamp(x, -maxX, maxX), clamp(y, -maxY, maxY))
     *
     * At scale = 1, offset is strictly (0, 0).
     */
    fun clampOffset(
        scale: Float,
        offsetX: Float,
        offsetY: Float,
        containerWidth: Float,
        containerHeight: Float,
        contentWidth: Float,
        contentHeight: Float
    ): Pair<Float, Float> {
        val (dw, dh) = fitDimensions(contentWidth, contentHeight, containerWidth, containerHeight)
        val maxX = maxOf(0f, (dw * scale - containerWidth) / 2f)
        val maxY = maxOf(0f, (dh * scale - containerHeight) / 2f)
        val clampedX = offsetX.coerceIn(-maxX, maxX)
        val clampedY = offsetY.coerceIn(-maxY, maxY)
        return Pair(clampedX, clampedY)
    }

    /**
     * Derivation: content point under centroid remains under centroid:
     * offset' = c' * (1 - z) + z * offset + panDelta
     * where c' = centroid - containerCenter, and z = newScale / scale.
     */
    fun calculateFocalOffset(
        scale: Float,
        newScale: Float,
        currentOffsetX: Float,
        currentOffsetY: Float,
        centroidX: Float,
        centroidY: Float,
        containerWidth: Float,
        containerHeight: Float,
        panDeltaX: Float = 0f,
        panDeltaY: Float = 0f
    ): Pair<Float, Float> {
        val z = newScale / scale
        val cPrimeX = centroidX - containerWidth / 2f
        val cPrimeY = centroidY - containerHeight / 2f
        val unclampedX = cPrimeX * (1f - z) + z * currentOffsetX + panDeltaX
        val unclampedY = cPrimeY * (1f - z) + z * currentOffsetY + panDeltaY
        return Pair(unclampedX, unclampedY)
    }

    /**
     * Calculates updated [ZoomState] given zoom and pan gestures:
     * 1. newScale = clamp(scale * zoomChange, 1, 8)
     * 2. offset' = focal transformation + panDelta
     * 3. clamped to [maxX, maxY]
     */
    fun calculateZoom(
        current: ZoomState,
        zoomChange: Float,
        panDeltaX: Float = 0f,
        panDeltaY: Float = 0f,
        centroidX: Float,
        centroidY: Float,
        containerWidth: Float,
        containerHeight: Float,
        contentWidth: Float,
        contentHeight: Float
    ): ZoomState {
        val newScale = (current.scale * zoomChange).coerceIn(MIN_SCALE, MAX_SCALE)
        val (unclampedX, unclampedY) = calculateFocalOffset(
            scale = current.scale,
            newScale = newScale,
            currentOffsetX = current.offsetX,
            currentOffsetY = current.offsetY,
            centroidX = centroidX,
            centroidY = centroidY,
            containerWidth = containerWidth,
            containerHeight = containerHeight,
            panDeltaX = panDeltaX,
            panDeltaY = panDeltaY
        )
        val (clampedX, clampedY) = clampOffset(
            scale = newScale,
            offsetX = unclampedX,
            offsetY = unclampedY,
            containerWidth = containerWidth,
            containerHeight = containerHeight,
            contentWidth = contentWidth,
            contentHeight = contentHeight
        )
        return ZoomState(scale = newScale, offsetX = clampedX, offsetY = clampedY)
    }

    /**
     * Toggles double-tap zoom: 1 -> 3 -> 1.
     * If currently zoomed in (scale > 1.05), resets to 1x and (0, 0).
     * Otherwise, zooms to [DOUBLE_TAP_SCALE] (3f) focused around the tap point.
     */
    fun toggleDoubleTap(
        current: ZoomState,
        containerWidth: Float,
        containerHeight: Float,
        contentWidth: Float,
        contentHeight: Float,
        tapX: Float = containerWidth / 2f,
        tapY: Float = containerHeight / 2f
    ): ZoomState {
        return if (current.scale > 1.05f) {
            ZoomState(scale = 1f, offsetX = 0f, offsetY = 0f)
        } else {
            val targetScale = DOUBLE_TAP_SCALE
            val (unclampedX, unclampedY) = calculateFocalOffset(
                scale = current.scale,
                newScale = targetScale,
                currentOffsetX = current.offsetX,
                currentOffsetY = current.offsetY,
                centroidX = tapX,
                centroidY = tapY,
                containerWidth = containerWidth,
                containerHeight = containerHeight
            )
            val (clampedX, clampedY) = clampOffset(
                scale = targetScale,
                offsetX = unclampedX,
                offsetY = unclampedY,
                containerWidth = containerWidth,
                containerHeight = containerHeight,
                contentWidth = contentWidth,
                contentHeight = contentHeight
            )
            ZoomState(scale = targetScale, offsetX = clampedX, offsetY = clampedY)
        }
    }
}
