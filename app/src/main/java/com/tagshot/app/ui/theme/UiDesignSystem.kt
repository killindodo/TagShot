package com.tagshot.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
    val searchBarRadius: Dp,
    val photoCellRadius: Dp,
    val navDockRadius: Dp,
    val chipRadius: Dp,
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
        searchBarRadius = 24.dp,
        photoCellRadius = 16.dp,
        navDockRadius = 28.dp,
        chipRadius = 8.dp,
        isFrostedGlass = false,
        isRetroBeveled = false,
        isSquircle = false
    ),
    LIQUID_GLASS(
        displayName = "Liquid Glass",
        subtitle = "Cupertino iOS frosted acrylic & specular glow",
        cardRadius = 16.dp,
        buttonRadius = 13.dp,
        dialogRadius = 22.dp,
        searchBarRadius = 21.dp,
        photoCellRadius = 13.dp,
        navDockRadius = 20.dp,
        chipRadius = 12.dp,
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
        searchBarRadius = 18.dp,
        photoCellRadius = 16.dp,
        navDockRadius = 18.dp,
        chipRadius = 10.dp,
        isFrostedGlass = false,
        isRetroBeveled = false,
        isSquircle = true
    ),
    ONE_UI(
        displayName = "Samsung One UI",
        subtitle = "Ergonomic thumb-zone focus & generous curvature",
        cardRadius = 24.dp,
        buttonRadius = 18.dp,
        dialogRadius = 26.dp,
        searchBarRadius = 26.dp,
        photoCellRadius = 20.dp,
        navDockRadius = 26.dp,
        chipRadius = 16.dp,
        isFrostedGlass = false,
        isRetroBeveled = false,
        isSquircle = false
    ),
    RETRO_WIN95(
        displayName = "Retro Windows 95",
        subtitle = "Classic 3D beveled silver panels & vintage PC title bars",
        cardRadius = 0.dp,
        buttonRadius = 0.dp,
        dialogRadius = 0.dp,
        searchBarRadius = 0.dp,
        photoCellRadius = 0.dp,
        navDockRadius = 0.dp,
        chipRadius = 0.dp,
        isFrostedGlass = false,
        isRetroBeveled = true,
        isSquircle = false
    ),
    PIXEL_CLEAN(
        displayName = "Pixel Clean",
        subtitle = "Google minimalist edge-to-edge stock simplicity",
        cardRadius = 8.dp,
        buttonRadius = 6.dp,
        dialogRadius = 14.dp,
        searchBarRadius = 8.dp,
        photoCellRadius = 6.dp,
        navDockRadius = 8.dp,
        chipRadius = 6.dp,
        isFrostedGlass = false,
        isRetroBeveled = false,
        isSquircle = false
    );

    val cardShape: CornerBasedShape
        get() = if (isRetroBeveled) RoundedCornerShape(0.dp) else RoundedCornerShape(cardRadius)

    val buttonShape: CornerBasedShape
        get() = when {
            isRetroBeveled -> RoundedCornerShape(0.dp)
            this == MATERIAL_YOU -> CircleShape
            else -> RoundedCornerShape(buttonRadius)
        }

    val dialogShape: CornerBasedShape
        get() = if (isRetroBeveled) RoundedCornerShape(0.dp) else RoundedCornerShape(dialogRadius)

    val searchBarShape: CornerBasedShape
        get() = if (isRetroBeveled) RoundedCornerShape(0.dp) else RoundedCornerShape(searchBarRadius)

    val photoCellShape: CornerBasedShape
        get() = if (isRetroBeveled) RoundedCornerShape(0.dp) else RoundedCornerShape(photoCellRadius)

    val navDockShape: CornerBasedShape
        get() = if (isRetroBeveled) RoundedCornerShape(0.dp) else RoundedCornerShape(navDockRadius)

    val chipShape: CornerBasedShape
        get() = if (isRetroBeveled) RoundedCornerShape(0.dp) else RoundedCornerShape(chipRadius)

    // Backward compatibility helpers
    fun cardShape(): CornerBasedShape = cardShape
    fun buttonShape(): CornerBasedShape = buttonShape

    companion object {
        fun fromNameOrDefault(name: String?): UiDesignSystem {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: LIQUID_GLASS
        }
    }
}

/**
 * High-performance hardware-accelerated 3D Bevel drawing modifier for Retro Windows 95 aesthetic.
 * Draws raised or sunken bevel borders with exact pixel edges.
 */
