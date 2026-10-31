package com.tagshot.app.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Strict type-safe design tokens for the TagShot design system.
 * Centralizes all spacing, corner radii, borders, elevation, and alpha scales.
 */
object TagShotThemeTokens {
    object Spacing {
        val xxs: Dp = 2.dp
        val xs: Dp = 4.dp
        val sm: Dp = 8.dp
        val md: Dp = 12.dp
        val lg: Dp = 16.dp
        val xl: Dp = 24.dp
        val xxl: Dp = 32.dp
    }

    object CornerRadius {
        val xs: Dp = 4.dp
        val sm: Dp = 8.dp
        val md: Dp = 12.dp
        val lg: Dp = 18.dp
        val full: Dp = 999.dp
    }

    object Borders {
        val hairline: Dp = 0.5.dp
        val thin: Dp = 0.75.dp
        val regular: Dp = 1.dp
        val thick: Dp = 2.dp
    }

    object Elevation {
        val none: Dp = 0.dp
        val subtle: Dp = 2.dp
        val floating: Dp = 8.dp
    }

    object Alpha {
        const val glass: Float = 0.65f
        const val highlight: Float = 0.15f
        const val scrim: Float = 0.85f
    }
}

// Convenient top-level accessors
val Spacing = TagShotThemeTokens.Spacing
val CornerRadius = TagShotThemeTokens.CornerRadius
val Borders = TagShotThemeTokens.Borders
val Elevation = TagShotThemeTokens.Elevation
val Alpha = TagShotThemeTokens.Alpha
