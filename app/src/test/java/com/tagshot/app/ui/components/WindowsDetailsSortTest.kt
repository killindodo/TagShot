package com.tagshot.app.ui.components

import com.tagshot.app.data.PhotoEntity
import com.tagshot.app.data.PhotoWithTags
import com.tagshot.app.data.TagEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WindowsDetailsSortTest {

    private fun makePhoto(
        id: Long,
        filename: String,
        addedAt: Long,
        sizeBytes: Long,
        tags: List<String>
    ): PhotoWithTags {
        val photo = PhotoEntity(
            id = id,
            originalPath = "/storage/emulated/0/DCIM/Camera/$filename",
            thumbPath = "thumbs/$id.webp",
            sha256 = "hash_$id",
            mimeType = "image/jpeg",
            width = 100,
            height = 100,
            sizeBytes = sizeBytes,
            addedAt = addedAt,
            takenAt = null,
            folderName = "Camera"
        )
        val tagList = tags.mapIndexed { idx, name ->
            TagEntity(
                id = idx.toLong() + 1,
                name = name,
                normalizedName = name.lowercase().trim()
            )
        }
        return PhotoWithTags(photo = photo, tags = tagList)
    }

    @Test
    fun testSortDirectionToggle() {
        assertEquals(SortDirection.DESCENDING, SortDirection.ASCENDING.toggle())
        assertEquals(SortDirection.ASCENDING, SortDirection.DESCENDING.toggle())
    }

    @Test
    fun testColumnSortStateDefaults() {
        val defaultState = ColumnSortState()
        assertEquals(WindowsDetailsColumn.DATE, defaultState.column)
        assertEquals(SortDirection.DESCENDING, defaultState.direction)
    }

    @Test
    fun testWindowsDetailsColumnLabels() {
        assertEquals("Name", WindowsDetailsColumn.NAME.label)
        assertEquals("Date", WindowsDetailsColumn.DATE.label)
        assertEquals("Size", WindowsDetailsColumn.SIZE.label)
        assertEquals("Tags", WindowsDetailsColumn.TAGS.label)
    }

    @Test
    fun testSortByNameAscendingAndDescending() {
        val p1 = makePhoto(1, "charlie.jpg", 1000L, 500L, emptyList())
        val p2 = makePhoto(2, "alpha.jpg", 2000L, 200L, emptyList())
        val p3 = makePhoto(3, "bravo.jpg", 3000L, 800L, emptyList())
        val list = listOf(p1, p2, p3)

        val asc = sortPhotosByColumn(list, WindowsDetailsColumn.NAME, SortDirection.ASCENDING)
        assertEquals(listOf(p2, p3, p1), asc)

        val desc = sortPhotosByColumn(list, WindowsDetailsColumn.NAME, SortDirection.DESCENDING)
        assertEquals(listOf(p1, p3, p2), desc)
    }

    @Test
    fun testSortByDateAscendingAndDescending() {
        val p1 = makePhoto(1, "p1.jpg", 1000L, 500L, emptyList())
        val p2 = makePhoto(2, "p2.jpg", 3000L, 200L, emptyList())
        val p3 = makePhoto(3, "p3.jpg", 2000L, 800L, emptyList())
        val list = listOf(p1, p2, p3)

        // Ascending -> oldest first
        val asc = sortPhotosByColumn(list, WindowsDetailsColumn.DATE, SortDirection.ASCENDING)
        assertEquals(listOf(p1, p3, p2), asc)

        // Descending -> newest first
        val desc = sortPhotosByColumn(list, WindowsDetailsColumn.DATE, SortDirection.DESCENDING)
        assertEquals(listOf(p2, p3, p1), desc)
    }

    @Test
    fun testSortBySizeAscendingAndDescending() {
        val p1 = makePhoto(1, "small.jpg", 1000L, 100L, emptyList())
        val p2 = makePhoto(2, "large.jpg", 2000L, 900L, emptyList())
        val p3 = makePhoto(3, "medium.jpg", 3000L, 500L, emptyList())
        val list = listOf(p1, p2, p3)

        val asc = sortPhotosByColumn(list, WindowsDetailsColumn.SIZE, SortDirection.ASCENDING)
        assertEquals(listOf(p1, p3, p2), asc)

        val desc = sortPhotosByColumn(list, WindowsDetailsColumn.SIZE, SortDirection.DESCENDING)
        assertEquals(listOf(p2, p3, p1), desc)
    }

    @Test
    fun testSortByTagsAscendingAndDescending() {
        val p1 = makePhoto(1, "sunset.jpg", 1000L, 100L, listOf("sunset", "nature"))
        val p2 = makePhoto(2, "apple.jpg", 2000L, 200L, listOf("apple"))
        val p3 = makePhoto(3, "untagged.jpg", 3000L, 300L, emptyList())
        val list = listOf(p1, p2, p3)

        // Ascending -> "apple" first, then "nature"/"sunset", untagged last
        val asc = sortPhotosByColumn(list, WindowsDetailsColumn.TAGS, SortDirection.ASCENDING)
        assertEquals(listOf(p2, p1, p3), asc)

        // Descending -> reversed
        val desc = sortPhotosByColumn(list, WindowsDetailsColumn.TAGS, SortDirection.DESCENDING)
        assertEquals(listOf(p3, p1, p2), desc)
    }
}
