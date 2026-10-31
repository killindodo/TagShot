package com.tagshot.app.domain

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.tagshot.app.data.PhotoWithTags
import java.io.File
import kotlin.math.roundToInt

/**
 * Type of match discovered between two duplicate or similar photos.
 */
enum class DuplicateMatchType(val label: String) {
    EXACT_HASH("Exact SHA-256 Match"),
    EXACT_SIZE_NAME("Identical File Size & Name"),
    VISUAL_SIMILARITY("Visual Pixel Similarity (dHash)")
}

/**
 * Represents a single comparison match between two photos.
 */
data class DuplicatePair(
    val photoA: PhotoWithTags,
    val photoB: PhotoWithTags,
    val matchType: DuplicateMatchType,
    val similarityPercentage: Int,
    val hammingDistance: Int
)

/**
 * A resolved cluster of duplicate or near-duplicate photos with a smart recommendation.
 */
data class DuplicateCluster(
    val id: String,
    val keepPhoto: PhotoWithTags,
    val duplicatePhotos: List<PhotoWithTags>,
    val matchType: DuplicateMatchType,
    val reclaimableBytes: Long,
    val recommendationReason: String
)

/**
 * Offline Dual-Engine Duplicate & Visual Similarity Finder.
 * Computes exact metadata matches and 64-bit perceptual difference hashes (dHash)
 * to group duplicate and burst photographs with zero cloud dependencies.
 */
object DuplicateFinder {

    /**
     * Computes the 64-bit Difference Hash (dHash) of a Bitmap by downsampling to a 9x8 grayscale matrix.
     * Horizontal pixel intensity gradient yields an 8x8 (64-bit) fingerprint.
     */
    fun computeDHash(bitmap: Bitmap): Long {
        val scaled = if (bitmap.width == 9 && bitmap.height == 8) {
            bitmap
        } else {
            Bitmap.createScaledBitmap(bitmap, 9, 8, true)
        }

        val pixels = IntArray(72)
        scaled.getPixels(pixels, 0, 9, 0, 0, 9, 8)

        // Precompute grayscale luminance for each of the 72 pixels
        val gray = IntArray(72)
        for (i in 0 until 72) {
            val color = pixels[i]
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF
            // Rec. 601 luma conversion
            gray[i] = (r * 299 + g * 587 + b * 114) / 1000
        }

        var hash = 0L
        var bitIndex = 0
        for (y in 0 until 8) {
            for (x in 0 until 8) {
                val left = gray[y * 9 + x]
                val right = gray[y * 9 + (x + 1)]
                if (left > right) {
                    hash = hash or (1L shl bitIndex)
                }
                bitIndex++
            }
        }

        if (scaled !== bitmap && !scaled.isRecycled) {
            scaled.recycle()
        }

        return hash
    }

