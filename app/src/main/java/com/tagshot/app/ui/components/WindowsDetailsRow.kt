package com.tagshot.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import com.tagshot.app.data.PhotoWithTags
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Columns supported by Windows Details view.
 */
enum class WindowsDetailsColumn(val label: String) {
    NAME("Name"),
    DATE("Date"),
    SIZE("Size"),
    TAGS("Tags")
}

/**
 * Direction of sort order for Windows Details columns.
 */
enum class SortDirection {
    ASCENDING,
    DESCENDING;

    fun toggle(): SortDirection = when (this) {
        ASCENDING -> DESCENDING
        DESCENDING -> ASCENDING
    }
}

/**
 * State of the active sort column and direction.
 */
data class ColumnSortState(
    val column: WindowsDetailsColumn = WindowsDetailsColumn.DATE,
    val direction: SortDirection = SortDirection.DESCENDING
)

/**
 * Deterministically sorts a collection of [PhotoWithTags] according to [column] and [direction].
 */
fun sortPhotosByColumn(
    photos: List<PhotoWithTags>,
    column: WindowsDetailsColumn,
    direction: SortDirection
): List<PhotoWithTags> {
    val comparator: Comparator<PhotoWithTags> = when (column) {
        WindowsDetailsColumn.NAME -> Comparator { a, b ->
            val nameA = a.photo.originalPath.substringAfterLast('/')
            val nameB = b.photo.originalPath.substringAfterLast('/')
            val cmp = nameA.compareTo(nameB, ignoreCase = true)
            if (cmp != 0) cmp else a.photo.id.compareTo(b.photo.id)
        }
        WindowsDetailsColumn.DATE -> Comparator { a, b ->
            val dateA = a.photo.takenAt ?: a.photo.addedAt
            val dateB = b.photo.takenAt ?: b.photo.addedAt
            val cmp = dateA.compareTo(dateB)
            if (cmp != 0) cmp else a.photo.id.compareTo(b.photo.id)
        }
        WindowsDetailsColumn.SIZE -> Comparator { a, b ->
            val cmp = a.photo.sizeBytes.compareTo(b.photo.sizeBytes)
            if (cmp != 0) cmp else a.photo.id.compareTo(b.photo.id)
        }
        WindowsDetailsColumn.TAGS -> Comparator { a, b ->
            val tagA = a.tags.minOfOrNull { it.name.lowercase() }
            val tagB = b.tags.minOfOrNull { it.name.lowercase() }
            when {
                tagA == null && tagB != null -> 1
                tagA != null && tagB == null -> -1
                tagA != null && tagB != null -> {
                    val cmp = tagA.compareTo(tagB)
                    if (cmp != 0) cmp else (b.photo.takenAt ?: b.photo.addedAt).compareTo(a.photo.takenAt ?: a.photo.addedAt)
                }
                else -> (b.photo.takenAt ?: b.photo.addedAt).compareTo(a.photo.takenAt ?: a.photo.addedAt)
            }
        }
    }

    val finalComparator = if (direction == SortDirection.ASCENDING) {
        comparator
    } else {
        Comparator { a, b -> comparator.compare(b, a) }
    }

    return photos.sortedWith(finalComparator)
}

/**
 * Interactive sortable column header row for Windows Details view.
 * Displays sortable column pills ("Name ▲", "Date ▼", "Size", "Tags")
 * allowing 1-tap sorting toggles with smooth spring-animated caret rotation.
 */
