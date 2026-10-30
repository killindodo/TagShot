package com.tagshot.app.domain

import java.text.Normalizer
import java.util.Locale

sealed interface TagResult {
    data class Valid(val display: String, val normalized: String) : TagResult
    sealed interface Invalid : TagResult {
        data object Empty : Invalid
        data object TooLong : Invalid
    }
}

object TagNormalizer {

    /**
     * Determines whether a given Unicode code point is considered whitespace.
     * Checks [Character.isWhitespace] and general category Z* (Space, Line, and Paragraph Separators).
     */
    fun isUnicodeWhitespace(codePoint: Int): Boolean {
        val type = Character.getType(codePoint).toByte()
        return Character.isWhitespace(codePoint) ||
                type == Character.SPACE_SEPARATOR ||
                type == Character.LINE_SEPARATOR ||
                type == Character.PARAGRAPH_SEPARATOR
    }

    /**
     * Replaces every contiguous run of Unicode whitespace characters with a single ASCII space (' ').
     */
    private fun replaceRunsOfUnicodeWhitespace(input: String): String {
        val sb = StringBuilder(input.length)
        var inWhitespace = false
        var i = 0
        while (i < input.length) {
            val cp = input.codePointAt(i)
            val charCount = Character.charCount(cp)
            if (isUnicodeWhitespace(cp)) {
                if (!inWhitespace) {
                    sb.append(' ')
                    inWhitespace = true
                }
            } else {
                sb.appendCodePoint(cp)
                inWhitespace = false
            }
            i += charCount
        }
        return sb.toString()
    }

    /**
     * Step function S(x) per Section 5.1:
     * 1. Normalizer.normalize(x, Normalizer.Form.NFKC)
     * 2. Remove all leading '#' characters
     * 3. Replace every run of Unicode whitespace with single ASCII space
     * 4. trim()
     */
    fun step(x: String): String {
        val nfkc = Normalizer.normalize(x, Normalizer.Form.NFKC)
        var noLeadingHash = nfkc
        while (noLeadingHash.startsWith('#')) {
            noLeadingHash = noLeadingHash.substring(1)
        }
        val collapsedWs = replaceRunsOfUnicodeWhitespace(noLeadingHash)
        return collapsedWs.trim()
    }

    /**
     * Applies [f] until output equals input, up to [maxIterations] times.
     * Throws [IllegalStateException] if it does not converge within [maxIterations].
     */
    fun fixpoint(initial: String, maxIterations: Int = 4, f: (String) -> String): String {
        var current = initial
        for (i in 0 until maxIterations) {
            val next = f(current)
            if (next == current) {
                return next
            }
            current = next
        }
        throw IllegalStateException("Fixpoint did not converge within $maxIterations iterations for input: $initial")
    }

    /**
     * Normalizes a raw tag string per Section 5.1 specification.
     *
     * display = fixpoint(S)
     * normalized = fixpoint(lowercase(Locale.ROOT) ∘ S)
     */
    fun normalize(raw: String): TagResult {
        val display = fixpoint(raw, maxIterations = 4) { step(it) }
        val normalized = fixpoint(raw, maxIterations = 4) { step(it).lowercase(Locale.ROOT) }

        return when {
            normalized.isEmpty() -> TagResult.Invalid.Empty
            normalized.codePointCount(0, normalized.length) > MAX_TAG_CODEPOINTS -> TagResult.Invalid.TooLong
            else -> TagResult.Valid(display = display, normalized = normalized)
        }
    }
}
