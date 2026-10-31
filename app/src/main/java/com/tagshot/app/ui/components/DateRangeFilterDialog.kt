package com.tagshot.app.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.tagshot.app.ui.theme.LocalUiDesignSystem
import com.tagshot.app.ui.theme.UiDesignSystem
import com.tagshot.app.ui.theme.retroBevel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tagshot.app.data.PhotoWithTags
import com.tagshot.app.ui.PhotoViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * High-End Apple/Google Photos style Date Range Filter Dialog.
 * Allows filtering photos taken or imported between dates, with 1-tap fast presets,
 * interactive date pickers, live matching count, and clear functionality.
 */
@Composable
fun DateRangeFilterDialog(
    initialStartDate: Long?,
    initialEndDate: Long?,
    allPhotos: List<PhotoWithTags>,
    onApplyRange: (startDate: Long?, endDate: Long?) -> Unit,
    onClearRange: () -> Unit,
    onDismiss: () -> Unit,
    hapticsEnabled: Boolean = true
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var selectedStart by remember { mutableStateOf(initialStartDate) }
    var selectedEnd by remember { mutableStateOf(initialEndDate) }

    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    // Real-time matching count
    val matchingPhotosCount = remember(selectedStart, selectedEnd, allPhotos) {
        allPhotos.count { photo ->
            val timestamp = photo.photo.takenAt ?: photo.photo.addedAt
            val afterStart = selectedStart == null || timestamp >= selectedStart!!
            val beforeEnd = selectedEnd == null || timestamp <= selectedEnd!!
            afterStart && beforeEnd
        }
    }

    val designSystem = LocalUiDesignSystem.current
    val isDark = isSystemInDarkTheme()
    val dialogShape = designSystem.dialogShape

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val dialogModifier = if (designSystem == UiDesignSystem.RETRO_WIN95) {
            Modifier
                .fillMaxWidth(0.92f)
                .retroBevel(
                    isSunken = false,
                    strokeWidth = 2.dp,
                    highlightColor = if (isDark) Color(0xFF6B7280) else Color.White,
                    shadowColor = if (isDark) Color(0xFF14171D) else Color(0xFF404040),
                    fillColor = if (isDark) Color(0xFF2C3038) else Color(0xFFC0C0C0)
                )
        } else {
            Modifier
                .fillMaxWidth(0.92f)
                .clip(dialogShape)
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                            Color.Transparent
                        )
                    ),
                    shape = dialogShape
                )
        }

        Surface(
            modifier = dialogModifier,
            color = if (designSystem == UiDesignSystem.RETRO_WIN95) Color.Transparent else MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
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
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Filter By Date",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Browse photos captured between dates",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(14.dp))

                // Fast Date Presets Row
                Text(
                    text = "QUICK PRESETS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(PhotoViewModel.DatePreset.values()) { preset ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                if (hapticsEnabled) {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                }
                                val cal = Calendar.getInstance()
                                cal.set(Calendar.HOUR_OF_DAY, 23)
                                cal.set(Calendar.MINUTE, 59)
                                cal.set(Calendar.SECOND, 59)
                                cal.set(Calendar.MILLISECOND, 999)
                                val endToday = cal.timeInMillis

                                val start = when (preset) {
                                    PhotoViewModel.DatePreset.TODAY -> {
                                        cal.set(Calendar.HOUR_OF_DAY, 0)
                                        cal.set(Calendar.MINUTE, 0)
                                        cal.set(Calendar.SECOND, 0)
                                        cal.set(Calendar.MILLISECOND, 0)
                                        cal.timeInMillis
                                    }
                                    PhotoViewModel.DatePreset.PAST_7_DAYS -> {
                                        cal.add(Calendar.DAY_OF_YEAR, -6)
                                        cal.set(Calendar.HOUR_OF_DAY, 0)
                                        cal.set(Calendar.MINUTE, 0)
                                        cal.set(Calendar.SECOND, 0)
                                        cal.set(Calendar.MILLISECOND, 0)
                                        cal.timeInMillis
                                    }
                                    PhotoViewModel.DatePreset.THIS_MONTH -> {
                                        cal.set(Calendar.DAY_OF_MONTH, 1)
                                        cal.set(Calendar.HOUR_OF_DAY, 0)
                                        cal.set(Calendar.MINUTE, 0)
                                        cal.set(Calendar.SECOND, 0)
                                        cal.set(Calendar.MILLISECOND, 0)
                                        cal.timeInMillis
                                    }
                                    PhotoViewModel.DatePreset.PAST_30_DAYS -> {
                                        cal.add(Calendar.DAY_OF_YEAR, -29)
                                        cal.set(Calendar.HOUR_OF_DAY, 0)
                                        cal.set(Calendar.MINUTE, 0)
                                        cal.set(Calendar.SECOND, 0)
                                        cal.set(Calendar.MILLISECOND, 0)
                                        cal.timeInMillis
                                    }
                                    PhotoViewModel.DatePreset.THIS_YEAR -> {
                                        cal.set(Calendar.DAY_OF_YEAR, 1)
                                        cal.set(Calendar.HOUR_OF_DAY, 0)
                                        cal.set(Calendar.MINUTE, 0)
                                        cal.set(Calendar.SECOND, 0)
                                        cal.set(Calendar.MILLISECOND, 0)
                                        cal.timeInMillis
                                    }
                                }
                                selectedStart = start
                                selectedEnd = endToday
                            },
                            label = { Text(preset.label, fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Custom Range Pickers Row
                Text(
                    text = "CUSTOM RANGE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Start Date Button Box
                    DateSelectorCard(
                        title = "FROM DATE",
                        dateMillis = selectedStart,
                        defaultLabel = "Earliest",
                        dateFormat = dateFormat,
                        hapticsEnabled = hapticsEnabled,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val initial = Calendar.getInstance().apply {
                                if (selectedStart != null) timeInMillis = selectedStart!!
                            }
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val picked = Calendar.getInstance().apply {
                                        set(year, month, dayOfMonth, 0, 0, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    selectedStart = picked.timeInMillis
                                    if (selectedEnd != null && selectedEnd!! < selectedStart!!) {
                                        selectedEnd = selectedStart
                                    }
                                },
                                initial.get(Calendar.YEAR),
                                initial.get(Calendar.MONTH),
                                initial.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                    )

                    // End Date Button Box
                    DateSelectorCard(
                        title = "TO DATE",
                        dateMillis = selectedEnd,
                        defaultLabel = "Latest",
                        dateFormat = dateFormat,
                        hapticsEnabled = hapticsEnabled,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val initial = Calendar.getInstance().apply {
                                if (selectedEnd != null) timeInMillis = selectedEnd!!
                            }
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val picked = Calendar.getInstance().apply {
                                        set(year, month, dayOfMonth, 23, 59, 59)
                                        set(Calendar.MILLISECOND, 999)
                                    }
                                    selectedEnd = picked.timeInMillis
                                    if (selectedStart != null && selectedStart!! > selectedEnd!!) {
                                        selectedStart = selectedEnd
                                    }
                                },
                                initial.get(Calendar.YEAR),
                                initial.get(Calendar.MONTH),
                                initial.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Real-time Match Summary Pill
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (selectedStart == null && selectedEnd == null) {
                                    "Showing all ${allPhotos.size} photos"
                                } else {
                                    "Matches $matchingPhotosCount of ${allPhotos.size} photos"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (selectedStart != null || selectedEnd != null) {
                            IconButton(
                                onClick = {
                                    selectedStart = null
                                    selectedEnd = null
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Reset",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (hapticsEnabled) {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            }
                            onClearRange()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (hapticsEnabled) {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            }
                            onApplyRange(selectedStart, selectedEnd)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1.3f),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Apply Range", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DateSelectorCard(
    title: String,
    dateMillis: Long?,
    defaultLabel: String,
    dateFormat: SimpleDateFormat,
    hapticsEnabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val displayDate = remember(dateMillis) {
        if (dateMillis != null) dateFormat.format(Date(dateMillis)) else defaultLabel
    }
    val isSet = dateMillis != null
    val primaryColor = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(14.dp)

    Column(
        modifier = modifier
            .clip(shape)
            .background(
                if (isSet) primaryColor.copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            )
            .border(
                width = 1.dp,
                color = if (isSet) primaryColor.copy(alpha = 0.45f)
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                shape = shape
            )
            .bouncyClickable(
                scaleDownFactor = 0.92f,
                hapticsEnabled = hapticsEnabled,
                onClick = onClick
            )
            .padding(12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            ),
            color = if (isSet) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Event,
                contentDescription = null,
                tint = if (isSet) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = displayDate,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSet) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 13.sp
                ),
                color = if (isSet) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 1
            )
        }
    }
}
