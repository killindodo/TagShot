package com.tagshot.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Difference
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.tagshot.app.data.PhotoWithTags
import com.tagshot.app.domain.DuplicateCluster
import com.tagshot.app.domain.DuplicateMatchType
import com.tagshot.app.ui.components.bouncyClickable
import com.tagshot.app.ui.theme.Borders
import com.tagshot.app.ui.theme.CornerRadius
import com.tagshot.app.ui.theme.Spacing
import java.io.File

/**
 * High-End Decision Hub Dialog for reviewing and safely resolving duplicate and burst photos.
 * Displays side-by-side quality comparison, storage reclaim estimation, and smart recommendations.
 */
@Composable
fun DuplicateDecisionHubDialog(
    clusters: List<DuplicateCluster>,
    onDismiss: () -> Unit,
    onCleanCluster: (DuplicateCluster) -> Unit,
    onCleanAllClusters: () -> Unit,
    onDismissCluster: (String) -> Unit
) {
    val totalReclaimableBytes = remember(clusters) {
        clusters.sumOf { it.reclaimableBytes }
    }

    val formattedReclaimable = remember(totalReclaimableBytes) {
        when {
            totalReclaimableBytes >= 1024 * 1024 -> "%.1f MB".format(totalReclaimableBytes / (1024.0 * 1024.0))
            totalReclaimableBytes >= 1024 -> "%.1f KB".format(totalReclaimableBytes / 1024.0)
            else -> "$totalReclaimableBytes B"
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(24.dp)
                ),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Spacing.md)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Difference,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Column {
                            Text(
                                text = "Duplicate Decision Hub",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${clusters.size} clusters found • Reclaim $formattedReclaimable",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.sm))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(Spacing.sm))

                // Clusters List or Empty State
                if (clusters.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            Text(
                                text = "No Duplicates Found!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Your library is clean, deduplicated, and pristine.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(Spacing.md)
                    ) {
                        items(clusters, key = { it.id }) { cluster ->
                            DuplicateClusterCard(
                                cluster = cluster,
                                onClean = { onCleanCluster(cluster) },
                                onIgnore = { onDismissCluster(cluster.id) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.sm))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(Spacing.sm))

                // Bottom Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = "Close")
                    }

                    if (clusters.isNotEmpty()) {
                        Button(
                            onClick = onCleanAllClusters,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            shape = RoundedCornerShape(CornerRadius.full)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Auto-Clean All ($formattedReclaimable)",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Card representing a single cluster with side-by-side comparison.
 */
@Composable
private fun DuplicateClusterCard(
    cluster: DuplicateCluster,
    onClean: () -> Unit,
    onIgnore: () -> Unit
) {
    val reclaimText = when {
        cluster.reclaimableBytes >= 1024 * 1024 -> "%.1f MB".format(cluster.reclaimableBytes / (1024.0 * 1024.0))
        cluster.reclaimableBytes >= 1024 -> "%.1f KB".format(cluster.reclaimableBytes / 1024.0)
        else -> "${cluster.reclaimableBytes} B"
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header with match type pill & reclaimable space
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (cluster.matchType == DuplicateMatchType.EXACT_HASH) Color(0xFF10B981).copy(alpha = 0.20f)
                            else MaterialTheme.colorScheme.secondaryContainer
                        )
                        .padding(horizontal = 7.dp, vertical = 2.5.dp)
                ) {
                    Text(
                        text = cluster.matchType.label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (cluster.matchType == DuplicateMatchType.EXACT_HASH) Color(0xFF10B981)
                        else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Text(
                    text = "Reclaim $reclaimText",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Recommendation Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = cluster.recommendationReason,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Side-by-side comparison: Keep Photo vs Duplicate Photo
            val firstDuplicate = cluster.duplicatePhotos.first()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PhotoComparisonCard(
                    photoWithTags = cluster.keepPhoto,
                    isKeepWinner = true,
                    modifier = Modifier.weight(1f)
                )
                PhotoComparisonCard(
                    photoWithTags = firstDuplicate,
                    isKeepWinner = false,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onIgnore,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "Ignore", fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Button(
                    onClick = onClean,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "Prune Duplicate", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Individual card inside the comparison row.
 */
@Composable
private fun PhotoComparisonCard(
    photoWithTags: PhotoWithTags,
    isKeepWinner: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageFile = remember(photoWithTags.photo.thumbPath, photoWithTags.photo.originalPath) {
        val relThumb = File(context.filesDir, photoWithTags.photo.thumbPath)
        val relOrig = File(context.filesDir, photoWithTags.photo.originalPath)
        val absOrig = File(photoWithTags.photo.originalPath)
        when {
            relThumb.exists() && relThumb.length() > 0 -> relThumb
            relOrig.exists() && relOrig.length() > 0 -> relOrig
            absOrig.exists() && absOrig.length() > 0 -> absOrig
            else -> relThumb
        }
    }

    val pixels = photoWithTags.photo.width.toLong() * photoWithTags.photo.height.toLong()
    val mpFormatted = if (pixels > 0) "%.1f MP".format(pixels / 1_000_000.0) else "Unknown"
    val sizeFormatted = when {
        photoWithTags.photo.sizeBytes >= 1024 * 1024 -> "%.1f MB".format(photoWithTags.photo.sizeBytes / (1024.0 * 1024.0))
        photoWithTags.photo.sizeBytes >= 1024 -> "%.0f KB".format(photoWithTags.photo.sizeBytes / 1024.0)
        else -> "${photoWithTags.photo.sizeBytes} B"
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isKeepWinner) Color(0xFF10B981).copy(alpha = 0.08f)
                else MaterialTheme.colorScheme.surface
            )
            .border(
                width = 1.dp,
                color = if (isKeepWinner) Color(0xFF10B981).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(10.dp)
            )
            .padding(6.dp)
    ) {
        // Thumbnail with badge overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageFile)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Winner badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isKeepWinner) Color(0xFF10B981) else Color(0xFFE11D48))
                    .padding(horizontal = 4.5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (isKeepWinner) "✓ KEEP" else "🗑️ REMOVE",
                    color = Color.White,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Resolution & Size details
        Text(
            text = "$mpFormatted • $sizeFormatted",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isKeepWinner) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )

        // Dimensions
        Text(
            text = "${photoWithTags.photo.width} × ${photoWithTags.photo.height}",
            fontSize = 9.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
        )

        // Tags Count
        if (photoWithTags.tags.isNotEmpty()) {
            Text(
                text = "${photoWithTags.tags.size} tag(s)",
                fontSize = 9.5.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
