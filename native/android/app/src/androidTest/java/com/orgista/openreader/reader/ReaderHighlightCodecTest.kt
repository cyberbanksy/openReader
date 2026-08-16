package com.orgista.openreader.reader

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.publication.Locator

@RunWith(AndroidJUnit4::class)
class ReaderHighlightCodecTest {
    @Test
    fun highlightsRoundTripWithoutLosingLocatorText() {
        val locator = requireNotNull(
            Locator.fromJSON(
                JSONObject(
                    """{"href":"chapter-1.xhtml","type":"application/xhtml+xml","text":{"highlight":"down the rabbit-hole"}}""",
                ),
            ),
        )
        val original = listOf(ReaderHighlight("highlight-1", locator, 0xFFF2C96D.toInt()))

        val restored = ReaderHighlightCodec.decode(ReaderHighlightCodec.encode(original))

        assertEquals(original.first().id, restored.first().id)
        assertEquals(original.first().tint, restored.first().tint)
        assertEquals("down the rabbit-hole", restored.first().locator.text.highlight)
        assertTrue(JSONObject(restored.first().locator.toJSON().toString()).has("href"))
    }

    @Test
    fun highlightPersistsThroughReaderPreferences() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = ReaderPreferences(context, "reader-highlight-device-test")
        val locator = requireNotNull(
            Locator.fromJSON(
                JSONObject(
                    """{"href":"chapter-1.xhtml","type":"application/xhtml+xml","text":{"highlight":"down the rabbit-hole"}}""",
                ),
            ),
        )
        val original = listOf(ReaderHighlight("highlight-persisted", locator, 0xFFF2C96D.toInt()))

        assertTrue(preferences.saveHighlightsImmediately(original))

        val restored = ReaderPreferences(context, "reader-highlight-device-test").loadHighlights()
        assertEquals("highlight-persisted", restored.single().id)
        assertEquals("down the rabbit-hole", restored.single().locator.text.highlight)
    }
}
