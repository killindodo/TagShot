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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PhotoDeleteUndoWindowTest {

    @Test
    fun testFilesAreDeletedExactlyAfter8SecondsUndoWindow() = runBlocking(Dispatchers.Default) {
        val app = ApplicationProvider.getApplicationContext<TagShotApp>()
        val container = app.container
        val fileManager = container.fileManager
        val repository = container.photoRepository

        // 1. Generate unique test ID
        val testId = java.util.UUID.randomUUID().toString()

        // 2. Insert into the application's repository/DB first so orphan sweeper does not touch it
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
        val photoId = container.database.photoDao().insertPhotoWithTags(
            photoEntity,
            listOf(TagResult.Valid(display = "Test", normalized = "test"))
        )

        // 3. Create original and thumb file on disk
        val origFile = File(fileManager.origDir, "$testId.jpg").apply { writeText("original photo data") }
        val thumbFile = File(fileManager.thumbDir, "$testId.jpg").apply { writeText("thumb data") }

        assertTrue(origFile.exists())
        assertTrue(thumbFile.exists())

        // 3. Create ViewModel on Main dispatcher
        val viewModel = kotlinx.coroutines.withContext(Dispatchers.Main) {
            PhotoViewModel(repository, SavedStateHandle())
        }

        // 4. Trigger delete on Main dispatcher
        kotlinx.coroutines.withContext(Dispatchers.Main) {
            viewModel.deletePhotos(listOf(photoId))
        }

        // 5. At t = 2 seconds (during 8s undo window): files MUST still exist on disk!
        delay(2000L)
        assertTrue("Files must NOT be deleted during the 8s undo window", origFile.exists())
        assertTrue("Thumb must NOT be deleted during the 8s undo window", thumbFile.exists())

        // 6. Wait until 8.5 seconds (past the 8,000ms window)
        delay(6500L)

        // 7. Verify files are now permanently deleted from disk!
        assertFalse("Original file must be deleted after 8 seconds", origFile.exists())
        assertFalse("Thumbnail file must be deleted after 8 seconds", thumbFile.exists())
    }
}