@Composable
fun WindowsDetailsHeaderRow(
    activeColumn: WindowsDetailsColumn,
    sortDirection: SortDirection,
    onColumnClick: (WindowsDetailsColumn) -> Unit,
    modifier: Modifier = Modifier,
    showFileSize: Boolean = true,
    showDate: Boolean = true
) {
    val columns = buildList {
        add(WindowsDetailsColumn.NAME)
        if (showDate) add(WindowsDetailsColumn.DATE)
        if (showFileSize) add(WindowsDetailsColumn.SIZE)
        add(WindowsDetailsColumn.TAGS)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        columns.forEach { column ->
            val isActive = activeColumn == column

            val targetRotation = if (sortDirection == SortDirection.ASCENDING) 0f else 180f
            val caretRotation by animateFloatAsState(
                targetValue = if (isActive) targetRotation else 180f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "caretRotation_${column.name}"
            )

            val pillShape = RoundedCornerShape(16.dp)
            val primaryColor = MaterialTheme.colorScheme.primary

            val activeBorder = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.70f),
                    primaryColor.copy(alpha = 0.55f),
                    primaryColor.copy(alpha = 0.20f)
                ),
                start = Offset.Zero,
                end = Offset.Infinite
            )

            val inactiveBorder = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.30f),
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f)
                ),
                start = Offset.Zero,
                end = Offset.Infinite
            )

            val activeBg = Brush.verticalGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.22f),
                    primaryColor.copy(alpha = 0.10f)
                )
            )

            val inactiveBg = Brush.verticalGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                )
            )

            val contentColor = if (isActive) {
                primaryColor
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
            }

            Box(
                modifier = Modifier
                    .shadow(
                        elevation = if (isActive) 2.dp else 0.dp,
                        shape = pillShape,
                        spotColor = if (isActive) primaryColor.copy(alpha = 0.40f) else Color.Transparent,
                        ambientColor = if (isActive) primaryColor.copy(alpha = 0.20f) else Color.Transparent
                    )
                    .clip(pillShape)
                    .background(if (isActive) activeBg else inactiveBg)
                    .border(
                        width = 0.5.dp,
                        brush = if (isActive) activeBorder else inactiveBorder,
                        shape = pillShape
                    )
                    .bouncyClickable(
                        scaleDownFactor = 0.94f,
                        onClick = { onColumnClick(column) }
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = column.label,
                        fontSize = 11.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                        letterSpacing = 0.1.sp,
                        color = contentColor
                    )

                    if (isActive) {
                        Canvas(
                            modifier = Modifier
                                .size(9.dp)
                                .rotate(caretRotation)
                        ) {
                            val path = Path().apply {
                                moveTo(size.width * 0.18f, size.height * 0.68f)
                                lineTo(size.width * 0.50f, size.height * 0.28f)
                                lineTo(size.width * 0.82f, size.height * 0.68f)
                            }
                            drawPath(
                                path = path,
                                color = contentColor,
                                style = Stroke(
                                    width = 1.6.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Overload for [WindowsDetailsHeaderRow] taking a [ColumnSortState].
 */
@Composable
fun WindowsDetailsHeaderRow(
    sortState: ColumnSortState,
    onSortStateChange: (ColumnSortState) -> Unit,
    modifier: Modifier = Modifier,
    showFileSize: Boolean = true,
    showDate: Boolean = true
) {
    WindowsDetailsHeaderRow(
        activeColumn = sortState.column,
        sortDirection = sortState.direction,
        onColumnClick = { clickedCol ->
            val newDirection = if (clickedCol == sortState.column) {
                sortState.direction.toggle()
            } else {
                when (clickedCol) {
                    WindowsDetailsColumn.DATE -> SortDirection.DESCENDING
                    WindowsDetailsColumn.SIZE -> SortDirection.DESCENDING
                    WindowsDetailsColumn.NAME -> SortDirection.ASCENDING
                    WindowsDetailsColumn.TAGS -> SortDirection.ASCENDING
                }
            }
            onSortStateChange(ColumnSortState(clickedCol, newDirection))
        },
        modifier = modifier,
        showFileSize = showFileSize,
        showDate = showDate
    )
}

/**
 * Self-managing overload for [WindowsDetailsHeaderRow].
 */
@Composable
fun WindowsDetailsHeaderRow(
    modifier: Modifier = Modifier,
    initialColumn: WindowsDetailsColumn = WindowsDetailsColumn.DATE,
    initialDirection: SortDirection = SortDirection.DESCENDING,
    showFileSize: Boolean = true,
    showDate: Boolean = true,
    onSortChange: (WindowsDetailsColumn, SortDirection) -> Unit = { _, _ -> }
) {
    var sortState by remember { mutableStateOf(ColumnSortState(initialColumn, initialDirection)) }
    WindowsDetailsHeaderRow(
        sortState = sortState,
        onSortStateChange = { newState ->
            sortState = newState
            onSortChange(newState.column, newState.direction)
        },
        modifier = modifier,
        showFileSize = showFileSize,
        showDate = showDate
    )
}

/**
 * Windows File Explorer-style row component for photos.
 * Displays photo thumbnail, file name, row-wise customizable metadata, and tags.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WindowsDetailsRow(
    photoWithTags: PhotoWithTags,
    isSelected: Boolean,
    isInSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    tagDisplaySize: TagDisplaySize = TagDisplaySize.THIN_MICRO,
    showFileSize: Boolean = true,
    showDate: Boolean = true,
    showFolder: Boolean = true
) {
    val context = LocalContext.current
    val thumbFile = remember(photoWithTags.photo.thumbPath) {
        File(context.filesDir, photoWithTags.photo.thumbPath)
    }
    var isError by remember(photoWithTags.photo.id) { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()) }
    val dateText = remember(photoWithTags.photo.addedAt, photoWithTags.photo.takenAt) {
        val timestamp = photoWithTags.photo.takenAt ?: photoWithTags.photo.addedAt
        dateFormat.format(Date(timestamp))
    }
    val sizeText = remember(photoWithTags.photo.sizeBytes, photoWithTags.photo.width, photoWithTags.photo.height) {
        val sizeFormatted = formatFileSize(photoWithTags.photo.sizeBytes)
        if (photoWithTags.photo.width > 0 && photoWithTags.photo.height > 0) {
            "$sizeFormatted • ${photoWithTags.photo.width}×${photoWithTags.photo.height}"
        } else {
            sizeFormatted
        }
    }

    val fileName = remember(photoWithTags.photo.originalPath) {
        photoWithTags.photo.originalPath.substringAfterLast('/').ifEmpty { "Photo #${photoWithTags.photo.id}" }
    }

    val tagDescriptions = if (photoWithTags.tags.isEmpty()) "untagged" else photoWithTags.tags.joinToString(", ") { it.name }
    val rowDescription = "$fileName, tags: $tagDescriptions"

    val shape = RoundedCornerShape(12.dp)
    val cardBg = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    }

    Card(
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = cardBg),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 0.75.dp,
                color = borderColor,
                shape = shape
            )
            .semantics { contentDescription = rowDescription }
            .bouncyClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox indicator in selection mode
            if (isInSelectionMode) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else Color.Black.copy(alpha = 0.4f)
                        )
                        .border(
                            width = 1.5.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.White.copy(alpha = 0.7f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
            }

            // Thumbnail
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (isError) {
                    Icon(
                        imageVector = Icons.Default.BrokenImage,
                        contentDescription = "Missing image",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(thumbFile)
                            .crossfade(true)
                            .size(256)
                            .precision(Precision.EXACT)
                            .listener(onError = { _, _ -> isError = true })
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(54.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Main Info Column
            Column(
                modifier = Modifier
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Row 1: File name and optional Folder Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = fileName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (showFolder && photoWithTags.photo.folderName.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = photoWithTags.photo.folderName,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Row 2: Tag Badges Row
                if (photoWithTags.tags.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        maxItemsInEachRow = 5
                    ) {
                        for (tag in photoWithTags.tags) {
                            TagBadge(
                                normalizedName = tag.normalizedName,
                                displayName = tag.name,
                                displaySize = tagDisplaySize
                            )
                        }
                    }
                } else {
                    Text(
                        text = "No tags",
                        fontSize = 9.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }

                // Row 3: Metadata (Date & Size)
                if (showDate || showFileSize) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (showDate) {
                            Text(
                                text = dateText,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }

                        if (showDate && showFileSize) {
                            Text(
                                text = "•",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }

                        if (showFileSize) {
                            Text(
                                text = sizeText,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(Locale.US, kb)
    val mb = kb / 1024.0
    return "%.2f MB".format(Locale.US, mb)
}
