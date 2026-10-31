package com.tagshot.app.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import com.tagshot.app.R
import com.tagshot.app.ui.components.bouncyClickable
import com.tagshot.app.ui.components.glassBorder
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.LabelOff
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import com.tagshot.app.domain.SortOrder
import com.tagshot.app.domain.TagMatchMode
import com.tagshot.app.ui.components.BottomPillNavDock
import com.tagshot.app.ui.components.DateRangeFilterDialog
import com.tagshot.app.ui.components.FastScrollScrubber
import com.tagshot.app.ui.components.PhotoCell
import com.tagshot.app.ui.components.StickyTimelineHeaderOverlay
import com.tagshot.app.ui.components.TagDisplaySize
import com.tagshot.app.ui.components.WindowsDetailsRow
import com.tagshot.app.ui.components.pinchZoomGrid
import com.tagshot.app.ui.theme.ScrubberBubbleStyle
import com.tagshot.app.ui.theme.ScrubberStyle
import com.tagshot.app.ui.theme.ScrubberWidth
import com.tagshot.app.ui.theme.TagAnimation
import com.tagshot.app.ui.theme.TagPalettePreset
import com.tagshot.app.ui.theme.TagPosition
import com.tagshot.app.ui.theme.TagShape
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: PhotoViewModel,
    onNavigateToDetail: (photoId: Long) -> Unit,
    onNavigateToTagManager: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val selectedPhotoIds by viewModel.selectedPhotoIds.collectAsState()
    val gridColumns by viewModel.gridColumns.collectAsState()
    val hapticsEnabled by viewModel.hapticsEnabled.collectAsState()
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

    val viewingStyle = remember(viewingStyleStr) {
        try { ViewingStyle.valueOf(viewingStyleStr) } catch (_: Exception) { ViewingStyle.GRID }
    }
    val tagDisplaySize = remember(tagDisplaySizeStr) {
        try { TagDisplaySize.valueOf(tagDisplaySizeStr) } catch (_: Exception) { TagDisplaySize.THIN_MICRO }
    }
    val tagShape = remember(tagShapeStr) { TagShape.fromNameOrDefault(tagShapeStr) }
    val tagPosition = remember(tagPositionStr) { TagPosition.fromNameOrDefault(tagPositionStr) }
    val tagPreset = remember(tagPresetStr) { TagPalettePreset.fromNameOrDefault(tagPresetStr) }
    val tagAnimation = remember(tagAnimationStr) { TagAnimation.fromNameOrDefault(tagAnimationStr) }
    val scrubberStyle = remember(scrubberStyleStr) { ScrubberStyle.fromNameOrDefault(scrubberStyleStr) }
    val scrubberWidth = remember(scrubberWidthStr) { ScrubberWidth.fromNameOrDefault(scrubberWidthStr) }
    val scrubberBubbleStyle = remember(scrubberBubbleStyleStr) { ScrubberBubbleStyle.fromNameOrDefault(scrubberBubbleStyleStr) }
    val snackbarHostState = remember { SnackbarHostState() }

    val startDate by viewModel.startDateFlow.collectAsState()
    val endDate by viewModel.endDateFlow.collectAsState()
    var showDateRangeDialog by remember { mutableStateOf(false) }

    val currentTab by remember(uiState) {
        derivedStateOf { (uiState as? MainUiState.Success)?.activeTab ?: MainTab.ALL_PHOTOS }
    }
    val isInSelectionMode by remember {
        derivedStateOf { selectedPhotoIds.isNotEmpty() }
    }
    val isInsideFolder by remember(uiState) {
        derivedStateOf {
            (uiState as? MainUiState.Success)?.let {
                it.activeTab == MainTab.FOLDER_WISE && it.selectedFolder.isNotEmpty()
            } == true
        }
    }
    val isSettingsTab by remember(currentTab) {
        derivedStateOf { currentTab == MainTab.SETTINGS }
    }

    // Back button exits selection mode first per Section 9.1; if inside folder, returns to folders list; if on settings, returns to gallery
    BackHandler(enabled = isInSelectionMode || isInsideFolder || isSettingsTab) {
        if (isInSelectionMode) {
            viewModel.clearSelection()
        } else if (isInsideFolder) {
            viewModel.onFolderSelected("")
        } else if (isSettingsTab) {
            viewModel.onTabSelected(MainTab.ALL_PHOTOS)
        }
    }

    // Pending camera file preserved across process death per Section 4.1
    var pendingCameraFileName by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingImportUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var pendingFolderUri by remember { mutableStateOf<Uri?>(null) }
    var showTagImportDialog by remember { mutableStateOf(false) }
    var showCameraTagDialog by remember { mutableStateOf(false) }
    var showFolderTagDialog by remember { mutableStateOf(false) }
    var showBulkAddTagDialog by remember { mutableStateOf(false) }
    var showBulkRemoveTagDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    // Folder Picker Launcher
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) {
            pendingFolderUri = treeUri
            showFolderTagDialog = true
        }
    }

    // Camera Capture Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val fileName = pendingCameraFileName
        if (success && fileName != null) {
            val file = File(context.cacheDir, "camera/$fileName")
            if (file.exists()) {
                showCameraTagDialog = true
            } else {
                pendingCameraFileName = null
            }
        } else {
            pendingCameraFileName = null
        }
    }

    // Photo Picker Launcher
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            pendingImportUris = uris
            showTagImportDialog = true
        }
    }

    // Handle UI Events
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is UiEvent.ImportFinished -> {
                    val s = event.summary
                    val msg = "${s.imported} imported, ${s.duplicates} duplicates skipped, ${s.failed} failed"
                    snackbarHostState.showSnackbar(msg)
                }
                is UiEvent.ShowUndoSnackbar -> {
                    val result = snackbarHostState.showSnackbar(
                        message = "${event.count} photo(s) deleted",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undoDelete()
                    }
                }
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (isInSelectionMode) {
                SelectionTopBar(
                    selectedCount = selectedPhotoIds.size,
                    onClearSelection = { viewModel.clearSelection() },
                    onBulkAddTag = { showBulkAddTagDialog = true },
                    onBulkRemoveTag = { showBulkRemoveTagDialog = true },
                    onShare = {
                        val photos = (uiState as? MainUiState.Success)?.photos
                            ?.filter { selectedPhotoIds.contains(it.photo.id) }
                            ?: emptyList()
                        sharePhotos(context, photos.map { it.photo.originalPath })
                    },
                    onDelete = { showDeleteConfirmation = true }
                )
            } else if (currentTab != MainTab.SETTINGS) {
                MainTopBar(
                    viewModel = viewModel,
                    onNavigateToTagManager = onNavigateToTagManager,
                    onSelectFolderClick = { folderPickerLauncher.launch(null) },
                    onOpenDateRange = { showDateRangeDialog = true }
                )
            }
        },
        floatingActionButton = {
            if (!isInSelectionMode && currentTab != MainTab.SETTINGS) {
                MainFab(
                    onCameraClick = {
                        try {
                            val cameraDir = File(context.cacheDir, "camera").apply { mkdirs() }
                            val newFileName = "${UUID.randomUUID()}.jpg"
                            val newFile = File(cameraDir, newFileName)
                            pendingCameraFileName = newFileName
                            val authority = "${context.packageName}.fileprovider"
                            val uri = FileProvider.getUriForFile(context, authority, newFile)
                            cameraLauncher.launch(uri)
                        } catch (e: ActivityNotFoundException) {
                            pendingCameraFileName = null
                            viewModel.showMessage("No camera application available")
                        }
                    },
                    onGalleryClick = {
                        pickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onFolderClick = {
                        folderPickerLauncher.launch(null)
                    },
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(bottom = 72.dp)
                )
            }
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 80.dp)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                when (val state = uiState) {
                    is MainUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is MainUiState.Success -> {
                        // Non-blocking Import Progress Bar
                        if (state.isImporting) {
                            val progress = state.importProgress
                            if (progress != null && progress.total > 0) {
                                LinearProgressIndicator(
                                    progress = { progress.done.toFloat() / progress.total },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            }
                        }

                        when (state.activeTab) {
                            MainTab.SETTINGS -> {
                                SettingsScreen(
                                    viewModel = viewModel,
                                    onNavigateToTagManager = onNavigateToTagManager,
                                    onShowMessage = { msg -> viewModel.showMessage(msg) },
                                    modifier = Modifier.statusBarsPadding()
                                )
                            }
                            MainTab.FOLDER_WISE -> {
                                if (state.selectedFolder.isEmpty()) {
                                    // Viewing all folders as cards/grid
                                    if (state.allFolders.isEmpty()) {
                                        EmptyFolderView(onSelectFolder = { folderPickerLauncher.launch(null) })
                                    } else {
                                        FolderGridView(
                                            folders = state.allFolders,
                                            onFolderClick = { folder ->
                                                viewModel.onFolderSelected(folder.name)
                                            },
                                            onSelectNewFolder = { folderPickerLauncher.launch(null) }
                                        )
                                    }
                                } else {
                                    // Viewing pictures inside the selected folder
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        IconButton(onClick = { viewModel.onFolderSelected("") }) {
                                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to folders")
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = state.selectedFolder,
                                                style = MaterialTheme.typography.titleMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${state.photos.size} photo(s)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    // Tag Filter Chips Row for photos in this folder
                                    TagChipsRow(
                                        viewModel = viewModel,
                                        state = state,
                                        startDate = startDate,
                                        endDate = endDate,
                                        onOpenDateRange = { showDateRangeDialog = true }
                                    )

                                    if (state.photos.isEmpty()) {
                                        EmptyFilterView(onClearFilters = { viewModel.onClearFilters() })
                                    } else {
                                        PhotoCollectionView(
                                            photos = state.photos,
                                            viewingStyle = viewingStyle,
                                            gridColumns = gridColumns,
                                            tagDisplaySize = tagDisplaySize,
                                            tagShape = tagShape,
                                            tagPosition = tagPosition,
                                            tagPreset = tagPreset,
                                            tagAnimation = tagAnimation,
                                            scrubberStyle = scrubberStyle,
                                            scrubberWidth = scrubberWidth,
                                            scrubberBubbleStyle = scrubberBubbleStyle,
                                            showListFileSize = showListFileSize,
                                            showListDate = showListDate,
                                            showListFolder = showListFolder,
                                            selectedPhotoIds = selectedPhotoIds,
                                            isInSelectionMode = isInSelectionMode,
                                            onPhotoClick = { id ->
                                                if (isInSelectionMode) {
                                                    viewModel.toggleSelection(id)
                                                } else {
                                                    onNavigateToDetail(id)
                                                }
                                            },
                                            onPhotoLongClick = { id ->
                                                if (!isInSelectionMode) {
                                                    viewModel.enterSelection(id)
                                                }
                                            },
                                            onColumnsChange = { newCols ->
                                                viewModel.setGridColumns(newCols)
                                            }
                                        )
                                    }
                                }
                            }
                            MainTab.ALL_PHOTOS -> {
                                TagChipsRow(
                                    viewModel = viewModel,
                                    state = state,
                                    startDate = startDate,
                                    endDate = endDate,
                                    onOpenDateRange = { showDateRangeDialog = true }
                                )

                                if (state.totalLibraryCount == 0) {
                                    EmptyLibraryView()
                                } else if (state.photos.isEmpty()) {
                                    EmptyFilterView(onClearFilters = { viewModel.onClearFilters() })
                                } else {
                                    PhotoCollectionView(
                                        photos = state.photos,
                                        viewingStyle = viewingStyle,
                                        gridColumns = gridColumns,
                                        tagDisplaySize = tagDisplaySize,
                                        tagShape = tagShape,
                                        tagPosition = tagPosition,
                                        tagPreset = tagPreset,
                                        tagAnimation = tagAnimation,
                                        scrubberStyle = scrubberStyle,
                                        scrubberWidth = scrubberWidth,
                                        scrubberBubbleStyle = scrubberBubbleStyle,
                                        showListFileSize = showListFileSize,
                                        showListDate = showListDate,
                                        showListFolder = showListFolder,
                                        selectedPhotoIds = selectedPhotoIds,
                                        isInSelectionMode = isInSelectionMode,
                                        onPhotoClick = { id ->
                                            if (isInSelectionMode) {
                                                viewModel.toggleSelection(id)
                                            } else {
                                                onNavigateToDetail(id)
                                            }
                                        },
                                        onPhotoLongClick = { id ->
                                            if (!isInSelectionMode) {
                                                viewModel.enterSelection(id)
                                            }
                                        },
                                        onColumnsChange = { newCols ->
                                            viewModel.setGridColumns(newCols)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Floating Bottom Pill Navigation Dock
            if (!isInSelectionMode) {
                BottomPillNavDock(
                    activeTab = currentTab,
                    onTabSelected = { viewModel.onTabSelected(it) },
                    hapticsEnabled = hapticsEnabled,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 8.dp)
                )
            }
        }
    }

    // Dialog: Date Range Filter
    if (showDateRangeDialog) {
        val allPhotos = (uiState as? MainUiState.Success)?.photos ?: emptyList()
        DateRangeFilterDialog(
            initialStartDate = startDate,
            initialEndDate = endDate,
            allPhotos = allPhotos,
            onApplyRange = { start, end -> viewModel.setDateRange(start, end) },
            onClearRange = { viewModel.clearDateRange() },
            onDismiss = { showDateRangeDialog = false },
            hapticsEnabled = hapticsEnabled
        )
    }

    // Dialog: Tag input when importing photos from gallery
    if (showTagImportDialog) {
        val allTags = (uiState as? MainUiState.Success)?.allTags ?: emptyList()
        TagInputDialog(
            title = "Tag ${pendingImportUris.size} Photo(s)",
            allExistingTags = allTags,
            onDismiss = {
                viewModel.importPhotos(pendingImportUris, "")
                pendingImportUris = emptyList()
                showTagImportDialog = false
            },
            onConfirm = { rawTags ->
                viewModel.importPhotos(pendingImportUris, rawTags)
                pendingImportUris = emptyList()
                showTagImportDialog = false
            }
        )
    }

    // Dialog: Tag input when importing photos from a folder
    if (showFolderTagDialog && pendingFolderUri != null) {
        val treeUri = pendingFolderUri!!
        val allTags = (uiState as? MainUiState.Success)?.allTags ?: emptyList()
        TagInputDialog(
            title = "Tag Folder Photos (Optional)",
            allExistingTags = allTags,
            onDismiss = {
                viewModel.importFolder(treeUri, "")
                pendingFolderUri = null
                showFolderTagDialog = false
            },
            onConfirm = { rawTags ->
                viewModel.importFolder(treeUri, rawTags)
                pendingFolderUri = null
                showFolderTagDialog = false
            }
        )
    }

    // Dialog: Tag input when importing photo from camera
    if (showCameraTagDialog && pendingCameraFileName != null) {
        val fileName = pendingCameraFileName!!
        val file = File(context.cacheDir, "camera/$fileName")
        val allTags = (uiState as? MainUiState.Success)?.allTags ?: emptyList()
        TagInputDialog(
            title = "Tag Photo",
            allExistingTags = allTags,
            onDismiss = {
                viewModel.importCameraPhoto(file, "")
                pendingCameraFileName = null
                showCameraTagDialog = false
            },
            onConfirm = { rawTags ->
                viewModel.importCameraPhoto(file, rawTags)
                pendingCameraFileName = null
                showCameraTagDialog = false
            }
        )
    }

    // Dialog: Bulk Add Tag
    if (showBulkAddTagDialog) {
        val allTags = (uiState as? MainUiState.Success)?.allTags ?: emptyList()
        TagInputDialog(
            title = "Add Tags to ${selectedPhotoIds.size} Photo(s)",
            allExistingTags = allTags,
            onDismiss = { showBulkAddTagDialog = false },
            onConfirm = { rawTags ->
                viewModel.addTagsToPhotos(selectedPhotoIds.toList(), rawTags)
                showBulkAddTagDialog = false
                viewModel.clearSelection()
            }
        )
    }

    // Dialog: Bulk Remove Tag
    if (showBulkRemoveTagDialog) {
        val selectedPhotos = (uiState as? MainUiState.Success)?.photos
            ?.filter { selectedPhotoIds.contains(it.photo.id) }
            ?: emptyList()
        val distinctTags = selectedPhotos.flatMap { it.tags }.distinctBy { it.id }
        RemoveTagFromSelectedDialog(
            distinctTags = distinctTags,
            onDismiss = { showBulkRemoveTagDialog = false },
            onTagSelected = { tagId ->
                viewModel.removeTagFromPhotos(selectedPhotoIds.toList(), tagId)
                showBulkRemoveTagDialog = false
                viewModel.clearSelection()
            }
        )
    }

    // Dialog: Confirm Delete
    if (showDeleteConfirmation) {
        ConfirmDeleteDialog(
            count = selectedPhotoIds.size,
            onDismiss = { showDeleteConfirmation = false },
            onConfirm = {
                viewModel.deletePhotos(selectedPhotoIds.toList())
                showDeleteConfirmation = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionTopBar(
    selectedCount: Int,
    onClearSelection: () -> Unit,
    onBulkAddTag: () -> Unit,
    onBulkRemoveTag: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    TopAppBar(
        title = { Text("$selectedCount selected") },
        navigationIcon = {
            IconButton(onClick = onClearSelection) {
                Icon(Icons.Default.Close, contentDescription = "Exit selection")
            }
        },
        actions = {
            IconButton(onClick = onBulkAddTag) {
                Icon(Icons.AutoMirrored.Filled.Label, contentDescription = "Add tags to selected")
            }
            IconButton(onClick = onBulkRemoveTag) {
                Icon(Icons.AutoMirrored.Filled.LabelOff, contentDescription = "Remove tag from selected")
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Default.Share, contentDescription = "Share selected")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete selected")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        windowInsets = WindowInsets(0, 0, 0, 0),
        modifier = Modifier.statusBarsPadding()
    )
}

@Composable
private fun GlassActionPillButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    isActive: Boolean = false,
    hapticsEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(13.dp)
    val activeBg = primaryColor.copy(alpha = 0.18f)
    val defaultBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f)
    val activeBorder = primaryColor.copy(alpha = 0.45f)
    val defaultBorder = Color.White.copy(alpha = 0.16f)

    Box(
        modifier = modifier
            .size(42.dp)
            .clip(shape)
            .background(if (isActive) activeBg else defaultBg)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        if (isActive) activeBorder else defaultBorder,
                        Color.Transparent
                    )
                ),
                shape = shape
            )
            .bouncyClickable(
                scaleDownFactor = 0.88f,
                hapticsEnabled = hapticsEnabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isActive) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTopBar(
    viewModel: PhotoViewModel,
    onNavigateToTagManager: () -> Unit,
    onSelectFolderClick: () -> Unit,
    onOpenDateRange: () -> Unit
) {
    val query by viewModel.queryFlow.collectAsState()
    val sortOrderName by viewModel.sortOrderFlow.collectAsState()
    val hapticsEnabled by viewModel.hapticsEnabled.collectAsState()
    val startDate by viewModel.startDateFlow.collectAsState()
    val endDate by viewModel.endDateFlow.collectAsState()
    val isDateFiltered = startDate != null || endDate != null
    var showSortMenu by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    val currentSort = try { SortOrder.valueOf(sortOrderName) } catch (_: Exception) { SortOrder.NEWEST }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Liquid Frosted Glass Capsule Search Widget
            val searchShape = RoundedCornerShape(21.dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(searchShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f))
                    .glassBorder(
                        shape = searchShape,
                        strokeWidth = 1.dp,
                        highlightColor = Color.White.copy(alpha = 0.18f),
                        shadowColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                    )
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_sf_search),
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = query,
                        onValueChange = { viewModel.onQueryChanged(it) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (query.isEmpty()) {
                                    Text(
                                        text = "Search photos, tags...",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.60f),
                                            fontSize = 14.sp
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                    if (query.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f))
                                .bouncyClickable(
                                    scaleDownFactor = 0.85f,
                                    hapticsEnabled = hapticsEnabled,
                                    onClick = { viewModel.onQueryChanged("") }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            // Date Range Filter Button
            GlassActionPillButton(
                icon = Icons.Default.DateRange,
                contentDescription = "Filter photos by date range",
                isActive = isDateFiltered,
                hapticsEnabled = hapticsEnabled,
                onClick = onOpenDateRange
            )

            // Sort Menu Button
            Box {
                GlassActionPillButton(
                    icon = Icons.AutoMirrored.Filled.Sort,
                    contentDescription = "Sort options",
                    isActive = showSortMenu || currentSort != SortOrder.NEWEST,
                    hapticsEnabled = hapticsEnabled,
                    onClick = { showSortMenu = true }
                )
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Date Added (Newest)") },
                        trailingIcon = { if (currentSort == SortOrder.NEWEST) Icon(Icons.Default.Check, null) },
                        onClick = {
                            viewModel.onSortOrderSelected(SortOrder.NEWEST)
                            showSortMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Date Added (Oldest)") },
                        trailingIcon = { if (currentSort == SortOrder.OLDEST) Icon(Icons.Default.Check, null) },
                        onClick = {
                            viewModel.onSortOrderSelected(SortOrder.OLDEST)
                            showSortMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Date Taken (Newest)") },
                        trailingIcon = { if (currentSort == SortOrder.DATE_TAKEN_NEWEST) Icon(Icons.Default.Check, null) },
                        onClick = {
                            viewModel.onSortOrderSelected(SortOrder.DATE_TAKEN_NEWEST)
                            showSortMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Date Taken (Oldest)") },
                        trailingIcon = { if (currentSort == SortOrder.DATE_TAKEN_OLDEST) Icon(Icons.Default.Check, null) },
                        onClick = {
                            viewModel.onSortOrderSelected(SortOrder.DATE_TAKEN_OLDEST)
                            showSortMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Tag name (A-Z)") },
                        trailingIcon = { if (currentSort == SortOrder.TAG_NAME) Icon(Icons.Default.Check, null) },
                        onClick = {
                            viewModel.onSortOrderSelected(SortOrder.TAG_NAME)
                            showSortMenu = false
                        }
                    )
                }
            }

            // Quick View Switcher Button
            val viewingStyleStr by viewModel.viewingStyleFlow.collectAsState()
            val viewingStyle = remember(viewingStyleStr) {
                try { ViewingStyle.valueOf(viewingStyleStr) } catch (_: Exception) { ViewingStyle.GRID }
            }
            val viewIcon = when (viewingStyle) {
                ViewingStyle.GRID -> Icons.Default.GridView
                ViewingStyle.LIST_DETAILS -> Icons.AutoMirrored.Filled.ViewList
                ViewingStyle.MASONRY -> Icons.Default.Dashboard
            }
            val viewLabel = when (viewingStyle) {
                ViewingStyle.GRID -> "Grid View"
                ViewingStyle.LIST_DETAILS -> "Windows Details List"
                ViewingStyle.MASONRY -> "Masonry View"
            }
            GlassActionPillButton(
                icon = viewIcon,
                contentDescription = "Switch viewing style (current: $viewLabel)",
                isActive = viewingStyle != ViewingStyle.GRID,
                hapticsEnabled = hapticsEnabled,
                onClick = { viewModel.cycleViewingStyle() }
            )

            // Overflow Menu Button
            Box {
                GlassActionPillButton(
                    icon = Icons.Default.MoreVert,
                    contentDescription = "More options",
                    isActive = showOverflowMenu,
                    hapticsEnabled = hapticsEnabled,
                    onClick = { showOverflowMenu = true }
                )
                DropdownMenu(
                    expanded = showOverflowMenu,
                    onDismissRequest = { showOverflowMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Select Folder") },
                        leadingIcon = { Icon(Icons.Default.CreateNewFolder, null) },
                        onClick = {
                            showOverflowMenu = false
                            onSelectFolderClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Tag Manager") },
                        onClick = {
                            showOverflowMenu = false
                            onNavigateToTagManager()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TagChipsRow(
    viewModel: PhotoViewModel,
    state: MainUiState.Success,
    startDate: Long?,
    endDate: Long?,
    onOpenDateRange: () -> Unit
) {
    val selectedIds = state.filterState.selectedTagIds
    val untaggedOnly = state.filterState.untaggedOnly
    val isAllSelected by remember(selectedIds, untaggedOnly, startDate, endDate) {
        derivedStateOf { selectedIds.isEmpty() && !untaggedOnly && startDate == null && endDate == null }
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Active Date Range Filter Chip
        if (startDate != null || endDate != null) {
            item(key = "chip_date_range_active") {
                val startStr = startDate?.let { SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(it)) } ?: "Earliest"
                val endStr = endDate?.let { SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(it)) } ?: "Latest"
                val rangeLabel = if (startDate != null && endDate != null) "$startStr – $endStr" else if (startDate != null) "From $startStr" else "Up to $endStr"

                FilterChip(
                    selected = true,
                    onClick = onOpenDateRange,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = { viewModel.clearDateRange() },
                            modifier = Modifier.size(18.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear date filter",
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    },
                    label = { Text("📅 $rangeLabel", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
                        selectedTrailingIconColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        // "All" Chip
        item(key = "chip_all") {
            FilterChip(
                selected = isAllSelected,
                onClick = {
                    viewModel.onClearFilters()
                    viewModel.clearDateRange()
                },
                label = { Text("All (${state.totalLibraryCount})") },
                colors = FilterChipDefaults.filterChipColors()
            )
        }

        // "Untagged" Chip
        item(key = "chip_untagged") {
            FilterChip(
                selected = untaggedOnly,
                onClick = { viewModel.onUntaggedToggled() },
                label = { Text("Untagged (${state.untaggedCount})") }
            )
        }

        // Tag Chips
        items(
            items = state.allTags,
            key = { it.id }
        ) { tag ->
            val isSelected = selectedIds.contains(tag.id)
            FilterChip(
                selected = isSelected,
                onClick = { viewModel.onTagChipToggled(tag.id) },
                label = { Text("#${tag.name} (${tag.photoCount})") }
            )
        }

        // Multi-Tag Match Mode Toggle Chip (ANY vs ALL)
        if (selectedIds.size > 1) {
            item(key = "chip_match_mode") {
                val isAllMode = state.filterState.matchMode == TagMatchMode.ALL
                FilterChip(
                    selected = true,
                    onClick = { viewModel.toggleTagMatchMode() },
                    label = {
                        Text(if (isAllMode) "Match: ALL (AND)" else "Match: ANY (OR)")
                    }
                )
            }
        }
    }
}

@Composable
private fun MainFab(
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onFolderClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        AnimatedVisibility(visible = expanded) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        expanded = false
                        onFolderClick()
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = "Select Folder")
                }
                FloatingActionButton(
                    onClick = {
                        expanded = false
                        onCameraClick()
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Take Photo")
                }
                FloatingActionButton(
                    onClick = {
                        expanded = false
                        onGalleryClick()
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = "Pick from Gallery")
                }
            }
        }

        FloatingActionButton(
            onClick = { expanded = !expanded }
        ) {
            Icon(
                imageVector = if (expanded) Icons.Default.Close else Icons.Default.Add,
                contentDescription = if (expanded) "Close add menu" else "Add photo"
            )
        }
    }
}

@Composable
private fun EmptyLibraryView() {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("No photos saved yet", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Tap + below to take a photo or pick from gallery", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun FolderGridView(
    folders: List<FolderItem>,
    onFolderClick: (FolderItem) -> Unit,
    onSelectNewFolder: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Folders (${folders.size})",
                style = MaterialTheme.typography.titleMedium
            )
            Button(
                onClick = onSelectNewFolder,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CreateNewFolder,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Select Folder")
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 140.dp),
            contentPadding = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 96.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = folders,
                key = { it.name }
            ) { folder ->
                FolderCard(
                    folder = folder,
                    onClick = { onFolderClick(folder) }
                )
            }
        }
    }
}

@Composable
private fun FolderCard(
    folder: FolderItem,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val coverFile = remember(folder.coverThumbPath) {
        folder.coverThumbPath?.let { File(context.filesDir, it) }
    }

    Card(
        onClick = onClick,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (coverFile != null && coverFile.exists()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(coverFile)
                            .crossfade(false)
                            .build(),
                        contentDescription = folder.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${folder.photoCount} photo${if (folder.photoCount == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyFolderView(onSelectFolder: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text("No folders selected yet", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Select a folder to view and tag photos inside it", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onSelectFolder) {
                Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Select Folder")
            }
        }
    }
}

@Composable
private fun EmptyFilterView(onClearFilters: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("No photos match your filters", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onClearFilters) {
                Text("Clear filters")
            }
        }
    }
}

private fun sharePhotos(context: android.content.Context, relativePaths: List<String>) {
    if (relativePaths.isEmpty()) return
    val authority = "${context.packageName}.fileprovider"
    val uris = ArrayList<Uri>()
    for (rel in relativePaths) {
        val file = File(context.filesDir, rel)
        if (file.exists()) {
            uris.add(FileProvider.getUriForFile(context, authority, file))
        }
    }
    if (uris.isEmpty()) return

    val intent = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            clipData = android.content.ClipData.newUri(context.contentResolver, "Photo", uris[0])
            putExtra(Intent.EXTRA_STREAM, uris[0])
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    } else {
        val clipData = android.content.ClipData.newUri(context.contentResolver, "Photo", uris[0])
        for (i in 1 until uris.size) {
            clipData.addItem(android.content.ClipData.Item(uris[i]))
        }
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/*"
            this.clipData = clipData
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    context.startActivity(Intent.createChooser(intent, "Share Photo(s)"))
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun PhotoCollectionView(
    photos: List<com.tagshot.app.data.PhotoWithTags>,
    viewingStyle: ViewingStyle,
    gridColumns: Int,
    tagDisplaySize: TagDisplaySize,
    tagShape: TagShape = TagShape.SQUIRCLE,
    tagPosition: TagPosition = TagPosition.BOTTOM_LEFT,
    tagPreset: TagPalettePreset = TagPalettePreset.DYNAMIC_AUTO,
    tagAnimation: TagAnimation = TagAnimation.NONE,
    scrubberStyle: ScrubberStyle = ScrubberStyle.CUPERTINO_CAPSULE,
    scrubberWidth: ScrubberWidth = ScrubberWidth.STANDARD,
    scrubberBubbleStyle: ScrubberBubbleStyle = ScrubberBubbleStyle.FROSTED_GLASS_LENS,
    showListFileSize: Boolean,
    showListDate: Boolean,
    showListFolder: Boolean,
    selectedPhotoIds: Set<Long>,
    isInSelectionMode: Boolean,
    onPhotoClick: (Long) -> Unit,
    onPhotoLongClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onColumnsChange: ((Int) -> Unit)? = null
) {
    val pinchModifier = if (onColumnsChange != null) {
        Modifier.pinchZoomGrid(
            currentColumns = gridColumns,
            onColumnsChange = onColumnsChange
        )
    } else {
        Modifier
    }

    Crossfade(
        targetState = viewingStyle,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "viewingStyleTransition",
        modifier = modifier.then(pinchModifier)
    ) { currentViewingStyle ->
        when (currentViewingStyle) {
            ViewingStyle.GRID -> {
                val gridState = rememberLazyGridState()
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(gridColumns),
                        contentPadding = PaddingValues(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 96.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = photos,
                            key = { it.photo.id },
                            contentType = { "photo_grid" }
                        ) { item ->
                            PhotoCell(
                                photoWithTags = item,
                                isSelected = selectedPhotoIds.contains(item.photo.id),
                                isInSelectionMode = isInSelectionMode,
                                tagDisplaySize = tagDisplaySize,
                                tagShape = tagShape,
                                tagPosition = tagPosition,
                                tagPreset = tagPreset,
                                tagAnimation = tagAnimation,
                                aspectRatio = 1f,
                                onClick = { onPhotoClick(item.photo.id) },
                                onLongClick = { onPhotoLongClick(item.photo.id) },
                                modifier = Modifier.animateItemPlacement(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                            )
                        }
                    }

                    // Floating sticky timeline header pill
                    StickyTimelineHeaderOverlay(
                        gridState = gridState,
                        photos = photos,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )

                    // Interactive fast-scroll scrubber with Month/Year magnifier bubble
                    FastScrollScrubber(
                        gridState = gridState,
                        photos = photos,
                        scrubberStyle = scrubberStyle,
                        scrubberWidth = scrubberWidth,
                        scrubberBubbleStyle = scrubberBubbleStyle,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }
            }
            ViewingStyle.LIST_DETAILS -> {
                val listState = rememberLazyListState()
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(start = 10.dp, top = 8.dp, end = 10.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = photos,
                            key = { it.photo.id },
                            contentType = { "photo_list" }
                        ) { item ->
                            WindowsDetailsRow(
                                photoWithTags = item,
                                isSelected = selectedPhotoIds.contains(item.photo.id),
                                isInSelectionMode = isInSelectionMode,
                                tagDisplaySize = tagDisplaySize,
                                showFileSize = showListFileSize,
                                showDate = showListDate,
                                showFolder = showListFolder,
                                onClick = { onPhotoClick(item.photo.id) },
                                onLongClick = { onPhotoLongClick(item.photo.id) }
                            )
                        }
                    }

                    // Floating sticky timeline header pill
                    StickyTimelineHeaderOverlay(
                        listState = listState,
                        photos = photos,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )

                    // Interactive fast-scroll scrubber with Month/Year magnifier bubble
                    FastScrollScrubber(
                        listState = listState,
                        photos = photos,
                        scrubberStyle = scrubberStyle,
                        scrubberWidth = scrubberWidth,
                        scrubberBubbleStyle = scrubberBubbleStyle,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }
            }
            ViewingStyle.MASONRY -> {
                val staggeredGridState = rememberLazyStaggeredGridState()
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyVerticalStaggeredGrid(
                        state = staggeredGridState,
                        columns = StaggeredGridCells.Fixed(gridColumns),
                        contentPadding = PaddingValues(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 96.dp),
                        verticalItemSpacing = 6.dp,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = photos,
                            key = { it.photo.id },
                            contentType = { "photo_masonry" }
                        ) { item ->
                            val ratio = remember(item.photo.width, item.photo.height) {
                                if (item.photo.width > 0 && item.photo.height > 0) {
                                    (item.photo.width.toFloat() / item.photo.height.toFloat()).coerceIn(0.65f, 1.5f)
                                } else {
                                    1f
                                }
                            }
                            PhotoCell(
                                photoWithTags = item,
                                isSelected = selectedPhotoIds.contains(item.photo.id),
                                isInSelectionMode = isInSelectionMode,
                                tagDisplaySize = tagDisplaySize,
                                tagShape = tagShape,
                                tagPosition = tagPosition,
                                tagPreset = tagPreset,
                                tagAnimation = tagAnimation,
                                aspectRatio = ratio,
                                onClick = { onPhotoClick(item.photo.id) },
                                onLongClick = { onPhotoLongClick(item.photo.id) }
                            )
                        }
                    }

                    // Floating sticky timeline header pill
                    StickyTimelineHeaderOverlay(
                        staggeredGridState = staggeredGridState,
                        photos = photos,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )

                    // Interactive fast-scroll scrubber with Month/Year magnifier bubble
                    FastScrollScrubber(
                        staggeredGridState = staggeredGridState,
                        photos = photos,
                        scrubberStyle = scrubberStyle,
                        scrubberWidth = scrubberWidth,
                        scrubberBubbleStyle = scrubberBubbleStyle,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }
            }
        }
    }
}
