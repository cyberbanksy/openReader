package com.orgista.openreader.data

import com.orgista.openreader.domain.BookFormat
import com.orgista.openreader.domain.CatalogAvailability
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class BookshelfCatalogParserTest {
    @Test
    fun monitoredUpdateIncludesEditionsRequiredByReadarr() {
        val editions = JSONArray("""[{"id": 501, "monitored": true}]""")

        val update = BookshelfRequestPayload.monitored(
            book = JSONObject("""{"id": 166, "title": "Judy Moody", "monitored": false}"""),
            editions = editions,
        )

        assertEquals(true, update.getBoolean("monitored"))
        assertEquals(501, update.getJSONArray("editions").getJSONObject(0).getInt("id"))
    }

    @Test
    fun unmonitoredBookWithoutFileIsRequestable() {
        val book = parseBook(monitored = false, fileCount = 0)

        assertEquals("arr-ebook-166", book.id)
        assertEquals("Judy Moody Was in a Mood. Not a Good Mood. A Bad Mood.", book.title)
        assertEquals("Megan McDonald", book.creator)
        assertEquals(BookFormat.Ebook, book.format)
        assertEquals("https://example.test/judy.jpg", book.coverUrl)
        assertEquals("ebook:166", book.sources.single().id)
        assertEquals(CatalogAvailability.Requestable, book.sources.single().availability)
    }

    @Test
    fun usesEditionCoverWhenSelectedBookHasNoDirectCover() {
        val book = BookshelfCatalogParser.parse(
            books = JSONArray(
                """[{"id":166,"title":"Judy Moody","monitored":true,"statistics":{"bookFileCount":0}}]""",
            ),
            authorNames = emptyMap(),
            managerId = "audiobook",
            managerName = "Bookshelf · Audiobooks",
            format = BookFormat.Audiobook,
            queuedBookIds = emptySet(),
            fallbackCoverUrls = mapOf(166 to "https://example.test/judy-edition.jpg"),
        ).single()

        assertEquals("https://example.test/judy-edition.jpg", book.coverUrl)
    }

    @Test
    fun prefersMonitoredEditionCoverThenFallsBackToAnyEdition() {
        val editions = JSONArray(
            """[
              {"id":1,"monitored":false,"images":[{"coverType":"cover","url":"https://example.test/first.jpg"}]},
              {"id":2,"monitored":true,"images":[{"coverType":"cover","url":"https://example.test/selected.jpg"}]}
            ]""",
        )

        assertEquals("https://example.test/selected.jpg", BookshelfCoverParser.fromEditions(editions))
    }

    @Test
    fun monitoredBookMovesThroughRequestedAndDownloadingStates() {
        val requested = parseBook(monitored = true, fileCount = 0)
        val downloading = parseBook(monitored = true, fileCount = 0, queuedBookIds = setOf(166))

        assertEquals(CatalogAvailability.Requested, requested.sources.single().availability)
        assertEquals(CatalogAvailability.Downloading, downloading.sources.single().availability)
    }

    @Test
    fun downloadedBookWaitsForAudiobookshelfImport() {
        val book = parseBook(monitored = true, fileCount = 1)

        assertEquals(CatalogAvailability.Importing, book.sources.single().availability)
    }

    @Test
    fun resolvesAuthorFromTheManagerAuthorCatalog() {
        val book = BookshelfCatalogParser.parse(
            books = JSONArray(
                """[
                  {
                    "id": 166,
                    "title": "Judy Moody Was in a Mood. Not a Good Mood. A Bad Mood.",
                    "authorId": 2,
                    "monitored": false,
                    "statistics": {"bookFileCount": 0}
                  }
                ]""",
            ),
            authorNames = mapOf(2 to "Megan McDonald"),
            managerId = "ebook",
            managerName = "Bookshelf · Ebooks",
            format = BookFormat.Ebook,
            queuedBookIds = emptySet(),
        ).single()

        assertEquals("Megan McDonald", book.creator)
    }

    private fun parseBook(
        monitored: Boolean,
        fileCount: Int,
        queuedBookIds: Set<Int> = emptySet(),
    ) = BookshelfCatalogParser.parse(
        books = JSONArray(
            """[
              {
                "id": 166,
                "title": "Judy Moody Was in a Mood. Not a Good Mood. A Bad Mood.",
                "seriesTitle": "Judy Moody #1",
                "monitored": $monitored,
                "author": {"authorName": "Megan McDonald"},
                "statistics": {"bookFileCount": $fileCount},
                "images": [{"remoteUrl": "https://example.test/judy.jpg", "coverType": "cover"}]
              }
            ]""",
        ),
        authorNames = emptyMap(),
        managerId = "ebook",
        managerName = "Bookshelf · Ebooks",
        format = BookFormat.Ebook,
        queuedBookIds = queuedBookIds,
    ).single()
}
