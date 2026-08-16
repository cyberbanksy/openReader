package com.orgista.openreader.reader

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.publication.Locator

@RunWith(AndroidJUnit4::class)
class ReaderDeviceStateTest {
    @Test
    fun setRequestedReaderState() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val arguments = InstrumentationRegistry.getArguments()
        val bookId = requireNotNull(arguments.getString("book_id"))
        val layout = ReaderLayout.valueOf(requireNotNull(arguments.getString("layout")))
        val href = requireNotNull(arguments.getString("href"))
        val progression = arguments.getString("progression")?.toDoubleOrNull() ?: 0.0
        val preferences = ReaderPreferences(instrumentation.targetContext, bookId)
        val locator = requireNotNull(
            Locator.fromJSON(
                JSONObject(
                    """{"href":"$href","type":"application/xhtml+xml","locations":{"progression":$progression}}""",
                ),
            ),
        )

        preferences.layout = layout
        preferences.saveLocator(locator)
        Thread.sleep(500L)

        assertEquals(layout, preferences.layout)
        assertEquals(href, preferences.loadLocator()?.href.toString())
    }
}
