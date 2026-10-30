package com.tagshot.app.domain

import java.text.Normalizer
import java.util.Locale

data class FilterPhoto(
    val id: Long,
    val normalizedTags: Set<String>,
    val tagIds: Set<Long>
)

data class FilterState(
    val query: String = "",
    val selectedTagIds: Set<Long> = emptySet(),
    val untaggedOnly: Boolean = false
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
     * Tests if a photo contains all selected tag IDs:
     * selectedTagIds ⊆ photo.tagIds
     */
    fun matchChips(photo: FilterPhoto, selectedTagIds: Set<Long>): Boolean {
        if (selectedTagIds.isEmpty()) return true
        return photo.tagIds.containsAll(selectedTagIds)
    }

    /**
     * Tests if untagged criteria is satisfied:
     * (!untaggedOnly) || photo.normalizedTags.isEmpty()
     */
    fun matchUntagged(photo: FilterPhoto, untaggedOnly: Boolean): Boolean {
        return !untaggedOnly || photo.normalizedTags.isEmpty()
    }

    /**
     * Evaluates total visibility predicate:
     * visible(p) = matchText(p) ∧ matchChips(p) ∧ matchUntagged(p)
     */
    fun matches(photo: FilterPhoto, tokens: List<String>, state: FilterState): Boolean {
        return matchText(photo, tokens) &&
                matchChips(photo, state.selectedTagIds) &&
                matchUntagged(photo, state.untaggedOnly)
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
