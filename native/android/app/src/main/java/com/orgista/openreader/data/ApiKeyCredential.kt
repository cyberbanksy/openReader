package com.orgista.openreader.data

object ApiKeyCredential {
    fun create(serverInput: String, apiKey: String): ServerSession {
        val token = apiKey.trim()
        require(token.isNotEmpty()) { "Enter an API key." }
        return ServerSession(
            serverUrl = ServerAddress.normalize(serverInput),
            username = "API key",
            accessToken = token,
            refreshToken = null,
        )
    }
}
