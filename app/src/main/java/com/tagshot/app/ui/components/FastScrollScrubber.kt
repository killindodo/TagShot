package com.tagshot.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tagshot.app.data.PhotoWithTags
import com.tagshot.app.ui.theme.Borders
import com.tagshot.app.ui.theme.CornerRadius
import com.tagshot.app.ui.theme.Elevation
import com.tagshot.app.ui.theme.Spacing
import com.tagshot.app.ui.theme.TagShotGradients
import com.tagshot.app.ui.theme.TagShotThemeTokens
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Formats epoch millisecond timestamp into a Month Year string (e.g. "October 2026").
 */
fun formatMonthYear(epochMillis: Long): String {
    if (epochMillis <= 0L) return ""
    val formatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    return formatter.format(Date(epochMillis))
}

/**
 * Draggable fast-scroll scrubber with a floating frosted magnifier bubble
 * displaying Month/Year derived from item timestamp.
 */
@Composable
fun FastScrollScrubber(
    itemCount: Int,
    scrollProgress: Float,
    onScrollToIndex: (Int) -> Unit,
    dateProvider: (Int) -> String?,
    modifier: Modifier = Modifier,
    hapticsEnabled: Boolean = true
) {
    if (itemCount <= 1) return

    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }
    var lastHapticIndex by remember { mutableIntStateOf(-1) }

    // Effective progress: during drag use touch fraction, otherwise use scrollProgress
    val effectiveProgress = if (isDragging) dragProgress else scrollProgress.coerceIn(0f, 1f)

    val currentTargetIndex = remember(effectiveProgress, itemCount) {
        ((itemCount - 1) * effectiveProgress).roundToInt().coerceIn(0, itemCount - 1)
    }

    val currentDateLabel = remember(currentTargetIndex) {
        dateProvider(currentTargetIndex).orEmpty()
    }

    // Trigger haptic tick whenever scrub crosses a different date label
    if (isDragging && hapticsEnabled && currentTargetIndex != lastHapticIndex) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        lastHapticIndex = currentTargetIndex
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .width(64.dp)
    ) {
        val trackHeightPx = constraints.maxHeight.toFloat()
        val handleHeight = 44.dp
        val handleHeightPx = with(density) { handleHeight.toPx() }
        val maxTravelPx = (trackHeightPx - handleHeightPx).coerceAtLeast(0f)

        // Animated handle offset
        val handleOffsetPx = (maxTravelPx * effectiveProgress).coerceIn(0f, maxTravelPx)

        // Floating frosted magnifier bubble
        AnimatedVisibility(
            visible = isDragging && currentDateLabel.isNotBlank(),
            enter = fadeIn(spring(stiffness = Spring.StiffnessHigh)) + scaleIn(spring(stiffness = Spring.StiffnessMedium)),
            exit = fadeOut(spring(stiffness = Spring.StiffnessMedium)) + scaleOut(spring(stiffness = Spring.StiffnessHigh)),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset {
                    val bubbleOffset = (handleOffsetPx - 8.dp.toPx()).coerceIn(
                        0f,
                        (trackHeightPx - 60.dp.toPx()).coerceAtLeast(0f)
                    )
                    IntOffset(
                        x = (-56.dp.toPx()).roundToInt(),
                        y = bubbleOffset.roundToInt()
                    )
                }
        ) {
            FrostedDateMagnifierBubble(label = currentDateLabel)
        }

        // Draggable vertical handle bar
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .fillMaxHeight()
                .width(48.dp)
                .pointerInput(itemCount, trackHeightPx) {
                    detectTapGestures { offset ->
                        val progress = (offset.y / trackHeightPx).coerceIn(0f, 1f)
                        val target = ((itemCount - 1) * progress).roundToInt().coerceIn(0, itemCount - 1)
                        dragProgress = progress
                        onScrollToIndex(target)
                    }
                }
                .pointerInput(itemCount, trackHeightPx) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragProgress = (offset.y / trackHeightPx).coerceIn(0f, 1f)
                            val target = ((itemCount - 1) * dragProgress).roundToInt().coerceIn(0, itemCount - 1)
                            onScrollToIndex(target)
                        },
                        onDragEnd = {
                            isDragging = false
                        },
                        onDragCancel = {
                            isDragging = false
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val progress = (change.position.y / trackHeightPx).coerceIn(0f, 1f)
                            dragProgress = progress
                            val target = ((itemCount - 1) * progress).roundToInt().coerceIn(0, itemCount - 1)
                            onScrollToIndex(target)
                        }
                    )
                }
        ) {
            // Pill Handle
            val handleScale by animateFloatAsState(
                targetValue = if (isDragging) 1.15f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "handle_scale"
            )

            FastScrollHandlePill(
                isDragging = isDragging,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = Spacing.xs)
                    .offset { IntOffset(0, handleOffsetPx.roundToInt()) }
                    .scale(handleScale)
                    .height(handleHeight)
            )
        }
    }
}

/**
 * Frosted acrylic date magnifier bubble.
 */
