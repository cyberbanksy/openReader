package com.orgista.openreader.data

import org.junit.Assert.assertEquals
import org.junit.Test

class EbookFileTest {
    @Test
    fun usesAudiobookshelfEbookEndpoint() {
        assertEquals(
            "/api/items/book-123/file/987654321/download",
            EbookFile.endpoint("book-123", "987654321"),
        )
    }

    @Test
    fun createsStableSafeCacheName() {
        assertEquals("book_123.epub", EbookFile.cacheName("book/123"))
        assertEquals("book.epub", EbookFile.cacheName(".."))
    }
}
