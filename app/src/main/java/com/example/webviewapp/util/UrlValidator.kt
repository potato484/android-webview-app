package com.example.webviewapp.util

object UrlValidator {
    fun isValidScheme(url: String): Boolean {
        val lower = url.lowercase()
        return lower.startsWith("http://") || lower.startsWith("https://")
    }

    fun isHttps(url: String): Boolean = url.lowercase().startsWith("https://")

    fun isHttp(url: String): Boolean = url.lowercase().startsWith("http://")

    private val SCHEME_REGEX = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:")

    fun normalize(rawUrl: String): String {
        var url = rawUrl.trim()
        if (!SCHEME_REGEX.containsMatchIn(url)) url = "https://$url"

        return try {
            val uri = java.net.URI(url)
            val scheme = uri.scheme.lowercase()
            val host = uri.host?.lowercase() ?: return url
            val port = when {
                uri.port == -1 -> ""
                uri.port == 80 && scheme == "http" -> ""
                uri.port == 443 && scheme == "https" -> ""
                else -> ":${uri.port}"
            }
            val path = uri.rawPath?.let { if (it == "/") "" else it } ?: ""
            val query = uri.rawQuery?.let { "?$it" } ?: ""
            val fragment = uri.rawFragment?.let { "#$it" } ?: ""
            "$scheme://$host$port$path$query$fragment"
        } catch (_: Exception) {
            url
        }
    }
}
