package com.orgista.openreader.domain

/**
 * A small, always-available ebook+audiobook pair shipped as app assets so features like
 * audiobook follow-along work out of the box, with no server connection required. Sourced
 * generically through [CatalogSourceKind.Bundled] like any other catalog source — nothing
 * about the reader, playback, or follow-along pipeline is specific to this content.
 */
object BundledContent {
    const val EBOOK_ID = "bundled-quiet-hour-ebook"
    const val AUDIOBOOK_ID = "bundled-quiet-hour-audio"

    private const val EBOOK_ASSET_PATH = "sample/the_quiet_hour.epub"
    private const val AUDIOBOOK_ASSET_PATH = "sample/the_quiet_hour.m4a"
    private const val AUDIOBOOK_DURATION_SECONDS = 48.42

    private val source = BookSource(
        id = "bundled-sample",
        name = "Included sample",
        kind = CatalogSourceKind.Bundled,
        availability = CatalogAvailability.Ready,
        detail = "Included with OpenReader",
    )

    val books = listOf(
        LibraryBook(
            id = EBOOK_ID,
            libraryId = "bundled",
            title = "The Quiet Hour",
            creator = "OpenReader Sample",
            format = BookFormat.Ebook,
            sources = listOf(source),
        ),
        LibraryBook(
            id = AUDIOBOOK_ID,
            libraryId = "bundled",
            title = "The Quiet Hour",
            creator = "OpenReader Sample",
            narrator = "Sample narration",
            format = BookFormat.Audiobook,
            durationSeconds = AUDIOBOOK_DURATION_SECONDS,
            sources = listOf(source),
        ),
    )

    fun ebookAssetPath(bookId: String): String? = EBOOK_ASSET_PATH.takeIf { bookId == EBOOK_ID }

    data class AudiobookAsset(val assetPath: String, val durationSeconds: Double)

    fun audiobookAsset(bookId: String): AudiobookAsset? =
        AudiobookAsset(AUDIOBOOK_ASSET_PATH, AUDIOBOOK_DURATION_SECONDS).takeIf { bookId == AUDIOBOOK_ID }
}
