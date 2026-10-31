package com.tagshot.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import com.tagshot.app.data.PhotoWithTags
import com.tagshot.app.ui.theme.TagShotGradients
import java.io.File

/**
 * Specialist-grade PhotoCell featuring tactile spring bounce,
 * bottom vignette scrim, micro-capsule tag pills, and animated selection states.
 */
@Composable
fun PhotoCell(
    photoWithTags: PhotoWithTags,
    isSelected: Boolean,
    isInSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    tagDisplaySize: TagDisplaySize = TagDisplaySize.THIN_MICRO,
    aspectRatio: Float = 1f
) {
    val context = LocalContext.current
    val thumbFile = remember(photoWithTags.photo.thumbPath) {
        File(context.filesDir, photoWithTags.photo.thumbPath)
    }
    var isError by remember(photoWithTags.photo.id) { mutableStateOf(false) }

    val tagDescriptions = if (photoWithTags.tags.isEmpty()) {
        "untagged"
    } else {
        photoWithTags.tags.joinToString(", ") { it.name }
    }
    val cellDescription = "Photo, tags: $tagDescriptions"

    // Fluid animated selection scale & border
    val cellScale by animateFloatAsState(
        targetValue = if (isSelected) 0.93f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cellSelectScale"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primary
            isInSelectionMode -> Color.White.copy(alpha = 0.12f)
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = 200),
        label = "cellBorderColor"
    )

    val cellShape = RoundedCornerShape(12.dp)
    val baseModifier = if (aspectRatio > 0f) modifier.aspectRatio(aspectRatio) else modifier

    Box(
        modifier = baseModifier
            .scale(cellScale)
            .clip(cellShape)
            .border(
                width = if (isSelected) 2.5.dp else if (isInSelectionMode) 1.dp else 0.dp,
                color = borderColor,
                shape = cellShape
            )
            .semantics { contentDescription = cellDescription }
            .bouncyClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        if (isError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.BrokenImage,
                    contentDescription = "Missing file",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(32.dp)
                )
            }
        } else {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(thumbFile)
                    .crossfade(true)
                    .size(512)
                    .precision(Precision.EXACT)
                    .listener(
                        onError = { _, _ -> isError = true }
                    )
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Bottom Vignette Gradient Scrim for crisp badge legibility
        if (photoWithTags.tags.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(TagShotGradients.bottomVignetteScrim)
            )

            // Micro-capsule Tag Badges (fits 2-3+ tags cleanly based on tagDisplaySize)
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val maxTagsToShow = when (tagDisplaySize) {
                    TagDisplaySize.THIN_MICRO -> 3
                    TagDisplaySize.COLOR_DOT -> 4
                    TagDisplaySize.STANDARD -> 2
                }
                val displayTags = photoWithTags.tags.take(maxTagsToShow)
                val remainingCount = photoWithTags.tags.size - maxTagsToShow

                for (tag in displayTags) {
                    TagBadge(
                        normalizedName = tag.normalizedName,
                        displayName = tag.name,
                        displaySize = tagDisplaySize
                    )
                }

                if (remainingCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.75f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+$remainingCount",
                            color = Color.White,
                            fontSize = if (tagDisplaySize == TagDisplaySize.THIN_MICRO) 7.5.sp else 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 10.sp
                        )
                    }
                }
            }
        }

        // Selection Checkmark Overlay with spring pop-in physics
        if (isInSelectionMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(7.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else Color.Black.copy(alpha = 0.5f)
                    )
                    .border(
                        width = 1.5.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.White.copy(alpha = 0.75f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                AnimatedVisibility(
                    visible = isSelected,
                    enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
