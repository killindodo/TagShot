package com.tagshot.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale
import java.util.Random

class TagNormalizerTest {

    @Test
    fun testLeadingHashAndSpaces() {
        val result = TagNormalizer.normalize("  #Receipt ")
        assertTrue(result is TagResult.Valid)
        val valid = result as TagResult.Valid
        assertEquals("Receipt", valid.display)
        assertEquals("receipt", valid.normalized)
    }

    @Test
    fun testMultipleLeadingHashesAndInternalWhitespace() {
        val result = TagNormalizer.normalize("##A   B")
        assertTrue(result is TagResult.Valid)
        val valid = result as TagResult.Valid
        assertEquals("A B", valid.display)
        assertEquals("a b", valid.normalized)
    }

    @Test
    fun testEmptyVariations() {
        val emptyInputs = listOf("", "#", "   ", "  ###  ", " \t \n ")
        for (input in emptyInputs) {
            val result = TagNormalizer.normalize(input)
            assertTrue("Expected Invalid.Empty for '$input'", result is TagResult.Invalid.Empty)
        }
    }

    @Test
    fun testCodePointLimits() {
        // 32 code points exactly
        val exactly32 = "a".repeat(32)
        val result32 = TagNormalizer.normalize(exactly32)
        assertTrue(result32 is TagResult.Valid)
        assertEquals(32, (result32 as TagResult.Valid).normalized.codePointCount(0, result32.normalized.length))

        // 33 code points -> TooLong
        val exactly33 = "a".repeat(33)
        val result33 = TagNormalizer.normalize(exactly33)
        assertTrue(result33 is TagResult.Invalid.TooLong)
    }

    @Test
    fun testEmojiCodePointCounting() {
        // "📷" is a single Unicode code point (U+1F4F7) even though it takes 2 UTF-16 chars (surrogate pair)
        val emojiTag = "📷"
        val result = TagNormalizer.normalize(emojiTag)
        assertTrue(result is TagResult.Valid)
        val valid = result as TagResult.Valid
        assertEquals("📷", valid.display)
        assertEquals("📷", valid.normalized)
        assertEquals(1, valid.normalized.codePointCount(0, valid.normalized.length))

        // 32 emojis is valid
        val thirtyTwoEmojis = "📷".repeat(32)
        val validEmojis = TagNormalizer.normalize(thirtyTwoEmojis)
        assertTrue(validEmojis is TagResult.Valid)

        // 33 emojis is TooLong
        val thirtyThreeEmojis = "📷".repeat(33)
        val tooManyEmojis = TagNormalizer.normalize(thirtyThreeEmojis)
        assertTrue(tooManyEmojis is TagResult.Invalid.TooLong)
    }

    @Test
    fun testFullWidthCharacters() {
        // Full-width characters "ＡＢＣ" normalized with NFKC should become ASCII "abc"
        val result = TagNormalizer.normalize("ＡＢＣ")
        assertTrue(result is TagResult.Valid)
        val valid = result as TagResult.Valid
        assertEquals("ABC", valid.display)
        assertEquals("abc", valid.normalized)
    }

    @Test
    fun testTurkishDottedIconvergence() {
        // Turkish dotted "İ" (U+0130) lowercase in ROOT decomposes to 'i' + combining dot
        val result = TagNormalizer.normalize("İ")
        assertTrue(result is TagResult.Valid)
        val valid = result as TagResult.Valid
        assertEquals("İ", valid.display)
        assertEquals("i\u0307", valid.normalized)

        // Normalizing the normalized string again must be idempotent
        val secondPass = TagNormalizer.normalize(valid.normalized)
        assertTrue(secondPass is TagResult.Valid)
        assertEquals(valid.normalized, (secondPass as TagResult.Valid).normalized)
    }

    @Test
    fun testPropertyIdempotenceAndConvergence() {
        val alphabet = listOf(
            " ", "\t", "#", "e", "é", "e\u0301", "Ａ", "Ｂ", "📷", "İ", "ı", "a", "b", "c"
        )
        val rng = Random(42)

        for (i in 0 until 10000) {
            val len = rng.nextInt(31)
            val sb = java.lang.StringBuilder()
            for (j in 0 until len) {
                sb.append(alphabet[rng.nextInt(alphabet.size)])
            }
            val raw = sb.toString()

            // 1. Must converge within 4 iterations without throwing IllegalStateException
            val result = TagNormalizer.normalize(raw)

            if (result is TagResult.Valid) {
                // 2. Idempotence: normalize(normalize(x).normalized).normalized == normalize(x).normalized
                val secondResult = TagNormalizer.normalize(result.normalized)
                assertTrue("Second pass must be valid", secondResult is TagResult.Valid)
                assertEquals(
                    "Idempotence failure for '$raw'",
                    result.normalized,
                    (secondResult as TagResult.Valid).normalized
                )

                // 3. display.lowercase(ROOT) normalized equals normalized
                val displayLowerNormalized = TagNormalizer.normalize(result.display.lowercase(Locale.ROOT))
                assertTrue("Display lower pass must be valid", displayLowerNormalized is TagResult.Valid)
                assertEquals(
                    "display.lowercase(ROOT) normalized must match normalized for '$raw'",
                    result.normalized,
                    (displayLowerNormalized as TagResult.Valid).normalized
                )
            }
        }
    }
}
