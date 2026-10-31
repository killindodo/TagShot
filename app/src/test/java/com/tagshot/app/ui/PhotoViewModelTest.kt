package com.tagshot.app.ui

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.tagshot.app.data.BatchImportSummary
import com.tagshot.app.data.DeletedPhotoBackup
import com.tagshot.app.data.PhotoEntity
import com.tagshot.app.data.PhotoRepository
import com.tagshot.app.data.PhotoWithTags
import com.tagshot.app.data.SingleImportResult
import com.tagshot.app.data.TagEntity
import com.tagshot.app.data.TagWithCount
import com.tagshot.app.domain.SortOrder
import com.tagshot.app.domain.TagResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class FakePhotoRepository : PhotoRepository {
    val photosFlow = MutableStateFlow<List<PhotoWithTags>>(emptyList())
    val tagsFlow = MutableStateFlow<List<TagWithCount>>(emptyList())
    val foldersFlow = MutableStateFlow<List<String>>(emptyList())

    val deletedBackups = mutableListOf<DeletedPhotoBackup>()
    val finalizedDeletes = mutableListOf<DeletedPhotoBackup>()
    val restoredBackups = mutableListOf<DeletedPhotoBackup>()

    override fun observePhotos(): Flow<List<PhotoWithTags>> = photosFlow
    override fun observeTags(): Flow<List<TagWithCount>> = tagsFlow
    override fun observeFolderNames(): Flow<List<String>> = foldersFlow

    override suspend fun getPhotoWithTagsById(id: Long): PhotoWithTags? =
        photosFlow.value.find { it.photo.id == id }

    override suspend fun importUris(
        uris: List<Uri>,
        tags: List<TagResult.Valid>,
        folderName: String,
        onProgress: (BatchImportSummary) -> Unit
    ): BatchImportSummary {
        val summary = BatchImportSummary(done = uris.size, total = uris.size, imported = uris.size, duplicates = 0, failed = 0)
        onProgress(summary)
        return summary
    }

    override suspend fun importFolder(
        treeUri: Uri,
        tags: List<TagResult.Valid>,
        onProgress: (BatchImportSummary) -> Unit
    ): Pair<String, BatchImportSummary> {
        val summary = BatchImportSummary(done = 0, total = 0, imported = 0, duplicates = 0, failed = 0)
        return Pair("TestFolder", summary)
    }

    override suspend fun importCameraFile(file: File, tags: List<TagResult.Valid>): SingleImportResult {
        return SingleImportResult.Success(100L)
    }

    override suspend fun addTagsToPhotos(photoIds: List<Long>, tags: List<TagResult.Valid>) {}
    override suspend fun removeTagFromPhoto(photoId: Long, tagId: Long) {}
    override suspend fun updateNote(photoId: Long, note: String) {}

    override suspend fun prepareDelete(photoIds: List<Long>): List<DeletedPhotoBackup> {
        val toDelete = photosFlow.value.filter { photoIds.contains(it.photo.id) }
        val backups = toDelete.map { DeletedPhotoBackup(it.photo, it.tags) }
        deletedBackups.addAll(backups)
        photosFlow.value = photosFlow.value.filterNot { photoIds.contains(it.photo.id) }
        return backups
    }

    override suspend fun restorePhotos(backups: List<DeletedPhotoBackup>) {
        restoredBackups.addAll(backups)
        val restored = backups.map { PhotoWithTags(it.photo, it.tags) }
        photosFlow.value = photosFlow.value + restored
    }

    override suspend fun finalizeDelete(backups: List<DeletedPhotoBackup>) {
        finalizedDeletes.addAll(backups)
    }

    override suspend fun renameOrMergeTag(tagId: Long, newRaw: String): Boolean = true
    override suspend fun deleteTagEverywhere(tagId: Long) {}
}

@OptIn(ExperimentalCoroutinesApi::class)
class PhotoViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakePhotoRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakePhotoRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createSampleData() {
        val tag1 = TagEntity(id = 1L, name = "Receipt", normalizedName = "receipt")
        val tag2 = TagEntity(id = 2L, name = "Office", normalizedName = "office")

