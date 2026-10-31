package com.tagshot.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.Difference
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.ShapeLine
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.tagshot.app.domain.DuplicateCluster
import com.tagshot.app.ui.components.TagBadge
import com.tagshot.app.ui.components.TagDisplaySize
import com.tagshot.app.ui.theme.ScrubberBubbleStyle
import com.tagshot.app.ui.theme.ScrubberStyle
import com.tagshot.app.ui.theme.ScrubberWidth
import com.tagshot.app.ui.theme.TagAnimation
import com.tagshot.app.ui.theme.TagPalettePreset
import com.tagshot.app.ui.theme.TagPosition
import com.tagshot.app.ui.theme.TagShape
import com.tagshot.app.ui.theme.UiDesignSystem
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
    val tagShapeStr by viewModel.tagShapeFlow.collectAsState()
    val tagPositionStr by viewModel.tagPositionFlow.collectAsState()
    val tagPresetStr by viewModel.tagPalettePresetFlow.collectAsState()
    val tagAnimationStr by viewModel.tagAnimationFlow.collectAsState()
    val scrubberStyleStr by viewModel.scrubberStyleFlow.collectAsState()
    val scrubberWidthStr by viewModel.scrubberWidthFlow.collectAsState()
    val scrubberBubbleStyleStr by viewModel.scrubberBubbleStyleFlow.collectAsState()
    val uiDesignSystemStr by viewModel.uiDesignSystemFlow.collectAsState()

    val currentTagShape = remember(tagShapeStr) { TagShape.fromNameOrDefault(tagShapeStr) }
    val currentTagPosition = remember(tagPositionStr) { TagPosition.fromNameOrDefault(tagPositionStr) }
    val currentTagPreset = remember(tagPresetStr) { TagPalettePreset.fromNameOrDefault(tagPresetStr) }
    val currentTagAnimation = remember(tagAnimationStr) { TagAnimation.fromNameOrDefault(tagAnimationStr) }
    val currentScrubberStyle = remember(scrubberStyleStr) { ScrubberStyle.fromNameOrDefault(scrubberStyleStr) }
    val currentScrubberWidth = remember(scrubberWidthStr) { ScrubberWidth.fromNameOrDefault(scrubberWidthStr) }
    val currentScrubberBubble = remember(scrubberBubbleStyleStr) { ScrubberBubbleStyle.fromNameOrDefault(scrubberBubbleStyleStr) }
    val currentUiDesignSystem = remember(uiDesignSystemStr) { UiDesignSystem.fromNameOrDefault(uiDesignSystemStr) }

    val duplicateClusters by viewModel.duplicateClustersFlow.collectAsState()
    val isScanningDuplicates by viewModel.isScanningDuplicatesFlow.collectAsState()
    var showDecisionHubDialog by remember { mutableStateOf(false) }
    var wasScanning by remember { mutableStateOf(false) }

    LaunchedEffect(isScanningDuplicates) {
        if (wasScanning && !isScanningDuplicates && duplicateClusters.isNotEmpty()) {
            showDecisionHubDialog = true
        }
        wasScanning = isScanningDuplicates
    }

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

        // --- FastScroll Slider Customizer ---
        SectionHeader(title = "TIMELINE SCRUBBER / SLIDER")
        ScrubberCustomizerCard(
            selectedStyle = currentScrubberStyle,
            selectedWidth = currentScrubberWidth,
            selectedBubble = currentScrubberBubble,
            onSelectStyle = { viewModel.setScrubberStyle(it) },
            onSelectWidth = { viewModel.setScrubberWidth(it) },
            onSelectBubble = { viewModel.setScrubberBubbleStyle(it) },
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

        // --- Tag Style & Shape Customizer ---
        SectionHeader(title = "TAG SHAPE & STYLE CUSTOMIZER")
        TagStyleCustomizerCard(
            selectedShape = currentTagShape,
            selectedPosition = currentTagPosition,
            selectedPreset = currentTagPreset,
            selectedAnimation = currentTagAnimation,
            onSelectShape = { viewModel.setTagShape(it) },
            onSelectPosition = { viewModel.setTagPosition(it) },
            onSelectPreset = { viewModel.setTagPalettePreset(it) },
            onSelectAnimation = { viewModel.setTagAnimation(it) },
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

        // --- UI Design System Switcher ---
        SectionHeader(title = "UI DESIGN SYSTEM")
        UiDesignSystemSelectorCard(
            selectedSystem = currentUiDesignSystem,
            onSelectSystem = { viewModel.setUiDesignSystem(it) },
            hapticsEnabled = hapticsEnabled
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

        Spacer(modifier = Modifier.height(20.dp))

        // --- Duplicate & Similar Photo Finder ---
        SectionHeader(title = "DUPLICATE & SIMILAR PHOTO CLEANER")
        DuplicateFinderCard(
            isScanning = isScanningDuplicates,
            clusters = duplicateClusters,
            onScan = { viewModel.scanForDuplicates(context) },
            onOpenDecisionHub = { showDecisionHubDialog = true },
            hapticsEnabled = hapticsEnabled
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- 5. Special Attribution & Branding Section with Easter Egg ---
        AttributionCard(
            hapticsEnabled = hapticsEnabled,
            onShowMessage = onShowMessage
        )

        // Extra bottom spacing to ensure bottom dock doesn't obscure any content
        Spacer(modifier = Modifier.height(110.dp))
    }

    if (showDecisionHubDialog) {
        DuplicateDecisionHubDialog(
            clusters = duplicateClusters,
            onDismiss = { showDecisionHubDialog = false },
            onCleanCluster = { cluster ->
                viewModel.deleteDuplicatesInCluster(cluster) {
                    onShowMessage("Duplicate cluster cleaned")
                }
            },
            onCleanAllClusters = {
                val allIds = duplicateClusters.flatMap { it.duplicatePhotos }.map { it.photo.id }
                viewModel.deletePhotos(allIds)
                for (c in duplicateClusters) {
                    viewModel.dismissDuplicateCluster(c.id)
                }
                showDecisionHubDialog = false
                onShowMessage("All duplicate photos cleaned successfully")
            },
            onDismissCluster = { clusterId ->
                viewModel.dismissDuplicateCluster(clusterId)
            }
        )
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
private fun AttributionCard(
    hapticsEnabled: Boolean,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var eggTapCount by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var lastTapTime by remember { androidx.compose.runtime.mutableLongStateOf(0L) }
    var isEggUnlocked by remember { androidx.compose.runtime.mutableStateOf(false) }

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

            // Prominent Luminous Gradient Accent Badge with 7-tap Easter Egg
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
                        scaleDownFactor = 0.90f,
                        hapticsEnabled = hapticsEnabled,
                        onClick = {
                            val now = System.currentTimeMillis()
                            if (now - lastTapTime > 3500L) {
                                eggTapCount = 1
                            } else {
                                eggTapCount++
                            }
                            lastTapTime = now

                            if (eggTapCount in 1..4) {
                                if (hapticsEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            } else if (eggTapCount in 5..6) {
                                if (hapticsEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                onShowMessage("Entering creator's realm in ${7 - eggTapCount}...")
                            } else if (eggTapCount >= 7) {
                                if (hapticsEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                isEggUnlocked = true
                                eggTapCount = 0
                                onShowMessage("🏆 killindodo Realm Unlocked! Visiting GitHub...")
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/killindodo")).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    onShowMessage("Visit https://github.com/killindodo")
                                }
                            }
                        }
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

            // Unlocked Creator Easter Egg Badge
            if (isEggUnlocked) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF10B981).copy(alpha = 0.20f))
                        .border(1.dp, Color(0xFF10B981), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "🏆 killindodo Guest of Honor Unlocked!",
                            color = Color(0xFF10B981),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
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

/**
 * Interactive customizer card for tag geometric shapes, photo positions, color presets, and moving animations.
 */
@Composable
private fun TagStyleCustomizerCard(
    selectedShape: TagShape,
    selectedPosition: TagPosition,
    selectedPreset: TagPalettePreset,
    selectedAnimation: TagAnimation,
    onSelectShape: (TagShape) -> Unit,
    onSelectPosition: (TagPosition) -> Unit,
    onSelectPreset: (TagPalettePreset) -> Unit,
    onSelectAnimation: (TagAnimation) -> Unit,
    hapticsEnabled: Boolean
) {
    val haptic = LocalHapticFeedback.current

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Live Interactive Preview Box
            Text(
                text = "Live Tag Preview",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF1E293B),
                                Color(0xFF0F172A)
                            )
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                // Background simulated photo vignette
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color.White.copy(alpha = 0.05f), Color.Transparent),
                                radius = 200f
                            )
                        )
                )

                val previewAlignment = when (selectedPosition) {
                    TagPosition.BOTTOM_LEFT -> Alignment.BottomStart
                    TagPosition.BOTTOM_RIGHT -> Alignment.BottomEnd
                    TagPosition.TOP_LEFT -> Alignment.TopStart
                    TagPosition.TOP_RIGHT -> Alignment.TopEnd
                }

                Row(
                    modifier = Modifier.align(previewAlignment),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TagBadge(
                        normalizedName = "sunset",
                        displayName = "Sunset",
                        tagShape = selectedShape,
                        tagPreset = selectedPreset,
                        tagAnimation = selectedAnimation,
                        displaySize = TagDisplaySize.THIN_MICRO
                    )
                    TagBadge(
                        normalizedName = "travel",
                        displayName = "Travel",
                        tagShape = selectedShape,
                        tagPreset = selectedPreset,
                        tagAnimation = selectedAnimation,
                        displaySize = TagDisplaySize.THIN_MICRO
                    )
                    TagBadge(
                        normalizedName = "pro",
                        displayName = "Pro",
                        tagShape = selectedShape,
                        tagPreset = selectedPreset,
                        tagAnimation = selectedAnimation,
                        displaySize = TagDisplaySize.THIN_MICRO
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Geometric Shape Selector
            Text(
                text = "TAG GEOMETRIC SHAPE",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TagShape.entries.take(3).forEach { shape ->
                    val isSelected = selectedShape == shape
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                            .border(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .bouncyClickable(
                                hapticsEnabled = hapticsEnabled,
                                onClick = {
                                    if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelectShape(shape)
                                }
                            )
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = shape.displayName.split(" ").first(),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TagShape.entries.drop(3).forEach { shape ->
                    val isSelected = selectedShape == shape
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                            .border(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .bouncyClickable(
                                hapticsEnabled = hapticsEnabled,
                                onClick = {
                                    if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelectShape(shape)
                                }
                            )
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = shape.displayName.split(" ").first(),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Photo Anchor Position
            Text(
                text = "PHOTO ANCHOR POSITION",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TagPosition.entries.forEach { pos ->
                    val isSelected = selectedPosition == pos
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)
                            .border(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.secondary else Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .bouncyClickable(
                                hapticsEnabled = hapticsEnabled,
                                onClick = {
                                    if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelectPosition(pos)
                                }
                            )
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (pos) {
                                TagPosition.BOTTOM_LEFT -> "Bot Left"
                                TagPosition.BOTTOM_RIGHT -> "Bot Right"
                                TagPosition.TOP_LEFT -> "Top Left"
                                TagPosition.TOP_RIGHT -> "Top Right"
                            },
                            fontSize = 10.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Color Preset Swatches
            Text(
                text = "COLOR & PALETTE PRESET",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TagPalettePreset.entries.take(4).forEach { preset ->
                        val isSelected = selectedPreset == preset
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            preset.primaryColor.copy(alpha = if (isSelected) 0.85f else 0.25f),
                                            preset.secondaryColor.copy(alpha = if (isSelected) 0.70f else 0.15f)
                                        )
                                    )
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.5.dp,
                                    color = if (isSelected) Color.White else preset.primaryColor.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .bouncyClickable(
                                    hapticsEnabled = hapticsEnabled,
                                    onClick = {
                                        if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSelectPreset(preset)
                                    }
                                )
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = preset.displayName.split(" ").first(),
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TagPalettePreset.entries.drop(4).forEach { preset ->
                        val isSelected = selectedPreset == preset
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            preset.primaryColor.copy(alpha = if (isSelected) 0.85f else 0.25f),
                                            preset.secondaryColor.copy(alpha = if (isSelected) 0.70f else 0.15f)
                                        )
                                    )
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.5.dp,
                                    color = if (isSelected) Color.White else preset.primaryColor.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .bouncyClickable(
                                    hapticsEnabled = hapticsEnabled,
                                    onClick = {
                                        if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSelectPreset(preset)
                                    }
                                )
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = preset.displayName.split(" ").first(),
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Moving Animations
            Text(
                text = "DYNAMIC MOVING ANIMATION",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TagAnimation.entries.forEach { anim ->
                    val isSelected = selectedAnimation == anim
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surface)
                            .border(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.tertiary else Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .bouncyClickable(
                                hapticsEnabled = hapticsEnabled,
                                onClick = {
                                    if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelectAnimation(anim)
                                }
                            )
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = anim.displayName.split(" ").first(),
                            fontSize = 10.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

/**
 * Interactive customizer card for FastScroll scrubber style, width, and date bubble.
 */
@Composable
private fun ScrubberCustomizerCard(
    selectedStyle: ScrubberStyle,
    selectedWidth: ScrubberWidth,
    selectedBubble: ScrubberBubbleStyle,
    onSelectStyle: (ScrubberStyle) -> Unit,
    onSelectWidth: (ScrubberWidth) -> Unit,
    onSelectBubble: (ScrubberBubbleStyle) -> Unit,
    hapticsEnabled: Boolean
) {
    val haptic = LocalHapticFeedback.current

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 1. Slider Visual Style
            Text(
                text = "SLIDER TRACK STYLE",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ScrubberStyle.entries.forEach { style ->
                    val isSelected = selectedStyle == style
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                            .border(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .bouncyClickable(
                                hapticsEnabled = hapticsEnabled,
                                onClick = {
                                    if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelectStyle(style)
                                }
                            )
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = style.displayName.split(" ").first(),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Track Width / Thickness
            Text(
                text = "TRACK WIDTH / THICKNESS",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ScrubberWidth.entries.forEach { width ->
                    val isSelected = selectedWidth == width
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)
                            .border(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.secondary else Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .bouncyClickable(
                                hapticsEnabled = hapticsEnabled,
                                onClick = {
                                    if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelectWidth(width)
                                }
                            )
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = width.displayName.split(" ").first(),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Floating Date Bubble Style
            Text(
                text = "FLOATING DATE BUBBLE",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ScrubberBubbleStyle.entries.forEach { bubble ->
                    val isSelected = selectedBubble == bubble
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surface)
                            .border(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.tertiary else Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .bouncyClickable(
                                hapticsEnabled = hapticsEnabled,
                                onClick = {
                                    if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelectBubble(bubble)
                                }
                            )
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = bubble.displayName.split(" ").first(),
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

/**
 * Interactive selector card for the 6 foundational UI Design Systems.
 */
@Composable
private fun UiDesignSystemSelectorCard(
    selectedSystem: UiDesignSystem,
    onSelectSystem: (UiDesignSystem) -> Unit,
    hapticsEnabled: Boolean
) {
    val haptic = LocalHapticFeedback.current

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            UiDesignSystem.entries.forEach { system ->
                val isSelected = selectedSystem == system

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                        .border(
                            width = if (isSelected) 1.5.dp else 0.5.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .bouncyClickable(
                            hapticsEnabled = hapticsEnabled,
                            onClick = {
                                if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectSystem(system)
                            }
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = system.displayName,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = system.subtitle,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Interactive card for scanning and managing duplicate and similar burst photographs.
 */
@Composable
private fun DuplicateFinderCard(
    isScanning: Boolean,
    clusters: List<DuplicateCluster>,
    onScan: () -> Unit,
    onOpenDecisionHub: () -> Unit,
    hapticsEnabled: Boolean
) {
    val haptic = LocalHapticFeedback.current

    val totalReclaimableBytes = remember(clusters) {
        clusters.sumOf { it.reclaimableBytes }
    }
    val reclaimText = remember(totalReclaimableBytes) {
        when {
            totalReclaimableBytes >= 1024 * 1024 -> "%.1f MB".format(totalReclaimableBytes / (1024.0 * 1024.0))
            totalReclaimableBytes >= 1024 -> "%.1f KB".format(totalReclaimableBytes / 1024.0)
            else -> "$totalReclaimableBytes B"
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Dual-Engine Duplicate Finder",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "100% offline 64-bit dHash perceptual analysis & bitwise SHA-256",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (clusters.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF10B981).copy(alpha = 0.15f))
                        .border(1.dp, Color(0xFF10B981).copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${clusters.size} Duplicate Cluster(s) Found",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF10B981)
                            )
                            Text(
                                text = "Reclaimable Storage: $reclaimText",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Button(
                            onClick = {
                                if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onOpenDecisionHub()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981),
                                contentColor = Color.White
                            ),
                            shape = CircleShape
                        ) {
                            Text(text = "Review", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Button(
                onClick = {
                    if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onScan()
                },
                enabled = !isScanning,
                modifier = Modifier.fillMaxWidth(),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Scanning Library dHash...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(
                        imageVector = Icons.Default.Difference,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (clusters.isEmpty()) "Scan Library For Duplicates" else "Rescan Library",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
