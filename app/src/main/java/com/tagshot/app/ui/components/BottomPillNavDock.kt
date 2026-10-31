package com.tagshot.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.graphics.Brush
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
 * Features 28.dp frosted glass capsule contours, ambient neon underglow
 * matching the active theme accent (MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
 * a smooth spring-animated sliding pill indicator between tabs, and tactile bouncy feedback.
 */
@Composable
fun BottomPillNavDock(
    activeTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
    hapticsEnabled: Boolean = true
) {
    val capsuleShape = RoundedCornerShape(28.dp)
    val itemShape = RoundedCornerShape(22.dp)
    val primaryColor = MaterialTheme.colorScheme.primary
    val neonUnderglowColor = primaryColor.copy(alpha = 0.25f)

    val activeIndex = NAV_ITEMS.indexOfFirst { it.tab == activeTab }.coerceAtLeast(0)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Ambient neon underglow layer matching primary accent
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(horizontal = 8.dp, vertical = 2.dp)
                .shadow(
                    elevation = 22.dp,
                    shape = capsuleShape,
                    spotColor = primaryColor.copy(alpha = 0.70f),
                    ambientColor = neonUnderglowColor
                )
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            neonUnderglowColor,
                            primaryColor.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    ),
                    shape = capsuleShape
                )
        )

        // Frosted glass capsule dock frame
        Box(
            modifier = Modifier
                .shadow(
                    elevation = 18.dp,
                    shape = capsuleShape,
                    spotColor = Color.Black.copy(alpha = 0.55f),
                    ambientColor = neonUnderglowColor
                )
                .clip(capsuleShape)
                .background(TagShotGradients.cardGlassSurface)
                .glassBorder(
                    shape = capsuleShape,
                    strokeWidth = 1.dp,
                    highlightColor = Color.White.copy(alpha = 0.20f),
                    shadowColor = primaryColor.copy(alpha = 0.15f)
                )
                .padding(horizontal = 6.dp, vertical = 6.dp)
        ) {
            val totalInnerWidth = 300.dp
            val tabCount = NAV_ITEMS.size
            val indicatorWidth = totalInnerWidth / tabCount

            // Smooth animated sliding pill offset with spring bounce physics
            val animatedIndicatorOffset by animateDpAsState(
                targetValue = indicatorWidth * activeIndex,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "dockSlidingPillOffset"
            )

            Box(
                modifier = Modifier.width(totalInnerWidth),
                contentAlignment = Alignment.CenterStart
            ) {
                // Animated sliding pill indicator with luminous specular gradient
                Box(
                    modifier = Modifier
                        .offset(x = animatedIndicatorOffset)
                        .width(indicatorWidth)
                        .height(48.dp)
                        .clip(itemShape)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.24f),
                                    primaryColor.copy(alpha = 0.10f)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.55f),
                                    primaryColor.copy(alpha = 0.18f)
                                )
                            ),
                            shape = itemShape
                        )
                )

                // Tab buttons row
                Row(
                    modifier = Modifier.width(totalInnerWidth),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NAV_ITEMS.forEach { item ->
                        val isSelected = activeTab == item.tab

                        val iconScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.15f else 1.0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "dockIconScale"
                        )

                        val contentColor by animateColorAsState(
                            targetValue = if (isSelected) {
                                primaryColor
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                            },
                            animationSpec = tween(durationMillis = 180),
                            label = "dockContentColor"
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(itemShape)
                                .bouncyClickable(
                                    scaleDownFactor = 0.92f,
                                    hapticsEnabled = hapticsEnabled,
                                    onClick = { onTabSelected(item.tab) }
                                )
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
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
    }
}
