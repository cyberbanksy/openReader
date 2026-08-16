package com.orgista.openreader.data

import java.io.File
import java.util.zip.ZipFile

object EbookFile {
    fun endpoint(bookId: String, fileId: String): String {
        requireSafeId(bookId, "book")
        requireSafeId(fileId, "ebook file")
        return "/api/items/$bookId/file/$fileId/download"
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

    private fun requireSafeId(value: String, label: String) {
        require(value.isNotBlank()) { "The $label ID is missing." }
        require(value.all { it.isLetterOrDigit() || it == '-' || it == '_' }) {
            "The $label ID contains unsupported characters."
        }
    }
}
