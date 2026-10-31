package com.tagshot.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

/**
 * Apple-grade animated shimmer brush composable modifier for skeleton loading and progressive image placeholders.
 * Sweeps a smooth linear gradient across the component bounds at a natural diagonal angle.
 * Dynamically switches between dark obsidian/titanium and luminous porcelain palettes.
 *
 * When [visible] is false, returns the modifier directly without remembering transitions,
 * eliminating frame animation overhead completely during steady-state rendering.
 */
fun Modifier.shimmerPlaceholder(
    visible: Boolean,
    shape: Shape = RoundedCornerShape(8.dp)
): Modifier = if (!visible) {
    this
} else {
    composed {
        val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

        // High-fidelity porcelain and obsidian palette with specular highlight core
        val shimmerColors = if (isDark) {
            listOf(
                Color(0xFF131922),
                Color(0xFF1D2633),
                Color(0xFF2E3E53),
                Color(0xFF1D2633),
                Color(0xFF131922)
            )
        } else {
            listOf(
                Color(0xFFE2E8F0),
                Color(0xFFEDF2F7),
                Color(0xFFFFFFFF),
                Color(0xFFEDF2F7),
                Color(0xFFE2E8F0)
            )
        }

        val transition = rememberInfiniteTransition(label = "shimmerTransition")
        val progress by transition.animateFloat(
            initialValue = -1f,
            targetValue = 2f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1350, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmerTranslate"
        )

        this
            .clip(shape)
            .drawBehind {
                val w = size.width
                val h = size.height
                if (w <= 0f || h <= 0f) return@drawBehind

                val xOffset = w * progress
                val yOffset = h * progress

                val brush = Brush.linearGradient(
                    colors = shimmerColors,
                    start = Offset(xOffset - w * 0.75f, yOffset - h * 0.75f),
                    end = Offset(xOffset + w * 0.75f, yOffset + h * 0.75f)
                )
                drawRect(brush = brush)
            }
    }
}

/**
 * Standalone skeleton placeholder box for empty loading states.
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp)
) {
    Box(
        modifier = modifier.shimmerPlaceholder(visible = true, shape = shape)
    )
}
