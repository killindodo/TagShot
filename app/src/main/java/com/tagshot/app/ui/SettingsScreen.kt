package com.tagshot.app.ui

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.tagshot.app.ui.components.TagBadge
import com.tagshot.app.ui.components.TagDisplaySize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tagshot.app.R
import com.tagshot.app.TagShotApp
import com.tagshot.app.ui.components.bouncyClickable
import com.tagshot.app.ui.components.glassBorder
import com.tagshot.app.ui.theme.AccentPalette
import com.tagshot.app.ui.theme.TagShotGradients
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Specialist-grade Settings Screen for TagShot Pro.
 * Provides customisation for Grid Density, Tactile Haptics, Theme Mode,
 * Storage & Library Cache sweep, and the prominent "Made with ❤️ by killindodo" attribution.
 */
@Composable
fun SettingsScreen(
    viewModel: PhotoViewModel,
    onNavigateToTagManager: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val uiState by viewModel.uiState.collectAsState()
    val gridColumns by viewModel.gridColumns.collectAsState()
    val hapticsEnabled by viewModel.hapticsEnabled.collectAsState()
    val themeMode by viewModel.themeModeFlow.collectAsState()
    val accentPaletteStr by viewModel.accentPaletteFlow.collectAsState()
    val currentPalette = remember(accentPaletteStr) {
        AccentPalette.fromNameOrDefault(accentPaletteStr)
    }
    val viewingStyleStr by viewModel.viewingStyleFlow.collectAsState()
    val tagDisplaySizeStr by viewModel.tagDisplaySizeFlow.collectAsState()
    val showListFileSize by viewModel.showListFileSizeFlow.collectAsState()
    val showListDate by viewModel.showListDateFlow.collectAsState()
    val showListFolder by viewModel.showListFolderFlow.collectAsState()

    val viewingStyle = remember(viewingStyleStr) {
        try { ViewingStyle.valueOf(viewingStyleStr) } catch (_: Exception) { ViewingStyle.GRID }
    }
    val tagDisplaySize = remember(tagDisplaySizeStr) {
        try { TagDisplaySize.valueOf(tagDisplaySizeStr) } catch (_: Exception) { TagDisplaySize.THIN_MICRO }
    }

    val totalPhotos = (uiState as? MainUiState.Success)?.totalLibraryCount ?: 0
    val totalTags = (uiState as? MainUiState.Success)?.allTags?.size ?: 0

    var cacheSizeText by remember { mutableStateOf("Calculating...") }
    var isSweeping by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            cacheSizeText = calculateCacheSizeFormatted(context)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // --- Header Section ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Settings & Customization",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Tune gallery density, feedback & library performance",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 1. Viewing Style Section ---
        SectionHeader(title = "VIEWING STYLE & LAYOUT")
        ViewingStyleCard(
            selectedStyle = viewingStyle,
            onSelectStyle = { viewModel.setViewingStyle(it) },
            hapticsEnabled = hapticsEnabled
        )

        Spacer(modifier = Modifier.height(20.dp))

        // --- 2. Tag Badge Customization ---
        SectionHeader(title = "TAG BADGE CUSTOMIZATION")
        TagSizeCard(
            selectedSize = tagDisplaySize,
            onSelectSize = { viewModel.setTagDisplaySize(it) },
            hapticsEnabled = hapticsEnabled
        )

        Spacer(modifier = Modifier.height(20.dp))

        // --- 3. Windows List Details Options ---
        SectionHeader(title = "WINDOWS LIST DETAILS OPTIONS")
        WindowsDetailsOptionsCard(
            showFileSize = showListFileSize,
            showDate = showListDate,
            showFolder = showListFolder,
            onToggleFileSize = { viewModel.setShowListFileSize(!showListFileSize) },
            onToggleDate = { viewModel.setShowListDate(!showListDate) },
            onToggleFolder = { viewModel.setShowListFolder(!showListFolder) },
            hapticsEnabled = hapticsEnabled
        )

        Spacer(modifier = Modifier.height(20.dp))

        // --- 4. Grid Density Section ---
        SectionHeader(title = "GRID DENSITY")
        GridDensityCard(
            selectedColumns = gridColumns,
            onSelectColumns = { viewModel.setGridColumns(it) },
            hapticsEnabled = hapticsEnabled
        )

        Spacer(modifier = Modifier.height(20.dp))

        // --- 2. Tactile Haptics Section ---
        SectionHeader(title = "INTERACTIONS & FEEDBACK")
        HapticsCard(
            enabled = hapticsEnabled,
            onToggle = { viewModel.setHapticsEnabled(!hapticsEnabled) }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // --- Accent Theme & Palettes Section ---
        SectionHeader(title = "ACCENT THEME & PALETTES")
        AccentPaletteSelectorCard(
            selectedPalette = currentPalette,
            onSelectPalette = { palette: AccentPalette -> viewModel.setAccentPalette(palette) },
            hapticsEnabled = hapticsEnabled
        )

        Spacer(modifier = Modifier.height(20.dp))

        // --- 3. Theme Mode Section ---
        SectionHeader(title = "APPEARANCE")
        ThemeSelectorCard(
            selectedMode = themeMode,
            onSelectMode = { viewModel.setThemeMode(it) },
            hapticsEnabled = hapticsEnabled
        )

        Spacer(modifier = Modifier.height(20.dp))

        // --- 4. Storage & Library Stats ---
        SectionHeader(title = "STORAGE & MAINTENANCE")
        StorageStatsCard(
            totalPhotos = totalPhotos,
            totalTags = totalTags,
            cacheSize = cacheSizeText,
            isSweeping = isSweeping,
            onSweepOrphans = {
                coroutineScope.launch {
                    isSweeping = true
                    val sweptCount = withContext(Dispatchers.IO) {
                        val app = context.applicationContext as? TagShotApp
                        val swept = try {
                            app?.container?.orphanSweeper?.sweep() ?: 0
                        } catch (_: Exception) {
                            0
                        }
                        // Also clear temporary camera cache
                        val cameraDir = File(context.cacheDir, "camera")
                        cameraDir.listFiles()?.forEach { if (it.isFile) it.delete() }
                        swept
                    }
                    cacheSizeText = withContext(Dispatchers.IO) {
                        calculateCacheSizeFormatted(context)
                    }
                    isSweeping = false
                    onShowMessage("Storage maintenance complete: $sweptCount unreferenced file(s) swept")
                }
            },
            onManageTags = onNavigateToTagManager,
            hapticsEnabled = hapticsEnabled
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- 5. Special Attribution & Branding Section ---
        AttributionCard(hapticsEnabled = hapticsEnabled)

        // Extra bottom spacing to ensure bottom dock doesn't obscure any content
        Spacer(modifier = Modifier.height(110.dp))
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        ),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun ViewingStyleCard(
    selectedStyle: ViewingStyle,
    onSelectStyle: (ViewingStyle) -> Unit,
    hapticsEnabled: Boolean
) {
    val haptic = LocalHapticFeedback.current
    val styles = listOf(
        Triple(ViewingStyle.GRID, Icons.Default.GridView, "Grid"),
        Triple(ViewingStyle.LIST_DETAILS, Icons.AutoMirrored.Filled.ViewList, "Windows List"),
        Triple(ViewingStyle.MASONRY, Icons.Default.Dashboard, "Masonry")
    )

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .glassBorder(shape = RoundedCornerShape(18.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Default Gallery Layout",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = "Choose between classic square grid, Windows File Explorer-style details list, or dynamic masonry",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                styles.forEach { (style, icon, label) ->
                    val isSelected = selectedStyle == style
                    val itemBg by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        label = "styleBg"
                    )
                    val iconTint by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        label = "styleIconTint"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(itemBg)
                            .border(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .bouncyClickable(
                                hapticsEnabled = hapticsEnabled,
                                onClick = {
                                    if (hapticsEnabled) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    onSelectStyle(style)
                                }
                            )
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = iconTint,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TagSizeCard(
    selectedSize: TagDisplaySize,
    onSelectSize: (TagDisplaySize) -> Unit,
    hapticsEnabled: Boolean
) {
    val haptic = LocalHapticFeedback.current
    val sizes = listOf(
        Pair(TagDisplaySize.THIN_MICRO, "Thin / Micro"),
        Pair(TagDisplaySize.STANDARD, "Standard"),
        Pair(TagDisplaySize.COLOR_DOT, "Color Dot")
    )

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .glassBorder(shape = RoundedCornerShape(18.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Photo Card Tag Size",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = "Thin / Micro fits 2-3 tags neatly on photos without obscuring the photo or getting hidden",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Selector Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sizes.forEach { (size, label) ->
                    val isSelected = selectedSize == size
                    val itemBg by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        label = "tagSizeBg"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(itemBg)
                            .border(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .bouncyClickable(
                                hapticsEnabled = hapticsEnabled,
                                onClick = {
                                    if (hapticsEnabled) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    onSelectSize(size)
                                }
                            )
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Interactive Live Preview Frame
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .border(0.75.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Column {
                    Text(
                        text = "LIVE PREVIEW (3 TAGS ON SQUARE PHOTO)",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.6f),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TagBadge(normalizedName = "vacation", displayName = "vacation", displaySize = selectedSize)
                        TagBadge(normalizedName = "nature", displayName = "nature", displaySize = selectedSize)
                        TagBadge(normalizedName = "sunset", displayName = "sunset", displaySize = selectedSize)
                    }
                }
            }
        }
    }
}

@Composable
private fun WindowsDetailsOptionsCard(
    showFileSize: Boolean,
    showDate: Boolean,
    showFolder: Boolean,
    onToggleFileSize: () -> Unit,
    onToggleDate: () -> Unit,
    onToggleFolder: () -> Unit,
    hapticsEnabled: Boolean
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .glassBorder(shape = RoundedCornerShape(18.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Details List Row Columns",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = "Configure metadata elements visible in the Windows File Explorer details view",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Toggle 1: File Size & Dimensions
            SettingToggleRow(
                title = "File Size & Dimensions",
                description = "Shows image storage size (e.g. 2.4 MB) and resolution",
                checked = showFileSize,
                onCheckedChange = { onToggleFileSize() },
                hapticsEnabled = hapticsEnabled
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // Toggle 2: Date & Timestamp
            SettingToggleRow(
                title = "Timestamp",
                description = "Shows photo capture or import date & time",
                checked = showDate,
                onCheckedChange = { onToggleDate() },
                hapticsEnabled = hapticsEnabled
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // Toggle 3: Folder Location
            SettingToggleRow(
                title = "Folder Location Pill",
                description = "Shows the source album or directory pill next to the title",
                checked = showFolder,
                onCheckedChange = { onToggleFolder() },
                hapticsEnabled = hapticsEnabled
            )
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    hapticsEnabled: Boolean
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = {
                if (hapticsEnabled) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                onCheckedChange(it)
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}

@Composable
private fun GridDensityCard(
    selectedColumns: Int,
    onSelectColumns: (Int) -> Unit,
    hapticsEnabled: Boolean
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .glassBorder(shape = RoundedCornerShape(18.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Gallery Photo Columns",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = "Choose your preferred layout density for browsing photos",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                DensityOptionItem(
                    title = "Detail",
                    subtitle = "1 Column",
                    icon = Icons.Default.ViewAgenda,
                    isSelected = selectedColumns == 1,
                    onClick = { onSelectColumns(1) },
                    hapticsEnabled = hapticsEnabled,
                    modifier = Modifier.weight(1f)
                )
                DensityOptionItem(
                    title = "Standard",
                    subtitle = "2 Columns",
                    icon = Icons.Default.GridView,
                    isSelected = selectedColumns == 2,
                    onClick = { onSelectColumns(2) },
                    hapticsEnabled = hapticsEnabled,
                    modifier = Modifier.weight(1f)
                )
                DensityOptionItem(
                    title = "Compact",
                    subtitle = "3 Columns",
                    icon = Icons.Default.ViewModule,
                    isSelected = selectedColumns == 3,
                    onClick = { onSelectColumns(3) },
                    hapticsEnabled = hapticsEnabled,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun DensityOptionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    hapticsEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val bgAnim by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        },
        animationSpec = tween(200),
        label = "densityBg"
    )
    val borderAnim by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            Color.Transparent
        },
        animationSpec = tween(200),
        label = "densityBorder"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgAnim)
            .border(1.5.dp, borderAnim, RoundedCornerShape(12.dp))
            .bouncyClickable(
                scaleDownFactor = 0.94f,
                hapticsEnabled = hapticsEnabled,
                onClick = onClick
            )
            .padding(vertical = 12.dp, horizontal = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HapticsCard(
    enabled: Boolean,
    onToggle: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .glassBorder(shape = RoundedCornerShape(18.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surface
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Vibration,
                    contentDescription = null,
                    tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Tactile Haptics",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = "Spring vibrations on dock taps, chip toggles & photo selection",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}

@Composable
private fun AccentPaletteSelectorCard(
    selectedPalette: AccentPalette,
    onSelectPalette: (AccentPalette) -> Unit,
    hapticsEnabled: Boolean
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .glassBorder(shape = RoundedCornerShape(18.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Accent Engine",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = selectedPalette.shortName.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                text = "Dynamic aerospace colorways with real-time reactive theme engine",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
            )

            // 6 Circular Color Swatches Row
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                AccentPalette.entries.forEach { palette ->
                    val isSelected = palette == selectedPalette
                    AccentSwatchItem(
                        palette = palette,
                        isSelected = isSelected,
                        onClick = { onSelectPalette(palette) },
                        hapticsEnabled = hapticsEnabled
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Active Palette Info Banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                modifier = Modifier
                    .fillMaxWidth()
                    .glassBorder(
                        shape = RoundedCornerShape(12.dp),
                        strokeWidth = 0.5.dp,
                        highlightColor = selectedPalette.primaryColor.copy(alpha = 0.35f)
                    )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(selectedPalette.primaryColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = selectedPalette.title,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• ${selectedPalette.subtitle}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AccentSwatchItem(
    palette: AccentPalette,
    isSelected: Boolean,
    onClick: () -> Unit,
    hapticsEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val ringColor by animateColorAsState(
        targetValue = if (isSelected) palette.primaryColor else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "swatchRingColor"
    )

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "swatchScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .scale(scale)
            .bouncyClickable(
                hapticsEnabled = hapticsEnabled,
                onClick = {
                    if (hapticsEnabled) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    onClick()
                }
            )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .border(
                    width = if (isSelected) 2.5.dp else 1.dp,
                    color = if (isSelected) ringColor else Color.White.copy(alpha = 0.12f),
                    shape = CircleShape
                )
                .padding(3.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            listOf(
                                palette.primaryColor,
                                palette.secondaryColor
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = if (palette == AccentPalette.OBSIDIAN_MONO) Color(0xFF0F172A) else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = palette.shortName,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
            ),
            color = if (isSelected) palette.primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ThemeSelectorCard(
    selectedMode: String,
    onSelectMode: (String) -> Unit,
    hapticsEnabled: Boolean
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .glassBorder(shape = RoundedCornerShape(18.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Theme Palette",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = "Bespoke Titanium Dark or crisp Porcelain Light",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ThemeOptionItem(
                    title = "System",
                    icon = Icons.Default.SettingsBrightness,
                    isSelected = selectedMode == "SYSTEM",
                    onClick = { onSelectMode("SYSTEM") },
                    hapticsEnabled = hapticsEnabled,
                    modifier = Modifier.weight(1f)
                )
                ThemeOptionItem(
                    title = "Dark Titanium",
                    icon = Icons.Default.DarkMode,
                    isSelected = selectedMode == "DARK",
                    onClick = { onSelectMode("DARK") },
                    hapticsEnabled = hapticsEnabled,
                    modifier = Modifier.weight(1f)
                )
                ThemeOptionItem(
                    title = "Light Porcelain",
                    icon = Icons.Default.LightMode,
                    isSelected = selectedMode == "LIGHT",
                    onClick = { onSelectMode("LIGHT") },
                    hapticsEnabled = hapticsEnabled,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ThemeOptionItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    hapticsEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val bgAnim by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        },
        animationSpec = tween(200),
        label = "themeBg"
    )
    val borderAnim by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            Color.Transparent
        },
        animationSpec = tween(200),
        label = "themeBorder"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgAnim)
            .border(1.5.dp, borderAnim, RoundedCornerShape(12.dp))
            .bouncyClickable(
                scaleDownFactor = 0.94f,
                hapticsEnabled = hapticsEnabled,
                onClick = onClick
            )
            .padding(vertical = 10.dp, horizontal = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 10.5.sp
            ),
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun StorageStatsCard(
    totalPhotos: Int,
    totalTags: Int,
    cacheSize: String,
    isSweeping: Boolean,
    onSweepOrphans: () -> Unit,
    onManageTags: () -> Unit,
    hapticsEnabled: Boolean
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .glassBorder(shape = RoundedCornerShape(18.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Library Overview",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Local Storage",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stat counters row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatTile(
                    title = "Photos",
                    value = totalPhotos.toString(),
                    icon = Icons.Default.PhotoLibrary,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    title = "Tags",
                    value = totalTags.toString(),
                    icon = Icons.AutoMirrored.Filled.Label,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    title = "Cache",
                    value = cacheSize,
                    icon = Icons.Default.Storage,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(14.dp))

            // Safe Orphan Sweep Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Orphan Cleaner",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "Sweep unreferenced thumbnails & camera cache files safely",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onSweepOrphans,
                    enabled = !isSweeping,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    if (isSweeping) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CleaningServices,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sweep")
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tag Manager link
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                    .bouncyClickable(
                        scaleDownFactor = 0.96f,
                        hapticsEnabled = hapticsEnabled,
                        onClick = onManageTags
                    )
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Label,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Open Tag Manager",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                    Text(
                        text = "Manage →",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun StatTile(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            maxLines = 1
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AttributionCard(hapticsEnabled: Boolean) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .glassBorder(
                shape = RoundedCornerShape(22.dp),
                strokeWidth = 1.dp,
                highlightColor = Color.White.copy(alpha = 0.22f),
                shadowColor = Color.White.copy(alpha = 0.04f)
            )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Logo Icon
            Icon(
                painter = painterResource(id = R.drawable.ic_tagshot_pro_logo),
                contentDescription = "TagShot Pro Logo",
                tint = Color.Unspecified,
                modifier = Modifier
                    .size(54.dp)
                    .padding(bottom = 8.dp)
            )

            // App Title & Version Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "TagShot Pro",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "v1.0.0",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Precision Offline Photo Catalog & Organizer",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Prominent Luminous Gradient Accent Badge: Made with ❤️ by killindodo
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFF43F5E), // Rose Coral
                                Color(0xFF8B5CF6), // Radiant Violet
                                Color(0xFF0EA5E9)  // Radiant Sky Blue
                            )
                        )
                    )
                    .bouncyClickable(
                        scaleDownFactor = 0.94f,
                        hapticsEnabled = hapticsEnabled,
                        onClick = { /* Tactile badge tap */ }
                    )
                    .padding(horizontal = 18.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_heart_glow),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Made with ❤️ by killindodo",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 0.4.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))

            // Vector Icons & Open Source Attribution
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Open Source Graphics & Icon Attribution",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Crafted using premium vector icons and glyphs from Arcticons, Tabler Icons, and Google Material Symbols under Apache 2.0 and MIT open-source licenses.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "100% Offline • Zero Analytics • Pure Local Privacy",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp
                ),
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

private fun calculateCacheSizeFormatted(context: Context): String {
    var totalBytes = 0L
    try {
        val thumbDir = File(context.filesDir, "photos/thumbs")
        if (thumbDir.exists()) {
            thumbDir.walkTopDown().forEach { file ->
                if (file.isFile) totalBytes += file.length()
            }
        }
        val cameraDir = File(context.cacheDir, "camera")
        if (cameraDir.exists()) {
            cameraDir.walkTopDown().forEach { file ->
                if (file.isFile) totalBytes += file.length()
            }
        }
        context.cacheDir.listFiles()?.forEach { file ->
            if (file.isFile) totalBytes += file.length()
        }
    } catch (_: Exception) {
        return "0 KB"
    }

    return when {
        totalBytes >= 1024 * 1024 -> "%.1f MB".format(totalBytes / (1024.0 * 1024.0))
        totalBytes >= 1024 -> "%.1f KB".format(totalBytes / 1024.0)
        else -> "$totalBytes B"
    }
}
