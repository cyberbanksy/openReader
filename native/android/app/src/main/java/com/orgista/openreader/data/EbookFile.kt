package com.orgista.openreader.data

import java.io.File
import java.util.zip.ZipFile

object EbookFile {
    fun endpoint(bookId: String): String {
        require(bookId.isNotBlank()) { "The book ID is missing." }
        require(bookId.all { it.isLetterOrDigit() || it == '-' || it == '_' }) {
            "The book ID contains unsupported characters."
        }
        return "/api/items/$bookId/ebook"
    }

    fun cacheName(bookId: String): String {
        val safeId = bookId
            .replace(Regex("[^A-Za-z0-9_-]+"), "_")
            .trim('.', '_', '-')
            .ifBlank { "book" }
        return "$safeId.epub"
    }

    fun requireValid(file: File) {
        require(file.isFile && file.length() > 0L) { "Audiobookshelf returned an empty ebook." }
        runCatching {
            ZipFile(file).use { archive ->
                requireNotNull(archive.getEntry("META-INF/container.xml")) {
                    "The downloaded file is not a valid EPUB."
                }
            }
        }.getOrElse { failure ->
            throw IllegalArgumentException(
                failure.message ?: "The downloaded file is not a valid EPUB.",
                failure,
            )
        }
    }
}
