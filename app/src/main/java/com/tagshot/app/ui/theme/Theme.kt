package com.tagshot.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = ElectricSapphire,
    onPrimary = ObsidianBg,
    primaryContainer = ElectricSapphireDim,
    onPrimaryContainer = TextPrimaryDark,

    secondary = EmeraldAccent,
    onSecondary = ObsidianBg,
    secondaryContainer = Color(0xFF064E3B),
    onSecondaryContainer = EmeraldAccent,

    tertiary = AmberAccent,
    onTertiary = ObsidianBg,

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

private val LightColorScheme = lightColorScheme(
    primary = SapphirePrimaryLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = SapphirePrimaryLight,

    secondary = EmeraldAccentLight,
    onSecondary = Color.White,

    tertiary = AmberAccentLight,
    onTertiary = Color.White,

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
}

@Composable
fun TagShotTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep bespoke Titanium/Obsidian theme consistent
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
