package com.orgista.openreader.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ServerAddressTest {
    @Test
    fun normalizesTrailingSlashAndHostCase() {
        assertEquals(
            "https://books.example.com",
            ServerAddress.normalize(" HTTPS://Books.Example.Com/ "),
        )
    }

    @Test
    fun permitsCleartextForPrivateLanServers() {
        assertEquals(
            "http://192.168.1.87:13378",
            ServerAddress.normalize("http://192.168.1.87:13378/"),
        )
    }

    @Test
    fun rejectsCredentialsEmbeddedInUrls() {
        assertThrows(IllegalArgumentException::class.java) {
            ServerAddress.normalize("https://user:password@books.example.com")
        }
    }

    @Test
    fun rejectsPublicCleartextServers() {
        assertThrows(IllegalArgumentException::class.java) {
            ServerAddress.normalize("http://books.example.com")
        }
    }
}
