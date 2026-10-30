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

    val deletedBackups = mutableListOf<DeletedPhotoBackup>()
    val finalizedDeletes = mutableListOf<DeletedPhotoBackup>()
    val restoredBackups = mutableListOf<DeletedPhotoBackup>()

    override fun observePhotos(): Flow<List<PhotoWithTags>> = photosFlow
    override fun observeTags(): Flow<List<TagWithCount>> = tagsFlow

    override suspend fun getPhotoWithTagsById(id: Long): PhotoWithTags? =
        photosFlow.value.find { it.photo.id == id }

    override suspend fun importUris(
        uris: List<Uri>,
        tags: List<TagResult.Valid>,
        onProgress: (BatchImportSummary) -> Unit
    ): BatchImportSummary {
        val summary = BatchImportSummary(done = uris.size, total = uris.size, imported = uris.size, duplicates = 0, failed = 0)
        onProgress(summary)
        return summary
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
}
