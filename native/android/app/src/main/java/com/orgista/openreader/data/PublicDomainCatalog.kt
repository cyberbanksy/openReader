package com.orgista.openreader.data

import com.orgista.openreader.domain.BookFormat
import com.orgista.openreader.domain.BookSource
import com.orgista.openreader.domain.CatalogAvailability
import com.orgista.openreader.domain.CatalogMerger
import com.orgista.openreader.domain.CatalogSourceKind
import com.orgista.openreader.domain.LibraryBook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import javax.xml.parsers.DocumentBuilderFactory

enum class PublicDomainCatalog {
    Gutenberg,
    StandardEbooks,
}

data class PublicDomainSourceReference(
    val catalog: PublicDomainCatalog,
    val url: String,
) {
    val encoded: String
        get() = when (catalog) {
            PublicDomainCatalog.Gutenberg -> "gutenberg:$url"
            PublicDomainCatalog.StandardEbooks -> "standard-ebooks:$url"
        }

    init {
        validateCatalogUrl(catalog, url, acquisition = catalog == PublicDomainCatalog.StandardEbooks)
    }

    companion object {
        fun parse(value: String): PublicDomainSourceReference = when {
            value.startsWith("gutenberg:") -> PublicDomainSourceReference(
                PublicDomainCatalog.Gutenberg,
                value.removePrefix("gutenberg:"),
            )
            value.startsWith("standard-ebooks:") -> PublicDomainSourceReference(
                PublicDomainCatalog.StandardEbooks,
                value.removePrefix("standard-ebooks:"),
            )
            else -> throw IllegalArgumentException("The public-domain book reference is invalid.")
        }
    }
}

object OpdsCatalogParser {
    private const val ATOM = "http://www.w3.org/2005/Atom"

    fun parseGutenbergSearch(xml: String): List<LibraryBook> = entries(xml).mapNotNull { entry ->
        val detailUrl = entry.directText("id") ?: return@mapNotNull null
        if (!GUTENBERG_DETAIL.matches(detailUrl)) return@mapNotNull null
        val title = entry.directText("title") ?: return@mapNotNull null
        val creator = entry.directText("content")?.takeUnless { it.endsWith(" downloads") }
            ?: "Unknown author"
        val reference = PublicDomainSourceReference(PublicDomainCatalog.Gutenberg, detailUrl)
        publicDomainBook(
            reference = reference,
            title = title,
            creator = creator,
            sourceName = "Project Gutenberg",
        )
    }

    fun parseStandardEbooks(xml: String): List<LibraryBook> = entries(xml).mapNotNull { entry ->
        val title = entry.directText("title") ?: return@mapNotNull null
        val creator = entry.directChild("author")?.directText("name") ?: "Unknown author"
        val acquisition = preferredLink(entry, EPUB_TYPES) ?: return@mapNotNull null
        val resolved = URI(STANDARD_EBOOKS_FEED).resolve(acquisition.href).toString()
        val reference = PublicDomainSourceReference(PublicDomainCatalog.StandardEbooks, resolved)
        publicDomainBook(
            reference = reference,
            title = title,
            creator = creator,
            sourceName = "Standard Ebooks",
            coverUrl = preferredLink(entry, IMAGE_TYPES)?.let { URI(STANDARD_EBOOKS_FEED).resolve(it.href).toString() },
        )
    }

    fun preferredAcquisition(xml: String, baseUrl: String): String {
        val candidates = entries(xml).flatMap { entry -> entry.links(EPUB_TYPES) }
        val preferred = candidates.minByOrNull(::acquisitionPriority)
            ?: throw IllegalArgumentException("This catalog entry does not offer an EPUB download.")
        return URI(baseUrl).resolve(preferred.href).toString()
    }

    private fun publicDomainBook(
        reference: PublicDomainSourceReference,
        title: String,
        creator: String,
        sourceName: String,
        coverUrl: String? = null,
    ) = LibraryBook(
        id = "public-${reference.catalog.name.lowercase()}-${stableId(reference.url)}",
        libraryId = "public-domain",
        title = title,
        creator = creator,
        format = BookFormat.Ebook,
        coverUrl = coverUrl,
        sources = listOf(
            BookSource(
                id = reference.encoded,
                name = sourceName,
                kind = CatalogSourceKind.PublicDomain,
                availability = CatalogAvailability.Available,
                detail = "Public domain in the USA",
            ),
        ),
    )

