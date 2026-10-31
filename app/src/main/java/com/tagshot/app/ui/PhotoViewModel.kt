package com.tagshot.app.ui

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tagshot.app.data.BatchImportSummary
import com.tagshot.app.data.DeletedPhotoBackup
import com.tagshot.app.data.PhotoRepository
import com.tagshot.app.data.PhotoWithTags
import com.tagshot.app.data.SingleImportResult
import com.tagshot.app.data.TagWithCount
import com.tagshot.app.domain.FilterPhoto
import com.tagshot.app.domain.FilterState
import com.tagshot.app.domain.Limits
import com.tagshot.app.domain.ParseResult
import com.tagshot.app.domain.PhotoFilter
import com.tagshot.app.domain.PhotoSorter
import com.tagshot.app.domain.SortOrder
import com.tagshot.app.domain.SortablePhoto
import com.tagshot.app.domain.TagParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

enum class MainTab {
    ALL_PHOTOS,
    FOLDER_WISE,
    SETTINGS
}

data class FolderItem(
    val name: String,
    val photoCount: Int,
    val coverThumbPath: String? = null
)

sealed interface MainUiState {
    data object Loading : MainUiState
    data class Success(
        val photos: List<PhotoWithTags>,
        val allTags: List<TagWithCount>,
        val allFolders: List<FolderItem> = emptyList(),
        val activeTab: MainTab = MainTab.ALL_PHOTOS,
        val selectedFolder: String = "",
        val totalLibraryCount: Int,
        val untaggedCount: Int,
        val filterState: FilterState,
        val sortOrder: SortOrder,
        val isImporting: Boolean = false,
        val importProgress: BatchImportSummary? = null
    ) : MainUiState
}

sealed interface UiEvent {
    data class ShowMessage(val message: String) : UiEvent
    data class ImportFinished(val summary: BatchImportSummary) : UiEvent
    data class ShowUndoSnackbar(val count: Int) : UiEvent
}

private data class FilterOptions(
    val query: String,
    val selectedTagIds: List<Long>,
    val untaggedOnly: Boolean,
    val sortOrderName: String,
    val activeTabName: String,
    val selectedFolder: String
)

private data class ImportState(
    val isImporting: Boolean,
    val progress: BatchImportSummary?
)

