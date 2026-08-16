package com.orgista.openreader.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogMergerTest {
    @Test
    fun ownedAudiobookshelfItemAbsorbsMatchingArrSource() {
        val owned = book(
            id = "abs-1",
            title = "Judy Moody Gets Famous!",
            source = BookSource(
                id = "abs-library",
                name = "Audiobookshelf",
                kind = CatalogSourceKind.Audiobookshelf,
                availability = CatalogAvailability.Ready,
            ),
        )
        val managed = book(
            id = "arr-audiobook-42",
            title = "Judy Moody Gets Famous",
            source = BookSource(
                id = "audiobook:42",
                name = "Bookshelf · Audiobooks",
                kind = CatalogSourceKind.Arr,
                availability = CatalogAvailability.Importing,
            ),
        )

        val result = CatalogMerger.merge(listOf(owned), listOf(managed))

        assertEquals(1, result.size)
        assertEquals("abs-1", result.single().id)
        assertEquals(
            listOf(CatalogSourceKind.Audiobookshelf, CatalogSourceKind.Arr),
            result.single().sources.map(BookSource::kind),
        )
    }

    @Test
    fun differentFormatsRemainSeparate() {
        val ebook = book("ebook", "Judy Moody", BookFormat.Ebook, arrSource("ebook:1"))
        val audiobook = book("audio", "Judy Moody", BookFormat.Audiobook, arrSource("audiobook:1"))

        val result = CatalogMerger.merge(emptyList(), listOf(ebook, audiobook))

        assertEquals(2, result.size)
        assertTrue(result.any { it.format == BookFormat.Ebook })
        assertTrue(result.any { it.format == BookFormat.Audiobook })
    }

    @Test
    fun ownedItemUsesManagerCoverAsFallback() {
        val owned = book("abs-1", "Judy Moody", source = readySource())
        val managed = book("arr-1", "Judy Moody", source = arrSource("audiobook:1"))
            .copy(coverUrl = "https://example.test/judy.jpg")

        val result = CatalogMerger.merge(listOf(owned), listOf(managed))

        assertEquals("https://example.test/judy.jpg", result.single().coverUrl)
    }

    private fun book(
        id: String,
        title: String,
        format: BookFormat = BookFormat.Audiobook,
        source: BookSource,
    ) = LibraryBook(
        id = id,
        libraryId = "library",
        title = title,
        creator = "Megan McDonald",
        format = format,
        sources = listOf(source),
    )

    private fun arrSource(id: String) = BookSource(
        id = id,
        name = "Bookshelf",
        kind = CatalogSourceKind.Arr,
        availability = CatalogAvailability.Requestable,
    )

    private fun readySource() = BookSource(
        id = "abs-library",
        name = "Audiobookshelf",
        kind = CatalogSourceKind.Audiobookshelf,
        availability = CatalogAvailability.Ready,
    )
}
