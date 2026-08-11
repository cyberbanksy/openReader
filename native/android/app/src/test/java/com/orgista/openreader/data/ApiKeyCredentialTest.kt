package com.orgista.openreader.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class ApiKeyCredentialTest {
    @Test
    fun createsSessionWithoutRefreshToken() {
        val session = ApiKeyCredential.create(
            serverInput = "http://192.168.1.87:13378/",
            apiKey = "  test-api-key  ",
        )

        assertEquals("http://192.168.1.87:13378", session.serverUrl)
        assertEquals("API key", session.username)
        assertEquals("test-api-key", session.accessToken)
        assertNull(session.refreshToken)
    }

    @Test
    fun rejectsBlankApiKey() {
        assertThrows(IllegalArgumentException::class.java) {
            ApiKeyCredential.create("https://books.example.com", "  ")
        }
    }
}
