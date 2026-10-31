package com.tagshot.app.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Fundamental UI Design Systems supported by TagShot.
 * Completely alters the shapes, elevations, borders, and tactile identity of the application.
 */
enum class UiDesignSystem(
    val displayName: String,
    val subtitle: String,
    val cardRadius: Dp,
    val buttonRadius: Dp,
    val dialogRadius: Dp,
    val isFrostedGlass: Boolean,
    val isRetroBeveled: Boolean,
    val isSquircle: Boolean
) {
    MATERIAL_YOU(
        displayName = "Material You",
        subtitle = "Expressive M3 tonal surfaces & fluid pill curves",
        cardRadius = 18.dp,
        buttonRadius = 24.dp,
        dialogRadius = 28.dp,
        isFrostedGlass = false,
        isRetroBeveled = false,
        isSquircle = false
    ),
    LIQUID_GLASS(
        displayName = "Liquid Glass",
        subtitle = "Cupertino iOS frosted acrylic & specular glow",
        cardRadius = 14.dp,
        buttonRadius = 12.dp,
        dialogRadius = 20.dp,
        isFrostedGlass = true,
        isRetroBeveled = false,
        isSquircle = false
    ),
    HYPER_OS(
        displayName = "HyperOS / MIUI",
        subtitle = "Continuous super-ellipse squircles & vivid contrast",
        cardRadius = 16.dp,
        buttonRadius = 14.dp,
        dialogRadius = 22.dp,
        isFrostedGlass = false,
        isRetroBeveled = false,
        isSquircle = true
    ),
    ONE_UI(
        displayName = "Samsung One UI",
        subtitle = "Ergonomic thumb-zone focus & generous curvature",
        cardRadius = 24.dp,
        buttonRadius = 20.dp,
        dialogRadius = 26.dp,
        isFrostedGlass = false,
        isRetroBeveled = false,
        isSquircle = false
    ),
    RETRO_WIN95(
        displayName = "Retro Windows 95",
        subtitle = "Classic 3D beveled silver panels & vintage PC title bars",
        cardRadius = 0.dp,
        buttonRadius = 0.dp,
        dialogRadius = 2.dp,
        isFrostedGlass = false,
        isRetroBeveled = true,
        isSquircle = false
    ),
    PIXEL_CLEAN(
        displayName = "Pixel Clean",
        subtitle = "Google minimalist edge-to-edge stock simplicity",
        cardRadius = 8.dp,
        buttonRadius = 10.dp,
        dialogRadius = 16.dp,
        isFrostedGlass = false,
        isRetroBeveled = false,
        isSquircle = false
    );

    fun cardShape(): CornerBasedShape = when {
        isRetroBeveled -> RoundedCornerShape(0.dp)
        cardRadius >= 20.dp -> RoundedCornerShape(cardRadius)
        cardRadius == 0.dp -> RoundedCornerShape(0.dp)
        else -> RoundedCornerShape(cardRadius)
    }

    fun buttonShape(): CornerBasedShape = when {
        isRetroBeveled -> RoundedCornerShape(0.dp)
        this == MATERIAL_YOU -> CircleShape
        else -> RoundedCornerShape(buttonRadius)
    }

    companion object {
        fun fromNameOrDefault(name: String?): UiDesignSystem {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: LIQUID_GLASS
        }
    }
}
