package com.orgista.openreader.data

import com.orgista.openreader.domain.BookFormat
import org.junit.Assert.assertEquals
import org.junit.Test

class ArrCredentialTest {
    @Test
    fun normalizesManagerAddressAndTrimsApiKey() {
        val credential = ArrCredential.create(
            serverInput = "http://192.168.1.87:8787/",
            apiKey = "  secret-key  ",
            format = BookFormat.Ebook,
        )

        assertEquals("http://192.168.1.87:8787", credential.serverUrl)
        assertEquals("secret-key", credential.apiKey)
        assertEquals(BookFormat.Ebook, credential.format)
    }
}
