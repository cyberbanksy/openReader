package com.orgista.openreader.data

import com.orgista.openreader.domain.BookFormat
import com.orgista.openreader.domain.LibraryBook
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CoverRequestResolverTest {
    @Test
    fun triesAuthenticatedAudiobookshelfCoverBeforeManagerFallback() {
        val requests = CoverRequestResolver.resolve(
            book = book(
                coverEndpoint = "/api/items/abs-1/cover",
                coverUrl = "https://example.test/judy.jpg",
            ),
            session = ServerSession(
                serverUrl = "http://books.test",
                username = "reader",
                accessToken = "secret-token",
                refreshToken = null,
            ),
        )

        assertEquals("http://books.test/api/items/abs-1/cover", requests[0].url)
        assertEquals("Bearer secret-token", requests[0].authorization)
        assertEquals("https://example.test/judy.jpg", requests[1].url)
        assertNull(requests[1].authorization)
    }

    @Test
    fun ignoresRelativeOrUnsupportedManagerCoverUrls() {
        val requests = CoverRequestResolver.resolve(
            book = book(coverEndpoint = null, coverUrl = "file:///tmp/cover.jpg"),
            session = null,
        )

        assertEquals(emptyList<CoverRequest>(), requests)
    }

    private fun book(coverEndpoint: String?, coverUrl: String?) = LibraryBook(
        id = "book-1",
        libraryId = "library",
        title = "Judy Moody",
        creator = "Megan McDonald",
        format = BookFormat.Ebook,
        coverEndpoint = coverEndpoint,
        coverUrl = coverUrl,
    )
}
