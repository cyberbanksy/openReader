package com.orgista.openreader.domain

enum class BookFormat {
    Audiobook,
    Ebook;

    companion object {
        fun from(ebookFormat: String?, durationSeconds: Double): BookFormat =
            if (!ebookFormat.isNullOrBlank()) Ebook
            else if (durationSeconds > 0.0) Audiobook
            else Ebook
    }
}

enum class CatalogSourceKind {
    Audiobookshelf,
    Arr,
    PublicLibrary,
    PublicDomain,
}

enum class CatalogAvailability {
    Ready,
    Available,
    Borrowed,
    OnHold,
    Requestable,
    Requested,
    Downloading,
    Importing,
    Unavailable,
}

data class BookSource(
    val id: String,
    val name: String,
    val kind: CatalogSourceKind,
    val availability: CatalogAvailability,
    val detail: String? = null,
)

data class LibraryBook(
    val id: String,
    val libraryId: String,
    val title: String,
    val creator: String,
    val narrator: String? = null,
    val format: BookFormat,
    val durationSeconds: Double = 0.0,
    val progress: Float = 0f,
    val description: String? = null,
    val coverEndpoint: String? = null,
    val coverUrl: String? = null,
    val sources: List<BookSource> = emptyList(),
    val isDemo: Boolean = false,
)

object CatalogMerger {
    fun merge(owned: List<LibraryBook>, managed: List<LibraryBook>): List<LibraryBook> {
        val merged = owned.toMutableList()
        managed.forEach { managedBook ->
            val index = merged.indexOfFirst { candidate ->
                candidate.format == managedBook.format &&
                    normalize(candidate.title) == normalize(managedBook.title) &&
                    normalize(candidate.creator) == normalize(managedBook.creator)
            }
            if (index == -1) {
                merged += managedBook
            } else {
                val ownedBook = merged[index]
                merged[index] = ownedBook.copy(
                    description = ownedBook.description ?: managedBook.description,
                    coverUrl = ownedBook.coverUrl ?: managedBook.coverUrl,
                    sources = (ownedBook.sources + managedBook.sources)
                        .distinctBy { source -> source.kind to source.id },
                )
            }
        }
        return merged.sortedWith(compareBy<LibraryBook> { it.format }.thenBy { it.title.lowercase() })
    }

    private fun normalize(value: String): String = value
        .lowercase()
        .filter(Char::isLetterOrDigit)
}

object DemoCatalog {
    private val publicDomainSource = BookSource(
        id = "preview-public-domain",
        name = "Public domain",
        kind = CatalogSourceKind.PublicDomain,
        availability = CatalogAvailability.Available,
        detail = "Preview catalog",
    )

    val books = listOf(
        LibraryBook(
            id = "demo-alice-audio",
            libraryId = "demo",
            title = "Alice's Adventures in Wonderland",
            creator = "Lewis Carroll",
            narrator = "LibriVox",
            format = BookFormat.Audiobook,
            durationSeconds = 10_038.0,
            progress = 0.34f,
            sources = listOf(publicDomainSource),
            isDemo = true,
        ),
        LibraryBook(
            id = "demo-alice-ebook",
            libraryId = "demo",
            title = "Alice's Adventures in Wonderland",
            creator = "Lewis Carroll",
            format = BookFormat.Ebook,
            progress = 0.18f,
            sources = listOf(publicDomainSource),
            isDemo = true,
        ),
        LibraryBook(
            id = "demo-secret-garden",
            libraryId = "demo",
            title = "The Secret Garden",
            creator = "Frances Hodgson Burnett",
            format = BookFormat.Ebook,
            sources = listOf(publicDomainSource),
            isDemo = true,
        ),
        LibraryBook(
            id = "demo-treasure-island",
            libraryId = "demo",
            title = "Treasure Island",
            creator = "Robert Louis Stevenson",
            format = BookFormat.Audiobook,
            durationSeconds = 18_900.0,
            sources = listOf(publicDomainSource),
            isDemo = true,
        ),
        LibraryBook(
            id = "demo-anne",
            libraryId = "demo",
            title = "Anne of Green Gables",
            creator = "L. M. Montgomery",
            format = BookFormat.Ebook,
            sources = listOf(publicDomainSource),
            isDemo = true,
        ),
        LibraryBook(
            id = "demo-wizard-oz",
            libraryId = "demo",
            title = "The Wonderful Wizard of Oz",
            creator = "L. Frank Baum",
            format = BookFormat.Audiobook,
            durationSeconds = 14_760.0,
            sources = listOf(publicDomainSource),
            isDemo = true,
        ),
    )
}
