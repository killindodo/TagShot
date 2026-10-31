package com.tagshot.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tagshot.app.domain.TagColor

/**
 * High-end micro-capsule badge for photo tags.
 */
@Composable
fun TagBadge(
    normalizedName: String,
    displayName: String,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val colorResult = TagColor.getColorScheme(normalizedName, isDark)
    val bgColor = Color(colorResult.backgroundArgb).copy(alpha = if (isDark) 0.85f else 0.95f)
    val textColor = Color(colorResult.textArgb)
    val borderColor = textColor.copy(alpha = 0.28f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(width = 0.75.dp, color = borderColor, shape = RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "#",
                color = textColor.copy(alpha = 0.65f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 11.sp
            )
            Text(
                text = displayName,
                color = textColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 12.sp,
                maxLines = 1
            )
        }
    }
}
