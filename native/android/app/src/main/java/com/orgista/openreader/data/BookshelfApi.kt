package com.orgista.openreader.data

import com.orgista.openreader.domain.BookFormat
import com.orgista.openreader.domain.BookSource
import com.orgista.openreader.domain.CatalogAvailability
import com.orgista.openreader.domain.CatalogSourceKind
import com.orgista.openreader.domain.LibraryBook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class BookshelfApi {
    suspend fun test(connections: ArrConnections) = withContext(Dispatchers.IO) {
        coroutineScope {
            listOf(connections.ebook, connections.audiobook).map { credential ->
                async {
                    val status = requestObject(credential, "/api/v1/system/status")
                    require(status.optString("appName").equals("Readarr", ignoreCase = true)) {
                        "${credential.managerName} did not identify as a Readarr-compatible server."
                    }
                }
            }.awaitAll()
        }
    }

    suspend fun catalog(connections: ArrConnections): List<LibraryBook> = withContext(Dispatchers.IO) {
        coroutineScope {
            listOf(connections.ebook, connections.audiobook).map { credential ->
                async { catalog(credential) }
            }.awaitAll().flatten()
        }.sortedWith(compareBy<LibraryBook> { it.format }.thenBy { it.title.lowercase() })
    }

    suspend fun request(connections: ArrConnections, sourceId: String) = withContext(Dispatchers.IO) {
        val reference = ArrBookReference.parse(sourceId)
        val credential = connections.credential(reference.managerId)
        val book = requestObject(credential, "/api/v1/book/${reference.bookId}")
        val editions = requestArray(credential, "/api/v1/edition?bookId=${reference.bookId}")
        requestObject(
            credential = credential,
            path = "/api/v1/book/${reference.bookId}",
            method = "PUT",
            body = BookshelfRequestPayload.monitored(book, editions),
        )
        requestObject(
            credential = credential,
            path = "/api/v1/command",
            method = "POST",
            body = JSONObject()
                .put("name", "BookSearch")
                .put("bookIds", JSONArray().put(reference.bookId)),
        )
    }

    private fun catalog(credential: ArrCredential): List<LibraryBook> {
        val authors = requestArray(credential, "/api/v1/author")
        val authorNames = (0 until authors.length()).mapNotNull { index ->
            val author = authors.optJSONObject(index) ?: return@mapNotNull null
            val id = author.optInt("id")
            val name = author.optString("authorName")
            if (id > 0 && name.isNotBlank()) id to name else null
        }.toMap()
        val queue = requestObject(
            credential,
            "/api/v1/queue?page=1&pageSize=1000&includeUnknownBookItems=true",
        )
        val records = queue.optJSONArray("records") ?: JSONArray()
        val queuedBookIds = (0 until records.length()).mapNotNull { index ->
            records.optJSONObject(index)?.optInt("bookId")?.takeIf { it > 0 }
        }.toSet()
        val books = requestArray(credential, "/api/v1/book")
        val fallbackCoverUrls = (0 until books.length()).mapNotNull { index ->
            val book = books.optJSONObject(index) ?: return@mapNotNull null
            val id = book.optInt("id")
            if (id <= 0 || BookshelfCoverParser.fromImages(book.optJSONArray("images")) != null) {
                return@mapNotNull null
            }
            val editions = runCatching {
                requestArray(credential, "/api/v1/edition?bookId=$id")
            }.getOrNull() ?: return@mapNotNull null
            BookshelfCoverParser.fromEditions(editions)?.let { id to it }
        }.toMap()
        return BookshelfCatalogParser.parse(
            books = books,
            authorNames = authorNames,
            managerId = credential.managerId,
            managerName = credential.managerName,
            format = credential.format,
            queuedBookIds = queuedBookIds,
            fallbackCoverUrls = fallbackCoverUrls,
        )
    }

    private fun requestObject(
        credential: ArrCredential,
        path: String,
        method: String = "GET",
        body: JSONObject? = null,
    ): JSONObject = JSONObject(request(credential, path, method, body))

    private fun requestArray(credential: ArrCredential, path: String): JSONArray =
        JSONArray(request(credential, path))

    private fun request(
        credential: ArrCredential,
        path: String,
        method: String = "GET",
        body: JSONObject? = null,
    ): String {
        val connection = URL("${credential.serverUrl}$path").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 10_000
            connection.readTimeout = 30_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("X-Api-Key", credential.apiKey)
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val status = connection.responseCode
            val responseText = responseStream(connection, status).bufferedReader().use { it.readText() }
            if (status !in 200..299) {
                val message = runCatching {
                    JSONObject(responseText).optString("message")
                }.getOrNull()?.takeIf(String::isNotBlank)
                    ?: "${credential.managerName} returned HTTP $status."
                throw ApiException(status, message)
            }
            return responseText.ifBlank { "{}" }
        } finally {
            connection.disconnect()
        }
    }

    private fun responseStream(connection: HttpURLConnection, status: Int): InputStream =
        if (status in 200..399) connection.inputStream else connection.errorStream
            ?: connection.inputStream
}

