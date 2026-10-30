package com.tagshot.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tagshot.app.domain.TagColor

@Composable
fun TagBadge(
    normalizedName: String,
    displayName: String,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val colorResult = TagColor.getColorScheme(normalizedName, isDark)
    val bgColor = Color(colorResult.backgroundArgb)
    val textColor = Color(colorResult.textArgb)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = "#$displayName",
            color = textColor,
            fontSize = 11.sp,
            maxLines = 1
        )
    }
}