class PhotoViewModel(
    private val repository: PhotoRepository,
    private val savedStateHandle: SavedStateHandle,
    private val defaultDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {


    companion object {
        private const val KEY_QUERY = "key_query"
        private const val KEY_SELECTED_TAG_IDS = "key_selected_tag_ids"
        private const val KEY_UNTAGGED_ONLY = "key_untagged_only"
        private const val KEY_SORT_ORDER = "key_sort_order"
        private const val KEY_ACTIVE_TAB = "key_active_tab"
        private const val KEY_SELECTED_FOLDER = "key_selected_folder"
        private const val KEY_GRID_COLUMNS = "key_grid_columns"
        private const val KEY_HAPTICS_ENABLED = "key_haptics_enabled"
        private const val KEY_THEME_MODE = "key_theme_mode"
    }

    // Persisted state via SavedStateHandle
    val queryFlow = savedStateHandle.getStateFlow(KEY_QUERY, "")
    val selectedTagIdsFlow = savedStateHandle.getStateFlow<List<Long>>(KEY_SELECTED_TAG_IDS, emptyList())
    val untaggedOnlyFlow = savedStateHandle.getStateFlow(KEY_UNTAGGED_ONLY, false)
    val sortOrderFlow = savedStateHandle.getStateFlow(KEY_SORT_ORDER, SortOrder.NEWEST.name)
    val activeTabFlow = savedStateHandle.getStateFlow(KEY_ACTIVE_TAB, MainTab.ALL_PHOTOS.name)
    val selectedFolderFlow = savedStateHandle.getStateFlow(KEY_SELECTED_FOLDER, "")
    val gridColumns: StateFlow<Int> = savedStateHandle.getStateFlow(KEY_GRID_COLUMNS, 2)
    val hapticsEnabled: StateFlow<Boolean> = savedStateHandle.getStateFlow(KEY_HAPTICS_ENABLED, true)
    val themeModeFlow: StateFlow<String> = savedStateHandle.getStateFlow(KEY_THEME_MODE, "SYSTEM")

    fun setGridColumns(cols: Int) {
        savedStateHandle[KEY_GRID_COLUMNS] = cols.coerceIn(1, 4)
    }

    fun setHapticsEnabled(enabled: Boolean) {
        savedStateHandle[KEY_HAPTICS_ENABLED] = enabled
    }

    fun setThemeMode(mode: String) {
        savedStateHandle[KEY_THEME_MODE] = mode
    }

    // Selection mode state
    private val _selectedPhotoIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedPhotoIds: StateFlow<Set<Long>> = _selectedPhotoIds.asStateFlow()

    // Import progress state
    private val _isImporting = MutableStateFlow(false)
    private val _importProgress = MutableStateFlow<BatchImportSummary?>(null)

    // One-shot events channel
    private val _eventChannel = Channel<UiEvent>(Channel.BUFFERED)
    val events: Flow<UiEvent> = _eventChannel.receiveAsFlow()

    fun showMessage(message: String) {
        viewModelScope.launch {
            _eventChannel.send(UiEvent.ShowMessage(message))
        }
    }

    // Undo delete state
    private var pendingDeleteBackups: List<DeletedPhotoBackup>? = null
    private var pendingDeleteJob: Job? = null

    private val filterOptionsFlow: Flow<FilterOptions> = combine(
        queryFlow,
        selectedTagIdsFlow,
        untaggedOnlyFlow,
        sortOrderFlow,
        activeTabFlow
    ) { query, selectedTagIds, untaggedOnly, sortOrderName, activeTabName ->
        FilterOptions(query, selectedTagIds, untaggedOnly, sortOrderName, activeTabName, selectedFolderFlow.value)
    }.combine(selectedFolderFlow) { opts, folder ->
        opts.copy(selectedFolder = folder)
    }

    private val importStateFlow: Flow<ImportState> = combine(
        _isImporting,
        _importProgress
    ) { isImp, prog ->
        ImportState(isImp, prog)
    }

    // Combined UI State
    val uiState: StateFlow<MainUiState> = combine(
        repository.observePhotos(),
        repository.observeTags(),
        filterOptionsFlow,
        importStateFlow
    ) { photos, tags, filterOpts, impState ->
        val existingTagIds = tags.map { it.id }.toSet()
        // Automatic removal of vanished tag IDs per Section 8.1
        val validSelectedTagIds = filterOpts.selectedTagIds.filter { existingTagIds.contains(it) }.toSet()
        if (validSelectedTagIds.size != filterOpts.selectedTagIds.size) {
            viewModelScope.launch(Dispatchers.Main.immediate) {
                savedStateHandle[KEY_SELECTED_TAG_IDS] = validSelectedTagIds.toList()
            }
        }

        val activeTab = try {
            MainTab.valueOf(filterOpts.activeTabName)
        } catch (_: Exception) {
            MainTab.ALL_PHOTOS
        }

        val allFolders = photos
            .filter { it.photo.folderName.isNotBlank() }
            .groupBy { it.photo.folderName }
            .map { (folderName, photoList) ->
                val latestPhoto = photoList.maxByOrNull { it.photo.addedAt }
                FolderItem(
                    name = folderName,
                    photoCount = photoList.size,
                    coverThumbPath = latestPhoto?.photo?.thumbPath
                )
            }
            .sortedBy { it.name }

        val filterState = FilterState(
            query = filterOpts.query,
            selectedTagIds = validSelectedTagIds,
            untaggedOnly = filterOpts.untaggedOnly
        )

        val sortOrder = try {
            SortOrder.valueOf(filterOpts.sortOrderName)
        } catch (_: Exception) {
            SortOrder.NEWEST
        }

        val totalLibraryCount = photos.size
        val untaggedCount = photos.count { it.tags.isEmpty() }

        // Filter by folder if on FOLDER_WISE tab
        val basePhotos = if (activeTab == MainTab.FOLDER_WISE) {
            if (filterOpts.selectedFolder.isNotBlank()) {
                photos.filter { it.photo.folderName == filterOpts.selectedFolder }
            } else {
                photos.filter { it.photo.folderName.isNotBlank() }
            }
        } else {
            photos
        }

        // 1. Filter using domain PhotoFilter
        val filtered = PhotoFilter.filter(basePhotos, filterState) { p ->
            FilterPhoto(
                id = p.photo.id,
                normalizedTags = p.tags.map { it.normalizedName }.toSet(),
                tagIds = p.tags.map { it.id }.toSet()
            )
        }

        // 2. Sort using domain PhotoSorter
        val sorted = PhotoSorter.sort(filtered, sortOrder) { p ->
            SortablePhoto(
                id = p.photo.id,
                addedAt = p.photo.addedAt,
                normalizedTags = p.tags.map { it.normalizedName }.toSet()
            )
        }

        MainUiState.Success(
            photos = sorted,
            allTags = tags,
            allFolders = allFolders,
            activeTab = activeTab,
            selectedFolder = filterOpts.selectedFolder,
            totalLibraryCount = totalLibraryCount,
            untaggedCount = untaggedCount,
            filterState = filterState,
            sortOrder = sortOrder,
            isImporting = impState.isImporting,
            importProgress = impState.progress
        )
    }.flowOn(defaultDispatcher).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState.Loading
    )


    // --- Search & Filter Actions ---

    fun onTabSelected(tab: MainTab) {
        savedStateHandle[KEY_ACTIVE_TAB] = tab.name
    }

    fun onFolderSelected(folderName: String) {
        savedStateHandle[KEY_SELECTED_FOLDER] = folderName
    }

    fun onQueryChanged(newQuery: String) {
        savedStateHandle[KEY_QUERY] = newQuery
    }

    fun onTagChipToggled(tagId: Long) {
        val current = selectedTagIdsFlow.value.toMutableSet()
        if (current.contains(tagId)) {
            current.remove(tagId)
        } else {
            current.add(tagId)
            savedStateHandle[KEY_UNTAGGED_ONLY] = false
        }
        savedStateHandle[KEY_SELECTED_TAG_IDS] = current.toList()
    }

    fun onUntaggedToggled() {
        val currentUntagged = untaggedOnlyFlow.value
        val newUntagged = !currentUntagged
        savedStateHandle[KEY_UNTAGGED_ONLY] = newUntagged
        if (newUntagged) {
            savedStateHandle[KEY_SELECTED_TAG_IDS] = emptyList<Long>()
        }
    }

    fun onClearFilters() {
        savedStateHandle[KEY_QUERY] = ""
        savedStateHandle[KEY_SELECTED_TAG_IDS] = emptyList<Long>()
        savedStateHandle[KEY_UNTAGGED_ONLY] = false
    }

    fun onSortOrderSelected(order: SortOrder) {
        savedStateHandle[KEY_SORT_ORDER] = order.name
    }

    // --- Selection Mode Actions ---

    fun enterSelection(initialPhotoId: Long) {
        _selectedPhotoIds.value = setOf(initialPhotoId)
    }

    fun toggleSelection(photoId: Long) {
        val current = _selectedPhotoIds.value.toMutableSet()
        if (current.contains(photoId)) {
            current.remove(photoId)
        } else {
            current.add(photoId)
        }
        _selectedPhotoIds.value = current
    }

    fun selectAll(allPhotoIds: List<Long>) {
        _selectedPhotoIds.value = allPhotoIds.toSet()
    }

    fun clearSelection() {
        _selectedPhotoIds.value = emptySet()
    }

    // --- Import Actions ---

    fun importPhotos(uris: List<Uri>, rawTags: String) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val tags = when (val parseResult = TagParser.parse(rawTags)) {
                is ParseResult.Success -> parseResult.tags
                is ParseResult.Failure.TooLong -> {
                    _eventChannel.send(UiEvent.ShowMessage("Tag too long: ${parseResult.rawText}"))
                    return@launch
                }
                is ParseResult.Failure.TooMany -> {
                    _eventChannel.send(UiEvent.ShowMessage("Too many tags (max ${Limits.MAX_TAGS_PER_PHOTO})"))
                    return@launch
                }
            }

            _isImporting.value = true
            _importProgress.value = BatchImportSummary(0, uris.size, 0, 0, 0)

            val summary = repository.importUris(uris, tags) { progress ->
                _importProgress.value = progress
            }

            _isImporting.value = false
            _importProgress.value = null
            _eventChannel.send(UiEvent.ImportFinished(summary))
        }
    }

    fun importCameraPhoto(file: File, rawTags: String) {
        viewModelScope.launch {
            val parseResult = TagParser.parse(rawTags)
            val tags = if (parseResult is ParseResult.Success) parseResult.tags else emptyList()

            when (repository.importCameraFile(file, tags)) {
                is SingleImportResult.Success -> {
                    _eventChannel.send(UiEvent.ShowMessage("Photo saved"))
                }
                is SingleImportResult.Duplicate -> {
                    _eventChannel.send(UiEvent.ShowMessage("Duplicate photo skipped"))
                }
                is SingleImportResult.Failed -> {
                    _eventChannel.send(UiEvent.ShowMessage("Failed to save camera photo"))
                }
            }
        }
    }

    fun importFolder(treeUri: Uri, rawTags: String) {
        viewModelScope.launch {
            val tags = when (val parseResult = TagParser.parse(rawTags)) {
                is ParseResult.Success -> parseResult.tags
                is ParseResult.Failure.TooLong -> {
                    _eventChannel.send(UiEvent.ShowMessage("Tag too long: ${parseResult.rawText}"))
                    return@launch
                }
                is ParseResult.Failure.TooMany -> {
                    _eventChannel.send(UiEvent.ShowMessage("Too many tags (max ${Limits.MAX_TAGS_PER_PHOTO})"))
                    return@launch
                }
            }

            _isImporting.value = true
            _importProgress.value = BatchImportSummary(0, 0, 0, 0, 0)

            val (folderName, summary) = try {
                repository.importFolder(treeUri, tags) { progress ->
                    _importProgress.value = progress
                }
            } catch (e: Exception) {
                _isImporting.value = false
                _importProgress.value = null
                _eventChannel.send(UiEvent.ShowMessage("Failed to import folder: ${e.message}"))
                return@launch
            }

            _isImporting.value = false
            _importProgress.value = null

            // Automatically switch to Folder Wise View and focus on the imported folder
            onTabSelected(MainTab.FOLDER_WISE)
            onFolderSelected(folderName)

            _eventChannel.send(UiEvent.ImportFinished(summary))
        }
    }

    // --- Tag & Note Mutations ---

    fun addTagsToPhotos(photoIds: List<Long>, rawTags: String) {
        if (photoIds.isEmpty() || rawTags.isBlank()) return
        viewModelScope.launch {
            when (val parseResult = TagParser.parse(rawTags)) {
                is ParseResult.Success -> {
                    repository.addTagsToPhotos(photoIds, parseResult.tags)
                }
                is ParseResult.Failure.TooLong -> {
                    _eventChannel.send(UiEvent.ShowMessage("Tag too long: ${parseResult.rawText}"))
                }
                is ParseResult.Failure.TooMany -> {
                    _eventChannel.send(UiEvent.ShowMessage("Too many tags (max ${Limits.MAX_TAGS_PER_PHOTO})"))
                }
            }
        }
    }

    fun removeTagFromPhoto(photoId: Long, tagId: Long) {
        viewModelScope.launch {
            repository.removeTagFromPhoto(photoId, tagId)
        }
    }

    fun removeTagFromPhotos(photoIds: List<Long>, tagId: Long) {
        viewModelScope.launch {
            for (id in photoIds) {
                repository.removeTagFromPhoto(id, tagId)
            }
        }
    }

    fun updateNote(photoId: Long, note: String) {
        val trimmed = if (note.length > Limits.MAX_NOTE_CHARS) note.take(Limits.MAX_NOTE_CHARS) else note
        viewModelScope.launch {
            repository.updateNote(photoId, trimmed)
        }
    }

    fun renameTag(tagId: Long, newRaw: String) {
        viewModelScope.launch {
            val success = repository.renameOrMergeTag(tagId, newRaw)
            if (!success) {
                _eventChannel.send(UiEvent.ShowMessage("Invalid tag name"))
            }
        }
    }

    fun deleteTag(tagId: Long) {
        viewModelScope.launch {
            repository.deleteTagEverywhere(tagId)
        }
    }

    // --- Delete & Undo Actions ---

    fun deletePhotos(photoIds: List<Long>) {
        if (photoIds.isEmpty()) return
        clearSelection()
        viewModelScope.launch {
            pendingDeleteJob?.cancel()
            pendingDeleteBackups?.let { repository.finalizeDelete(it) }

            val backups = repository.prepareDelete(photoIds)
            pendingDeleteBackups = backups
            _eventChannel.send(UiEvent.ShowUndoSnackbar(photoIds.size))

            pendingDeleteJob = launch {
                delay(Limits.UNDO_WINDOW_MS)
                pendingDeleteBackups?.let {
                    repository.finalizeDelete(it)
                    pendingDeleteBackups = null
                }
            }
        }
    }

    fun undoDelete() {
        val backups = pendingDeleteBackups ?: return
        pendingDeleteJob?.cancel()
        pendingDeleteBackups = null
        viewModelScope.launch {
            repository.restorePhotos(backups)
            _eventChannel.send(UiEvent.ShowMessage("Delete undone"))
        }
    }
}
