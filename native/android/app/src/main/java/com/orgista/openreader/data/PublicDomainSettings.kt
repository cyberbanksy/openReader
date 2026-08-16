package com.orgista.openreader.data

import android.content.Context

class PublicDomainSettings(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun standardEbooksEmail(): String? = preferences.getString(KEY_STANDARD_EBOOKS_EMAIL, null)
        ?.trim()
        ?.takeIf(String::isNotBlank)

    fun saveStandardEbooksEmail(email: String) {
        preferences.edit().putString(KEY_STANDARD_EBOOKS_EMAIL, email.trim()).apply()
    }

    fun clearStandardEbooksEmail() {
        preferences.edit().remove(KEY_STANDARD_EBOOKS_EMAIL).apply()
    }

    private companion object {
        const val PREFERENCES = "openreader_public_domain"
        const val KEY_STANDARD_EBOOKS_EMAIL = "standard_ebooks_email"
    }
}
