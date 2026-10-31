package com.tagshot.app.domain

import com.tagshot.app.data.PhotoEntity
import com.tagshot.app.data.PhotoWithTags
import com.tagshot.app.data.TagEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DuplicateFinderTest {

    private fun createPhoto(
        id: Long,
        width: Int = 1920,
        height: Int = 1080,
        sizeBytes: Long = 1_000_000L,
        sha256: String = "hash_$id",
        tags: List<String> = emptyList()
    ): PhotoWithTags {
        val entity = PhotoEntity(
            id = id,
            originalPath = "/orig/photo_$id.jpg",
            thumbPath = "/thumbs/thumb_$id.jpg",
            sha256 = sha256,
            mimeType = "image/jpeg",
            width = width,
            height = height,
            sizeBytes = sizeBytes,
            addedAt = 1000L + id,
            takenAt = 1000L + id
        )
        val tagEntities = tags.mapIndexed { idx, name ->
            TagEntity(id = idx.toLong() + 1, name = name, normalizedName = name.lowercase())
        }
        return PhotoWithTags(entity, tagEntities)
    }

    @Test
    fun testHammingDistance_identicalHashes_isZero() {
        val hash = 0x123456789ABCDEF0L
        assertEquals(0, DuplicateFinder.hammingDistance(hash, hash))
        assertEquals(100, DuplicateFinder.similarityPercentage(0))
    }

    @Test
    fun testHammingDistance_oneBitDifference() {
        val hashA = 0x1L
        val hashB = 0x3L // 1 bit different (bit 1)
        assertEquals(1, DuplicateFinder.hammingDistance(hashA, hashB))
        assertTrue(DuplicateFinder.similarityPercentage(1) >= 98)
    }

    @Test
    fun testEvaluateRecommendation_higherResolutionWins() {
        val highRes = createPhoto(id = 1, width = 4000, height = 3000, sizeBytes = 3_000_000L)
        val lowRes = createPhoto(id = 2, width = 1920, height = 1080, sizeBytes = 800_000L)

        val (keep, discard) = DuplicateFinder.evaluateRecommendation(lowRes, highRes)
        assertEquals(1L, keep.photo.id)
        assertEquals(2L, discard.photo.id)
    }

    @Test
    fun testEvaluateRecommendation_moreTagsWinsOnEqualResolution() {
        val withTags = createPhoto(id = 1, width = 1920, height = 1080, tags = listOf("sunset", "beach"))
        val noTags = createPhoto(id = 2, width = 1920, height = 1080, tags = emptyList())

        val (keep, discard) = DuplicateFinder.evaluateRecommendation(noTags, withTags)
        assertEquals(1L, keep.photo.id)
        assertEquals(2L, discard.photo.id)
    }

    @Test
    fun testFindClusters_exactSha256Matches() {
        val p1 = createPhoto(id = 1, sha256 = "duplicate_hash", tags = listOf("favorite"))
        val p2 = createPhoto(id = 2, sha256 = "duplicate_hash", tags = emptyList())
        val p3 = createPhoto(id = 3, sha256 = "unique_hash")

        val clusters = DuplicateFinder.findClusters(
            photos = listOf(p1, p2, p3),
            photoHashMap = emptyMap()
        )

        assertEquals(1, clusters.size)
        val cluster = clusters.first()
        assertEquals(DuplicateMatchType.EXACT_HASH, cluster.matchType)
        assertEquals(1L, cluster.keepPhoto.photo.id) // P1 has tags, so it is kept
        assertEquals(1, cluster.duplicatePhotos.size)
        assertEquals(2L, cluster.duplicatePhotos.first().photo.id)
    }

    @Test
    fun testFindClusters_visualSimilarityDHashMatches() {
        val p1 = createPhoto(id = 1, width = 4032, height = 3024, sizeBytes = 4_000_000L)
        val p2 = createPhoto(id = 2, width = 1920, height = 1080, sizeBytes = 800_000L) // near duplicate
        val p3 = createPhoto(id = 3, width = 1920, height = 1080, sizeBytes = 1_000_000L) // different photo

        val hashA = 0b1111000011110000L
        val hashB = 0b1111000011110001L // Hamming distance 1
        val hashC = 0b0000111100001111L // Completely different

        val map = mapOf(1L to hashA, 2L to hashB, 3L to hashC)

        val clusters = DuplicateFinder.findClusters(
            photos = listOf(p1, p2, p3),
            photoHashMap = map,
            maxHammingDistance = 5
        )

        assertEquals(1, clusters.size)
        val cluster = clusters.first()
        assertEquals(DuplicateMatchType.VISUAL_SIMILARITY, cluster.matchType)
        assertEquals(1L, cluster.keepPhoto.photo.id) // 4032x3024 kept over 1920x1080
        assertEquals(2L, cluster.duplicatePhotos.first().photo.id)
        assertEquals(800_000L, cluster.reclaimableBytes)
        assertTrue(cluster.recommendationReason.contains("higher resolution", ignoreCase = true))
    }
}
