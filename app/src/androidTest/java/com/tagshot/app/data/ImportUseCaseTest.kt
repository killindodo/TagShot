package com.tagshot.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tagshot.app.domain.TagResult
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class ImportUseCaseTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var fileManager: PhotoFileManager
    private lateinit var importUseCase: ImportUseCase
    private lateinit var orphanSweeper: OrphanSweeper

    private val tempFilesToClean = mutableListOf<File>()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        fileManager = PhotoFileManager(context)
        importUseCase = ImportUseCase(context, fileManager, database.photoDao())
        orphanSweeper = OrphanSweeper(fileManager, database.photoDao())
    }

    @After
    fun tearDown() {
        database.close()
        tempFilesToClean.forEach { it.delete() }
        fileManager.origDir.listFiles()?.forEach { it.delete() }
        fileManager.thumbDir.listFiles()?.forEach { it.delete() }
        fileManager.cameraDir.listFiles()?.forEach { it.delete() }
    }

    private fun createSampleImageFile(name: String, color: Int = Color.GREEN): File {
        val file = File(context.cacheDir, "$name.png")
        val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(color)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        bitmap.recycle()
        tempFilesToClean.add(file)
        return file
    }

    @Test
    fun testImportSingleAndDuplicateSkip() = runBlocking {
        val testFile = createSampleImageFile("test_sample_1")
        val uri = Uri.fromFile(testFile)

        val tags = listOf(TagResult.Valid(display = "Receipt", normalized = "receipt"))

        // First import -> Success
        val result1 = importUseCase.importSingle(uri, tags)
        assertTrue("First import must succeed", result1 is SingleImportResult.Success)
        val photoId = (result1 as SingleImportResult.Success).photoId

        val photoInDb = database.photoDao().getPhotoWithTagsById(photoId)
        assertNotNull(photoInDb)
        assertEquals(1, photoInDb!!.tags.size)

        // Second import of the exact same bytes -> Duplicate
        val result2 = importUseCase.importSingle(uri, tags)
        assertTrue("Second import must be Duplicate", result2 is SingleImportResult.Duplicate)

        // Ensure database still only has 1 photo
        val allPhotos = database.photoDao().getAllPhotos()
        assertEquals(1, allPhotos.size)
    }

    @Test
    fun testUnreadableStreamYieldsFailure() = runBlocking {
        val nonExistentUri = Uri.parse("file:///non_existent_path_12345/non_existent.jpg")
        val result = importUseCase.importSingle(nonExistentUri)
        assertTrue("Must fail with Unreadable", result is SingleImportResult.Failed.Unreadable)
    }

    @Test
    fun testOrphanSweeperDeletesOnlyUnreferencedFiles() = runBlocking {
        // 1. Import a real photo
        val validFile = createSampleImageFile("valid_sample")
        val importResult = importUseCase.importSingle(Uri.fromFile(validFile))
        assertTrue(importResult is SingleImportResult.Success)

        // 2. Create orphan files manually in orig and thumbs and camera cache
        val orphanOrig = File(fileManager.origDir, "orphan_orig.jpg").apply { writeText("fake orig") }
        val orphanThumb = File(fileManager.thumbDir, "orphan_thumb.jpg").apply { writeText("fake thumb") }
        val orphanCamera = File(fileManager.cameraDir, "orphan_camera.jpg").apply { writeText("fake camera") }

        assertTrue(orphanOrig.exists())
        assertTrue(orphanThumb.exists())
        assertTrue(orphanCamera.exists())

        // 3. Run sweeper
        val sweptCount = orphanSweeper.sweep()
        assertTrue("Swept count must be >= 3", sweptCount >= 3)

        // 4. Assert orphans deleted
        assertTrue("Orphan orig must be deleted", !orphanOrig.exists())
        assertTrue("Orphan thumb must be deleted", !orphanThumb.exists())
        assertTrue("Orphan camera must be deleted", !orphanCamera.exists())

        // 5. Assert referenced photo files still exist
        val dbPhoto = database.photoDao().getAllPhotos().first()
        assertTrue("Referenced orig must remain", fileManager.resolve(dbPhoto.originalPath).exists())
        assertTrue("Referenced thumb must remain", fileManager.resolve(dbPhoto.thumbPath).exists())
    }

    @Test
    fun testDatabaseFailureCompensatesAndCleansUpFiles() = runBlocking {
        val realDao = database.photoDao()
        val failingDao = object : PhotoDao by realDao {
            override suspend fun insertPhotoWithTags(
                photo: PhotoEntity,
                tags: List<TagResult.Valid>
            ): Long {
                throw java.sql.SQLException("Simulated database failure during insert")
            }
        }
        val failingImportUseCase = ImportUseCase(context, fileManager, failingDao)
        val sampleFile = createSampleImageFile("test_sample_db_fail")
        val uri = Uri.fromFile(sampleFile)

        val result = failingImportUseCase.importSingle(uri)
        assertTrue("Must report Error on DB failure", result is SingleImportResult.Failed.Error)

        val origFiles = fileManager.origDir.listFiles() ?: emptyArray()
        val thumbFiles = fileManager.thumbDir.listFiles() ?: emptyArray()
        assertEquals("Orig dir must be empty after compensation", 0, origFiles.size)
        assertEquals("Thumb dir must be empty after compensation", 0, thumbFiles.size)
    }
}
