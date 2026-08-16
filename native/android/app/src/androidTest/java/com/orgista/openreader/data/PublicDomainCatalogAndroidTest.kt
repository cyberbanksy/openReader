package com.orgista.openreader.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PublicDomainCatalogAndroidTest {
    @Test
    fun parsesOpdsWithTheAndroidXmlParser() {
        val books = OpdsCatalogParser.parseGutenbergSearch(
            """
            <feed xmlns="http://www.w3.org/2005/Atom">
              <entry>
                <id>https://www.gutenberg.org/ebooks/11.opds</id>
                <title>Alice's Adventures in Wonderland</title>
                <content>Lewis Carroll</content>
              </entry>
            </feed>
            """.trimIndent(),
        )

        assertEquals("Alice's Adventures in Wonderland", books.single().title)
    }
}
