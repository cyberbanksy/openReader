package com.orgista.openreader.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.orgista.openreader.BuildConfig
import com.orgista.openreader.domain.LibraryBook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

data class CoverRequest(val url: String, val authorization: String? = null)

object CoverRequestResolver {
    fun resolve(book: LibraryBook, session: ServerSession?): List<CoverRequest> = buildList {
        val endpoint = book.coverEndpoint
        if (endpoint != null && endpoint.startsWith('/') && session != null) {
            add(CoverRequest(session.serverUrl + endpoint, "Bearer ${session.accessToken}"))
        }
        book.coverUrl
            ?.takeIf { it.startsWith("https://") || it.startsWith("http://") }
            ?.let { add(CoverRequest(it)) }
    }.distinctBy(CoverRequest::url)
}

object CoverImageLoader {
    private const val MAX_COVER_BYTES = 10L * 1024L * 1024L
    private val memory = ConcurrentHashMap<String, Bitmap>()

    suspend fun load(context: Context, book: LibraryBook): Bitmap? = withContext(Dispatchers.IO) {
        val session = SessionStore(context).load()
        for (request in CoverRequestResolver.resolve(book, session)) {
            memory[request.url]?.let { return@withContext it }
            val cached = cacheFile(context, request.url)
            decode(cached)?.let {
                memory[request.url] = it
                return@withContext it
            }
            val downloaded = runCatching { download(request, cached) }.getOrNull()
            if (downloaded != null) {
                memory[request.url] = downloaded
                return@withContext downloaded
            }
        }
        null
    }

    private fun download(request: CoverRequest, destination: File): Bitmap? {
        destination.parentFile?.mkdirs()
        val partial = File.createTempFile("${destination.name}.", ".part", destination.parentFile)
        val connection = URL(request.url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 20_000
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("Accept", "image/avif,image/webp,image/jpeg,image/png,image/*")
            connection.setRequestProperty("User-Agent", "OpenReader/${BuildConfig.VERSION_NAME} (Android)")
            request.authorization?.let { connection.setRequestProperty("Authorization", it) }
            if (connection.responseCode !in 200..299) return null
            val declaredLength = connection.contentLengthLong
            if (declaredLength > MAX_COVER_BYTES) return null
            connection.inputStream.use { input ->
                FileOutputStream(partial).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= MAX_COVER_BYTES) { "The cover image is too large." }
                        output.write(buffer, 0, count)
                    }
                }
            }
            val bitmap = decode(partial) ?: return null
            if (!partial.renameTo(destination)) {
                partial.copyTo(destination, overwrite = true)
            }
            return bitmap
        } finally {
            connection.disconnect()
            partial.delete()
        }
    }

    private fun decode(file: File): Bitmap? = file.takeIf(File::isFile)
        ?.let { BitmapFactory.decodeFile(it.absolutePath) }

    private fun cacheFile(context: Context, url: String): File {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(url.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return File(File(context.cacheDir, "covers"), "$digest.image")
    }
}
