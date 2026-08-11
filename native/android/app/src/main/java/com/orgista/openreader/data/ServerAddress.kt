package com.orgista.openreader.data

import java.net.IDN
import java.net.URI
import java.util.Locale

object ServerAddress {
    fun normalize(value: String): String {
        val candidate = value.trim().trimEnd('/')
        require(candidate.isNotEmpty()) { "Enter a server address." }
        val withScheme = if (candidate.contains("://")) candidate else "https://$candidate"
        val parsed = URI(withScheme)
        val scheme = parsed.scheme?.lowercase(Locale.US)
        require(scheme == "http" || scheme == "https") { "Use an HTTP or HTTPS server address." }
        require(parsed.userInfo == null) { "Credentials cannot be embedded in the server address." }
        require(parsed.rawQuery == null && parsed.rawFragment == null) { "Remove query parameters and fragments." }
        require(parsed.path.isNullOrBlank() || parsed.path == "/") { "Use the server root address." }

        val host = parsed.host?.let(IDN::toASCII)?.lowercase(Locale.US)
            ?: throw IllegalArgumentException("The server address has no valid host.")
        if (scheme == "http") {
            require(isPrivateHost(host)) { "Cleartext HTTP is allowed only for a private LAN server." }
        }
        val port = if (parsed.port >= 0) ":${parsed.port}" else ""
        return "$scheme://$host$port"
    }

    private fun isPrivateHost(host: String): Boolean {
        if (host == "localhost" || host.endsWith(".local")) return true
        val octets = host.split('.').mapNotNull(String::toIntOrNull)
        if (octets.size != 4 || octets.any { it !in 0..255 }) return false
        return octets[0] == 10 ||
            (octets[0] == 127) ||
            (octets[0] == 192 && octets[1] == 168) ||
            (octets[0] == 172 && octets[1] in 16..31)
    }
}
