package com.tagshot.app.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.LabelOff
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import com.tagshot.app.domain.SortOrder
import com.tagshot.app.ui.components.PhotoCell
import java.io.File
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
    val snackbarHostState = remember { SnackbarHostState() }

    val isInSelectionMode = selectedPhotoIds.isNotEmpty()
    val isInsideFolder = (uiState as? MainUiState.Success)?.let {
        it.activeTab == MainTab.FOLDER_WISE && it.selectedFolder.isNotEmpty()
    } == true

    // Back button exits selection mode first per Section 9.1; if inside folder, returns to folders list
    BackHandler(enabled = isInSelectionMode || isInsideFolder) {
        if (isInSelectionMode) {
            viewModel.clearSelection()
        } else if (isInsideFolder) {
            viewModel.onFolderSelected("")
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
        topBar = {
            if (isInSelectionMode) {
                TopAppBar(
                    title = { Text("${selectedPhotoIds.size} selected") },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "Exit selection")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showBulkAddTagDialog = true }) {
                            Icon(Icons.AutoMirrored.Filled.Label, contentDescription = "Add tags to selected")
                        }
                        IconButton(onClick = { showBulkRemoveTagDialog = true }) {
                            Icon(Icons.AutoMirrored.Filled.LabelOff, contentDescription = "Remove tag from selected")
                        }
                        IconButton(onClick = {
                            val photos = (uiState as? MainUiState.Success)?.photos
                                ?.filter { selectedPhotoIds.contains(it.photo.id) }
                                ?: emptyList()
                            sharePhotos(context, photos.map { it.photo.originalPath })
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share selected")
                        }
                        IconButton(onClick = { showDeleteConfirmation = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete selected")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            } else {
                MainTopBar(
                    viewModel = viewModel,
                    onNavigateToTagManager = onNavigateToTagManager,
                    onSelectFolderClick = { folderPickerLauncher.launch(null) }
                )
            }
        },
        floatingActionButton = {
            if (!isInSelectionMode) {
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
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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

                    // Navigation Tabs: All Photos vs Folder Wise View
                    TabRow(
                        selectedTabIndex = if (state.activeTab == MainTab.ALL_PHOTOS) 0 else 1,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Tab(
                            selected = state.activeTab == MainTab.ALL_PHOTOS,
                            onClick = { viewModel.onTabSelected(MainTab.ALL_PHOTOS) },
                            text = { Text("All Photos") }
                        )
                        Tab(
                            selected = state.activeTab == MainTab.FOLDER_WISE,
                            onClick = { viewModel.onTabSelected(MainTab.FOLDER_WISE) },
                            text = { Text("Folder Wise View") }
                        )
                    }

                    if (state.activeTab == MainTab.FOLDER_WISE) {
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
                                state = state
                            )

                            if (state.photos.isEmpty()) {
                                EmptyFilterView(onClearFilters = { viewModel.onClearFilters() })
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Adaptive(minSize = 110.dp),
                                    contentPadding = PaddingValues(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(
                                        items = state.photos,
                                        key = { it.photo.id },
                                        contentType = { "photo" }
                                    ) { item ->
                                        PhotoCell(
                                            photoWithTags = item,
                                            isSelected = selectedPhotoIds.contains(item.photo.id),
                                            isInSelectionMode = isInSelectionMode,
                                            onClick = {
                                                if (isInSelectionMode) {
                                                    viewModel.toggleSelection(item.photo.id)
                                                } else {
                                                    onNavigateToDetail(item.photo.id)
                                                }
                                            },
                                            onLongClick = {
                                                if (!isInSelectionMode) {
                                                    viewModel.enterSelection(item.photo.id)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // ALL_PHOTOS Tab
                        TagChipsRow(
                            viewModel = viewModel,
                            state = state
                        )

                        if (state.totalLibraryCount == 0) {
                            EmptyLibraryView()
                        } else if (state.photos.isEmpty()) {
                            EmptyFilterView(onClearFilters = { viewModel.onClearFilters() })
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 110.dp),
                                contentPadding = PaddingValues(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(
                                    items = state.photos,
                                    key = { it.photo.id },
                                    contentType = { "photo" }
                                ) { item ->
                                    PhotoCell(
                                        photoWithTags = item,
                                        isSelected = selectedPhotoIds.contains(item.photo.id),
                                        isInSelectionMode = isInSelectionMode,
                                        onClick = {
                                            if (isInSelectionMode) {
                                                viewModel.toggleSelection(item.photo.id)
                                            } else {
                                                onNavigateToDetail(item.photo.id)
                                            }
                                        },
                                        onLongClick = {
                                            if (!isInSelectionMode) {
                                                viewModel.enterSelection(item.photo.id)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
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
private fun MainTopBar(
    viewModel: PhotoViewModel,
    onNavigateToTagManager: () -> Unit,
    onSelectFolderClick: () -> Unit
) {
    val query by viewModel.queryFlow.collectAsState()
    val sortOrderName by viewModel.sortOrderFlow.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    val currentSort = try { SortOrder.valueOf(sortOrderName) } catch (_: Exception) { SortOrder.NEWEST }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.onQueryChanged(it) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                placeholder = { Text("Search by tag...") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )

            // Sort Menu Button
            Box {
                IconButton(onClick = { showSortMenu = true }) {
                    Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort options")
                }
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Newest first") },
                        trailingIcon = { if (currentSort == SortOrder.NEWEST) Icon(Icons.Default.Check, null) },
                        onClick = {
                            viewModel.onSortOrderSelected(SortOrder.NEWEST)
                            showSortMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Oldest first") },
                        trailingIcon = { if (currentSort == SortOrder.OLDEST) Icon(Icons.Default.Check, null) },
                        onClick = {
                            viewModel.onSortOrderSelected(SortOrder.OLDEST)
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

            // Overflow Menu
            Box {
                IconButton(onClick = { showOverflowMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                }
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
    state: MainUiState.Success
) {
    val selectedIds = state.filterState.selectedTagIds
    val untaggedOnly = state.filterState.untaggedOnly
    val isAllSelected = selectedIds.isEmpty() && !untaggedOnly

    LazyRow(
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // "All" Chip
        item(key = "chip_all") {
            FilterChip(
                selected = isAllSelected,
                onClick = { viewModel.onClearFilters() },
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
    }
}

@Composable
private fun MainFab(
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onFolderClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp)
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
            contentPadding = PaddingValues(12.dp),
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