    private fun entries(xml: String): List<Element> {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            runCatching { isXIncludeAware = false }
            runCatching { setExpandEntityReferences(false) }
            runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
            runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
            runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
        }
        val document = factory.newDocumentBuilder().parse(
            ByteArrayInputStream(xml.toByteArray(StandardCharsets.UTF_8)),
        )
        val nodes = document.getElementsByTagNameNS(ATOM, "entry")
        return (0 until nodes.length).mapNotNull { nodes.item(it) as? Element }
    }

    private fun preferredLink(entry: Element, types: Set<String>): OpdsLink? =
        entry.links(types).minByOrNull(::acquisitionPriority)

    private fun Element.links(types: Set<String>): List<OpdsLink> = directChildren("link").mapNotNull { link ->
        val type = link.getAttribute("type")
        val href = link.getAttribute("href")
        if (type !in types || href.isBlank()) return@mapNotNull null
        OpdsLink(
            href = href,
            title = link.getAttribute("title"),
            rel = link.getAttribute("rel"),
        )
    }

    private fun acquisitionPriority(link: OpdsLink): Int {
        val title = link.title.lowercase()
        val descriptor = "$title ${link.href.lowercase()}"
        return when {
            "compatible" in title -> 0
            "epub3" in descriptor && "images" in descriptor && "noimages" !in descriptor -> 1
            "advanced" !in title && "images" in descriptor && "noimages" !in descriptor -> 2
            "no image" in title || "noimages" in descriptor -> 4
            "advanced" !in title -> 3
            else -> 5
        }
    }

    private fun Element.directText(name: String): String? = directChild(name)
        ?.textContent
        ?.trim()
        ?.takeIf(String::isNotBlank)

    private fun Element.directChild(name: String): Element? = directChildren(name).firstOrNull()

    private fun Element.directChildren(name: String): List<Element> = buildList {
        val children = childNodes
        for (index in 0 until children.length) {
            val element = children.item(index) as? Element ?: continue
            if (element.localName == name && element.namespaceURI == ATOM) add(element)
        }
    }

    private data class OpdsLink(val href: String, val title: String, val rel: String)

    private val EPUB_TYPES = setOf("application/epub+zip")
    private val IMAGE_TYPES = setOf("image/jpeg", "image/png", "image/webp")
    private val GUTENBERG_DETAIL = Regex("https://www\\.gutenberg\\.org/ebooks/[0-9]+\\.opds")
}

class PublicDomainCatalogApi {
    private var standardEbooksCatalog: List<LibraryBook>? = null

    suspend fun search(query: String, standardEbooksEmail: String?): List<LibraryBook> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext emptyList()
        val gutenbergUrl = "$GUTENBERG_SEARCH?query=" +
            URLEncoder.encode(trimmed, StandardCharsets.UTF_8.toString())
        val gutenberg = OpdsCatalogParser.parseGutenbergSearch(requestText(gutenbergUrl))
        val standard = standardEbooksEmail?.takeIf(String::isNotBlank)?.let { email ->
            standardEbooksCatalog ?: loadStandardEbooks(email).also { standardEbooksCatalog = it }
        }.orEmpty().filter { book ->
            val normalized = trimmed.lowercase()
            book.title.lowercase().contains(normalized) || book.creator.lowercase().contains(normalized)
        }
        CatalogMerger.merge(gutenberg, standard)
    }

    suspend fun connectStandardEbooks(email: String): List<LibraryBook> = withContext(Dispatchers.IO) {
        val normalized = email.trim()
        require(normalized.contains('@')) { "Enter the email address used for Standard Ebooks." }
        loadStandardEbooks(normalized).also {
            require(it.isNotEmpty()) { "Standard Ebooks returned an empty catalog." }
            standardEbooksCatalog = it
        }
    }

    fun clearStandardEbooks() {
        standardEbooksCatalog = null
    }

    suspend fun downloadEbook(
        sourceId: String,
        standardEbooksEmail: String?,
        cacheDirectory: File,
    ): File = withContext(Dispatchers.IO) {
        val reference = PublicDomainSourceReference.parse(sourceId)
        require(cacheDirectory.mkdirs() || cacheDirectory.isDirectory) {
            "The ebook cache is unavailable."
        }
        val destination = File(cacheDirectory, "public-${stableId(sourceId)}.epub")
        if (destination.exists()) {
            runCatching { EbookFile.requireValid(destination) }
                .onSuccess { return@withContext destination }
            destination.delete()
        }

        val downloadUrl = when (reference.catalog) {
            PublicDomainCatalog.Gutenberg -> OpdsCatalogParser.preferredAcquisition(
                requestText(reference.url),
                reference.url,
            )
            PublicDomainCatalog.StandardEbooks -> reference.url
        }
        val partial = File(cacheDirectory, "${destination.name}.part")
        try {
            download(
                catalog = reference.catalog,
                initialUrl = downloadUrl,
                basicEmail = standardEbooksEmail,
                destination = partial,
            )
            EbookFile.requireValid(partial)
            if (!partial.renameTo(destination)) {
                partial.copyTo(destination, overwrite = true)
                partial.delete()
            }
            destination
        } finally {
            partial.delete()
        }
    }

    private fun loadStandardEbooks(email: String): List<LibraryBook> =
        OpdsCatalogParser.parseStandardEbooks(requestText(STANDARD_EBOOKS_FEED, email))

    private fun requestText(url: String, basicEmail: String? = null): String {
        val connection = openConnection(url, basicEmail)
        try {
            val status = connection.responseCode
            if (status !in 200..299) throw catalogException(status, connection)
            return responseStream(connection, status).readLimited(MAX_CATALOG_BYTES).toString(StandardCharsets.UTF_8)
        } finally {
            connection.disconnect()
        }
    }

    private fun download(
        catalog: PublicDomainCatalog,
        initialUrl: String,
        basicEmail: String?,
        destination: File,
    ) {
        var currentUrl = initialUrl
        repeat(MAX_REDIRECTS + 1) { redirectCount ->
            validateCatalogUrl(catalog, currentUrl, acquisition = true)
            val connection = openConnection(
                currentUrl,
                basicEmail.takeIf { catalog == PublicDomainCatalog.StandardEbooks },
                accept = "application/epub+zip, application/octet-stream",
            )
            try {
                val status = connection.responseCode
                if (status in REDIRECT_CODES) {
                    require(redirectCount < MAX_REDIRECTS) { "The ebook download redirected too many times." }
                    val location = connection.getHeaderField("Location")
                        ?: throw IllegalArgumentException("The ebook download returned an invalid redirect.")
                    currentUrl = URI(currentUrl).resolve(location).toString()
                    return@repeat
                }
                if (status !in 200..299) throw catalogException(status, connection)
                responseStream(connection, status).use { input ->
                    FileOutputStream(destination).use { output -> input.copyLimitedTo(output, MAX_EPUB_BYTES) }
                }
                return
            } finally {
                connection.disconnect()
            }
        }
        throw IllegalArgumentException("The ebook download could not be completed.")
    }

    private fun openConnection(
        url: String,
        basicEmail: String?,
        accept: String = "application/atom+xml;profile=opds-catalog, application/xml",
    ): HttpURLConnection = (URL(url).openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 10_000
        readTimeout = 60_000
        instanceFollowRedirects = false
        setRequestProperty("Accept", accept)
        setRequestProperty("User-Agent", "OpenReader/0.1 (personal OPDS client)")
        basicEmail?.let {
            val credential = Base64.getEncoder().encodeToString("$it:".toByteArray(StandardCharsets.UTF_8))
            setRequestProperty("Authorization", "Basic $credential")
        }
    }

    private fun catalogException(status: Int, connection: HttpURLConnection): ApiException {
        val message = when (status) {
            401 -> "Standard Ebooks did not accept that Patrons Circle email address."
            403 -> "The public-domain catalog denied this request."
            else -> "The public-domain catalog returned HTTP $status."
        }
        responseStream(connection, status).close()
        return ApiException(status, message)
    }

    private fun responseStream(connection: HttpURLConnection, status: Int): InputStream =
        if (status in 200..399) connection.inputStream else connection.errorStream ?: connection.inputStream
}

