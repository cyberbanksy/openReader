package com.orgista.openreader.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FollowAlongPassageMapperTest {
    @Test
    fun centersTheActivePassageAndSkipsDuplicateNeighbors() {
        val passages = FollowAlongPassageMapper.fromTexts(
            listOf("Before", "Current passage", "Current passage", "After"),
            activeIndex = 1,
        )

        assertEquals("Before", passages.previous)
        assertEquals("Current passage", passages.current)
        assertEquals("After", passages.next)
    }

    @Test
    fun findsTheNearestTextWhenThePlaybackPositionIsAFrontMatterImage() {
        val passages = FollowAlongPassageMapper.fromTexts(
            listOf("", "", "The opening words of the story.", "What happens next."),
            activeIndex = 0,
        )

        assertEquals("The opening words of the story.", passages.current)
        assertEquals("What happens next.", passages.next)
    }

    @Test
    fun compactsWhitespaceAndBoundsLongPassages() {
        val passages = FollowAlongPassageMapper.fromTexts(
            listOf("  A   short previous line  ", "word ".repeat(80), "A next line"),
            activeIndex = 1,
        )

        assertEquals("A short previous line", passages.previous)
        assertFalse(passages.current.contains("  "))
        assertFalse(passages.current.length > 211)
        assertEquals("A next line", passages.next)
    }

    @Test
    fun parsesTheStringEncodedJavascriptArrayReturnedByReadium() {
        val parsed = FollowAlongDocumentTextParser.parse(
            "\"[\\\"A useful first paragraph.\\\",\\\"A useful second paragraph.\\\"]\"",
        )

        assertEquals(listOf("A useful first paragraph.", "A useful second paragraph."), parsed)
        assertTrue(FollowAlongDocumentTextParser.parse("null").isEmpty())
    }
}