    /**
     * Reads a thumbnail or original photo file with safe downsampling and calculates its 64-bit dHash.
     */
    fun computeDHashFromFile(file: File): Long? {
        if (!file.exists() || file.length() == 0L) return null
        return try {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, boundsOpts)
            val w = boundsOpts.outWidth
            val h = boundsOpts.outHeight
            if (w <= 0 || h <= 0) return null

            var sample = 1
            while ((w / sample) > 64 || (h / sample) > 64) {
                sample *= 2
            }

            val opts = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val bitmap = BitmapFactory.decodeFile(file.absolutePath, opts) ?: return null
            val hash = computeDHash(bitmap)
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
            hash
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Calculates the Hamming Distance (differing bits) between two 64-bit hashes.
     * Distance 0 = Visually identical.
     * Distance 1..5 = Near-duplicate / burst shot (>92% visual similarity).
     */
    fun hammingDistance(hashA: Long, hashB: Long): Int {
        return java.lang.Long.bitCount(hashA xor hashB)
    }

    /**
     * Converts Hamming Distance (0..64) into a human-readable similarity percentage.
     */
    fun similarityPercentage(distance: Int): Int {
        val clamped = distance.coerceIn(0, 64)
        return (((64 - clamped) / 64.0) * 100.0).roundToInt()
    }

    /**
     * Compares two photos and evaluates which one is higher quality to recommend keeping.
     */
    fun evaluateRecommendation(photoA: PhotoWithTags, photoB: PhotoWithTags): Pair<PhotoWithTags, PhotoWithTags> {
        val pixelsA = photoA.photo.width.toLong() * photoA.photo.height.toLong()
        val pixelsB = photoB.photo.width.toLong() * photoB.photo.height.toLong()

        // 1. Resolution superiority (significant difference > 15%)
        if (pixelsA > (pixelsB * 1.15)) return Pair(photoA, photoB)
        if (pixelsB > (pixelsA * 1.15)) return Pair(photoB, photoA)

        // 2. Tag richness (preserve photo with more tags)
        if (photoA.tags.size > photoB.tags.size) return Pair(photoA, photoB)
        if (photoB.tags.size > photoA.tags.size) return Pair(photoB, photoA)

        // 3. File size (higher fidelity encoding)
        if (photoA.photo.sizeBytes > photoB.photo.sizeBytes) return Pair(photoA, photoB)
        if (photoB.photo.sizeBytes > photoA.photo.sizeBytes) return Pair(photoB, photoA)

        // 4. Default: keep newer or first
        return if ((photoA.photo.takenAt ?: photoA.photo.addedAt) >= (photoB.photo.takenAt ?: photoB.photo.addedAt)) {
            Pair(photoA, photoB)
        } else {
            Pair(photoB, photoA)
        }
    }

    /**
     * Generates a descriptive recommendation message explaining why a photo should be kept.
     */
    fun generateReason(keep: PhotoWithTags, discard: PhotoWithTags): String {
        val pixelsKeep = keep.photo.width.toLong() * keep.photo.height.toLong()
        val pixelsDiscard = discard.photo.width.toLong() * discard.photo.height.toLong()

        return when {
            pixelsKeep > pixelsDiscard -> {
                val mpKeep = "%.1f".format(pixelsKeep / 1_000_000.0)
                val mpDiscard = "%.1f".format(pixelsDiscard / 1_000_000.0)
                "Keep higher resolution: ${mpKeep}MP vs ${mpDiscard}MP"
            }
            keep.tags.size > discard.tags.size -> {
                "Keep: has ${keep.tags.size} tags (vs ${discard.tags.size})"
            }
            keep.photo.sizeBytes > discard.photo.sizeBytes -> {
                val kbKeep = keep.photo.sizeBytes / 1024
                val kbDiscard = discard.photo.sizeBytes / 1024
                "Keep higher quality file: ${kbKeep}KB vs ${kbDiscard}KB"
            }
            else -> "Recommended: Primary original photo"
        }
    }

    /**
     * Scans a list of photos using provided hashes and groups them into duplicate clusters.
     * @param photos All candidate photos
     * @param photoHashMap Map of photo ID to precomputed 64-bit dHash
     * @param maxHammingDistance Maximum differing bits for visual similarity (default 5, ~92% similarity)
     */
    fun findClusters(
        photos: List<PhotoWithTags>,
        photoHashMap: Map<Long, Long>,
        maxHammingDistance: Int = 8
    ): List<DuplicateCluster> {
        if (photos.size < 2) return emptyList()

        val processedIds = mutableSetOf<Long>()
        val clusters = mutableListOf<DuplicateCluster>()

        // 1. Group exact SHA-256 matches first
        val shaGroups = photos.groupBy { it.photo.sha256 }.filter { it.value.size > 1 }
        for ((_, group) in shaGroups) {
            val sorted = group.sortedWith(
                compareByDescending<PhotoWithTags> { it.tags.size }
                    .thenByDescending { it.photo.sizeBytes }
            )
            val keep = sorted.first()
            val duplicates = sorted.drop(1)
            val reclaimable = duplicates.sumOf { it.photo.sizeBytes }

            clusters.add(
                DuplicateCluster(
                    id = "sha_${keep.photo.id}",
                    keepPhoto = keep,
                    duplicatePhotos = duplicates,
                    matchType = DuplicateMatchType.EXACT_HASH,
                    reclaimableBytes = reclaimable,
                    recommendationReason = "Exact 100% SHA-256 bitwise identical copy"
                )
            )
            group.forEach { processedIds.add(it.photo.id) }
        }

        // 2. Exact File Size & Resolution matches for remaining photos
        val sizeGroups = photos.filter { it.photo.id !in processedIds && it.photo.sizeBytes > 0 }
            .groupBy { "${it.photo.sizeBytes}_${it.photo.width}_${it.photo.height}" }
            .filter { it.value.size > 1 }
        for ((_, group) in sizeGroups) {
            val sorted = group.sortedWith(
                compareByDescending<PhotoWithTags> { it.tags.size }
                    .thenByDescending { it.photo.takenAt ?: it.photo.addedAt }
            )
            val keep = sorted.first()
            val duplicates = sorted.drop(1)
            val reclaimable = duplicates.sumOf { it.photo.sizeBytes }

            clusters.add(
                DuplicateCluster(
                    id = "size_${keep.photo.id}",
                    keepPhoto = keep,
                    duplicatePhotos = duplicates,
                    matchType = DuplicateMatchType.EXACT_SIZE_NAME,
                    reclaimableBytes = reclaimable,
                    recommendationReason = "Identical file size (${keep.photo.sizeBytes / 1024}KB) & resolution (${keep.photo.width}x${keep.photo.height})"
                )
            )
            group.forEach { processedIds.add(it.photo.id) }
        }

        // 3. Visual similarity matching via 64-bit dHash for remaining photos
        val remaining = photos.filter { it.photo.id !in processedIds }
        for (i in remaining.indices) {
            val photoA = remaining[i]
            if (photoA.photo.id in processedIds) continue

            val hashA = photoHashMap[photoA.photo.id] ?: continue
            val matchedDuplicates = mutableListOf<PhotoWithTags>()

            for (j in (i + 1) until remaining.size) {
                val photoB = remaining[j]
                if (photoB.photo.id in processedIds) continue

                val hashB = photoHashMap[photoB.photo.id] ?: continue
                val dist = hammingDistance(hashA, hashB)
                if (dist <= maxHammingDistance) {
                    matchedDuplicates.add(photoB)
                    processedIds.add(photoB.photo.id)
                }
            }

            if (matchedDuplicates.isNotEmpty()) {
                processedIds.add(photoA.photo.id)
                val allInCluster = listOf(photoA) + matchedDuplicates

                // Find the best photo to keep
                var currentBest = allInCluster.first()
                for (other in allInCluster.drop(1)) {
                    currentBest = evaluateRecommendation(currentBest, other).first
                }

                val duplicates = allInCluster.filter { it.photo.id != currentBest.photo.id }
                val reclaimable = duplicates.sumOf { it.photo.sizeBytes }
                val primaryReason = generateReason(currentBest, duplicates.first())

                clusters.add(
                    DuplicateCluster(
                        id = "visual_${currentBest.photo.id}",
                        keepPhoto = currentBest,
                        duplicatePhotos = duplicates,
                        matchType = DuplicateMatchType.VISUAL_SIMILARITY,
                        reclaimableBytes = reclaimable,
                        recommendationReason = primaryReason
                    )
                )
            }
        }

        return clusters
    }
}
