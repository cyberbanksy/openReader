package com.orgista.openreader.ui

import com.orgista.openreader.domain.BookFormat
import com.orgista.openreader.domain.BookSource
import com.orgista.openreader.domain.CatalogAvailability
import com.orgista.openreader.domain.CatalogSourceKind
import com.orgista.openreader.domain.LibraryBook
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryPresentationTest {
    private val ebook = LibraryBook(
        id = "ebook",
        libraryId = "library",
        title = "Alice's Adventures in Wonderland",
        creator = "Lewis Carroll",
        format = BookFormat.Ebook,
        progress = 0.12f,
    )
    private val audiobook = LibraryBook(
        id = "audio",
        libraryId = "library",
        title = "Through the Looking-Glass",
        creator = "Lewis Carroll",
        format = BookFormat.Audiobook,
    )

    @Test
    fun destinationsMatchTheRefreshedDesign() {
        assertEquals(
            listOf("Home", "Library", "Browse", "Sources"),
            AppDestination.entries.map(AppDestination::label),
        )
    }

    @Test
    fun primaryActionReflectsFormatForReadyBooks() {
        assertEquals("Read", primaryBookAction(ebook.copy(sources = listOf(readySource())), connected = true))
        assertEquals("Listen", primaryBookAction(audiobook.copy(sources = listOf(readySource())), connected = true))
        assertEquals(
            "Read",
            primaryBookAction(
                ebook.copy(sources = listOf(arrSource(CatalogAvailability.Importing), readySource())),
                connected = true,
            ),
        )
    }

    @Test
    fun primaryActionReflectsExternalAvailabilityWithoutRetailLanguage() {
        assertEquals("Borrow", primaryBookAction(ebook.copy(sources = listOf(publicLibrary(CatalogAvailability.Available))), true))
        assertEquals("Download", primaryBookAction(ebook.copy(sources = listOf(publicDomain())), false))
        assertEquals("Place hold", primaryBookAction(ebook.copy(sources = listOf(publicLibrary(CatalogAvailability.OnHold))), true))
        assertEquals("Request", primaryBookAction(ebook.copy(sources = listOf(arrSource(CatalogAvailability.Requestable))), true))
        assertEquals("Requested", primaryBookAction(ebook.copy(sources = listOf(arrSource(CatalogAvailability.Requested))), true))
        assertEquals("Importing", primaryBookAction(ebook.copy(sources = listOf(arrSource(CatalogAvailability.Importing))), true))
    }

    @Test
    fun primarySourcePrefersAnExistingRequestOverDuplicateRequestableMetadata() {
        val requestable = arrSource(CatalogAvailability.Requestable).copy(id = "duplicate")
        val requested = arrSource(CatalogAvailability.Requested).copy(id = "core")
        val merged = ebook.copy(sources = listOf(requestable, requested))

        assertEquals(requested, primaryBookSource(merged))
        assertEquals("Requested", primaryBookAction(merged, true))
    }

    @Test
    fun librarySearchMatchesTitleOrCreatorIgnoringCase() {
        assertEquals(listOf(ebook), filterLibraryBooks(listOf(ebook, audiobook), "adventures", LibraryFilter.All))
        assertEquals(listOf(ebook, audiobook), filterLibraryBooks(listOf(ebook, audiobook), "LEWIS", LibraryFilter.All))
    }

    @Test
    fun librarySearchMatchesArrSeriesMetadata() {
        val junie = ebook.copy(
            title = "Aloha-ha-ha!",
            creator = "Barbara Park",
            sources = listOf(
                arrSource(CatalogAvailability.Requested).copy(
                    detail = "Junie B. Jones #26 · Search requested",
                ),
            ),
        )

        assertEquals(
            listOf(junie),
            filterLibraryBooks(listOf(junie, audiobook), "Junie B Jones", LibraryFilter.All),
        )
    }

    @Test
    fun punctuationInsensitiveSearchStillHonorsWordBoundaries() {
        val boo = ebook.copy(id = "boo", title = "Boo...and I Mean It!")
        val book = ebook.copy(id = "book", title = "Judy Moody, Book Quiz Whiz")

        assertEquals(
            listOf(boo),
            filterLibraryBooks(listOf(boo, book), "Boo", LibraryFilter.All),
        )
    }

    @Test
    fun librarySearchAndFormatFilterCompose() {
        assertEquals(
            listOf(audiobook),
            filterLibraryBooks(listOf(ebook, audiobook), "looking", LibraryFilter.Audiobooks),
        )
        assertEquals(
            emptyList<LibraryBook>(),
            filterLibraryBooks(listOf(ebook, audiobook), "looking", LibraryFilter.Ebooks),
        )
    }

    @Test
    fun libraryKeepsOwnedAndArrivingBooksButLeavesDiscoveryInBrowse() {
        val ready = ebook.copy(id = "ready", sources = listOf(readySource()))
        val requested = ebook.copy(id = "requested", sources = listOf(arrSource(CatalogAvailability.Requested)))
        val requestable = ebook.copy(id = "requestable", sources = listOf(arrSource(CatalogAvailability.Requestable)))
        val available = ebook.copy(id = "available", sources = listOf(publicLibrary(CatalogAvailability.Available)))

        assertEquals(
            listOf(ready, requested),
            libraryCollection(listOf(requestable, requested, available, ready)),
        )
    }

    @Test
    fun libraryGroupsProgressReadyAndArrivingInThatOrder() {
        val continuing = ebook.copy(id = "continue", progress = 0.4f, sources = listOf(readySource()))
        val ready = ebook.copy(id = "ready", progress = 0f, sources = listOf(readySource()))
        val arriving = ebook.copy(id = "arriving", sources = listOf(arrSource(CatalogAvailability.Downloading)))

        val groups = organizeLibrary(listOf(arriving, ready, continuing), LibraryFilter.All)

        assertEquals(
            listOf(LibrarySection.Continue, LibrarySection.Ready, LibrarySection.Arriving),
            groups.map(LibraryGroup::section),
        )
        assertEquals(listOf(continuing), groups[0].books)
        assertEquals(listOf(ready), groups[1].books)
        assertEquals(listOf(arriving), groups[2].books)
    }

    @Test
    fun seriesSequenceSortsNumericallyWithinLibraryGroups() {
        val ten = ebook.copy(
            id = "ten",
            title = "Party Animal",
            sources = listOf(readySource().copy(detail = "Junie B. Jones #10")),
        )
        val two = ebook.copy(
            id = "two",
            title = "Little Monkey Business",
            sources = listOf(readySource().copy(detail = "Junie B. Jones #2")),
        )

        assertEquals(listOf(two, ten), organizeLibrary(listOf(ten, two), LibraryFilter.All).single().books)
    }

    @Test
    fun matchingFormatsMakeReadAndListenEasyToSwitch() {
        val ebookReady = ebook.copy(sources = listOf(readySource()))
        val audioReady = ebookReady.copy(id = "audio-ready", format = BookFormat.Audiobook)
        val unrelated = audiobook.copy(sources = listOf(readySource()))

        val options = matchingFormats(listOf(unrelated, audioReady, ebookReady), ebookReady)

        assertEquals(listOf(BookFormat.Ebook, BookFormat.Audiobook), options.map(LibraryBook::format))
    }

    @Test
    fun matchingFormatsIgnoresHarmlessEditionSuffixes() {
        val ebookReady = ebook.copy(sources = listOf(readySource()))
        val audioReady = ebookReady.copy(
            id = "audio-ready",
            title = "Alice's Adventures in Wonderland (version 6)",
            format = BookFormat.Audiobook,
        )

        assertEquals(
            listOf(BookFormat.Ebook, BookFormat.Audiobook),
            matchingFormats(listOf(audioReady, ebookReady), ebookReady).map(LibraryBook::format),
        )
    }

    @Test
    fun directActionIsEnabledOnlyWhenItCanDoSomethingNow() {
        assertTrue(primaryBookActionEnabled(ebook.copy(sources = listOf(readySource())), true))
        assertTrue(primaryBookActionEnabled(ebook.copy(sources = listOf(arrSource(CatalogAvailability.Requestable))), true))
        assertFalse(primaryBookActionEnabled(ebook.copy(sources = listOf(arrSource(CatalogAvailability.Requested))), true))
        assertFalse(primaryBookActionEnabled(ebook.copy(sources = listOf(arrSource(CatalogAvailability.Downloading))), true))
    }

    private fun readySource() = BookSource(
        id = "abs",
        name = "Audiobookshelf",
        kind = CatalogSourceKind.Audiobookshelf,
        availability = CatalogAvailability.Ready,
    )

    private fun publicLibrary(availability: CatalogAvailability) = BookSource(
        id = "library",
        name = "Public library",
        kind = CatalogSourceKind.PublicLibrary,
        availability = availability,
    )

    private fun publicDomain() = BookSource(
        id = "gutenberg:https://www.gutenberg.org/ebooks/11.opds",
        name = "Project Gutenberg",
        kind = CatalogSourceKind.PublicDomain,
        availability = CatalogAvailability.Available,
    )

    private fun arrSource(availability: CatalogAvailability) = BookSource(
        id = "readarr",
        name = "Readarr",
        kind = CatalogSourceKind.Arr,
        availability = availability,
    )
}