fun Modifier.retroBevel(
    isSunken: Boolean = false,
    strokeWidth: Dp = 2.dp,
    highlightColor: Color = Color.White,
    shadowColor: Color = Color(0xFF404040),
    fillColor: Color? = null
): Modifier = this.drawBehind {
    val strokePx = strokeWidth.toPx()
    val w = size.width
    val h = size.height

    if (fillColor != null) {
        drawRect(color = fillColor)
    }

    val topColor = if (isSunken) shadowColor else highlightColor
    val bottomColor = if (isSunken) highlightColor else shadowColor

    // Top edge
    drawLine(
        color = topColor,
        start = Offset(0f, strokePx / 2),
        end = Offset(w, strokePx / 2),
        strokeWidth = strokePx
    )
    // Left edge
    drawLine(
        color = topColor,
        start = Offset(strokePx / 2, 0f),
        end = Offset(strokePx / 2, h),
        strokeWidth = strokePx
    )
    // Right edge
    drawLine(
        color = bottomColor,
        start = Offset(w - strokePx / 2, 0f),
        end = Offset(w - strokePx / 2, h),
        strokeWidth = strokePx
    )
    // Bottom edge
    drawLine(
        color = bottomColor,
        start = Offset(0f, h - strokePx / 2),
        end = Offset(w, h - strokePx / 2),
        strokeWidth = strokePx
    )
}

/**
 * Unified border modifier that automatically applies the correct border treatment
 * matching the active UI Design System (3D bevel, specular glass, squircle outline, M3 border, or razor outline).
 */
fun Modifier.designCardBorder(
    uiDesignSystem: UiDesignSystem,
    isDark: Boolean
): Modifier = when (uiDesignSystem) {
    UiDesignSystem.RETRO_WIN95 -> this.retroBevel(
        isSunken = false,
        strokeWidth = 2.dp,
        highlightColor = if (isDark) Color(0xFF6B7280) else Color.White,
        shadowColor = if (isDark) Color(0xFF14171D) else Color(0xFF6B7280)
    )
    UiDesignSystem.LIQUID_GLASS -> this.border(
        width = 1.dp,
        brush = Brush.verticalGradient(
            0.0f to Color.White.copy(alpha = if (isDark) 0.22f else 0.35f),
            0.35f to Color.White.copy(alpha = if (isDark) 0.08f else 0.15f),
            1.0f to Color.White.copy(alpha = 0.04f)
        ),
        shape = uiDesignSystem.cardShape
    )
    UiDesignSystem.HYPER_OS -> this.border(
        width = 1.dp,
        color = if (isDark) Color.White.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.10f),
        shape = uiDesignSystem.cardShape
    )
    UiDesignSystem.ONE_UI -> this.border(
        width = 0.75.dp,
        color = if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.07f),
        shape = uiDesignSystem.cardShape
    )
    UiDesignSystem.MATERIAL_YOU -> this.border(
        width = 1.dp,
        color = if (isDark) Color(0xFF49454F).copy(alpha = 0.7f) else Color(0xFFCAC4D0).copy(alpha = 0.8f),
        shape = uiDesignSystem.cardShape
    )
    UiDesignSystem.PIXEL_CLEAN -> this.border(
        width = 0.5.dp,
        color = if (isDark) Color.White.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.12f),
        shape = uiDesignSystem.cardShape
    )
}

/**
 * Returns the characteristic container background color for a card under the given UI Design System.
 */
fun cardContainerColor(
    uiDesignSystem: UiDesignSystem,
    isDark: Boolean,
    surfaceVariantColor: Color
): Color = when (uiDesignSystem) {
    UiDesignSystem.RETRO_WIN95 -> if (isDark) Color(0xFF2C3038) else Color(0xFFC0C0C0)
    UiDesignSystem.LIQUID_GLASS -> if (isDark) Color(0xFF1E2836).copy(alpha = 0.65f) else Color(0xFFF1F5F9).copy(alpha = 0.75f)
    UiDesignSystem.HYPER_OS -> if (isDark) Color(0xFF161B23).copy(alpha = 0.88f) else Color(0xFFF3F4F6).copy(alpha = 0.95f)
    UiDesignSystem.ONE_UI -> surfaceVariantColor.copy(alpha = if (isDark) 0.65f else 0.85f)
    UiDesignSystem.MATERIAL_YOU -> surfaceVariantColor
    UiDesignSystem.PIXEL_CLEAN -> if (isDark) Color(0xFF1B1F24) else Color(0xFFF8F9FA)
}
