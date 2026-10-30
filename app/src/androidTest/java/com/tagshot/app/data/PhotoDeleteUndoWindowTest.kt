package com.tagshot.app.data

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tagshot.app.TagShotApp
import com.tagshot.app.domain.TagResult
import com.tagshot.app.ui.PhotoViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PhotoDeleteUndoWindowTest {

    @Test
    fun testFilesAreDeletedExactlyAfter8SecondsUndoWindow() = runBlocking(Dispatchers.Default) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val testBaseDir = File(context.cacheDir, "test_undo_window_1").apply { mkdirs() }
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        val fileManager = PhotoFileManager(context, testBaseDir)
        val importUseCase = ImportUseCase(context, fileManager, database.photoDao())
        val repository = DefaultPhotoRepository(database.photoDao(), fileManager, importUseCase)

        // 1. Generate unique test ID
        val testId = java.util.UUID.randomUUID().toString()

        // 2. Insert into the isolated repository/DB
        val photoEntity = PhotoEntity(
            originalPath = "photos/orig/$testId.jpg",
            thumbPath = "photos/thumbs/$testId.jpg",
            sha256 = testId,
            mimeType = "image/jpeg",
            width = 100,
            height = 100,
            sizeBytes = 100L,
            addedAt = 1000L,
            takenAt = null,
            note = ""
        )
        val photoId = database.photoDao().insertPhotoWithTags(
            photoEntity,
            listOf(TagResult.Valid(display = "Test", normalized = "test"))
        )

        // 3. Create original and thumb file on disk
        val origFile = File(fileManager.origDir, "$testId.jpg").apply { writeText("original photo data") }
        val thumbFile = File(fileManager.thumbDir, "$testId.jpg").apply { writeText("thumb data") }

        assertTrue(origFile.exists())
        assertTrue(thumbFile.exists())

        // 4. Create ViewModel on Main dispatcher
        val viewModel = kotlinx.coroutines.withContext(Dispatchers.Main) {
            PhotoViewModel(repository, SavedStateHandle())
        }

        // 5. Trigger delete on Main dispatcher
        kotlinx.coroutines.withContext(Dispatchers.Main) {
            viewModel.deletePhotos(listOf(photoId))
        }

        // 6. At t = 2 seconds (during 8s undo window): files MUST still exist on disk!
        kotlinx.coroutines.withContext(Dispatchers.Main) {
            delay(2000L)
        }
        assertTrue("Files must NOT be deleted during the 8s undo window", origFile.exists())
        assertTrue("Thumb must NOT be deleted during the 8s undo window", thumbFile.exists())

        // 7. Wait until past the 8,000ms window on Main dispatcher so delay queue pumps
        kotlinx.coroutines.withContext(Dispatchers.Main) {
            delay(7000L)
        }

        // 8. Verify files are now permanently deleted from disk!
        assertFalse("Original file must be deleted after 8 seconds", origFile.exists())
        assertFalse("Thumbnail file must be deleted after 8 seconds", thumbFile.exists())

        database.close()
        testBaseDir.deleteRecursively()
    }

    @Test
    fun testUndoDeleteRestoresPhotoAndTags(): Unit = runBlocking(Dispatchers.Default) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val testBaseDir = File(context.cacheDir, "test_undo_window_2").apply { mkdirs() }
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        val fileManager = PhotoFileManager(context, testBaseDir)
        val importUseCase = ImportUseCase(context, fileManager, database.photoDao())
        val repository = DefaultPhotoRepository(database.photoDao(), fileManager, importUseCase)

        val testId = java.util.UUID.randomUUID().toString()
        val photoEntity = PhotoEntity(
            originalPath = "photos/orig/$testId.jpg",
            thumbPath = "photos/thumbs/$testId.jpg",
            sha256 = testId,
            mimeType = "image/jpeg",
            width = 100,
            height = 100,
            sizeBytes = 100L,
            addedAt = 1000L,
            takenAt = null,
            note = "test note"
        )
        val photoId = database.photoDao().insertPhotoWithTags(
            photoEntity,
            listOf(TagResult.Valid(display = "UndoTag", normalized = "undotag"))
        )

        val origFile = File(fileManager.origDir, "$testId.jpg").apply { writeText("original photo data") }
        val thumbFile = File(fileManager.thumbDir, "$testId.jpg").apply { writeText("thumb data") }

        val viewModel = kotlinx.coroutines.withContext(Dispatchers.Main) {
            PhotoViewModel(repository, SavedStateHandle())
        }

        kotlinx.coroutines.withContext(Dispatchers.Main) {
            viewModel.deletePhotos(listOf(photoId))
        }

        delay(1000L)

        val photoAfterDelete = database.photoDao().getPhotoWithTagsById(photoId)
        assertTrue(photoAfterDelete == null)

        kotlinx.coroutines.withContext(Dispatchers.Main) {
            viewModel.undoDelete()
        }

        delay(1000L)

        val photoAfterUndo = database.photoDao().getPhotoWithTagsById(photoId)
        assertNotNull("Photo must be restored in DB", photoAfterUndo)
        assertEquals(1, photoAfterUndo!!.tags.size)
        assertEquals("undotag", photoAfterUndo.tags[0].normalizedName)
        assertEquals("test note", photoAfterUndo.photo.note)

        // Clean up
        database.close()
        testBaseDir.deleteRecursively()
    }
}