@Composable
private fun FrostedDateMagnifierBubble(
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(CornerRadius.md),
        color = MaterialTheme.colorScheme.surface.copy(alpha = TagShotThemeTokens.Alpha.glass),
        shadowElevation = Elevation.floating,
        tonalElevation = Elevation.subtle,
        modifier = modifier
            .border(
                width = Borders.thin,
                brush = TagShotGradients.glassBorderGlow,
                shape = RoundedCornerShape(CornerRadius.md)
            )
            .shadow(
                elevation = Elevation.floating,
                shape = RoundedCornerShape(CornerRadius.md),
                ambientColor = Color.Black.copy(alpha = 0.35f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(
                horizontal = Spacing.md,
                vertical = Spacing.sm
            )
        ) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(Spacing.xs))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

/**
 * Sleek vertical capsule handle with haptic grip lines.
 */
@Composable
private fun FastScrollHandlePill(
    isDragging: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .width(26.dp)
            .clip(RoundedCornerShape(CornerRadius.full))
            .background(
                brush = if (isDragging) TagShotGradients.primaryAccentGlow else TagShotGradients.cardGlassSurface
            )
            .border(
                width = Borders.hairline,
                color = if (isDragging) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.25f),
                shape = RoundedCornerShape(CornerRadius.full)
            )
    ) {
        // Vertical grip marks
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = Spacing.xxs)
        ) {
            Box(
                modifier = Modifier
                    .width(Borders.regular)
                    .height(14.dp)
                    .background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(CornerRadius.full))
            )
            Spacer(modifier = Modifier.width(2.dp))
            Box(
                modifier = Modifier
                    .width(Borders.regular)
                    .height(20.dp)
                    .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(CornerRadius.full))
            )
            Spacer(modifier = Modifier.width(2.dp))
            Box(
                modifier = Modifier
                    .width(Borders.regular)
                    .height(14.dp)
                    .background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(CornerRadius.full))
            )
        }
    }
}

/**
 * Convenience overload for LazyGridState with PhotoWithTags items.
 */
@Composable
fun FastScrollScrubber(
    gridState: LazyGridState,
    photos: List<PhotoWithTags>,
    modifier: Modifier = Modifier,
    hapticsEnabled: Boolean = true
) {
    if (photos.size <= 2) return
    val coroutineScope = rememberCoroutineScope()

    val scrollProgress by remember(gridState, photos.size) {
        derivedStateOf {
            val first = gridState.firstVisibleItemIndex
            val total = photos.size
            if (total <= 1) 0f else (first.toFloat() / (total - 1).toFloat()).coerceIn(0f, 1f)
        }
    }

    FastScrollScrubber(
        itemCount = photos.size,
        scrollProgress = scrollProgress,
        onScrollToIndex = { index ->
            coroutineScope.launch {
                gridState.scrollToItem(index)
            }
        },
        dateProvider = { index ->
            if (index in photos.indices) {
                val photo = photos[index].photo
                formatMonthYear(if (photo.takenAt != null && photo.takenAt > 0L) photo.takenAt else photo.addedAt)
            } else null
        },
        modifier = modifier,
        hapticsEnabled = hapticsEnabled
    )
}

/**
 * Convenience overload for LazyListState with PhotoWithTags items.
 */
@Composable
fun FastScrollScrubber(
    listState: LazyListState,
    photos: List<PhotoWithTags>,
    modifier: Modifier = Modifier,
    hapticsEnabled: Boolean = true
) {
    if (photos.size <= 2) return
    val coroutineScope = rememberCoroutineScope()

    val scrollProgress by remember(listState, photos.size) {
        derivedStateOf {
            val first = listState.firstVisibleItemIndex
            val total = photos.size
            if (total <= 1) 0f else (first.toFloat() / (total - 1).toFloat()).coerceIn(0f, 1f)
        }
    }

    FastScrollScrubber(
        itemCount = photos.size,
        scrollProgress = scrollProgress,
        onScrollToIndex = { index ->
            coroutineScope.launch {
                listState.scrollToItem(index)
            }
        },
        dateProvider = { index ->
            if (index in photos.indices) {
                val photo = photos[index].photo
                formatMonthYear(if (photo.takenAt != null && photo.takenAt > 0L) photo.takenAt else photo.addedAt)
            } else null
        },
        modifier = modifier,
        hapticsEnabled = hapticsEnabled
    )
}

/**
 * Convenience overload for LazyStaggeredGridState with PhotoWithTags items.
 */
@Composable
fun FastScrollScrubber(
    staggeredGridState: LazyStaggeredGridState,
    photos: List<PhotoWithTags>,
    modifier: Modifier = Modifier,
    hapticsEnabled: Boolean = true
) {
    if (photos.size <= 2) return
    val coroutineScope = rememberCoroutineScope()

    val scrollProgress by remember(staggeredGridState, photos.size) {
        derivedStateOf {
            val first = staggeredGridState.firstVisibleItemIndex
            val total = photos.size
            if (total <= 1) 0f else (first.toFloat() / (total - 1).toFloat()).coerceIn(0f, 1f)
        }
    }

    FastScrollScrubber(
        itemCount = photos.size,
        scrollProgress = scrollProgress,
        onScrollToIndex = { index ->
            coroutineScope.launch {
                staggeredGridState.scrollToItem(index)
            }
        },
        dateProvider = { index ->
            if (index in photos.indices) {
                val photo = photos[index].photo
                formatMonthYear(if (photo.takenAt != null && photo.takenAt > 0L) photo.takenAt else photo.addedAt)
            } else null
        },
        modifier = modifier,
        hapticsEnabled = hapticsEnabled
    )
}
