package com.tagshot.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TagParserTest {

    @Test
    fun testParseExampleFromSpec() {
        // Spec test: "a, b\nc,,A" -> [a, b, c] with first display kept
        val result = TagParser.parse("a, b\nc,,A")
        assertTrue(result is ParseResult.Success)
        val tags = (result as ParseResult.Success).tags
        assertEquals(3, tags.size)
        assertEquals(listOf("a", "b", "c"), tags.map { it.display })
        assertEquals(listOf("a", "b", "c"), tags.map { it.normalized })
    }

    @Test
    fun testPreservesDisplayOfFirstSeen() {
        val result = TagParser.parse("Receipt, receipt, RECEIPT, #receipt")
        assertTrue(result is ParseResult.Success)
        val tags = (result as ParseResult.Success).tags
        assertEquals(1, tags.size)
        assertEquals("Receipt", tags[0].display)
        assertEquals("receipt", tags[0].normalized)
    }

    @Test
    fun testTooLongPartFailsEntireParse() {
        val longTag = "x".repeat(33)
        val result = TagParser.parse("valid1, $longTag, valid2")
        assertTrue(result is ParseResult.Failure.TooLong)
        val failure = result as ParseResult.Failure.TooLong
        assertEquals(" $longTag", failure.rawText)
    }

    @Test
    fun testMaxTagsPerPhotoBoundary() {
        // Exactly 20 tags
        val exactly20 = (1..20).joinToString(", ") { "tag$it" }
        val successResult = TagParser.parse(exactly20)
        assertTrue(successResult is ParseResult.Success)
        assertEquals(20, (successResult as ParseResult.Success).tags.size)

        // 21 tags -> TooMany
        val twentyOne = (1..21).joinToString(", ") { "tag$it" }
        val failResult = TagParser.parse(twentyOne)
        assertTrue(failResult is ParseResult.Failure.TooMany)
    }

    @Test
    fun testEmptyAndWhitespaceInput() {
        val emptyResult = TagParser.parse("")
        assertTrue(emptyResult is ParseResult.Success)
        assertTrue((emptyResult as ParseResult.Success).tags.isEmpty())

        val commasOnly = TagParser.parse(",,, \n \n , ")
        assertTrue(commasOnly is ParseResult.Success)
        assertTrue((commasOnly as ParseResult.Success).tags.isEmpty())
    }

    @Test
    fun testOrderPreservation() {
        val input = "zebra, apple, mango, banana"
        val result = TagParser.parse(input)
        assertTrue(result is ParseResult.Success)
        val tags = (result as ParseResult.Success).tags
        assertEquals(listOf("zebra", "apple", "mango", "banana"), tags.map { it.display })
    }
}
