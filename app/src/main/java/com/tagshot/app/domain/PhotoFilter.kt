package com.tagshot.app.domain

import java.text.Normalizer
import java.util.Locale

enum class TagMatchMode {
    ANY,
    ALL
}

data class FilterPhoto(
    val id: Long,
    val normalizedTags: Set<String>,
    val tagIds: Set<Long>,
    val timestamp: Long = 0L
)

data class FilterState(
    val query: String = "",
    val selectedTagIds: Set<Long> = emptySet(),
    val untaggedOnly: Boolean = false,
    val matchMode: TagMatchMode = TagMatchMode.ANY,
    val startDate: Long? = null,
    val endDate: Long? = null
)

object PhotoFilter {

    /**
     * Extracts search tokens from a raw query string per Section 5.3:
     * 1. Split on whitespace.
     * 2. Normalize each token with NFKC.
     * 3. Strip leading '#' characters.
     * 4. Convert to lowercase with [Locale.ROOT].
     * 5. Drop empty tokens (tokens that were only '#' vanish).
     */
    fun extractTokens(query: String): List<String> {
        if (query.isBlank()) return emptyList()
        return query.split(Regex("\\s+")).mapNotNull { rawToken ->
            val nfkc = Normalizer.normalize(rawToken, Normalizer.Form.NFKC)
            val stripped = nfkc.dropWhile { it == '#' }
            val lower = stripped.lowercase(Locale.ROOT)
            if (lower.isEmpty()) null else lower
        }
    }

    /**
     * Tests if a photo matches the text search tokens:
     * ∀ t ∈ tokens: ∃ n ∈ photo.normalizedTags: n.contains(t)
     * (Empty tokens list yields true).
     */
    fun matchText(photo: FilterPhoto, tokens: List<String>): Boolean {
        if (tokens.isEmpty()) return true
        return tokens.all { token ->
            photo.normalizedTags.any { tag -> tag.contains(token) }
        }
    }

    /**
     * Tests if a photo matches selected tag IDs according to [matchMode]:
     * - When [TagMatchMode.ANY]: matches photos with at least one selected tag.
     * - When [TagMatchMode.ALL]: matches photos containing EVERY selected tag.
     * (Empty selectedTagIds yields true).
     */
    fun matchChips(
        photo: FilterPhoto,
        selectedTagIds: Set<Long>,
        matchMode: TagMatchMode = TagMatchMode.ANY
    ): Boolean {
        if (selectedTagIds.isEmpty()) return true
        return when (matchMode) {
            TagMatchMode.ANY -> photo.tagIds.any { selectedTagIds.contains(it) }
            TagMatchMode.ALL -> photo.tagIds.containsAll(selectedTagIds)
        }
    }

    /**
     * Filters photos by tag IDs and untagged criteria according to [matchMode].
     * - When [TagMatchMode.ANY]: matches photos with at least one selected tag.
     * - When [TagMatchMode.ALL]: matches photos containing EVERY selected tag.
     */
    fun filterByTags(
        photos: List<FilterPhoto>,
        tagIds: Set<Long>,
        untaggedOnly: Boolean = false,
        matchMode: TagMatchMode = TagMatchMode.ANY
    ): List<FilterPhoto> {
        return photos.filter { photo ->
            matchUntagged(photo, untaggedOnly) && matchChips(photo, tagIds, matchMode)
        }
    }

    /**
     * Generic overload of [filterByTags] allowing any item type with a mapper function.
     */
    fun <T> filterByTags(
        photos: List<T>,
        tagIds: Set<Long>,
        untaggedOnly: Boolean = false,
        matchMode: TagMatchMode = TagMatchMode.ANY,
        toFilterPhoto: (T) -> FilterPhoto
    ): List<T> {
        return photos.filter { item ->
            val photo = toFilterPhoto(item)
            matchUntagged(photo, untaggedOnly) && matchChips(photo, tagIds, matchMode)
        }
    }

    /**
     * Tests if untagged criteria is satisfied:
     * (!untaggedOnly) || photo.normalizedTags.isEmpty()
     */
    fun matchUntagged(photo: FilterPhoto, untaggedOnly: Boolean): Boolean {
        return !untaggedOnly || photo.normalizedTags.isEmpty()
    }

    /**
     * Tests if photo's timestamp falls within [startDate] and [endDate] bounds (inclusive).
     */
    fun matchDateRange(timestamp: Long, startDate: Long?, endDate: Long?): Boolean {
        if (startDate != null && timestamp < startDate) return false
        if (endDate != null && timestamp > endDate) return false
        return true
    }

    /**
     * Evaluates total visibility predicate:
     * visible(p) = matchText(p) ∧ matchChips(p, matchMode) ∧ matchUntagged(p) ∧ matchDateRange(p)
     */
    fun matches(photo: FilterPhoto, tokens: List<String>, state: FilterState): Boolean {
        return matchText(photo, tokens) &&
                matchChips(photo, state.selectedTagIds, state.matchMode) &&
                matchUntagged(photo, state.untaggedOnly) &&
                matchDateRange(photo.timestamp, state.startDate, state.endDate)
    }

    /**
     * Filters a list of photos according to [FilterState].
     * Preserves the relative order of the input list.
     */
    fun <T> filter(
        photos: List<T>,
        state: FilterState,
        toFilterPhoto: (T) -> FilterPhoto
    ): List<T> {
        val tokens = extractTokens(state.query)
        return photos.filter { item ->
            matches(toFilterPhoto(item), tokens, state)
        }
    }
}
