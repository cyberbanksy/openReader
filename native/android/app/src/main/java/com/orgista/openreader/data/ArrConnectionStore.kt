package com.orgista.openreader.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.orgista.openreader.domain.BookFormat
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class ArrConnectionStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun load(): ArrConnections? {
        val ebookUrl = decrypt(preferences.getString(KEY_EBOOK_URL, null)) ?: return null
        val ebookKey = decrypt(preferences.getString(KEY_EBOOK_API_KEY, null)) ?: return null
        val audiobookUrl = decrypt(preferences.getString(KEY_AUDIOBOOK_URL, null)) ?: return null
        val audiobookKey = decrypt(preferences.getString(KEY_AUDIOBOOK_API_KEY, null)) ?: return null
        return ArrConnections(
            ebook = ArrCredential(ebookUrl, ebookKey, BookFormat.Ebook),
            audiobook = ArrCredential(audiobookUrl, audiobookKey, BookFormat.Audiobook),
        )
    }

    fun save(connections: ArrConnections) {
        preferences.edit()
            .putString(KEY_EBOOK_URL, encrypt(connections.ebook.serverUrl))
            .putString(KEY_EBOOK_API_KEY, encrypt(connections.ebook.apiKey))
            .putString(KEY_AUDIOBOOK_URL, encrypt(connections.audiobook.serverUrl))
            .putString(KEY_AUDIOBOOK_API_KEY, encrypt(connections.audiobook.apiKey))
            .apply()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return listOf(cipher.iv, encrypted).joinToString(SEPARATOR) {
            Base64.encodeToString(it, Base64.NO_WRAP)
        }
    }

    private fun decrypt(value: String?): String? = runCatching {
        if (value.isNullOrBlank()) return null
        val parts = value.split(SEPARATOR, limit = 2)
        require(parts.size == 2)
        val iv = Base64.decode(parts[0], Base64.NO_WRAP)
        val encrypted = Base64.decode(parts[1], Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
        cipher.doFinal(encrypted).toString(Charsets.UTF_8)
    }.getOrNull()

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build(),
            )
            generateKey()
        }
    }

    private companion object {
        const val PREFERENCES = "openreader_arr_connections"
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val KEY_ALIAS = "openreader_arr_connections_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val SEPARATOR = "."
        const val KEY_EBOOK_URL = "ebook_url"
        const val KEY_EBOOK_API_KEY = "ebook_api_key"
        const val KEY_AUDIOBOOK_URL = "audiobook_url"
        const val KEY_AUDIOBOOK_API_KEY = "audiobook_api_key"
    }
}
