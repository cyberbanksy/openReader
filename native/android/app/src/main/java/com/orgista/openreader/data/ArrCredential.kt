package com.orgista.openreader.data

import com.orgista.openreader.domain.BookFormat

data class ArrCredential(
    val serverUrl: String,
    val apiKey: String,
    val format: BookFormat,
) {
    val managerId: String
        get() = if (format == BookFormat.Ebook) "ebook" else "audiobook"

    val managerName: String
        get() = if (format == BookFormat.Ebook) "Bookshelf · Ebooks" else "Bookshelf · Audiobooks"

    companion object {
        fun create(serverInput: String, apiKey: String, format: BookFormat): ArrCredential {
            val normalizedKey = apiKey.trim()
            require(normalizedKey.isNotBlank()) { "Enter the ${format.label()} manager API key." }
            return ArrCredential(
                serverUrl = ServerAddress.normalize(serverInput),
                apiKey = normalizedKey,
                format = format,
            )
        }
    }
}

data class ArrConnections(
    val ebook: ArrCredential,
    val audiobook: ArrCredential,
) {
    fun credential(managerId: String): ArrCredential = when (managerId) {
        ebook.managerId -> ebook
        audiobook.managerId -> audiobook
        else -> error("Unknown ARR manager: $managerId")
    }
}

private fun BookFormat.label(): String = if (this == BookFormat.Ebook) "ebook" else "audiobook"
