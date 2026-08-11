package com.orgista.openreader.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryItemTest {
    @Test
    fun ebookFormatTakesPriorityOverZeroDuration() {
        assertEquals(BookFormat.Ebook, BookFormat.from(ebookFormat = "epub", durationSeconds = 0.0))
    }

    @Test
    fun positiveDurationMapsToAudiobook() {
        assertEquals(BookFormat.Audiobook, BookFormat.from(ebookFormat = null, durationSeconds = 7200.0))
    }
}
