package com.tagshot.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Collections
import java.util.Random

class PhotoSorterTest {

    @Test
    fun testNewestSortOrderWithTieBreak() {
        val p1 = SortablePhoto(id = 1L, addedAt = 1000L, normalizedTags = setOf("a"))
        val p2 = SortablePhoto(id = 2L, addedAt = 2000L, normalizedTags = setOf("b"))
        // Tied addedAt with p2, but different id
        val p3 = SortablePhoto(id = 3L, addedAt = 2000L, normalizedTags = setOf("c"))

        val photos = listOf(p1, p2, p3)
        val sorted = PhotoSorter.sort(photos, SortOrder.NEWEST) { it }

        // Expected: addedAt desc (2000 > 1000). For tied 2000, id desc (3 > 2). Then 1000 (id 1).
        assertEquals(listOf(p3, p2, p1), sorted)
    }

    @Test
    fun testOldestSortOrderWithTieBreak() {
        val p1 = SortablePhoto(id = 1L, addedAt = 1000L, normalizedTags = setOf("a"))
        val p2 = SortablePhoto(id = 2L, addedAt = 2000L, normalizedTags = setOf("b"))
        val p3 = SortablePhoto(id = 3L, addedAt = 2000L, normalizedTags = setOf("c"))

        val photos = listOf(p3, p1, p2)
        val sorted = PhotoSorter.sort(photos, SortOrder.OLDEST) { it }

        // Expected: addedAt asc (1000 < 2000). For tied 2000, id asc (2 < 3).
        assertEquals(listOf(p1, p2, p3), sorted)
    }

    @Test
    fun testTagNameSortWithUntaggedLastAndTieBreak() {
        val taggedZ = SortablePhoto(id = 1L, addedAt = 3000L, normalizedTags = setOf("zebra"))
        val taggedA = SortablePhoto(id = 2L, addedAt = 1000L, normalizedTags = setOf("apple", "orange"))
        val taggedB1 = SortablePhoto(id = 3L, addedAt = 1500L, normalizedTags = setOf("banana"))
        val taggedB2 = SortablePhoto(id = 4L, addedAt = 2500L, normalizedTags = setOf("banana")) // same min tag "banana", newer addedAt
        val untagged1 = SortablePhoto(id = 5L, addedAt = 5000L, normalizedTags = emptySet())
        val untagged2 = SortablePhoto(id = 6L, addedAt = 6000L, normalizedTags = emptySet())

        val list = listOf(taggedZ, untagged1, taggedB1, untagged2, taggedA, taggedB2)
        val sorted = PhotoSorter.sort(list, SortOrder.TAG_NAME) { it }

        // Expected order:
        // 1. "apple" (taggedA, id 2)
        // 2. "banana" tied: taggedB2 (addedAt 2500) before taggedB1 (addedAt 1500)
        // 3. "banana" (taggedB1, id 3)
        // 4. "zebra" (taggedZ, id 1)
        // 5. untagged tied: untagged2 (addedAt 6000) before untagged1 (addedAt 5000)
        // 6. untagged1 (id 5)
        assertEquals(listOf(taggedA, taggedB2, taggedB1, taggedZ, untagged2, untagged1), sorted)
    }

    @Test
    fun testDateTakenSortOrder() {
        val p1 = SortablePhoto(id = 1L, addedAt = 5000L, normalizedTags = setOf("a"), takenAt = 1000L)
        val p2 = SortablePhoto(id = 2L, addedAt = 1000L, normalizedTags = setOf("b"), takenAt = 3000L)
        val p3 = SortablePhoto(id = 3L, addedAt = 3000L, normalizedTags = setOf("c"), takenAt = 2000L)

        val photos = listOf(p1, p2, p3)

        // DATE_TAKEN_NEWEST: p2 (3000) > p3 (2000) > p1 (1000)
        val sortedNewest = PhotoSorter.sort(photos, SortOrder.DATE_TAKEN_NEWEST) { it }
        assertEquals(listOf(p2, p3, p1), sortedNewest)

        // DATE_TAKEN_OLDEST: p1 (1000) < p3 (2000) < p2 (3000)
        val sortedOldest = PhotoSorter.sort(photos, SortOrder.DATE_TAKEN_OLDEST) { it }
        assertEquals(listOf(p1, p3, p2), sortedOldest)
    }

    @Test
    fun testPermutationIdempotenceAndShuffleStability() {
        val rng = Random(42)
        val tags = listOf("apple", "banana", "cat", "dog", "elephant")
        val baseList = (1..20).map { id ->
            val tagCount = rng.nextInt(3)
            val photoTags = (0 until tagCount).map { tags[rng.nextInt(tags.size)] }.toSet()
            SortablePhoto(
                id = id.toLong(),
                addedAt = 1000L + rng.nextInt(5) * 100L, // some tied addedAt
                normalizedTags = photoTags
            )
        }

        for (order in SortOrder.values()) {
            val referenceSorted = PhotoSorter.sort(baseList, order) { it }

            // 1. Permutation of input
            assertEquals(baseList.size, referenceSorted.size)
            assertTrue(referenceSorted.containsAll(baseList))

            // 2. Idempotent: sort(sort(list)) == sort(list)
            val secondSort = PhotoSorter.sort(referenceSorted, order) { it }
            assertEquals("Idempotence failed for $order", referenceSorted, secondSort)

            // 3. Stable under shuffling: sorting any permutation yields identical result
            for (shuffleRound in 0 until 50) {
                val shuffled = ArrayList(baseList)
                Collections.shuffle(shuffled, rng)
                val sortedShuffled = PhotoSorter.sort(shuffled, order) { it }
                assertEquals("Shuffle stability failed for $order", referenceSorted, sortedShuffled)
            }
        }
    }
}