        val p1 = PhotoWithTags(
            photo = PhotoEntity(
                id = 1L, originalPath = "photos/orig/1.jpg", thumbPath = "photos/thumbs/1.jpg",
                sha256 = "sha1", mimeType = "image/jpeg", width = 100, height = 100,
                sizeBytes = 1000L, addedAt = 1000L, takenAt = null, note = "Receipt 1"
            ),
            tags = listOf(tag1)
        )
        val p2 = PhotoWithTags(
            photo = PhotoEntity(
                id = 2L, originalPath = "photos/orig/2.jpg", thumbPath = "photos/thumbs/2.jpg",
                sha256 = "sha2", mimeType = "image/jpeg", width = 100, height = 100,
                sizeBytes = 1000L, addedAt = 2000L, takenAt = null, note = "Office Doc"
            ),
            tags = listOf(tag2)
        )
        val p3 = PhotoWithTags(
            photo = PhotoEntity(
                id = 3L, originalPath = "photos/orig/3.jpg", thumbPath = "photos/thumbs/3.jpg",
                sha256 = "sha3", mimeType = "image/jpeg", width = 100, height = 100,
                sizeBytes = 1000L, addedAt = 3000L, takenAt = null, note = "Untagged photo"
            ),
            tags = emptyList()
        )

