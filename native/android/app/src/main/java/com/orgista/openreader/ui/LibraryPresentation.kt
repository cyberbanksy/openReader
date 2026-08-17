package com.orgista.openreader.ui

import com.orgista.openreader.domain.BookFormat
import com.orgista.openreader.domain.BookSource
import com.orgista.openreader.domain.CatalogAvailability
import com.orgista.openreader.domain.LibraryBook

enum class AppDestination(val label: String) {
    Home("Home"),
    Library("Library"),
    Browse("Browse"),
    Sources("Sources"),
}

enum class LibrarySection(val title: String, val subtitle: String) {
    Continue("Continue", "Pick up where you left off"),
    Ready("Ready now", "Downloaded and ready to open"),
    Arriving("Requests & downloads", "Titles on their way to your library"),
}

data class LibraryGroup(
    val section: LibrarySection,
    val books: List<LibraryBook>,
)

fun primaryBookAction(book: LibraryBook, connected: Boolean): String {
    if (book.isDemo) return "Connect"
    val source = primaryBookSource(book)
    if (source?.kind == com.orgista.openreader.domain.CatalogSourceKind.PublicDomain &&
        source.availability == CatalogAvailability.Available
    ) {
        return "Download"
    }
    if (source?.kind == com.orgista.openreader.domain.CatalogSourceKind.Bundled &&
        source.availability == CatalogAvailability.Ready
    ) {
        return if (book.format == BookFormat.Audiobook) "Listen" else "Read"
    }
    if (!connected) return "Connect"
    val availability = source?.availability ?: CatalogAvailability.Ready
    return when (availability) {
        CatalogAvailability.Ready,
        CatalogAvailability.Borrowed,
        -> if (book.format == BookFormat.Audiobook) "Listen" else "Read"
        CatalogAvailability.Available -> "Borrow"
        CatalogAvailability.OnHold -> "Place hold"
        CatalogAvailability.Requestable -> "Request"
        CatalogAvailability.Requested -> "Requested"
        CatalogAvailability.Downloading -> "Downloading"
        CatalogAvailability.Importing -> "Importing"
        CatalogAvailability.Unavailable -> "Unavailable"
    }
}

fun primaryBookActionEnabled(book: LibraryBook, connected: Boolean): Boolean =
    primaryBookAction(book, connected) !in setOf("Requested", "Downloading", "Importing", "Unavailable")

fun primaryBookSource(book: LibraryBook): BookSource? =
    book.sources.minByOrNull { source -> availabilityPriority(source.availability) }

private fun availabilityPriority(availability: CatalogAvailability): Int = when (availability) {
    CatalogAvailability.Ready,
    CatalogAvailability.Borrowed,
    -> 0
    CatalogAvailability.Downloading -> 1
    CatalogAvailability.Importing -> 2
    CatalogAvailability.Requested -> 3
    CatalogAvailability.Requestable -> 4
    CatalogAvailability.Available -> 5
    CatalogAvailability.OnHold -> 6
    CatalogAvailability.Unavailable -> 7
}

fun libraryCollection(books: List<LibraryBook>): List<LibraryBook> = books
    .filter { book ->
        book.isDemo || primaryBookSource(book)?.availability in setOf(
            CatalogAvailability.Ready,
            CatalogAvailability.Borrowed,
            CatalogAvailability.Requested,
            CatalogAvailability.Downloading,
            CatalogAvailability.Importing,
            CatalogAvailability.OnHold,
        )
    }
    .sortedWith(compareBy<LibraryBook>(::librarySectionRank).then(libraryBookComparator))

fun organizeLibrary(books: List<LibraryBook>, filter: LibraryFilter): List<LibraryGroup> {
    val filtered = filterLibraryBooks(libraryCollection(books), "", filter)
    return LibrarySection.entries.mapNotNull { section ->
        filtered.filter { librarySection(it) == section }
            .takeIf(List<LibraryBook>::isNotEmpty)
            ?.let { LibraryGroup(section, it.sortedWith(libraryBookComparator)) }
    }
}

fun matchingFormats(books: List<LibraryBook>, selected: LibraryBook): List<LibraryBook> {
    val selectedTitle = normalizeTitleIdentity(selected.title)
    val selectedCreator = normalizeIdentity(selected.creator)
    return books.filter { candidate ->
        normalizeTitleIdentity(candidate.title) == selectedTitle &&
            normalizeIdentity(candidate.creator) == selectedCreator
    }
        .distinctBy(LibraryBook::format)
        .sortedBy { if (it.format == BookFormat.Ebook) 0 else 1 }
        .ifEmpty { listOf(selected) }
}

private fun librarySection(book: LibraryBook): LibrarySection {
    val availability = primaryBookSource(book)?.availability
    return when {
        book.progress > 0f && availability in setOf(CatalogAvailability.Ready, CatalogAvailability.Borrowed) ->
            LibrarySection.Continue
        book.isDemo || availability in setOf(CatalogAvailability.Ready, CatalogAvailability.Borrowed) ->
            LibrarySection.Ready
        else -> LibrarySection.Arriving
    }
}

private fun librarySectionRank(book: LibraryBook): Int = librarySection(book).ordinal

private val libraryBookComparator = compareBy<LibraryBook>(
    { it.creator.lowercase() },
    { seriesParts(it).first },
    { seriesParts(it).second },
    { it.title.lowercase() },
    { it.format.ordinal },
)

private fun seriesParts(book: LibraryBook): Pair<String, Int> {
    val label = book.sources.asSequence()
        .mapNotNull(BookSource::detail)
        .map { it.substringBefore(" · ") }
        .firstOrNull { SERIES_NUMBER.matches(it) }
        ?: book.title
    val match = SERIES_NUMBER.matchEntire(label)
    return if (match == null) {
        label.lowercase() to Int.MAX_VALUE
    } else {
        match.groupValues[1].trim().lowercase() to (match.groupValues[2].toIntOrNull() ?: Int.MAX_VALUE)
    }
}

private val SERIES_NUMBER = Regex("(.+?)\\s+#(\\d+)")

fun filterLibraryBooks(
    books: List<LibraryBook>,
    query: String,
    filter: LibraryFilter,
): List<LibraryBook> {
    val matchingFormat = when (filter) {
        LibraryFilter.All -> books
        LibraryFilter.Audiobooks -> books.filter { it.format == BookFormat.Audiobook }
        LibraryFilter.Ebooks -> books.filter { it.format == BookFormat.Ebook }
    }
    val normalizedQuery = normalizeSearchText(query)
    if (normalizedQuery.isEmpty()) return matchingFormat
    return matchingFormat.filter { book ->
        listOfNotNull(
            book.title,
            book.creator,
            book.narrator,
            *book.sources.mapNotNull { it.detail }.toTypedArray(),
        ).any { searchable ->
            " ${normalizeSearchText(searchable)} ".contains(" $normalizedQuery ")
        }
    }
}

private fun normalizeSearchText(value: String): String = value
    .lowercase()
    .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
    .trim()

private fun normalizeIdentity(value: String): String = value
    .lowercase()
    .filter(Char::isLetterOrDigit)

private fun normalizeTitleIdentity(value: String): String = normalizeIdentity(
    value.replace(EDITION_SUFFIX, ""),
)

private val EDITION_SUFFIX = Regex(
    "\\s*\\((?:version|edition)\\s+\\d+\\)\\s*$",
    RegexOption.IGNORE_CASE,
)
