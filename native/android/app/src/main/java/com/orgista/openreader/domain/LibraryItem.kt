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
    val isDemo: Boolean = false,
)

object DemoCatalog {
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
            isDemo = true,
        ),
        LibraryBook(
            id = "demo-alice-ebook",
            libraryId = "demo",
            title = "Alice's Adventures in Wonderland",
            creator = "Lewis Carroll",
            format = BookFormat.Ebook,
            progress = 0.18f,
            isDemo = true,
        ),
        LibraryBook(
            id = "demo-secret-garden",
            libraryId = "demo",
            title = "The Secret Garden",
            creator = "Frances Hodgson Burnett",
            format = BookFormat.Ebook,
            isDemo = true,
        ),
        LibraryBook(
            id = "demo-treasure-island",
            libraryId = "demo",
            title = "Treasure Island",
            creator = "Robert Louis Stevenson",
            format = BookFormat.Audiobook,
            durationSeconds = 18_900.0,
            isDemo = true,
        ),
        LibraryBook(
            id = "demo-anne",
            libraryId = "demo",
            title = "Anne of Green Gables",
            creator = "L. M. Montgomery",
            format = BookFormat.Ebook,
            isDemo = true,
        ),
        LibraryBook(
            id = "demo-wizard-oz",
            libraryId = "demo",
            title = "The Wonderful Wizard of Oz",
            creator = "L. Frank Baum",
            format = BookFormat.Audiobook,
            durationSeconds = 14_760.0,
            isDemo = true,
        ),
    )
}
