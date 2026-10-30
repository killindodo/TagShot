package com.tagshot.app.data

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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
    fun testFilesAreDeletedExactlyAfter8SecondsUndoWindow() = runBlocking(Dispatchers.Main) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val fileManager = PhotoFileManager(context)
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val importUseCase = ImportUseCase(context, fileManager, database.photoDao())
        val repository = DefaultPhotoRepository(database.photoDao(), fileManager, importUseCase)

        // 1. Create original and thumb file on disk
        val origFile = File(fileManager.origDir, "undo_window_orig.jpg").apply { writeText("original photo data") }
        val thumbFile = File(fileManager.thumbDir, "undo_window_thumb.jpg").apply { writeText("thumb data") }

        assertTrue(origFile.exists())
        assertTrue(thumbFile.exists())

        // 2. Insert into DB
        val photoEntity = PhotoEntity(
            originalPath = "photos/orig/${origFile.name}",
            thumbPath = "photos/thumbs/${thumbFile.name}",
            sha256 = "sha-undo-window-test",
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

        // 3. Create ViewModel
        val viewModel = PhotoViewModel(repository, SavedStateHandle())

        // 4. Trigger delete
        viewModel.deletePhotos(listOf(photoId))

        // 5. At t = 2 seconds (during 8s undo window): files MUST still exist on disk!
        delay(2000L)
        assertTrue("Files must NOT be deleted during the 8s undo window", origFile.exists())
        assertTrue("Thumb must NOT be deleted during the 8s undo window", thumbFile.exists())

        // 6. Wait until 8.5 seconds (past the 8,000ms window)
        delay(6500L)

        // 7. Verify files are now permanently deleted from disk!
        assertFalse("Original file must be deleted after 8 seconds", origFile.exists())
        assertFalse("Thumbnail file must be deleted after 8 seconds", thumbFile.exists())

        database.close()
    }
}
