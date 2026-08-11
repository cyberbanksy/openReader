package com.orgista.openreader.data

import android.os.Build
import com.orgista.openreader.BuildConfig
import com.orgista.openreader.domain.BookFormat
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

data class PlaybackTrack(
    val index: Int,
    val title: String,
    val url: String,
    val durationSeconds: Double,
)

data class PlaybackDescriptor(
    val sessionId: String,
    val title: String,
    val creator: String,
    val coverUrl: String,
    val currentTimeSeconds: Double,
    val tracks: List<PlaybackTrack>,
)

class AudiobookshelfApi {
    suspend fun login(serverInput: String, username: String, password: String): ServerSession =
        withContext(Dispatchers.IO) {
            val serverUrl = ServerAddress.normalize(serverInput)
            require(username.isNotBlank()) { "Enter your username." }
            val payload = JSONObject()
                .put("username", username.trim())
                .put("password", password)
            val response = request(
                serverUrl = serverUrl,
                path = "/login",
                method = "POST",
                body = payload,
                headers = mapOf("x-return-tokens" to "true"),
            )
            val user = response.getJSONObject("user")
            val accessToken = user.optString("accessToken")
            require(accessToken.isNotBlank()) { "The server did not return an access token." }
            ServerSession(
                serverUrl = serverUrl,
                username = user.optString("username", username.trim()),
                accessToken = accessToken,
                refreshToken = user.optString("refreshToken").takeIf(String::isNotBlank),
            )
        }

    suspend fun refresh(session: ServerSession): ServerSession = withContext(Dispatchers.IO) {
        val refreshToken = requireNotNull(session.refreshToken) { "No refresh token is available." }
        val response = request(
            serverUrl = session.serverUrl,
            path = "/auth/refresh",
            method = "POST",
            headers = mapOf("x-refresh-token" to refreshToken),
        )
        val user = response.getJSONObject("user")
        session.copy(
            accessToken = user.getString("accessToken"),
            refreshToken = user.optString("refreshToken").takeIf(String::isNotBlank) ?: refreshToken,
        )
    }

    suspend fun catalog(session: ServerSession): List<LibraryBook> = withContext(Dispatchers.IO) {
        val response = authorizedRequest(session, "/api/libraries")
        val libraries = response.optJSONArray("libraries") ?: JSONArray()
        coroutineScope {
            (0 until libraries.length()).map { index ->
                async {
                    val library = libraries.getJSONObject(index)
                    val id = library.getString("id")
                    val name = library.optString("name")
                    val items = authorizedRequest(
                        session,
                        "/api/libraries/$id/items?limit=100&sort=media.metadata.title&desc=0",
                    )
                    parseItems(id, name, items)
                }
            }.awaitAll().flatten()
        }.sortedWith(compareBy<LibraryBook> { it.format }.thenBy { it.title.lowercase() })
    }

    suspend fun startPlayback(session: ServerSession, book: LibraryBook): PlaybackDescriptor =
        withContext(Dispatchers.IO) {
            require(book.format == BookFormat.Audiobook) { "Only audiobooks can be played." }
            val deviceInfo = JSONObject()
                .put("deviceId", "openreader-${Build.MANUFACTURER}-${Build.MODEL}")
                .put("manufacturer", Build.MANUFACTURER)
                .put("model", Build.MODEL)
                .put("sdkVersion", Build.VERSION.SDK_INT)
                .put("clientVersion", BuildConfig.VERSION_NAME)
            val payload = JSONObject()
                .put("mediaPlayer", "openreader-media3")
                .put("forceDirectPlay", true)
                .put("forceTranscode", false)
                .put("deviceInfo", deviceInfo)
            val response = authorizedRequest(
                session = session,
                path = "/api/items/${book.id}/play",
                method = "POST",
                body = payload,
            )
            val sessionId = response.getString("id")
            val tracksJson = response.optJSONArray("audioTracks") ?: JSONArray()
            val playMethod = response.optInt("playMethod", 0)
            val tracks = (0 until tracksJson.length()).map { index ->
                val track = tracksJson.getJSONObject(index)
                val trackIndex = track.optInt("index", index + 1)
                val path = if (playMethod == 0) {
                    "/public/session/$sessionId/track/$trackIndex"
                } else {
                    track.getString("contentUrl")
                }
                PlaybackTrack(
                    index = trackIndex,
                    title = track.optString("title", "Part ${index + 1}"),
                    url = "${session.serverUrl}$path",
                    durationSeconds = track.optDouble("duration", 0.0),
                )
            }
            PlaybackDescriptor(
                sessionId = sessionId,
                title = response.optString("displayTitle", book.title),
                creator = response.optString("displayAuthor", book.creator),
                coverUrl = "${session.serverUrl}/api/items/${book.id}/cover",
                currentTimeSeconds = response.optDouble("currentTime", 0.0),
                tracks = tracks,
            )
        }

    private fun parseItems(libraryId: String, libraryName: String, response: JSONObject): List<LibraryBook> {
        val results = response.optJSONArray("results") ?: JSONArray()
        return (0 until results.length()).mapNotNull { index ->
            val item = results.optJSONObject(index) ?: return@mapNotNull null
            val media = item.optJSONObject("media") ?: return@mapNotNull null
            val metadata = media.optJSONObject("metadata") ?: JSONObject()
            val id = item.optString("id")
            val title = metadata.optString("title")
            if (id.isBlank() || title.isBlank()) return@mapNotNull null
            val duration = media.optDouble("duration", 0.0)
            val format = BookFormat.from(media.optString("ebookFormat"), duration)
            val progress = item.optJSONObject("userMediaProgress")?.optDouble("progress", 0.0)
                ?.toFloat()?.coerceIn(0f, 1f) ?: 0f
            LibraryBook(
                id = id,
                libraryId = libraryId,
                title = title,
                creator = metadata.optString("authorName", "Unknown author"),
                narrator = metadata.optString("narratorName").takeIf(String::isNotBlank),
                format = format,
                durationSeconds = duration,
                progress = progress,
                description = metadata.optString("description").takeIf(String::isNotBlank),
                coverEndpoint = "/api/items/$id/cover",
            )
        }
    }

    private fun authorizedRequest(
        session: ServerSession,
        path: String,
        method: String = "GET",
        body: JSONObject? = null,
    ): JSONObject = request(
        serverUrl = session.serverUrl,
        path = path,
        method = method,
        body = body,
        headers = mapOf("Authorization" to "Bearer ${session.accessToken}"),
    )

    private fun request(
        serverUrl: String,
        path: String,
        method: String = "GET",
        body: JSONObject? = null,
        headers: Map<String, String> = emptyMap(),
    ): JSONObject {
        val connection = URL("$serverUrl$path").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 10_000
            connection.readTimeout = 20_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Accept", "application/json")
            headers.forEach(connection::setRequestProperty)
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val status = connection.responseCode
            val responseText = responseStream(connection, status).bufferedReader().use { it.readText() }
            if (status !in 200..299) {
                val message = runCatching { JSONObject(responseText).optString("error") }.getOrNull()
                    ?.takeIf(String::isNotBlank)
                    ?: "Audiobookshelf returned HTTP $status."
                throw ApiException(status, message)
            }
            return if (responseText.isBlank() || responseText == "OK") JSONObject() else JSONObject(responseText)
        } finally {
            connection.disconnect()
        }
    }

    private fun responseStream(connection: HttpURLConnection, status: Int): InputStream =
        if (status in 200..399) connection.inputStream else connection.errorStream
            ?: connection.inputStream
}

class ApiException(val statusCode: Int, message: String) : IllegalStateException(message)
