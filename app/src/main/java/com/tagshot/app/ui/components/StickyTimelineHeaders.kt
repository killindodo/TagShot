package com.tagshot.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tagshot.app.data.PhotoWithTags
import com.tagshot.app.ui.theme.Borders
import com.tagshot.app.ui.theme.CornerRadius
import com.tagshot.app.ui.theme.Elevation
import com.tagshot.app.ui.theme.Spacing
import com.tagshot.app.ui.theme.TagShotGradients
import com.tagshot.app.ui.theme.TagShotThemeTokens
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Formats an epoch millisecond timestamp into relative or timeline header strings:
 * - "Today" for current date
 * - "Yesterday" for previous date
 * - "October 2026", "September 2026", etc. for other months/years
 */
fun formatTimelineHeader(epochMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
    if (epochMillis <= 0L) return "Undated"

    val zone = try {
        ZoneId.systemDefault()
    } catch (_: Exception) {
        ZoneId.of("UTC")
    }

    val itemDate = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
    val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()

    return when {
        itemDate == today -> "Today"
        itemDate == today.minusDays(1) -> "Yesterday"
        else -> {
            val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
            itemDate.format(formatter)
        }
    }
}

/**
 * Group representation for timeline sectioning.
 */
data class TimelineGroup(
    val title: String,
    val photos: List<PhotoWithTags>
)

/**
 * Groups a sorted list of [PhotoWithTags] into timeline sections ("Today", "Yesterday", "October 2026", etc.).
 * Preserves the order of photos within each group.
 */
fun groupPhotosByTimeline(photos: List<PhotoWithTags>): List<TimelineGroup> {
    if (photos.isEmpty()) return emptyList()

    val groups = mutableListOf<TimelineGroup>()
    var currentTitle = ""
    var currentList = mutableListOf<PhotoWithTags>()

    for (item in photos) {
        val timestamp = if (item.photo.takenAt != null && item.photo.takenAt > 0L) {
            item.photo.takenAt
        } else {
            item.photo.addedAt
        }
        val header = formatTimelineHeader(timestamp)
        if (header != currentTitle) {
            if (currentList.isNotEmpty()) {
                groups.add(TimelineGroup(currentTitle, currentList))
            }
            currentTitle = header
            currentList = mutableListOf(item)
        } else {
            currentList.add(item)
        }
    }

    if (currentList.isNotEmpty()) {
        groups.add(TimelineGroup(currentTitle, currentList))
    }

    return groups
}

/**
 * Floating frosted acrylic pill displaying date timeline headers ("Today", "Yesterday", "October 2026", etc.).
 */
@Composable
fun FloatingTimelinePill(
    title: String,
    modifier: Modifier = Modifier,
    photoCount: Int? = null
) {
    Surface(
        shape = RoundedCornerShape(CornerRadius.full),
        color = MaterialTheme.colorScheme.surface.copy(alpha = TagShotThemeTokens.Alpha.glass),
        shadowElevation = Elevation.subtle,
        tonalElevation = Elevation.subtle,
        border = BorderStroke(Borders.thin, TagShotGradients.glassBorderGlow),
        modifier = modifier
            .shadow(
                elevation = Elevation.subtle,
                shape = RoundedCornerShape(CornerRadius.full),
                ambientColor = Color.Black.copy(alpha = 0.25f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(
                horizontal = Spacing.md,
                vertical = Spacing.xs
            )
        ) {
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(Spacing.xs))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            if (photoCount != null && photoCount > 0) {
                Spacer(modifier = Modifier.width(Spacing.xs))
                Text(
                    text = "• $photoCount",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Full-width sticky timeline header row suitable for LazyColumn.stickyHeader or section dividers.
 */
@Composable
fun StickyTimelineHeader(
    title: String,
    modifier: Modifier = Modifier,
    photoCount: Int? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.xs)
    ) {
        FloatingTimelinePill(
            title = title,
            photoCount = photoCount,
            modifier = Modifier.align(Alignment.CenterStart)
        )
    }
}

/**
 * Floating sticky timeline overlay anchored to the top-center of a LazyGridState.
 * Automatically updates its header based on the current first visible photo.
 */
@Composable
fun StickyTimelineHeaderOverlay(
    gridState: LazyGridState,
    photos: List<PhotoWithTags>,
    modifier: Modifier = Modifier
) {
    if (photos.isEmpty()) return

    val currentHeader by remember(gridState, photos) {
        derivedStateOf {
            val index = gridState.firstVisibleItemIndex
            if (index in photos.indices) {
                val photo = photos[index].photo
                val timestamp = if (photo.takenAt != null && photo.takenAt > 0L) photo.takenAt else photo.addedAt
                formatTimelineHeader(timestamp)
            } else {
                null
            }
        }
    }

    AnimatedVisibility(
        visible = currentHeader != null && photos.isNotEmpty(),
        enter = fadeIn() + slideInVertically { -it / 2 },
        exit = fadeOut() + slideOutVertically { -it / 2 },
        modifier = modifier.padding(top = Spacing.sm)
    ) {
        currentHeader?.let { title ->
            FloatingTimelinePill(title = title)
        }
    }
}

/**
 * Floating sticky timeline overlay anchored to the top-center of a LazyListState.
 */
@Composable
fun StickyTimelineHeaderOverlay(
    listState: LazyListState,
    photos: List<PhotoWithTags>,
    modifier: Modifier = Modifier
) {
    if (photos.isEmpty()) return

    val currentHeader by remember(listState, photos) {
        derivedStateOf {
            val index = listState.firstVisibleItemIndex
            if (index in photos.indices) {
                val photo = photos[index].photo
                val timestamp = if (photo.takenAt != null && photo.takenAt > 0L) photo.takenAt else photo.addedAt
                formatTimelineHeader(timestamp)
            } else {
                null
            }
        }
    }

    AnimatedVisibility(
        visible = currentHeader != null && photos.isNotEmpty(),
        enter = fadeIn() + slideInVertically { -it / 2 },
        exit = fadeOut() + slideOutVertically { -it / 2 },
        modifier = modifier.padding(top = Spacing.sm)
    ) {
        currentHeader?.let { title ->
            FloatingTimelinePill(title = title)
        }
    }
}

/**
 * Floating sticky timeline overlay anchored to the top-center of a LazyStaggeredGridState.
 */
@Composable
fun StickyTimelineHeaderOverlay(
    staggeredGridState: LazyStaggeredGridState,
    photos: List<PhotoWithTags>,
    modifier: Modifier = Modifier
) {
    if (photos.isEmpty()) return

    val currentHeader by remember(staggeredGridState, photos) {
        derivedStateOf {
            val index = staggeredGridState.firstVisibleItemIndex
            if (index in photos.indices) {
                val photo = photos[index].photo
                val timestamp = if (photo.takenAt != null && photo.takenAt > 0L) photo.takenAt else photo.addedAt
                formatTimelineHeader(timestamp)
            } else {
                null
            }
        }
    }

    AnimatedVisibility(
        visible = currentHeader != null && photos.isNotEmpty(),
        enter = fadeIn() + slideInVertically { -it / 2 },
        exit = fadeOut() + slideOutVertically { -it / 2 },
        modifier = modifier.padding(top = Spacing.sm)
    ) {
        currentHeader?.let { title ->
            FloatingTimelinePill(title = title)
        }
    }
}
