package com.tagshot.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.launch
import kotlin.math.hypot

/**
 * Modifier that attaches a two-finger pinch-to-zoom gesture handler to dynamically
 * scale grid column density between 1 and 4 columns.
 *
 * - Pinch zoom-in (fingers spreading apart): decreases column count (hero cards).
 * - Pinch zoom-out (fingers pinching together): increases column count (overview).
 * - Triggers tactile haptic feedback on each column step transition.
 * - Provides interactive elastic spring bounce (Spring.DampingRatioMediumBouncy) upon gesture release.
 */
fun Modifier.pinchZoomGrid(
    currentColumns: Int = 2,
    minColumns: Int = 1,
    maxColumns: Int = 4,
    hapticsEnabled: Boolean = true,
    onColumnsChange: (Int) -> Unit
): Modifier = composed {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val updatedOnColumnsChange by rememberUpdatedState(onColumnsChange)
    val visualScale = remember { Animatable(1f) }

    var activeColumns by remember(currentColumns) { mutableIntStateOf(currentColumns) }

    this
        .graphicsLayer {
            scaleX = visualScale.value
            scaleY = visualScale.value
        }
        .pointerInput(currentColumns, minColumns, maxColumns, hapticsEnabled) {
            awaitEachGesture {
                // Do not consume the initial down so standard taps/clicks continue working
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)

                var initialDistance: Float? = null

                do {
                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                    val activePointers = event.changes.filter { it.pressed }

                    if (activePointers.size >= 2) {
                        // Multi-touch pinch gesture active
                        val p0 = activePointers[0].position
                        val p1 = activePointers[1].position
                        val currentDistance = hypot(p0.x - p1.x, p0.y - p1.y)

                        if (initialDistance == null || initialDistance == 0f) {
                            initialDistance = currentDistance
                        } else {
                            val ratio = currentDistance / initialDistance

                            // Interactive elasticity: visual scale tracks finger pinch within bounds
                            val elasticScale = (1f + (ratio - 1f) * 0.25f).coerceIn(0.90f, 1.12f)
                            coroutineScope.launch {
                                visualScale.snapTo(elasticScale)
                            }

                            // Pinch Zoom-IN (fingers spreading apart, ratio > 1.28): decrease columns -> larger photos
                            if (ratio > 1.28f) {
                                if (activeColumns > minColumns) {
                                    activeColumns -= 1
                                    if (hapticsEnabled) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    updatedOnColumnsChange(activeColumns)
                                }
                                // Reset baseline to allow continuous stepwise zooming in a single pinch
                                initialDistance = currentDistance
                            }
                            // Pinch Zoom-OUT (fingers pinching together, ratio < 0.78): increase columns -> compact overview
                            else if (ratio < 0.78f) {
                                if (activeColumns < maxColumns) {
                                    activeColumns += 1
                                    if (hapticsEnabled) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    updatedOnColumnsChange(activeColumns)
                                }
                                // Reset baseline to allow continuous stepwise zooming in a single pinch
                                initialDistance = currentDistance
                            }
                        }

                        // Consume touch events during pinch so underlying grid doesn't scroll vertically
                        activePointers.forEach { it.consume() }
                    } else {
                        // Single pointer: reset gesture state without consuming
                        initialDistance = null
                    }
                } while (event.changes.any { it.pressed })

                // Gesture completed: snap back visual scale using Spring.DampingRatioMediumBouncy
                coroutineScope.launch {
                    visualScale.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                }
            }
        }
}

/**
 * Convenient overload accepting only [onColumnsChange], managing internal column state from 1 to 4.
 */
fun Modifier.pinchZoomGrid(
    onColumnsChange: (Int) -> Unit
): Modifier = composed {
    var columns by remember { mutableIntStateOf(2) }
    this.pinchZoomGrid(
        currentColumns = columns,
        minColumns = 1,
        maxColumns = 4,
        hapticsEnabled = true,
        onColumnsChange = { newCols ->
            columns = newCols
            onColumnsChange(newCols)
        }
    )
}
