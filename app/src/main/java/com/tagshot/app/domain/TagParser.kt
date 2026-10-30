package com.tagshot.app.domain

sealed interface ParseResult {
    data class Success(val tags: List<TagResult.Valid>) : ParseResult
    sealed interface Failure : ParseResult {
        data class TooLong(val rawText: String) : Failure
        data object TooMany : Failure
    }
}

object TagParser {

    /**
     * Parses comma- and newline-separated tag input per Section 5.2.
     *
     * Rules:
     * 1. Split on ',' and '\n'.
     * 2. Normalize each part using [TagNormalizer.normalize].
     * 3. Skip empty parts.
     * 4. If any part exceeds [MAX_TAG_CODEPOINTS], returns [ParseResult.Failure.TooLong] with the raw text.
     * 5. Deduplicate by `normalized`, keeping the first `display` casing and preserving relative order.
     * 6. If the count of unique tags exceeds [MAX_TAGS_PER_PHOTO], returns [ParseResult.Failure.TooMany].
     * 7. Otherwise, returns [ParseResult.Success] containing the list of valid tags.
     */
    fun parse(text: String): ParseResult {
        val parts = text.split(',', '\n')
        val seen = HashSet<String>()
        val tags = ArrayList<TagResult.Valid>()

        for (part in parts) {
            when (val result = TagNormalizer.normalize(part)) {
                is TagResult.Invalid.Empty -> continue
                is TagResult.Invalid.TooLong -> return ParseResult.Failure.TooLong(part)
                is TagResult.Valid -> {
                    if (seen.add(result.normalized)) {
                        tags.add(result)
                    }
                }
            }
        }

        if (tags.size > MAX_TAGS_PER_PHOTO) {
            return ParseResult.Failure.TooMany
        }

        return ParseResult.Success(tags)
    }
}
