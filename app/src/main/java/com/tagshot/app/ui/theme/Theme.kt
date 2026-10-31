package com.tagshot.app.ui.theme

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Builds the dark ColorScheme dynamically driven by the specified AccentPalette.
 */
fun buildDarkColorScheme(palette: AccentPalette): ColorScheme {
    val scheme = palette.darkScheme
    return darkColorScheme(
        primary = scheme.primary,
        onPrimary = scheme.onPrimary,
        primaryContainer = scheme.primaryContainer,
        onPrimaryContainer = scheme.onPrimaryContainer,

        secondary = scheme.secondary,
        onSecondary = scheme.onSecondary,
        secondaryContainer = scheme.secondaryContainer,
        onSecondaryContainer = scheme.onSecondaryContainer,

        tertiary = scheme.tertiary,
        onTertiary = scheme.onTertiary,

        background = ObsidianBg,
        onBackground = TextPrimaryDark,

        surface = ObsidianSurface,
        onSurface = TextPrimaryDark,
        surfaceVariant = ObsidianSurfaceElevated,
        onSurfaceVariant = TextSecondaryDark,

        outline = ObsidianBorder,
        outlineVariant = ObsidianBorderSubtle,

        error = CoralAccent,
        onError = ObsidianBg
    )
}

/**
 * Builds the light ColorScheme dynamically driven by the specified AccentPalette.
 */
fun buildLightColorScheme(palette: AccentPalette): ColorScheme {
    val scheme = palette.lightScheme
    return lightColorScheme(
        primary = scheme.primary,
        onPrimary = scheme.onPrimary,
        primaryContainer = scheme.primaryContainer,
        onPrimaryContainer = scheme.onPrimaryContainer,

        secondary = scheme.secondary,
        onSecondary = scheme.onSecondary,
        secondaryContainer = scheme.secondaryContainer,
        onSecondaryContainer = scheme.onSecondaryContainer,

        tertiary = scheme.tertiary,
        onTertiary = scheme.onTertiary,

        background = PorcelainBg,
        onBackground = TextPrimaryLight,

        surface = PorcelainSurface,
        onSurface = TextPrimaryLight,
        surfaceVariant = PorcelainSurfaceElevated,
        onSurfaceVariant = TextSecondaryLight,

        outline = PorcelainBorder,
        outlineVariant = Color(0xFFF1F5F9),

        error = Color(0xFFDC2626),
        onError = Color.White
    )
}

// Default fallback color schemes
private val DarkColorScheme = buildDarkColorScheme(AccentPalette.TITANIUM_SAPPHIRE)
private val LightColorScheme = buildLightColorScheme(AccentPalette.TITANIUM_SAPPHIRE)

/**
 * Edge-to-edge borderless photo wall styling tokens.
 * Calibrated for maximum photographic immersion, clean geometry, and high visual fidelity.
 */
object PhotoWallTokens {
    /** 0.dp corner radius for immersive borderless photo wall layout */
    val BorderlessRadius = 0.dp

    /** 12.dp rounded corner radius for floating card photo display */
    val RoundedCellRadius = 12.dp

    /** Ultra-thin 1.5.dp micro-gap between photos in edge-to-edge wall mode */
    val WallMicroGap = 1.5.dp

    /** Seamless 0.dp gap for uninterrupted mosaic wall layout */
    val WallSeamlessGap = 0.dp

    /** Standard 6.dp spacing for card-based gallery layout */
    val StandardGridGap = 6.dp

    /** Selection border width on active photo cells */
    val SelectionBorderWidth = 2.5.dp

    /** Subtle selection border width when selection mode is active */
    val InSelectionModeBorderWidth = 1.dp

    /** Scrim overlay height for bottom vignette */
    val BottomVignetteHeight = 48.dp
}

object TagShotGradients {
    val bottomVignetteScrim = Brush.verticalGradient(
        colors = listOf(
            Color.Transparent,
            Color.Black.copy(alpha = 0.25f),
            Color.Black.copy(alpha = 0.85f)
        )
    )

    val glassBorderGlow = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.22f),
            Color.White.copy(alpha = 0.04f)
        )
    )

    val cardGlassSurface = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF222B38).copy(alpha = 0.85f),
            Color(0xFF131A23).copy(alpha = 0.95f)
        )
    )

    val primaryAccentGlow = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF38BDF8),
            Color(0xFF6366F1)
        )
    )

    fun dynamicAccentGlow(palette: AccentPalette): Brush = Brush.horizontalGradient(
        colors = listOf(
            palette.primaryColor,
            palette.secondaryColor
        )
    )

    fun dynamicGlassSurface(palette: AccentPalette): Brush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF222B38).copy(alpha = 0.85f),
            palette.containerColor.copy(alpha = 0.35f),
            Color(0xFF131A23).copy(alpha = 0.95f)
        )
    )
}

/**
 * Frosted glass brush generator producing liquid translucency and specular gradient.
 */
fun liquidGlassBrush(
    tintColor: Color = Color(0xFF1E2836),
    alpha: Float = 0.78f
): Brush = Brush.verticalGradient(
    colors = listOf(
        tintColor.copy(alpha = (alpha + 0.12f).coerceAtMost(0.98f)),
        tintColor.copy(alpha = alpha),
        tintColor.copy(alpha = (alpha * 0.85f).coerceAtLeast(0.10f))
    )
)

/**
 * Liquid frosted glass background modifier providing blur simulation, translucent depth,
 * and specular surface gradient.
 */
fun Modifier.liquidGlassBackground(
    shape: Shape = RoundedCornerShape(16.dp),
    tint: Color = Color(0xFF1E2836),
    alpha: Float = 0.78f
): Modifier = this
    .clip(shape)
    .background(
        brush = liquidGlassBrush(tintColor = tint, alpha = alpha),
        shape = shape
    )

/**
 * Specular frosted glass border modifier creating an optical high-end edge with
 * top-down specular highlight reflection and deep shadow diffusion.
 */
fun Modifier.specularGlassBorder(
    shape: Shape = RoundedCornerShape(16.dp),
    strokeWidth: Dp = 1.dp,
    highlightColor: Color = Color.White.copy(alpha = 0.28f),
    shadowColor: Color = Color.White.copy(alpha = 0.04f)
): Modifier = this.border(
    width = strokeWidth,
    brush = Brush.verticalGradient(
        0.0f to highlightColor,
        0.35f to highlightColor.copy(alpha = highlightColor.alpha * 0.40f),
        1.0f to shadowColor
    ),
    shape = shape
)

/**
 * Composite liquid frosted glass modifier combining liquid background depth
 * and specular glass border.
 */
fun Modifier.liquidFrostedGlass(
    shape: Shape = RoundedCornerShape(16.dp),
    tint: Color = Color(0xFF1E2836),
    alpha: Float = 0.78f,
    strokeWidth: Dp = 1.dp,
    highlightColor: Color = Color.White.copy(alpha = 0.28f),
    shadowColor: Color = Color.White.copy(alpha = 0.04f)
): Modifier = this
    .liquidGlassBackground(shape = shape, tint = tint, alpha = alpha)
    .specularGlassBorder(
        shape = shape,
        strokeWidth = strokeWidth,
        highlightColor = highlightColor,
        shadowColor = shadowColor
    )

@Composable
fun TagShotTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentPalette: AccentPalette = AccentPalette.TITANIUM_SAPPHIRE,
    dynamicColor: Boolean = false, // Keep bespoke Titanium/Obsidian theme consistent
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> buildDarkColorScheme(accentPalette)
        else -> buildLightColorScheme(accentPalette)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