object BookshelfRequestPayload {
    fun monitored(book: JSONObject, editions: JSONArray): JSONObject =
        book.put("monitored", true).put("editions", editions)
}

object BookshelfCatalogParser {
    fun parse(
        books: JSONArray,
        authorNames: Map<Int, String>,
        managerId: String,
        managerName: String,
        format: BookFormat,
        queuedBookIds: Set<Int>,
        fallbackCoverUrls: Map<Int, String> = emptyMap(),
    ): List<LibraryBook> = (0 until books.length()).mapNotNull { index ->
        val book = books.optJSONObject(index) ?: return@mapNotNull null
        val id = book.optInt("id")
        val title = book.optString("title")
        if (id <= 0 || title.isBlank()) return@mapNotNull null
        val monitored = book.optBoolean("monitored", false)
        val fileCount = book.optJSONObject("statistics")?.optInt("bookFileCount", 0) ?: 0
        val availability = when {
            fileCount > 0 -> CatalogAvailability.Importing
            id in queuedBookIds -> CatalogAvailability.Downloading
            monitored -> CatalogAvailability.Requested
            else -> CatalogAvailability.Requestable
        }
        val author = book.optJSONObject("author")?.optString("authorName")
            ?.takeIf(String::isNotBlank)
            ?: authorNames[book.optInt("authorId")]
            ?: "Unknown author"
        val series = book.optString("seriesTitle").takeIf { it.isNotBlank() && it != "null" }
        val stateDetail = when (availability) {
            CatalogAvailability.Requestable -> "Ready to request"
            CatalogAvailability.Requested -> "Search requested"
            CatalogAvailability.Downloading -> "Download in progress"
            CatalogAvailability.Importing -> "Waiting for Audiobookshelf scan"
            else -> null
        }
        LibraryBook(
            id = "arr-$managerId-$id",
            libraryId = "arr-$managerId",
            title = title,
            creator = author,
            format = format,
            description = book.optString("overview").takeIf { it.isNotBlank() && it != "null" },
            coverUrl = BookshelfCoverParser.fromImages(book.optJSONArray("images"))
                ?: fallbackCoverUrls[id],
            sources = listOf(
                BookSource(
                    id = "$managerId:$id",
                    name = managerName,
                    kind = CatalogSourceKind.Arr,
                    availability = availability,
                    detail = listOfNotNull(series, stateDetail).joinToString(" · ").takeIf(String::isNotBlank),
                ),
            ),
        )
    }
}

object BookshelfCoverParser {
    fun fromEditions(editions: JSONArray): String? {
        val ordered = (0 until editions.length()).mapNotNull(editions::optJSONObject)
            .sortedByDescending { it.optBoolean("monitored", false) }
        return ordered.firstNotNullOfOrNull { fromImages(it.optJSONArray("images")) }
    }

    fun fromImages(images: JSONArray?): String? {
        if (images == null) return null
        return (0 until images.length()).firstNotNullOfOrNull { index ->
            val image = images.optJSONObject(index) ?: return@firstNotNullOfOrNull null
            if (!image.optString("coverType").equals("cover", ignoreCase = true)) {
                return@firstNotNullOfOrNull null
            }
            image.optString("remoteUrl").takeIf(String::isNotBlank)
                ?: image.optString("url").takeIf(String::isNotBlank)
        }
    }
}

data class ArrBookReference(val managerId: String, val bookId: Int) {
    companion object {
        fun parse(sourceId: String): ArrBookReference {
            val parts = sourceId.split(':', limit = 2)
            require(parts.size == 2) { "The ARR book reference is invalid." }
            val bookId = parts[1].toIntOrNull()
            require(bookId != null && bookId > 0) { "The ARR book reference is invalid." }
            return ArrBookReference(parts[0], bookId)
        }
    }
}
