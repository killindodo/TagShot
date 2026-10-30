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

        // matchChips
        assertTrue(PhotoFilter.matchChips(photo1, emptySet()))
        assertTrue(PhotoFilter.matchChips(photo1, setOf(10L, 20L)))
        assertTrue(PhotoFilter.matchChips(photo1, setOf(10L, 20L, 30L)))
        assertFalse(PhotoFilter.matchChips(photo1, setOf(10L, 99L)))
        assertTrue(PhotoFilter.matchChips(photoUntagged, emptySet()))
        assertFalse(PhotoFilter.matchChips(photoUntagged, setOf(10L)))

        // matchUntagged
        assertTrue(PhotoFilter.matchUntagged(photo1, untaggedOnly = false))
        assertFalse(PhotoFilter.matchUntagged(photo1, untaggedOnly = true))
        assertTrue(PhotoFilter.matchUntagged(photoUntagged, untaggedOnly = false))
        assertTrue(PhotoFilter.matchUntagged(photoUntagged, untaggedOnly = true))
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

        // Adding a chip never grows result
        val chipBaseState = FilterState(selectedTagIds = setOf(1L))
        val chipBaseResult = PhotoFilter.filter(photos, chipBaseState) { it }
        val chipNarrowedState = FilterState(selectedTagIds = setOf(1L, 3L))
        val chipNarrowedResult = PhotoFilter.filter(photos, chipNarrowedState) { it }
        assertTrue(chipBaseResult.containsAll(chipNarrowedResult))
        assertTrue(chipNarrowedResult.size <= chipBaseResult.size)

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

            val state = FilterState(
                query = queryStr,
                selectedTagIds = selectedChips,
                untaggedOnly = untaggedOnly
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
                val chipsOk = selectedChips.isEmpty() || selectedChips.all { chipId ->
                    p.tagIds.contains(chipId)
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
