package com.orgista.openreader.data

import org.junit.Assert.assertEquals
import org.junit.Test

class EbookFileTest {
    @Test
    fun usesAudiobookshelfEbookEndpoint() {
        assertEquals("/api/items/book-123/ebook", EbookFile.endpoint("book-123"))
    }

    @Test
    fun createsStableSafeCacheName() {
        assertEquals("book_123.epub", EbookFile.cacheName("book/123"))
        assertEquals("book.epub", EbookFile.cacheName(".."))
    }
}
