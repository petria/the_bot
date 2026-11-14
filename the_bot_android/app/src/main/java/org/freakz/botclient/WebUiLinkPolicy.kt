package org.freakz.botclient

import java.net.URI

object WebUiLinkPolicy {
    fun canOpen(url: String): Boolean {
        val uri = runCatching { URI(url.trim()) }.getOrNull() ?: return false
        val scheme = uri.scheme?.lowercase() ?: return false
        return (scheme == "http" || scheme == "https") && !uri.host.isNullOrBlank()
    }

    fun labelFor(url: String, buildBaseUrl: String): String {
        val normalized = normalize(url)
        val own = normalize(buildBaseUrl)
        if (own.isNotEmpty() && normalized.isNotEmpty() && normalized == own) return "this instance"
        if (url.contains("dev", ignoreCase = true)) return "develop instance"
        if (url.contains("main", ignoreCase = true)) return "main instance"
        return "configured link"
    }

    fun statusLabel(link: WebUiLink, buildBaseUrl: String): String {
        if (link.url.isBlank()) return "No URL configured"
        if (!canOpen(link.url)) return "Invalid URL"
        val origin = if (link.configured) "custom" else "default"
        val own = labelFor(link.url, buildBaseUrl).takeIf { it == "this instance" }?.let { " — $it" }.orEmpty()
        return "$origin$own"
    }

    private fun normalize(url: String): String = url.trim().trimEnd('/')
}
