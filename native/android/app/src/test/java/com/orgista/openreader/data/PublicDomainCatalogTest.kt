package com.orgista.openreader.data

import com.orgista.openreader.domain.CatalogAvailability
import com.orgista.openreader.domain.CatalogSourceKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PublicDomainCatalogTest {
    @Test
    fun parsesGutenbergSearchResultsWithoutNavigationEntries() {
        val books = OpdsCatalogParser.parseGutenbergSearch(
            """
            <feed xmlns="http://www.w3.org/2005/Atom">
              <entry>
                <id>https://www.gutenberg.org/ebooks/authors/search.opds/?query=alice</id>
                <title>Authors</title>
                <content>144 author names match your search.</content>
              </entry>
              <entry>
                <id>https://www.gutenberg.org/ebooks/11.opds</id>
                <title>Alice's Adventures in Wonderland</title>
                <content>Lewis Carroll</content>
              </entry>
            </feed>
            """.trimIndent(),
        )

        assertEquals(1, books.size)
        assertEquals("Alice's Adventures in Wonderland", books.single().title)
        assertEquals("Lewis Carroll", books.single().creator)
        assertEquals(CatalogSourceKind.PublicDomain, books.single().sources.single().kind)
        assertEquals(CatalogAvailability.Available, books.single().sources.single().availability)
    }

    @Test
    fun prefersIllustratedEpub3FromGutenbergDetailFeed() {
        val acquisition = OpdsCatalogParser.preferredAcquisition(
            """
            <feed xmlns="http://www.w3.org/2005/Atom">
              <entry>
                <title>Alice's Adventures in Wonderland</title>
                <link type="application/epub+zip" rel="http://opds-spec.org/acquisition"
                  title="EPUB (no images, older E-readers)" href="https://www.gutenberg.org/ebooks/11.epub.noimages" />
              </entry>
              <entry>
                <title>Alice's Adventures in Wonderland</title>
                <link type="application/epub+zip" rel="http://opds-spec.org/acquisition"
                  title="EPUB3 (E-readers incl. Send-to-Kindle)" href="https://www.gutenberg.org/ebooks/11.epub3.images" />
              </entry>
            </feed>
            """.trimIndent(),
            baseUrl = "https://www.gutenberg.org/ebooks/11.opds",
        )

        assertEquals("https://www.gutenberg.org/ebooks/11.epub3.images", acquisition)
    }

    @Test
    fun parsesStandardEbooksAcquisitionFeed() {
        val books = OpdsCatalogParser.parseStandardEbooks(
            """
            <feed xmlns="http://www.w3.org/2005/Atom">
              <entry>
                <id>https://standardebooks.org/ebooks/jane-austen/pride-and-prejudice</id>
                <title>Pride and Prejudice</title>
                <author><name>Jane Austen</name></author>
                <link type="application/epub+zip" rel="http://opds-spec.org/acquisition/open-access"
                  title="Compatible epub"
                  href="/ebooks/jane-austen/pride-and-prejudice/downloads/jane-austen_pride-and-prejudice.epub?source=feed" />
              </entry>
            </feed>
            """.trimIndent(),
        )

        assertEquals(1, books.size)
        assertEquals("Pride and Prejudice", books.single().title)
        assertEquals("Jane Austen", books.single().creator)
        assertTrue(books.single().sources.single().id.startsWith("standard-ebooks:"))
    }

    @Test
    fun sourceReferencesRejectUnexpectedHosts() {
        val failure = runCatching {
            PublicDomainSourceReference.parse("gutenberg:https://example.test/ebooks/11.opds")
        }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
    }
}
