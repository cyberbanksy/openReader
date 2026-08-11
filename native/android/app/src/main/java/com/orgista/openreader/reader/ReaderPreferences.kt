package com.orgista.openreader.reader

import android.content.Context
import org.json.JSONObject
import org.readium.r2.navigator.preferences.Theme
import org.readium.r2.shared.publication.Locator

class ReaderPreferences(context: Context, private val bookId: String) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun loadLocator(): Locator? = runCatching {
        preferences.getString(key("locator"), null)
            ?.let(::JSONObject)
            ?.let(Locator::fromJSON)
    }.getOrNull()

    fun saveLocator(locator: Locator) {
        preferences.edit().putString(key("locator"), locator.toJSON().toString()).apply()
    }

    var fontSize: Double
        get() = preferences.getFloat(key("font_size"), DEFAULT_FONT_SIZE.toFloat()).toDouble()
        set(value) {
            preferences.edit().putFloat(key("font_size"), value.toFloat()).apply()
        }

    var theme: Theme
        get() = runCatching {
            Theme.valueOf(preferences.getString(key("theme"), Theme.SEPIA.name)!!)
        }.getOrDefault(Theme.SEPIA)
        set(value) {
            preferences.edit().putString(key("theme"), value.name).apply()
        }

    private fun key(suffix: String) = "$bookId.$suffix"

    private companion object {
        const val PREFERENCES = "openreader_reader"
        const val DEFAULT_FONT_SIZE = 1.0
    }
}
