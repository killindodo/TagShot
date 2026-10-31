package com.tagshot.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class PhotoFilterTest {

    @Test
    fun testTokenExtraction() {
        val tokens = PhotoFilter.extractTokens("  #Receipt   #2024   travel  ###  ")
        assertEquals(listOf("receipt", "2024", "travel"), tokens)

        val emptyTokens = PhotoFilter.extractTokens("   ###  ## ")
        assertTrue(emptyTokens.isEmpty())
    }

    @Test
    fun testTruthTableFormulas() {
        val photo1 = FilterPhoto(
            id = 1L,
            normalizedTags = setOf("vacation", "beach", "summer 2024"),
            tagIds = setOf(10L, 20L, 30L)
        )
        val photoUntagged = FilterPhoto(
            id = 2L,
            normalizedTags = emptySet(),
            tagIds = emptySet()
        )

        // matchText
        assertTrue(PhotoFilter.matchText(photo1, emptyList()))
        assertTrue(PhotoFilter.matchText(photo1, listOf("vac", "beach")))
        assertTrue(PhotoFilter.matchText(photo1, listOf("2024")))
        assertFalse(PhotoFilter.matchText(photo1, listOf("winter")))
        assertFalse(PhotoFilter.matchText(photoUntagged, listOf("beach")))
        assertTrue(PhotoFilter.matchText(photoUntagged, emptyList()))

        // matchChips with TagMatchMode.ALL
        assertTrue(PhotoFilter.matchChips(photo1, emptySet(), TagMatchMode.ALL))
        assertTrue(PhotoFilter.matchChips(photo1, setOf(10L, 20L), TagMatchMode.ALL))
        assertTrue(PhotoFilter.matchChips(photo1, setOf(10L, 20L, 30L), TagMatchMode.ALL))
        assertFalse(PhotoFilter.matchChips(photo1, setOf(10L, 99L), TagMatchMode.ALL))
        assertTrue(PhotoFilter.matchChips(photoUntagged, emptySet(), TagMatchMode.ALL))
        assertFalse(PhotoFilter.matchChips(photoUntagged, setOf(10L), TagMatchMode.ALL))

        // matchChips with TagMatchMode.ANY
        assertTrue(PhotoFilter.matchChips(photo1, emptySet(), TagMatchMode.ANY))
        assertTrue(PhotoFilter.matchChips(photo1, setOf(10L, 99L), TagMatchMode.ANY))
        assertFalse(PhotoFilter.matchChips(photo1, setOf(98L, 99L), TagMatchMode.ANY))
        assertTrue(PhotoFilter.matchChips(photoUntagged, emptySet(), TagMatchMode.ANY))
        assertFalse(PhotoFilter.matchChips(photoUntagged, setOf(10L), TagMatchMode.ANY))

        // matchUntagged
        assertTrue(PhotoFilter.matchUntagged(photo1, untaggedOnly = false))
        assertFalse(PhotoFilter.matchUntagged(photo1, untaggedOnly = true))
        assertTrue(PhotoFilter.matchUntagged(photoUntagged, untaggedOnly = false))
        assertTrue(PhotoFilter.matchUntagged(photoUntagged, untaggedOnly = true))
    }

    @Test
    fun testFilterByTagsAnyAndAll() {
        val p1 = FilterPhoto(1L, setOf("landscape", "nature"), setOf(10L, 20L))
        val p2 = FilterPhoto(2L, setOf("portrait", "nature"), setOf(30L, 20L))
        val p3 = FilterPhoto(3L, setOf("architecture"), setOf(40L))
        val pUntagged = FilterPhoto(4L, emptySet(), emptySet())

        val photos = listOf(p1, p2, p3, pUntagged)

        // Empty tags returns all
        assertEquals(photos, PhotoFilter.filterByTags(photos, emptySet(), matchMode = TagMatchMode.ANY))
        assertEquals(photos, PhotoFilter.filterByTags(photos, emptySet(), matchMode = TagMatchMode.ALL))

        // Untagged only
        val untaggedResult = PhotoFilter.filterByTags(photos, emptySet(), untaggedOnly = true)
        assertEquals(listOf(pUntagged), untaggedResult)

        // TagMatchMode.ANY: matches photos with at least one selected tag
        val anyResult = PhotoFilter.filterByTags(photos, setOf(10L, 40L), matchMode = TagMatchMode.ANY)
        assertEquals(listOf(p1, p3), anyResult)

        // TagMatchMode.ALL: matches photos containing EVERY selected tag
        val allResult = PhotoFilter.filterByTags(photos, setOf(10L, 20L), matchMode = TagMatchMode.ALL)
        assertEquals(listOf(p1), allResult)

        val noneResult = PhotoFilter.filterByTags(photos, setOf(10L, 40L), matchMode = TagMatchMode.ALL)
        assertTrue(noneResult.isEmpty())
    }

    @Test
    fun testEmptyStateIsIdentity() {
        val photos = listOf(
            FilterPhoto(1L, setOf("a"), setOf(1L)),
            FilterPhoto(2L, emptySet(), emptySet()),
            FilterPhoto(3L, setOf("b"), setOf(2L))
        )
        val result = PhotoFilter.filter(photos, FilterState()) { it }
        assertEquals(photos, result)
    }

    @Test
    fun testMonotonicityAndOrderIndependence() {
        val photos = listOf(
            FilterPhoto(1L, setOf("red", "apple"), setOf(1L, 2L)),
            FilterPhoto(2L, setOf("red", "car"), setOf(1L, 3L)),
            FilterPhoto(3L, setOf("green", "apple"), setOf(4L, 2L)),
            FilterPhoto(4L, setOf("blue", "sky"), setOf(5L))
        )

        // Adding a token never grows result
        val baseState = FilterState(query = "red")
        val baseResult = PhotoFilter.filter(photos, baseState) { it }
        val narrowedState = FilterState(query = "red car")
        val narrowedResult = PhotoFilter.filter(photos, narrowedState) { it }
        assertTrue(baseResult.containsAll(narrowedResult))
        assertTrue(narrowedResult.size <= baseResult.size)

        // Adding a chip with ALL never grows result
        val chipBaseState = FilterState(selectedTagIds = setOf(1L), matchMode = TagMatchMode.ALL)
        val chipBaseResult = PhotoFilter.filter(photos, chipBaseState) { it }
        val chipNarrowedState = FilterState(selectedTagIds = setOf(1L, 3L), matchMode = TagMatchMode.ALL)
        val chipNarrowedResult = PhotoFilter.filter(photos, chipNarrowedState) { it }
        assertTrue(chipBaseResult.containsAll(chipNarrowedResult))
        assertTrue(chipNarrowedResult.size <= chipBaseResult.size)

        // Adding a chip with ANY never shrinks result
        val anyBaseState = FilterState(selectedTagIds = setOf(1L), matchMode = TagMatchMode.ANY)
        val anyBaseResult = PhotoFilter.filter(photos, anyBaseState) { it }
        val anyExpandedState = FilterState(selectedTagIds = setOf(1L, 5L), matchMode = TagMatchMode.ANY)
        val anyExpandedResult = PhotoFilter.filter(photos, anyExpandedState) { it }
        assertTrue(anyExpandedResult.containsAll(anyBaseResult))
        assertTrue(anyExpandedResult.size >= anyBaseResult.size)

        // Token order does not matter
        val queryOrder1 = FilterState(query = "apple red")
        val queryOrder2 = FilterState(query = "red apple")
        assertEquals(
            PhotoFilter.filter(photos, queryOrder1) { it },
            PhotoFilter.filter(photos, queryOrder2) { it }
        )
    }

    @Test
    fun testBruteForceOracleProperty10000Cases() {
        val rng = Random(42)
        val tagPool = listOf("nature", "portrait", "dog", "cat", "travel", "sunset", "food", "family")
        val tagIdMap = tagPool.mapIndexed { index, s -> s to (index + 1).toLong() }.toMap()

        // Generate synthetic library of 50 photos
        val library = (1..50).map { id ->
            val numTags = rng.nextInt(4)
            val tags = mutableSetOf<String>()
            while (tags.size < numTags) {
                tags.add(tagPool[rng.nextInt(tagPool.size)])
            }
            val ids = tags.map { tagIdMap[it]!! }.toSet()
            FilterPhoto(id.toLong(), tags, ids)
        }

        // Run 10,000 randomized filter states against a standalone oracle
        for (i in 0 until 10000) {
            val queryHasTokens = rng.nextBoolean()
            val queryStr = if (queryHasTokens) {
                val count = rng.nextInt(3) + 1
                (1..count).joinToString(" ") {
                    val tag = tagPool[rng.nextInt(tagPool.size)]
                    if (rng.nextBoolean()) "#$tag" else tag.substring(0, rng.nextInt(tag.length) + 1)
                }
            } else {
                ""
            }

            val untaggedOnly = rng.nextInt(10) == 0
            val selectedChips = if (untaggedOnly || rng.nextBoolean()) {
                emptySet()
            } else {
                val chipCount = rng.nextInt(3) + 1
                (1..chipCount).map { (rng.nextInt(tagPool.size) + 1).toLong() }.toSet()
            }

            val matchMode = if (rng.nextBoolean()) TagMatchMode.ANY else TagMatchMode.ALL

            val state = FilterState(
                query = queryStr,
                selectedTagIds = selectedChips,
                untaggedOnly = untaggedOnly,
                matchMode = matchMode
            )

            // Production filter
            val actual = PhotoFilter.filter(library, state) { it }

            // Brute-force independent oracle
            val oracleTokens = if (queryStr.isBlank()) {
                emptyList()
            } else {
                queryStr.trim().split(Regex("\\s+")).mapNotNull { token ->
                    val clean = token.trimStart('#').lowercase(java.util.Locale.ROOT)
                    if (clean.isEmpty()) null else clean
                }
            }

            val expected = library.filter { p ->
                val textOk = oracleTokens.isEmpty() || oracleTokens.all { t ->
                    p.normalizedTags.any { tag -> tag.contains(t) }
                }
                val chipsOk = if (selectedChips.isEmpty()) {
                    true
                } else when (matchMode) {
                    TagMatchMode.ANY -> selectedChips.any { chipId -> p.tagIds.contains(chipId) }
                    TagMatchMode.ALL -> selectedChips.all { chipId -> p.tagIds.contains(chipId) }
                }
                val untaggedOk = !untaggedOnly || p.normalizedTags.isEmpty()
                textOk && chipsOk && untaggedOk
            }

            assertEquals("Oracle mismatch on iteration $i", expected, actual)

            // Properties: output subset of input, order preserved
            assertTrue(library.containsAll(actual))
            var lastIdx = -1
            for (item in actual) {
                val idx = library.indexOf(item)
                assertTrue("Order preservation violated", idx > lastIdx)
                lastIdx = idx
            }
        }
    }
}
