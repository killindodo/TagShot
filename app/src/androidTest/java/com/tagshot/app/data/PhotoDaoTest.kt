package com.tagshot.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tagshot.app.domain.TagResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhotoDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var photoDao: PhotoDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        photoDao = database.photoDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun createPhoto(sha: String, addedAt: Long = 1000L): PhotoEntity {
        return PhotoEntity(
            originalPath = "photos/orig/$sha.jpg",
            thumbPath = "photos/thumbs/$sha.jpg",
            sha256 = sha,
            mimeType = "image/jpeg",
            width = 1920,
            height = 1080,
            sizeBytes = 204800L,
            addedAt = addedAt,
            takenAt = null,
            note = ""
        )
    }

    @Test
    fun testInsertPhotoWithThreeTags() = runBlocking {
        val photo = createPhoto("sha-001")
        val tags = listOf(
            TagResult.Valid(display = "Receipt", normalized = "receipt"),
            TagResult.Valid(display = "Office", normalized = "office"),
            TagResult.Valid(display = "2026", normalized = "2026")
        )

        val photoId = photoDao.insertPhotoWithTags(photo, tags)
        assertTrue("photoId must be positive", photoId > 0)

        val retrieved = photoDao.getPhotoWithTagsById(photoId)
        assertNotNull(retrieved)
        assertEquals(3, retrieved!!.tags.size)
        val tagNames = retrieved.tags.map { it.normalizedName }.toSet()
        assertEquals(setOf("receipt", "office", "2026"), tagNames)
    }

    @Test
    fun testCaseVariantTagInputYieldsOneTagRow() = runBlocking {
        val photo1 = createPhoto("sha-001")
        val photo2 = createPhoto("sha-002")

        val tags1 = listOf(TagResult.Valid(display = "Receipt", normalized = "receipt"))
        val tags2 = listOf(TagResult.Valid(display = "RECEIPT", normalized = "receipt"))

        photoDao.insertPhotoWithTags(photo1, tags1)
        photoDao.insertPhotoWithTags(photo2, tags2)

        val tagsWithCount = photoDao.observeTagsWithCount().first()
        assertEquals(1, tagsWithCount.size)
        assertEquals("receipt", tagsWithCount[0].normalizedName)
        assertEquals(2, tagsWithCount[0].photoCount)
    }

    @Test
    fun testUniqueShaViolationThrowsAndLeavesNoPartialRows() = runBlocking {
        val photo1 = createPhoto("duplicate-sha")
        photoDao.insertPhoto(photo1)

        val photo2 = createPhoto("duplicate-sha")
        try {
            photoDao.insertPhoto(photo2)
            fail("Expected exception on duplicate sha")
        } catch (e: Exception) {
            // Expected
        }

        val allPhotos = photoDao.getAllPhotos()
        assertEquals(1, allPhotos.size)
    }

    @Test
    fun testDeletingPhotoCascadesCrossRefs() = runBlocking {
        val photo = createPhoto("sha-cascade")
        val tags = listOf(TagResult.Valid(display = "Nature", normalized = "nature"))
        val photoId = photoDao.insertPhotoWithTags(photo, tags)

        val retrievedBefore = photoDao.getPhotoWithTagsById(photoId)
        assertNotNull(retrievedBefore)
        assertEquals(1, retrievedBefore!!.tags.size)

        photoDao.deletePhotosByIds(listOf(photoId))

        val retrievedAfter = photoDao.getPhotoWithTagsById(photoId)
        assertNull(retrievedAfter)

        val tag = photoDao.findTagByNormalizedName("nature")
        assertNotNull(tag)

        photoDao.pruneUnusedTags()
        val tagAfterPrune = photoDao.findTagByNormalizedName("nature")
        assertNull(tagAfterPrune)
    }

    @Test
    fun testPruneUnusedTagsRemovesOnlyUnreferencedTags() = runBlocking {
        val photo1 = createPhoto("sha-p1")
        photoDao.insertPhotoWithTags(
            photo1,
            listOf(TagResult.Valid(display = "ActiveTag", normalized = "activetag"))
        )
        photoDao.insertTag(TagEntity(name = "Orphan", normalizedName = "orphan"))

        val beforePrune = photoDao.observeTagsWithCount().first()
        assertEquals(2, beforePrune.size)

        photoDao.pruneUnusedTags()

        val afterPrune = photoDao.observeTagsWithCount().first()
        assertEquals(1, afterPrune.size)
        assertEquals("activetag", afterPrune[0].normalizedName)
        assertEquals(1, afterPrune[0].photoCount)
    }

    @Test
    fun testObserveTagsWithCountOrdering() = runBlocking {
        val photo1 = createPhoto("sha-c1")
        val photo2 = createPhoto("sha-c2")

        photoDao.insertPhotoWithTags(
            photo1,
            listOf(
                TagResult.Valid(display = "Alpha", normalized = "alpha"),
                TagResult.Valid(display = "Beta", normalized = "beta")
            )
        )
        photoDao.insertPhotoWithTags(
            photo2,
            listOf(TagResult.Valid(display = "Beta", normalized = "beta"))
        )

        val counts = photoDao.observeTagsWithCount().first()
        assertEquals(2, counts.size)
        assertEquals("beta", counts[0].normalizedName)
        assertEquals(2, counts[0].photoCount)
        assertEquals("alpha", counts[1].normalizedName)
        assertEquals(1, counts[1].photoCount)
    }

    @Test
    fun testRenameOrMergeTagRepointsRefsAndUpdatesCounts() = runBlocking {
        val photo1 = createPhoto("sha-m1")
        val photo2 = createPhoto("sha-m2")

        val id1 = photoDao.insertPhotoWithTags(
            photo1,
            listOf(TagResult.Valid(display = "Office", normalized = "office"))
        )
        val id2 = photoDao.insertPhotoWithTags(
            photo2,
            listOf(TagResult.Valid(display = "Receipt", normalized = "receipt"))
        )

        val officeTag = photoDao.findTagByNormalizedName("office")!!

        photoDao.renameOrMergeTag(
            tagId = officeTag.id,
            newDisplay = "Receipt",
            newNormalized = "receipt"
        )

        assertNull(photoDao.findTagByNormalizedName("office"))

        val tags = photoDao.observeTagsWithCount().first()
        assertEquals(1, tags.size)
        assertEquals("receipt", tags[0].normalizedName)
        assertEquals(2, tags[0].photoCount)

        val p1Tags = photoDao.getPhotoWithTagsById(id1)!!.tags
        assertEquals(1, p1Tags.size)
        assertEquals("receipt", p1Tags[0].normalizedName)

        val p2Tags = photoDao.getPhotoWithTagsById(id2)!!.tags
        assertEquals(1, p2Tags.size)
        assertEquals("receipt", p2Tags[0].normalizedName)
    }
}
