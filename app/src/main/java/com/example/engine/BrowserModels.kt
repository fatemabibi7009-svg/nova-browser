package com.example.engine

import java.util.UUID

enum class SearchEngine(
    val displayName: String,
    val searchUrl: String,
    val homeUrl: String,
    val shortCode: String
) {
    NOVA("Nova Search (Built-in)", "nova://search?q=%s", "nova://start", "Nova"),
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com/?q=%s", "https://duckduckgo.com", "DDG"),
    BRAVE("Brave Search", "https://search.brave.com/search?q=%s", "https://search.brave.com", "Brave"),
    GOOGLE("Google", "https://www.google.com/search?q=%s", "https://www.google.com", "Google"),
    BING("Bing", "https://www.bing.com/search?q=%s", "https://www.bing.com", "Bing"),
    ECOSIA("Ecosia", "https://www.ecosia.org/search?q=%s", "https://www.ecosia.org", "Ecosia"),
    STARTPAGE("Startpage", "https://www.startpage.com/sp/search?query=%s", "https://www.startpage.com", "Startpage"),
    YAHOO("Yahoo", "https://search.yahoo.com/search?p=%s", "https://search.yahoo.com", "Yahoo");

    fun buildQuery(query: String): String {
        val encoded = java.net.URLEncoder.encode(query, "UTF-8")
        return searchUrl.replace("%s", encoded)
    }
}

data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    val url: String = "nova://start",
    val title: String = "New Tab",
    val isIncognito: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val isDesktopMode: Boolean = false,
    val isReaderMode: Boolean = false,
    val isForceDarkMode: Boolean = false,
    val sslSecure: Boolean = false,
    val blockedCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    val isStartPage: Boolean
        get() = url.isBlank() || url == "nova://start" || url == "about:blank"

    val isNovaSearchPage: Boolean
        get() = url.startsWith("nova://search", ignoreCase = true)

    val novaSearchQuery: String
        get() {
            return try {
                val uri = android.net.Uri.parse(url)
                uri.getQueryParameter("q") ?: ""
            } catch (_: Exception) {
                ""
            }
        }

    val displayTitle: String
        get() = when {
            isStartPage -> "New Tab"
            isNovaSearchPage -> if (novaSearchQuery.isNotBlank()) "$novaSearchQuery - Nova Search" else "Nova Search"
            else -> title.ifBlank { url }
        }

    val domain: String
        get() {
            if (isStartPage) return "New Tab"
            if (isNovaSearchPage) return "Nova Search"
            return try {
                val uri = android.net.Uri.parse(url)
                val host = uri.host ?: url
                host.removePrefix("www.")
            } catch (_: Exception) {
                url
            }
        }
}

enum class ActiveSheet {
    NONE,
    TAB_GRID,
    BOOKMARKS,
    HISTORY,
    DOWNLOADS,
    SHIELDS,
    SETTINGS,
    QR_SCANNER,
    CLEAR_DATA
}