private fun validateCatalogUrl(catalog: PublicDomainCatalog, value: String, acquisition: Boolean) {
    val uri = runCatching { URI(value) }.getOrNull()
        ?: throw IllegalArgumentException("The public-domain catalog URL is invalid.")
    require(uri.scheme == "https" && uri.userInfo == null && uri.fragment == null) {
        "The public-domain catalog URL is invalid."
    }
    when (catalog) {
        PublicDomainCatalog.Gutenberg -> {
            require(uri.host == "www.gutenberg.org") { "The Project Gutenberg URL is invalid." }
            val validPath = if (acquisition) {
                uri.path.startsWith("/ebooks/") || uri.path.startsWith("/cache/epub/")
            } else {
                GUTENBERG_DETAIL_PATH.matches(uri.path)
            }
            require(validPath) { "The Project Gutenberg URL is invalid." }
        }
        PublicDomainCatalog.StandardEbooks -> {
            require(uri.host == "standardebooks.org" && uri.path.startsWith("/ebooks/")) {
                "The Standard Ebooks URL is invalid."
            }
        }
    }
}

private fun stableId(value: String): String = MessageDigest.getInstance("SHA-256")
    .digest(value.toByteArray(StandardCharsets.UTF_8))
    .take(12)
    .joinToString("") { byte -> "%02x".format(byte) }

private fun InputStream.readLimited(maxBytes: Long): ByteArray {
    val output = java.io.ByteArrayOutputStream()
    copyLimitedTo(output, maxBytes)
    return output.toByteArray()
}

private fun InputStream.copyLimitedTo(output: java.io.OutputStream, maxBytes: Long) {
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0L
    while (true) {
        val count = read(buffer)
        if (count < 0) break
        total += count
        require(total <= maxBytes) { "The catalog response is larger than OpenReader allows." }
        output.write(buffer, 0, count)
    }
}

private const val GUTENBERG_SEARCH = "https://www.gutenberg.org/ebooks/search.opds/"
private const val STANDARD_EBOOKS_FEED = "https://standardebooks.org/feeds/opds"
private val GUTENBERG_DETAIL_PATH = Regex("/ebooks/[0-9]+\\.opds")
private val REDIRECT_CODES = setOf(301, 302, 303, 307, 308)
private const val MAX_REDIRECTS = 4
private const val MAX_CATALOG_BYTES = 12L * 1024 * 1024
private const val MAX_EPUB_BYTES = 250L * 1024 * 1024
