package com.tagshot.app.ui

import android.content.Context
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
import com.tagshot.app.domain.DuplicateCluster
import com.tagshot.app.domain.DuplicateFinder
import com.tagshot.app.domain.FilterPhoto
import com.tagshot.app.domain.FilterState
import com.tagshot.app.domain.Limits
import com.tagshot.app.domain.ParseResult
import com.tagshot.app.domain.PhotoFilter
import com.tagshot.app.domain.PhotoSorter
import com.tagshot.app.domain.SortOrder
import com.tagshot.app.domain.SortablePhoto
import com.tagshot.app.domain.TagMatchMode
import com.tagshot.app.domain.TagParser
import com.tagshot.app.ui.theme.ScrubberBubbleStyle
import com.tagshot.app.ui.theme.ScrubberStyle
import com.tagshot.app.ui.theme.ScrubberWidth
import com.tagshot.app.ui.theme.TagAnimation
import com.tagshot.app.ui.theme.TagPalettePreset
import com.tagshot.app.ui.theme.TagPosition
import com.tagshot.app.ui.theme.TagShape
import com.tagshot.app.ui.theme.UiDesignSystem
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import com.tagshot.app.ui.components.TagDisplaySize
import com.tagshot.app.ui.theme.AccentPalette

enum class ViewingStyle {
    GRID,
    LIST_DETAILS,
    MASONRY
}

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
    val matchModeName: String,
    val sortOrderName: String,
    val activeTabName: String,
    val selectedFolder: String,
    val startDate: Long? = null,
    val endDate: Long? = null
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
        private const val KEY_TAG_MATCH_MODE = "key_tag_match_mode"
        private const val KEY_SORT_ORDER = "key_sort_order"
        private const val KEY_ACTIVE_TAB = "key_active_tab"
        private const val KEY_SELECTED_FOLDER = "key_selected_folder"
        private const val KEY_GRID_COLUMNS = "key_grid_columns"
        private const val KEY_HAPTICS_ENABLED = "key_haptics_enabled"
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_ACCENT_PALETTE = "key_accent_palette"
        private const val KEY_VIEWING_STYLE = "key_viewing_style"
        private const val KEY_TAG_DISPLAY_SIZE = "key_tag_display_size"
        private const val KEY_SHOW_LIST_FILE_SIZE = "key_show_list_file_size"
        private const val KEY_SHOW_LIST_DATE = "key_show_list_date"
        private const val KEY_SHOW_LIST_FOLDER = "key_show_list_folder"
        private const val KEY_TAG_SHAPE = "key_tag_shape"
        private const val KEY_TAG_POSITION = "key_tag_position"
        private const val KEY_TAG_PALETTE_PRESET = "key_tag_palette_preset"
        private const val KEY_TAG_ANIMATION = "key_tag_animation"
        private const val KEY_SCRUBBER_STYLE = "key_scrubber_style"
        private const val KEY_SCRUBBER_WIDTH = "key_scrubber_width"
        private const val KEY_SCRUBBER_BUBBLE_STYLE = "key_scrubber_bubble_style"
        private const val KEY_UI_DESIGN_SYSTEM = "key_ui_design_system"
        private const val KEY_START_DATE = "key_start_date"
        private const val KEY_END_DATE = "key_end_date"
    }

    // Persisted state via SavedStateHandle
    val queryFlow = savedStateHandle.getStateFlow(KEY_QUERY, "")
    val selectedTagIdsFlow = savedStateHandle.getStateFlow<List<Long>>(KEY_SELECTED_TAG_IDS, emptyList())
    val untaggedOnlyFlow = savedStateHandle.getStateFlow(KEY_UNTAGGED_ONLY, false)
    val tagMatchModeFlow: StateFlow<String> = savedStateHandle.getStateFlow(KEY_TAG_MATCH_MODE, TagMatchMode.ANY.name)
    val sortOrderFlow = savedStateHandle.getStateFlow(KEY_SORT_ORDER, SortOrder.NEWEST.name)
    val activeTabFlow = savedStateHandle.getStateFlow(KEY_ACTIVE_TAB, MainTab.ALL_PHOTOS.name)
    val selectedFolderFlow = savedStateHandle.getStateFlow(KEY_SELECTED_FOLDER, "")
    val startDateFlow: StateFlow<Long?> = savedStateHandle.getStateFlow<Long?>(KEY_START_DATE, null)
    val endDateFlow: StateFlow<Long?> = savedStateHandle.getStateFlow<Long?>(KEY_END_DATE, null)
    val gridColumns: StateFlow<Int> = savedStateHandle.getStateFlow(KEY_GRID_COLUMNS, 2)
    val hapticsEnabled: StateFlow<Boolean> = savedStateHandle.getStateFlow(KEY_HAPTICS_ENABLED, true)
    val themeModeFlow: StateFlow<String> = savedStateHandle.getStateFlow(KEY_THEME_MODE, "SYSTEM")
    val accentPaletteFlow: StateFlow<String> = savedStateHandle.getStateFlow(KEY_ACCENT_PALETTE, AccentPalette.TITANIUM_SAPPHIRE.name)
    val viewingStyleFlow: StateFlow<String> = savedStateHandle.getStateFlow(KEY_VIEWING_STYLE, ViewingStyle.GRID.name)
    val tagDisplaySizeFlow: StateFlow<String> = savedStateHandle.getStateFlow(KEY_TAG_DISPLAY_SIZE, TagDisplaySize.THIN_MICRO.name)
    val showListFileSizeFlow: StateFlow<Boolean> = savedStateHandle.getStateFlow(KEY_SHOW_LIST_FILE_SIZE, true)
    val showListDateFlow: StateFlow<Boolean> = savedStateHandle.getStateFlow(KEY_SHOW_LIST_DATE, true)
    val showListFolderFlow: StateFlow<Boolean> = savedStateHandle.getStateFlow(KEY_SHOW_LIST_FOLDER, true)
    val tagShapeFlow: StateFlow<String> = savedStateHandle.getStateFlow(KEY_TAG_SHAPE, TagShape.SQUIRCLE.name)
    val tagPositionFlow: StateFlow<String> = savedStateHandle.getStateFlow(KEY_TAG_POSITION, TagPosition.BOTTOM_LEFT.name)
    val tagPalettePresetFlow: StateFlow<String> = savedStateHandle.getStateFlow(KEY_TAG_PALETTE_PRESET, TagPalettePreset.DYNAMIC_AUTO.name)
    val tagAnimationFlow: StateFlow<String> = savedStateHandle.getStateFlow(KEY_TAG_ANIMATION, TagAnimation.NONE.name)
    val scrubberStyleFlow: StateFlow<String> = savedStateHandle.getStateFlow(KEY_SCRUBBER_STYLE, ScrubberStyle.CUPERTINO_CAPSULE.name)
    val scrubberWidthFlow: StateFlow<String> = savedStateHandle.getStateFlow(KEY_SCRUBBER_WIDTH, ScrubberWidth.STANDARD.name)
    val scrubberBubbleStyleFlow: StateFlow<String> = savedStateHandle.getStateFlow(KEY_SCRUBBER_BUBBLE_STYLE, ScrubberBubbleStyle.FROSTED_GLASS_LENS.name)
    val uiDesignSystemFlow: StateFlow<String> = savedStateHandle.getStateFlow(KEY_UI_DESIGN_SYSTEM, UiDesignSystem.LIQUID_GLASS.name)

    // Duplicate detection runtime state
    private val _duplicateClustersFlow = MutableStateFlow<List<DuplicateCluster>>(emptyList())
    val duplicateClustersFlow: StateFlow<List<DuplicateCluster>> = _duplicateClustersFlow.asStateFlow()

    private val _isScanningDuplicatesFlow = MutableStateFlow(false)
    val isScanningDuplicatesFlow: StateFlow<Boolean> = _isScanningDuplicatesFlow.asStateFlow()

    fun setGridColumns(cols: Int) {
        savedStateHandle[KEY_GRID_COLUMNS] = cols.coerceIn(1, 4)
    }

    fun setHapticsEnabled(enabled: Boolean) {
        savedStateHandle[KEY_HAPTICS_ENABLED] = enabled
    }

    fun setThemeMode(mode: String) {
        savedStateHandle[KEY_THEME_MODE] = mode
    }

    fun setAccentPalette(palette: AccentPalette) {
        savedStateHandle[KEY_ACCENT_PALETTE] = palette.name
    }

    fun setViewingStyle(style: ViewingStyle) {
        savedStateHandle[KEY_VIEWING_STYLE] = style.name
    }

    fun setTagDisplaySize(size: TagDisplaySize) {
        savedStateHandle[KEY_TAG_DISPLAY_SIZE] = size.name
    }

    fun setShowListFileSize(show: Boolean) {
        savedStateHandle[KEY_SHOW_LIST_FILE_SIZE] = show
    }

    fun setShowListDate(show: Boolean) {
        savedStateHandle[KEY_SHOW_LIST_DATE] = show
    }

    fun setShowListFolder(show: Boolean) {
        savedStateHandle[KEY_SHOW_LIST_FOLDER] = show
    }

    fun setTagShape(shape: TagShape) {
        savedStateHandle[KEY_TAG_SHAPE] = shape.name
    }

    fun setTagPosition(position: TagPosition) {
        savedStateHandle[KEY_TAG_POSITION] = position.name
    }

    fun setTagPalettePreset(preset: TagPalettePreset) {
        savedStateHandle[KEY_TAG_PALETTE_PRESET] = preset.name
    }

    fun setTagAnimation(animation: TagAnimation) {
        savedStateHandle[KEY_TAG_ANIMATION] = animation.name
    }

    fun setScrubberStyle(style: ScrubberStyle) {
        savedStateHandle[KEY_SCRUBBER_STYLE] = style.name
    }

    fun setScrubberWidth(width: ScrubberWidth) {
        savedStateHandle[KEY_SCRUBBER_WIDTH] = width.name
    }

    fun setScrubberBubbleStyle(bubbleStyle: ScrubberBubbleStyle) {
        savedStateHandle[KEY_SCRUBBER_BUBBLE_STYLE] = bubbleStyle.name
    }

    fun setUiDesignSystem(designSystem: UiDesignSystem) {
        savedStateHandle[KEY_UI_DESIGN_SYSTEM] = designSystem.name
    }

    fun setDateRange(startDate: Long?, endDate: Long?) {
        savedStateHandle[KEY_START_DATE] = startDate
        savedStateHandle[KEY_END_DATE] = endDate
    }

    fun clearDateRange() {
        setDateRange(null, null)
    }

    enum class DatePreset(val label: String) {
        TODAY("Today"),
        PAST_7_DAYS("Past 7 Days"),
        THIS_MONTH("This Month"),
        PAST_30_DAYS("Past 30 Days"),
        THIS_YEAR("This Year")
    }

    fun setDatePreset(preset: DatePreset) {
        val calendar = java.util.Calendar.getInstance()

        // End of today: 23:59:59.999
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
        calendar.set(java.util.Calendar.MINUTE, 59)
        calendar.set(java.util.Calendar.SECOND, 59)
        calendar.set(java.util.Calendar.MILLISECOND, 999)
        val endOfToday = calendar.timeInMillis

        val start = when (preset) {
            DatePreset.TODAY -> {
                calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                calendar.set(java.util.Calendar.MINUTE, 0)
                calendar.set(java.util.Calendar.SECOND, 0)
                calendar.set(java.util.Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
            DatePreset.PAST_7_DAYS -> {
                calendar.add(java.util.Calendar.DAY_OF_YEAR, -6)
                calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                calendar.set(java.util.Calendar.MINUTE, 0)
                calendar.set(java.util.Calendar.SECOND, 0)
                calendar.set(java.util.Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
            DatePreset.THIS_MONTH -> {
                calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
                calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                calendar.set(java.util.Calendar.MINUTE, 0)
                calendar.set(java.util.Calendar.SECOND, 0)
                calendar.set(java.util.Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
            DatePreset.PAST_30_DAYS -> {
                calendar.add(java.util.Calendar.DAY_OF_YEAR, -29)
                calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                calendar.set(java.util.Calendar.MINUTE, 0)
                calendar.set(java.util.Calendar.SECOND, 0)
                calendar.set(java.util.Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
            DatePreset.THIS_YEAR -> {
                calendar.set(java.util.Calendar.DAY_OF_YEAR, 1)
                calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                calendar.set(java.util.Calendar.MINUTE, 0)
                calendar.set(java.util.Calendar.SECOND, 0)
                calendar.set(java.util.Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
        }
        setDateRange(start, endOfToday)
    }

    fun scanForDuplicates(context: Context) {
        viewModelScope.launch(defaultDispatcher) {
            _isScanningDuplicatesFlow.value = true
            try {
                // Fetch the entire photo catalog to scan across all albums and folders
                val allPhotos = repository.observePhotos().first()

                if (allPhotos.size < 2) {
                    _duplicateClustersFlow.value = emptyList()
                    _eventChannel.send(UiEvent.ShowMessage("Need at least 2 photos in library to scan for duplicates."))
                    return@launch
                }

                val hashMap = mutableMapOf<Long, Long>()
                for (photo in allPhotos) {
                    val candidateFile = when {
                        File(context.filesDir, photo.photo.thumbPath).exists() -> File(context.filesDir, photo.photo.thumbPath)
                        File(context.filesDir, photo.photo.originalPath).exists() -> File(context.filesDir, photo.photo.originalPath)
                        File(photo.photo.originalPath).exists() -> File(photo.photo.originalPath)
                        else -> null
                    }
                    if (candidateFile != null) {
                        val hash = DuplicateFinder.computeDHashFromFile(candidateFile)
                        if (hash != null) {
                            hashMap[photo.photo.id] = hash
                        }
                    }
                }

                val clusters = DuplicateFinder.findClusters(allPhotos, hashMap, maxHammingDistance = 8)
                _duplicateClustersFlow.value = clusters

                if (clusters.isEmpty()) {
                    _eventChannel.send(UiEvent.ShowMessage("Scan complete: No duplicates found. Your library is 100% unique!"))
                } else {
                    val reclaimMb = clusters.sumOf { it.reclaimableBytes } / (1024.0 * 1024.0)
                    _eventChannel.send(UiEvent.ShowMessage("Found ${clusters.size} duplicate cluster(s)! Reclaimable: %.1f MB".format(reclaimMb)))
                }
            } catch (e: Exception) {
                _eventChannel.send(UiEvent.ShowMessage("Scan error: ${e.message}"))
            } finally {
                _isScanningDuplicatesFlow.value = false
            }
        }
    }

    fun dismissDuplicateCluster(clusterId: String) {
        _duplicateClustersFlow.value = _duplicateClustersFlow.value.filter { it.id != clusterId }
    }

    fun deleteDuplicatesInCluster(cluster: DuplicateCluster, onComplete: () -> Unit = {}) {
        val idsToDelete = cluster.duplicatePhotos.map { it.photo.id }
        deletePhotos(idsToDelete)
        dismissDuplicateCluster(cluster.id)
        onComplete()
    }

    fun setTagMatchMode(mode: TagMatchMode) {
        savedStateHandle[KEY_TAG_MATCH_MODE] = mode.name
    }

    fun toggleTagMatchMode() {
        val current = try {
            TagMatchMode.valueOf(tagMatchModeFlow.value)
        } catch (_: Exception) {
            TagMatchMode.ANY
        }
        val next = if (current == TagMatchMode.ANY) TagMatchMode.ALL else TagMatchMode.ANY
        setTagMatchMode(next)
    }

    fun cycleViewingStyle() {
        val current = try {
            ViewingStyle.valueOf(viewingStyleFlow.value)
        } catch (_: Exception) {
            ViewingStyle.GRID
        }
        val next = when (current) {
            ViewingStyle.GRID -> ViewingStyle.LIST_DETAILS
            ViewingStyle.LIST_DETAILS -> ViewingStyle.MASONRY
            ViewingStyle.MASONRY -> ViewingStyle.GRID
        }
        setViewingStyle(next)
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
        tagMatchModeFlow,
        sortOrderFlow
    ) { query, selectedTagIds, untaggedOnly, matchModeName, sortOrderName ->
        FilterOptions(
            query = query,
            selectedTagIds = selectedTagIds,
            untaggedOnly = untaggedOnly,
            matchModeName = matchModeName,
            sortOrderName = sortOrderName,
            activeTabName = activeTabFlow.value,
            selectedFolder = selectedFolderFlow.value,
            startDate = startDateFlow.value,
            endDate = endDateFlow.value
        )
    }.combine(activeTabFlow) { opts, tab ->
        opts.copy(activeTabName = tab)
    }.combine(selectedFolderFlow) { opts, folder ->
        opts.copy(selectedFolder = folder)
    }.combine(startDateFlow) { opts, start ->
        opts.copy(startDate = start)
    }.combine(endDateFlow) { opts, end ->
        opts.copy(endDate = end)
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

        val matchMode = try {
            TagMatchMode.valueOf(filterOpts.matchModeName)
        } catch (_: Exception) {
            TagMatchMode.ANY
        }

        val filterState = FilterState(
            query = filterOpts.query,
            selectedTagIds = validSelectedTagIds,
            untaggedOnly = filterOpts.untaggedOnly,
            matchMode = matchMode,
            startDate = filterOpts.startDate,
            endDate = filterOpts.endDate
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
                tagIds = p.tags.map { it.id }.toSet(),
                timestamp = p.photo.takenAt ?: p.photo.addedAt
            )
        }

        // 2. Sort using domain PhotoSorter
        val sorted = PhotoSorter.sort(filtered, sortOrder) { p ->
            SortablePhoto(
                id = p.photo.id,
                addedAt = p.photo.addedAt,
                normalizedTags = p.tags.map { it.normalizedName }.toSet(),
                takenAt = p.photo.takenAt
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
