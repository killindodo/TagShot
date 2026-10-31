package com.tagshot.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tagshot.app.ui.MainTab
import com.tagshot.app.ui.theme.TagShotGradients

data class NavItem(
    val tab: MainTab,
    val title: String,
    val icon: ImageVector
)

private val NAV_ITEMS = listOf(
    NavItem(MainTab.ALL_PHOTOS, "Gallery", Icons.Default.PhotoLibrary),
    NavItem(MainTab.FOLDER_WISE, "Folders", Icons.Default.Folder),
    NavItem(MainTab.SETTINGS, "Settings", Icons.Default.Settings)
)

/**
 * Specialist-grade floating glassmorphic pill navigation dock.
 * Features 28.dp frosted glass capsule contours, spring-animated active pill
 * indicators, and tactile bouncy press feedback.
 */
@Composable
fun BottomPillNavDock(
    activeTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
    hapticsEnabled: Boolean = true
) {
    val capsuleShape = RoundedCornerShape(28.dp)

    Box(
        modifier = modifier
            .shadow(
                elevation = 18.dp,
                shape = capsuleShape,
                spotColor = Color.Black.copy(alpha = 0.55f),
                ambientColor = Color.Black.copy(alpha = 0.25f)
            )
            .clip(capsuleShape)
            .background(TagShotGradients.cardGlassSurface)
            .glassBorder(
                shape = capsuleShape,
                strokeWidth = 1.dp,
                highlightColor = Color.White.copy(alpha = 0.20f),
                shadowColor = Color.White.copy(alpha = 0.04f)
            )
            .padding(horizontal = 6.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.width(300.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NAV_ITEMS.forEach { item ->
                val isSelected = activeTab == item.tab

                val indicatorBg by animateColorAsState(
                    targetValue = if (isSelected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                    } else {
                        Color.Transparent
                    },
                    animationSpec = tween(durationMillis = 200),
                    label = "dockIndicatorBg"
                )

                val indicatorBorder by animateColorAsState(
                    targetValue = if (isSelected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.40f)
                    } else {
                        Color.Transparent
                    },
                    animationSpec = tween(durationMillis = 200),
                    label = "dockIndicatorBorder"
                )

                val iconScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.12f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "dockIconScale"
                )

                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                    },
                    animationSpec = tween(durationMillis = 180),
                    label = "dockContentColor"
                )

                val itemShape = RoundedCornerShape(22.dp)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(itemShape)
                        .background(indicatorBg)
                        .border(1.dp, indicatorBorder, itemShape)
                        .bouncyClickable(
                            scaleDownFactor = 0.92f,
                            hapticsEnabled = hapticsEnabled,
                            onClick = { onTabSelected(item.tab) }
                        )
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier
                            .size(20.dp)
                            .scale(iconScale)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.title,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = contentColor,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