        repository.photosFlow.value = listOf(p1, p2, p3)
        repository.tagsFlow.value = listOf(
            TagWithCount(id = 1L, name = "Receipt", normalizedName = "receipt", photoCount = 1),
            TagWithCount(id = 2L, name = "Office", normalizedName = "office", photoCount = 1)
        )
    }

    @Test
    fun testFilterStateCombinationsQueryAndChipsAndSort() = runTest {
        createSampleData()
        val savedState = SavedStateHandle()
        val viewModel = PhotoViewModel(repository, savedState, testDispatcher)

        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        // Initial state: all 3 photos, newest first (p3, p2, p1)
        var state = viewModel.uiState.value as MainUiState.Success
        assertEquals(3, state.photos.size)
        assertEquals(3L, state.photos[0].photo.id)
        assertEquals(2L, state.photos[1].photo.id)
        assertEquals(1L, state.photos[2].photo.id)

        // 1. Query test
        viewModel.onQueryChanged("rece")
        advanceUntilIdle()
        state = viewModel.uiState.value as MainUiState.Success
        assertEquals(1, state.photos.size)
        assertEquals(1L, state.photos[0].photo.id)

        // Clear query
        viewModel.onQueryChanged("")
        advanceUntilIdle()

        // 2. Chip test: select Office (tagId 2)
        viewModel.onTagChipToggled(2L)
        advanceUntilIdle()
        state = viewModel.uiState.value as MainUiState.Success
        assertEquals(1, state.photos.size)
        assertEquals(2L, state.photos[0].photo.id)

        // 3. Untagged test: selecting untagged clears chips
        viewModel.onUntaggedToggled()
        advanceUntilIdle()
        state = viewModel.uiState.value as MainUiState.Success
        assertTrue(state.filterState.untaggedOnly)
        assertTrue(state.filterState.selectedTagIds.isEmpty())
        assertEquals(1, state.photos.size)
        assertEquals(3L, state.photos[0].photo.id)

        // 4. Sort test: OLDEST
        viewModel.onClearFilters()
        viewModel.onSortOrderSelected(SortOrder.OLDEST)
        advanceUntilIdle()
        state = viewModel.uiState.value as MainUiState.Success
        assertEquals(3, state.photos.size)
        assertEquals(1L, state.photos[0].photo.id)
        assertEquals(2L, state.photos[1].photo.id)
        assertEquals(3L, state.photos[2].photo.id)

        collectJob.cancel()
    }

    @Test
    fun testAutomaticRemovalOfVanishedSelectedTag() = runTest {
        createSampleData()
        val savedState = SavedStateHandle(mapOf("key_selected_tag_ids" to listOf(1L, 999L)))
        val viewModel = PhotoViewModel(repository, savedState, testDispatcher)

        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value as MainUiState.Success
        // Tag 999 does not exist in repository.tagsFlow, so it must be automatically pruned
        assertEquals(setOf(1L), state.filterState.selectedTagIds)
        assertEquals(listOf(1L), savedState.get<List<Long>>("key_selected_tag_ids"))

        collectJob.cancel()
    }

    @Test
    fun testSavedStateHandleRestoration() = runTest {
        createSampleData()
        val savedState = SavedStateHandle(
            mapOf(
                "key_query" to "receipt",
                "key_selected_tag_ids" to listOf(1L),
                "key_untagged_only" to false,
                "key_sort_order" to SortOrder.OLDEST.name
            )
        )
        val viewModel = PhotoViewModel(repository, savedState, testDispatcher)

        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value as MainUiState.Success
        assertEquals("receipt", state.filterState.query)
        assertEquals(setOf(1L), state.filterState.selectedTagIds)
        assertFalse(state.filterState.untaggedOnly)
        assertEquals(SortOrder.OLDEST, state.sortOrder)

        collectJob.cancel()
    }

    @Test
    fun testDeleteAndUndoRestoresRowsAndTags() = runTest {
        createSampleData()
        val savedState = SavedStateHandle()
        val viewModel = PhotoViewModel(repository, savedState, testDispatcher)

        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()
        var state = viewModel.uiState.value as MainUiState.Success
        assertEquals(3, state.photos.size)

        // Delete photo 1
        viewModel.deletePhotos(listOf(1L))
        // Advance only 1000ms (within 8000ms undo window)
        advanceTimeBy(1000L)
        testScheduler.runCurrent()

        state = viewModel.uiState.value as MainUiState.Success
        assertEquals(2, state.photos.size)
        assertEquals(1, repository.deletedBackups.size)

        // Undo delete within window
        viewModel.undoDelete()
        testScheduler.runCurrent()


        state = viewModel.uiState.value as MainUiState.Success
        assertEquals(3, state.photos.size)
        assertEquals(1, repository.restoredBackups.size)

        // Advance past undo window, finalize should not be called since undone
        advanceTimeBy(9000L)
        advanceUntilIdle()
        assertEquals(0, repository.finalizedDeletes.size)


        collectJob.cancel()
    }

    @Test
    fun testImportSummaryEventCountsCorrect() = runTest {
        val savedState = SavedStateHandle()
        val viewModel = PhotoViewModel(repository, savedState, testDispatcher)

        val events = mutableListOf<UiEvent>()
        val eventJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { events.add(it) }
        }

        viewModel.importPhotos(listOf(Uri.parse("file:///a.jpg"), Uri.parse("file:///b.jpg")), "tag1, tag2")
        advanceUntilIdle()

        assertEquals(1, events.size)
        val event = events[0] as UiEvent.ImportFinished
        assertEquals(2, event.summary.total)
        assertEquals(2, event.summary.imported)
        assertEquals(0, event.summary.duplicates)
        assertEquals(0, event.summary.failed)

        eventJob.cancel()
    }

    @Test
    fun testStateCombinations_QueryAndChipsAndSort() = runTest {
        val tagReceipt = TagEntity(id = 1L, name = "Receipt", normalizedName = "receipt")
        val tagOffice = TagEntity(id = 2L, name = "Office", normalizedName = "office")

        val p1 = PhotoWithTags(
            photo = PhotoEntity(
                id = 1L, originalPath = "photos/orig/1.jpg", thumbPath = "photos/thumbs/1.jpg",
                sha256 = "sha1", mimeType = "image/jpeg", width = 100, height = 100,
                sizeBytes = 1000L, addedAt = 1000L, takenAt = null, note = "Note 1"
            ),
            tags = listOf(tagReceipt)
        )
        val p2 = PhotoWithTags(
            photo = PhotoEntity(
                id = 2L, originalPath = "photos/orig/2.jpg", thumbPath = "photos/thumbs/2.jpg",
                sha256 = "sha2", mimeType = "image/jpeg", width = 100, height = 100,
                sizeBytes = 1000L, addedAt = 2000L, takenAt = null, note = "Note 2"
            ),
            tags = listOf(tagReceipt, tagOffice)
        )
        val p3 = PhotoWithTags(
            photo = PhotoEntity(
                id = 3L, originalPath = "photos/orig/3.jpg", thumbPath = "photos/thumbs/3.jpg",
                sha256 = "sha3", mimeType = "image/jpeg", width = 100, height = 100,
                sizeBytes = 1000L, addedAt = 3000L, takenAt = null, note = "Note 3"
            ),
            tags = listOf(tagOffice)
        )
        val p4 = PhotoWithTags(
            photo = PhotoEntity(
                id = 4L, originalPath = "photos/orig/4.jpg", thumbPath = "photos/thumbs/4.jpg",
                sha256 = "sha4", mimeType = "image/jpeg", width = 100, height = 100,
                sizeBytes = 1000L, addedAt = 4000L, takenAt = null, note = "Note 4"
            ),
            tags = emptyList() // Untagged
        )

        repository.photosFlow.value = listOf(p1, p2, p3, p4)
        repository.tagsFlow.value = listOf(
            TagWithCount(1L, "Receipt", "receipt", 2),
            TagWithCount(2L, "Office", "office", 2)
        )

        val savedState = SavedStateHandle()
        val viewModel = PhotoViewModel(repository, savedState, testDispatcher)

        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        // 1. Combination: Query "office" + Chip [Receipt]
        // p2 has tag Receipt AND matches query "office" (via tag Office)
        // p1 has tag Receipt but NOT query "office" -> excluded
        // p3 matches query "office" but NOT chip Receipt -> excluded
        viewModel.onQueryChanged("office")
        viewModel.onTagChipToggled(1L) // Receipt
        advanceUntilIdle()
        var state = viewModel.uiState.value as MainUiState.Success
        assertEquals(1, state.photos.size)
        assertEquals(2L, state.photos[0].photo.id)

        // 2. Combination: Query "receipt" + Untagged
        // Untagged photos have no tags, so matching query "receipt" yields 0
        viewModel.onUntaggedToggled()
        advanceUntilIdle()
        state = viewModel.uiState.value as MainUiState.Success
        // Untagged toggled cleared chips
        assertTrue(state.filterState.selectedTagIds.isEmpty())
        assertTrue(state.filterState.untaggedOnly)
        assertEquals(0, state.photos.size)

        // 3. Clear query, Untagged only
        viewModel.onQueryChanged("")
        advanceUntilIdle()
        state = viewModel.uiState.value as MainUiState.Success
        assertEquals(1, state.photos.size)
        assertEquals(4L, state.photos[0].photo.id)

        // 4. Toggling a tag chip clears untaggedOnly
        viewModel.onTagChipToggled(2L) // Office
        advanceUntilIdle()
        state = viewModel.uiState.value as MainUiState.Success
        assertFalse(state.filterState.untaggedOnly)
        assertEquals(setOf(2L), state.filterState.selectedTagIds)
        assertEquals(2, state.photos.size) // p2 and p3

        // 5. Test all 3 sort orders on the filtered subset (p2 addedAt=2000, p3 addedAt=3000)
        viewModel.onSortOrderSelected(SortOrder.NEWEST)
        advanceUntilIdle()
        state = viewModel.uiState.value as MainUiState.Success
        assertEquals(listOf(3L, 2L), state.photos.map { it.photo.id })

        viewModel.onSortOrderSelected(SortOrder.OLDEST)
        advanceUntilIdle()
        state = viewModel.uiState.value as MainUiState.Success
        assertEquals(listOf(2L, 3L), state.photos.map { it.photo.id })

        collectJob.cancel()
    }

    @Test
    fun testProcessDeathSavedStateHandleRestore() = runTest {
        createSampleData()
        val originalHandle = SavedStateHandle()
        val vm1 = PhotoViewModel(repository, originalHandle, testDispatcher)

        val job1 = launch(UnconfinedTestDispatcher(testScheduler)) {
            vm1.uiState.collect {}
        }
        advanceUntilIdle()

        // User mutates state in vm1
        vm1.onQueryChanged("office")
        vm1.onTagChipToggled(2L)
        vm1.onSortOrderSelected(SortOrder.TAG_NAME)
        advanceUntilIdle()

        // Verify state handle persisted all changes
        assertEquals("office", originalHandle.get<String>("key_query"))
        assertEquals(listOf(2L), originalHandle.get<List<Long>>("key_selected_tag_ids"))
        assertEquals(false, originalHandle.get<Boolean>("key_untagged_only"))
        assertEquals(SortOrder.TAG_NAME.name, originalHandle.get<String>("key_sort_order"))
        job1.cancel()

        // Simulate process recreation: new ViewModel with the same SavedStateHandle
        val vm2 = PhotoViewModel(repository, originalHandle, testDispatcher)
        val job2 = launch(UnconfinedTestDispatcher(testScheduler)) {
            vm2.uiState.collect {}
        }
        advanceUntilIdle()

        val state2 = vm2.uiState.value as MainUiState.Success
        assertEquals("office", state2.filterState.query)
        assertEquals(setOf(2L), state2.filterState.selectedTagIds)
        assertFalse(state2.filterState.untaggedOnly)
        assertEquals(SortOrder.TAG_NAME, state2.sortOrder)
        assertEquals(1, state2.photos.size)
        assertEquals(2L, state2.photos[0].photo.id)

        job2.cancel()
    }

    @Test
    fun testUndoRestoresPhotoRowsAndAllTagLinks() = runTest {
        val tag1 = TagEntity(id = 1L, name = "Receipt", normalizedName = "receipt")
        val tag2 = TagEntity(id = 2L, name = "Office", normalizedName = "office")

        val p1 = PhotoWithTags(
            photo = PhotoEntity(
                id = 1L, originalPath = "photos/orig/1.jpg", thumbPath = "photos/thumbs/1.jpg",
                sha256 = "sha1", mimeType = "image/jpeg", width = 100, height = 100,
                sizeBytes = 1000L, addedAt = 1000L, takenAt = null, note = ""
            ),
            tags = listOf(tag1)
        )
        val p2 = PhotoWithTags(
            photo = PhotoEntity(
                id = 2L, originalPath = "photos/orig/2.jpg", thumbPath = "photos/thumbs/2.jpg",
                sha256 = "sha2", mimeType = "image/jpeg", width = 100, height = 100,
                sizeBytes = 1000L, addedAt = 2000L, takenAt = null, note = ""
            ),
            tags = listOf(tag1, tag2)
        )

        repository.photosFlow.value = listOf(p1, p2)
        repository.tagsFlow.value = listOf(
            TagWithCount(1L, "Receipt", "receipt", 2),
            TagWithCount(2L, "Office", "office", 1)
        )

        val savedState = SavedStateHandle()
        val viewModel = PhotoViewModel(repository, savedState, testDispatcher)

        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        // Delete both photos
        viewModel.deletePhotos(listOf(1L, 2L))
        advanceTimeBy(1000L)
        testScheduler.runCurrent()

        var state = viewModel.uiState.value as MainUiState.Success
        assertEquals(0, state.photos.size)
        assertEquals(2, repository.deletedBackups.size)

        // Undo delete
        viewModel.undoDelete()
        testScheduler.runCurrent()

        state = viewModel.uiState.value as MainUiState.Success
        assertEquals(2, state.photos.size)

        // Verify tag links are preserved
        val restoredP1 = state.photos.find { it.photo.id == 1L }!!
        assertEquals(1, restoredP1.tags.size)
        assertEquals("receipt", restoredP1.tags[0].normalizedName)

        val restoredP2 = state.photos.find { it.photo.id == 2L }!!
        assertEquals(2, restoredP2.tags.size)
        val p2TagNames = restoredP2.tags.map { it.normalizedName }.toSet()
        assertEquals(setOf("receipt", "office"), p2TagNames)

        collectJob.cancel()
    }

    @Test
    fun testDynamicRemovalOfVanishedSelectedTagWhenTagsChange() = runTest {
        createSampleData()
        // Start with tags 1 and 2 selected
        val savedState = SavedStateHandle(mapOf("key_selected_tag_ids" to listOf(1L, 2L)))
        val viewModel = PhotoViewModel(repository, savedState, testDispatcher)

        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        var state = viewModel.uiState.value as MainUiState.Success
        assertEquals(setOf(1L, 2L), state.filterState.selectedTagIds)

        // Dynamically simulate tag 2 being deleted from database/repository
        repository.tagsFlow.value = listOf(TagWithCount(1L, "Receipt", "receipt", 2))
        advanceUntilIdle()

        state = viewModel.uiState.value as MainUiState.Success
        // Tag 2 must be automatically pruned from state and SavedStateHandle
        assertEquals(setOf(1L), state.filterState.selectedTagIds)
        assertEquals(listOf(1L), savedState.get<List<Long>>("key_selected_tag_ids"))

        collectJob.cancel()
    }

    @Test
    fun testFolderWiseViewFiltersPhotosAndGroupsByFolder() = runTest {
        val p1 = PhotoEntity(id = 1L, originalPath = "p1", thumbPath = "t1", sha256 = "s1", mimeType = "image/jpeg", width = 100, height = 100, sizeBytes = 100, addedAt = 1000, takenAt = null, note = "", folderName = "Receipts")
        val p2 = PhotoEntity(id = 2L, originalPath = "p2", thumbPath = "t2", sha256 = "s2", mimeType = "image/jpeg", width = 100, height = 100, sizeBytes = 100, addedAt = 2000, takenAt = null, note = "", folderName = "Trips")
        val p3 = PhotoEntity(id = 3L, originalPath = "p3", thumbPath = "t3", sha256 = "s3", mimeType = "image/jpeg", width = 100, height = 100, sizeBytes = 100, addedAt = 3000, takenAt = null, note = "", folderName = "Receipts")

        val tag1 = TagEntity(id = 1L, name = "Food", normalizedName = "food")
        val tag2 = TagEntity(id = 2L, name = "Hotel", normalizedName = "hotel")

        repository.photosFlow.value = listOf(
            PhotoWithTags(p1, listOf(tag1)),
            PhotoWithTags(p2, listOf(tag2)),
            PhotoWithTags(p3, listOf(tag1, tag2))
        )
        repository.tagsFlow.value = listOf(
            TagWithCount(1L, "Food", "food", 2),
            TagWithCount(2L, "Hotel", "hotel", 2)
        )

        val viewModel = PhotoViewModel(repository, SavedStateHandle(), testDispatcher)
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        var state = viewModel.uiState.value as MainUiState.Success
        assertEquals(3, state.photos.size)
        assertEquals(2, state.allFolders.size)
        assertEquals("Receipts", state.allFolders[0].name)
        assertEquals(2, state.allFolders[0].photoCount)
        assertEquals("t3", state.allFolders[0].coverThumbPath)
        assertEquals("Trips", state.allFolders[1].name)
        assertEquals(1, state.allFolders[1].photoCount)
        assertEquals("t2", state.allFolders[1].coverThumbPath)

        // Switch to Folder Wise View
        viewModel.onTabSelected(MainTab.FOLDER_WISE)
        advanceUntilIdle()

        state = viewModel.uiState.value as MainUiState.Success
        assertEquals(MainTab.FOLDER_WISE, state.activeTab)
        assertEquals(3, state.photos.size)

        // Select specific folder "Receipts"
        viewModel.onFolderSelected("Receipts")
        advanceUntilIdle()

        state = viewModel.uiState.value as MainUiState.Success
        assertEquals("Receipts", state.selectedFolder)
        assertEquals(2, state.photos.size)
        assertTrue(state.photos.all { it.photo.folderName == "Receipts" })

        // Apply tag filter within folder: filter by "Hotel"
        viewModel.onTagChipToggled(2L)
        advanceUntilIdle()

        state = viewModel.uiState.value as MainUiState.Success
        assertEquals(1, state.photos.size)
        assertEquals(3L, state.photos[0].photo.id)

        collectJob.cancel()
    }

    @Test
    fun settingsTabSelection_persistedAndReflectedInUiState() = runTest(testDispatcher) {
        val handle = SavedStateHandle()
        val viewModel = PhotoViewModel(repository, handle, testDispatcher)

        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        var state = viewModel.uiState.value as MainUiState.Success
        assertEquals(MainTab.ALL_PHOTOS, state.activeTab)

        viewModel.onTabSelected(MainTab.SETTINGS)
        advanceUntilIdle()

        state = viewModel.uiState.value as MainUiState.Success
        assertEquals(MainTab.SETTINGS, state.activeTab)
        assertEquals(MainTab.SETTINGS.name, viewModel.activeTabFlow.value)

        collectJob.cancel()
    }

    @Test
    fun gridColumnsPreference_persistedAndUpdated() = runTest(testDispatcher) {
        val handle = SavedStateHandle()
        val viewModel = PhotoViewModel(repository, handle, testDispatcher)

        assertEquals(2, viewModel.gridColumns.value)

        viewModel.setGridColumns(3)
        assertEquals(3, viewModel.gridColumns.value)

        viewModel.setGridColumns(1)
        assertEquals(1, viewModel.gridColumns.value)

        // Clamping check
        viewModel.setGridColumns(0)
        assertEquals(1, viewModel.gridColumns.value)

        viewModel.setGridColumns(5)
        assertEquals(4, viewModel.gridColumns.value)
    }

    @Test
    fun hapticsPreference_persistedAndUpdated() = runTest(testDispatcher) {
        val handle = SavedStateHandle()
        val viewModel = PhotoViewModel(repository, handle, testDispatcher)

        assertTrue(viewModel.hapticsEnabled.value)

        viewModel.setHapticsEnabled(false)
        assertFalse(viewModel.hapticsEnabled.value)

        viewModel.setHapticsEnabled(true)
        assertTrue(viewModel.hapticsEnabled.value)
    }

    @Test
    fun themeModePreference_persistedAndUpdated() = runTest(testDispatcher) {
        val handle = SavedStateHandle()
        val viewModel = PhotoViewModel(repository, handle, testDispatcher)

        assertEquals("SYSTEM", viewModel.themeModeFlow.value)

        viewModel.setThemeMode("DARK")
        assertEquals("DARK", viewModel.themeModeFlow.value)

        viewModel.setThemeMode("LIGHT")
        assertEquals("LIGHT", viewModel.themeModeFlow.value)
    }
}
