package com.tagshot.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * An Apple-grade interactive swipe-down-to-dismiss gesture container for photo viewers.
 *
 * - Allows dragging vertically (downwards).
 * - Tracks 1:1 translation with smooth scale reduction (1.0 -> 0.75) and alpha fade (1.0 -> 0.0).
 * - Releases past distance or velocity thresholds trigger [onDismiss].
 * - Otherwise snaps back to rest with high-fidelity spring physics (Spring.DampingRatioMediumBouncy).
 */
@Composable
fun SwipeDismissBox(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = Color.Black,
    dismissDistanceThreshold: Dp = 120.dp,
    dismissVelocityThreshold: Dp = 800.dp,
    maxDragDistance: Dp = 380.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val density = LocalDensity.current
    val dismissDistancePx = with(density) { dismissDistanceThreshold.toPx() }
    val dismissVelocityPx = with(density) { dismissVelocityThreshold.toPx() }
    val maxDragPx = with(density) { maxDragDistance.toPx() }

    val animOffsetY = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val currentOnDismiss by rememberUpdatedState(onDismiss)

    val currentOffsetY = animOffsetY.value
    // Progress normalized from 0.0 (idle) to 1.0 (fully dragged)
    val dragProgress = (currentOffsetY.coerceAtLeast(0f) / maxDragPx).coerceIn(0f, 1f)

    // Smooth scale reduction: 1.0 -> 0.75
    val contentScale = 1.0f - (dragProgress * 0.25f)

    // Smooth alpha fade: 1.0 -> 0.0
    val contentAlpha = (1.0f - dragProgress).coerceIn(0f, 1f)

    // Container background scrim alpha fades synchronously
    val bgAlpha = (1.0f - dragProgress).coerceIn(0f, 1f)

    val gestureModifier = if (enabled) {
        Modifier.pointerInput(enabled, dismissDistancePx, dismissVelocityPx, maxDragPx) {
            val velocityTracker = VelocityTracker()
            val touchSlop = viewConfiguration.touchSlop

            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                velocityTracker.resetTracking()
                velocityTracker.addPosition(down.uptimeMillis, down.position)

                var isDragging = false
                var accumulatedY = 0f
                var accumulatedX = 0f

                while (true) {
                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                    val pressedList = event.changes.filter { it.pressed }

                    // Allow pinch-to-zoom to take priority if multi-finger touches occur before vertical drag
                    if (!isDragging && pressedList.size > 1) {
                        break
                    }

                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) {
                        break
                    }

                    velocityTracker.addPosition(change.uptimeMillis, change.position)
                    val deltaY = change.position.y - change.previousPosition.y
                    val deltaX = change.position.x - change.previousPosition.x

                    accumulatedY += deltaY
                    accumulatedX += deltaX

                    if (!isDragging) {
                        // Start dragging downwards only if vertical movement exceeds touch slop and dominates horizontal
                        if (accumulatedY > touchSlop && accumulatedY > abs(accumulatedX) * 1.25f) {
                            isDragging = true
                            change.consume()
                        }
                    } else {
                        change.consume()
                        val currentVal = animOffsetY.value
                        val newTarget = if (currentVal + deltaY < 0f) {
                            // Upward drag resistance
                            currentVal + deltaY * 0.15f
                        } else {
                            currentVal + deltaY
                        }
                        coroutineScope.launch {
                            animOffsetY.snapTo(newTarget)
                        }
                    }
                }

                if (isDragging) {
                    val velocityY = velocityTracker.calculateVelocity().y
                    val finalOffset = animOffsetY.value

                    val isOverDistance = finalOffset >= dismissDistancePx
                    val isFastDownFlick = velocityY >= dismissVelocityPx && finalOffset > 24f

                    if (isOverDistance || isFastDownFlick) {
                        // Dismiss threshold reached: trigger dismiss and glide away
                        currentOnDismiss()
                        coroutineScope.launch {
                            animOffsetY.animateTo(
                                targetValue = maxDragPx * 1.5f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                        }
                    } else {
                        // Snap back with tactile Spring physics (Spring.DampingRatioMediumBouncy)
                        coroutineScope.launch {
                            animOffsetY.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                ),
                                initialVelocity = velocityY
                            )
                        }
                    }
                }
            }
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor.copy(alpha = bgAlpha))
            .then(gestureModifier)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = currentOffsetY
                    scaleX = contentScale
                    scaleY = contentScale
                    alpha = contentAlpha
                },
            content = content
        )
    }
}
