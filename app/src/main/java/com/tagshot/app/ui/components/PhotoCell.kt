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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import coil.size.Precision
import com.tagshot.app.data.PhotoWithTags
import com.tagshot.app.ui.theme.LocalUiDesignSystem
import com.tagshot.app.ui.theme.TagAnimation
import com.tagshot.app.ui.theme.TagPalettePreset
import com.tagshot.app.ui.theme.TagPosition
import com.tagshot.app.ui.theme.TagShape
import com.tagshot.app.ui.theme.TagShotGradients
import com.tagshot.app.ui.theme.UiDesignSystem
import com.tagshot.app.ui.theme.retroBevel
import java.io.File

/**
 * Specialist-grade PhotoCell featuring progressive shimmer skeleton loading,
 * tactile spring bounce, bottom vignette scrim, micro-capsule tag pills,
 * and animated selection states without blank flashes or stutter.
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
    tagShape: TagShape = TagShape.SQUIRCLE,
    tagPosition: TagPosition = TagPosition.BOTTOM_LEFT,
    tagPreset: TagPalettePreset = TagPalettePreset.DYNAMIC_AUTO,
    tagAnimation: TagAnimation = TagAnimation.NONE,
    aspectRatio: Float = 1f
) {
    val context = LocalContext.current
    val thumbFile = remember(photoWithTags.photo.thumbPath) {
        File(context.filesDir, photoWithTags.photo.thumbPath)
    }
    var isError by remember(photoWithTags.photo.id) { mutableStateOf(false) }
    var isLoading by remember(photoWithTags.photo.id) { mutableStateOf(true) }

    val hasTags by remember(photoWithTags.tags) {
        derivedStateOf { photoWithTags.tags.isNotEmpty() }
    }

    val tagDescriptions = remember(photoWithTags.tags) {
        if (photoWithTags.tags.isEmpty()) {
            "untagged"
        } else {
            photoWithTags.tags.joinToString(", ") { it.name }
        }
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

    val activeBorderColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primary
            isInSelectionMode -> Color.White.copy(alpha = 0.18f)
            else -> Color.White.copy(alpha = 0.06f)
        },
        animationSpec = tween(durationMillis = 200),
        label = "cellBorderColor"
    )

    val designSystem = LocalUiDesignSystem.current
    val cellShape = designSystem.photoCellShape
    val baseModifier = if (aspectRatio > 0f) modifier.aspectRatio(aspectRatio) else modifier

    val cellBorderModifier = if (designSystem == UiDesignSystem.RETRO_WIN95) {
        if (isSelected) {
            Modifier.border(width = 3.dp, color = MaterialTheme.colorScheme.primary, shape = cellShape)
        } else {
            Modifier.retroBevel(
                isSunken = true,
                strokeWidth = 2.dp,
                highlightColor = Color(0xFF6B7280),
                shadowColor = Color(0xFF1B1E24)
            )
        }
    } else {
        Modifier.border(
            width = if (isSelected) 2.5.dp else if (isInSelectionMode) 1.dp else 0.5.dp,
            color = activeBorderColor,
            shape = cellShape
        )
    }

    Box(
        modifier = baseModifier
            .scale(cellScale)
            .clip(cellShape)
            .then(cellBorderModifier)
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
            // Progressive Apple-grade shimmer skeleton while Coil decodes/loads thumbnail
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .shimmerPlaceholder(visible = true, shape = cellShape)
                )
            }

            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(thumbFile)
                    .crossfade(180)
                    .size(512)
                    .precision(Precision.EXACT)
                    .listener(
                        onSuccess = { _, _ -> isLoading = false },
                        onError = { _, _ ->
                            isLoading = false
                            isError = true
                        }
                    )
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onState = { state ->
                    when (state) {
                        is AsyncImagePainter.State.Success -> {
                            isLoading = false
                        }
                        is AsyncImagePainter.State.Error -> {
                            isLoading = false
                            isError = true
                        }
                        is AsyncImagePainter.State.Loading -> {
                            isLoading = true
                        }
                        is AsyncImagePainter.State.Empty -> {
                            // Initial state before load starts
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Vignette Gradient Scrim for crisp badge legibility
        if (hasTags) {
            val isTopPosition = tagPosition == TagPosition.TOP_LEFT || tagPosition == TagPosition.TOP_RIGHT
            val scrimAlignment = if (isTopPosition) Alignment.TopCenter else Alignment.BottomCenter
            val rowAlignment = when (tagPosition) {
                TagPosition.BOTTOM_LEFT -> Alignment.BottomStart
                TagPosition.BOTTOM_RIGHT -> Alignment.BottomEnd
                TagPosition.TOP_LEFT -> Alignment.TopStart
                TagPosition.TOP_RIGHT -> Alignment.TopEnd
            }
            val horizontalArrangement = when (tagPosition) {
                TagPosition.BOTTOM_LEFT, TagPosition.TOP_LEFT -> Arrangement.Start
                TagPosition.BOTTOM_RIGHT, TagPosition.TOP_RIGHT -> Arrangement.End
            }

            Box(
                modifier = Modifier
                    .align(scrimAlignment)
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(TagShotGradients.bottomVignetteScrim)
            )

            // Dynamic Tag Badges positioned at user's desired anchor
            Row(
                modifier = Modifier
                    .align(rowAlignment)
                    .fillMaxWidth()
                    .padding(
                        start = 6.dp,
                        end = if (isInSelectionMode && tagPosition == TagPosition.TOP_RIGHT) 38.dp else 6.dp,
                        top = if (isTopPosition) 6.dp else 0.dp,
                        bottom = if (!isTopPosition) 6.dp else 0.dp
                    ),
                horizontalArrangement = horizontalArrangement,
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
                        displaySize = tagDisplaySize,
                        tagShape = tagShape,
                        tagPreset = tagPreset,
                        tagAnimation = tagAnimation
                    )
                    Spacer(modifier = Modifier.width(3.dp))
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
