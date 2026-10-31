package com.tagshot.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tagshot.app.domain.TagColor

/**
 * Display sizing variations for tag badges.
 */
enum class TagDisplaySize {
    THIN_MICRO,
    STANDARD,
    COLOR_DOT
}

/**
 * Jewelry-grade floating micro-pill badge for photo tags.
 * Features 0.5dp directional specular glow borders, precision typography,
 * subtle micro-shadow elevation, and luminous color radiance.
 */
@Composable
fun TagBadge(
    normalizedName: String,
    displayName: String,
    modifier: Modifier = Modifier,
    displaySize: TagDisplaySize = TagDisplaySize.THIN_MICRO
) {
    val isDark = isSystemInDarkTheme()
    val colorResult = TagColor.getColorScheme(normalizedName, isDark)
    val baseBgColor = Color(colorResult.backgroundArgb)
    val textColor = Color(colorResult.textArgb)
    val baseBorderColor = textColor.copy(alpha = if (isDark) 0.35f else 0.40f)

    // Directional specular glow border: high luster on top-left, fading gently
    val specularGlowBorder = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = if (isDark) 0.55f else 0.75f),
            baseBorderColor,
            baseBorderColor.copy(alpha = 0.15f)
        ),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    // Subtle luster surface gradient
    val surfaceGradient = Brush.verticalGradient(
        colors = listOf(
            baseBgColor.copy(alpha = if (isDark) 0.92f else 0.98f),
            baseBgColor.copy(alpha = if (isDark) 0.80f else 0.88f)
        )
    )

    when (displaySize) {
        TagDisplaySize.COLOR_DOT -> {
            val shape = RoundedCornerShape(5.dp)
            Box(
                modifier = modifier
                    .shadow(
                        elevation = 1.5.dp,
                        shape = shape,
                        spotColor = Color.Black.copy(alpha = 0.25f),
                        ambientColor = Color.Black.copy(alpha = 0.12f)
                    )
                    .clip(shape)
                    .background(Color.Black.copy(alpha = 0.72f))
                    .border(width = 0.5.dp, brush = specularGlowBorder, shape = shape)
                    .padding(horizontal = 4.5.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.5.dp)
                            .clip(CircleShape)
                            .background(Color(colorResult.backgroundArgb))
                            .border(0.5.dp, Color.White.copy(alpha = 0.45f), CircleShape)
                    )
                    Text(
                        text = displayName,
                        color = Color.White,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.15.sp,
                        lineHeight = 10.sp,
                        maxLines = 1
                    )
                }
            }
        }
        TagDisplaySize.STANDARD -> {
            val shape = RoundedCornerShape(7.dp)
            Box(
                modifier = modifier
                    .shadow(
                        elevation = 2.dp,
                        shape = shape,
                        spotColor = Color.Black.copy(alpha = 0.22f),
                        ambientColor = Color.Black.copy(alpha = 0.10f)
                    )
                    .clip(shape)
                    .background(surfaceGradient)
                    .border(width = 0.5.dp, brush = specularGlowBorder, shape = shape)
                    .padding(horizontal = 6.5.dp, vertical = 2.5.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "#",
                        color = textColor.copy(alpha = 0.60f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.sp,
                        lineHeight = 11.sp
                    )
                    Spacer(modifier = Modifier.width(1.5.dp))
                    Text(
                        text = displayName,
                        color = textColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.15.sp,
                        lineHeight = 12.sp,
                        maxLines = 1
                    )
                }
            }
        }
        TagDisplaySize.THIN_MICRO -> {
            val shape = RoundedCornerShape(5.dp)
            Box(
                modifier = modifier
                    .shadow(
                        elevation = 1.dp,
                        shape = shape,
                        spotColor = Color.Black.copy(alpha = 0.20f),
                        ambientColor = Color.Black.copy(alpha = 0.08f)
                    )
                    .clip(shape)
                    .background(surfaceGradient)
                    .border(width = 0.5.dp, brush = specularGlowBorder, shape = shape)
                    .padding(horizontal = 4.5.dp, vertical = 1.5.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "#",
                        color = textColor.copy(alpha = 0.60f),
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.sp,
                        lineHeight = 9.sp
                    )
                    Spacer(modifier = Modifier.width(1.dp))
                    Text(
                        text = displayName,
                        color = textColor,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.1.sp,
                        lineHeight = 10.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Interactive jewelry-grade micro-pill tag chip.
 * Includes interactive state, 0.5dp specular glow border, tactile bouncy clicks,
 * and high-precision Apple SF-style typography.
 */
@Composable
fun TagChip(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    normalizedName: String = name.lowercase().trim(),
    count: Int? = null,
    hapticsEnabled: Boolean = true
) {
    val isDark = isSystemInDarkTheme()
    val colorResult = TagColor.getColorScheme(normalizedName, isDark)
    val tagColor = Color(colorResult.backgroundArgb)
    val tagTextColor = Color(colorResult.textArgb)
    val pillShape = RoundedCornerShape(16.dp)

    val activeBorder = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.75f),
            tagColor,
            tagColor.copy(alpha = 0.40f)
        )
    )

    val inactiveBorder = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = if (isDark) 0.25f else 0.40f),
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f)
        )
    )

    val backgroundBrush = if (isSelected) {
        Brush.verticalGradient(
            colors = listOf(
                tagColor.copy(alpha = if (isDark) 0.45f else 0.35f),
                tagColor.copy(alpha = if (isDark) 0.25f else 0.18f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.40f else 0.55f),
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.25f else 0.35f)
            )
        )
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = if (isSelected) 3.dp else 1.dp,
                shape = pillShape,
                spotColor = if (isSelected) tagColor.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.15f),
                ambientColor = if (isSelected) tagColor.copy(alpha = 0.20f) else Color.Transparent
            )
            .clip(pillShape)
            .background(backgroundBrush)
            .border(
                width = 0.5.dp,
                brush = if (isSelected) activeBorder else inactiveBorder,
                shape = pillShape
            )
            .bouncyClickable(
                scaleDownFactor = 0.94f,
                hapticsEnabled = hapticsEnabled,
                onClick = onClick
            )
            .padding(horizontal = 9.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "#",
                color = if (isSelected) tagTextColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp
            )
            Text(
                text = name,
                color = if (isSelected) tagTextColor else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                letterSpacing = 0.15.sp
            )
            if (count != null) {
                Text(
                    text = "($count)",
                    color = if (isSelected) tagTextColor.copy(alpha = 0.80f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 0.sp
                )
            }
        }
    }
}
