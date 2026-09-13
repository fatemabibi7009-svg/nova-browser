package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebStorage
import android.webkit.WebView
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.BookmarkEntity
import com.example.data.local.BrowserDatabase
import com.example.data.local.DownloadEntity
import com.example.data.local.HistoryEntity
import com.example.data.local.ShieldStatsEntity
import com.example.data.repository.BrowserRepository
import com.example.engine.ActiveSheet
import com.example.engine.BrowserTab
import com.example.engine.SearchEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BrowserRepository

    init {
        val db = BrowserDatabase.getInstance(application)
        repository = BrowserRepository(db.bookmarkDao(), db.historyDao(), db.shieldDao(), db.downloadDao())
    }

    val bookmarks: StateFlow<List<BookmarkEntity>> = repository.bookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<HistoryEntity>> = repository.history
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shieldStats: StateFlow<ShieldStatsEntity?> = repository.shieldStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val downloads: StateFlow<List<DownloadEntity>> = repository.downloads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Tabs Management
    private val initialTab = BrowserTab()
    private val _tabs = MutableStateFlow(listOf(initialTab))
    val tabs: StateFlow<List<BrowserTab>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow(initialTab.id)
    val activeTabId: StateFlow<String> = _activeTabId.asStateFlow()

    val activeTab: StateFlow<BrowserTab> = combine(_tabs, _activeTabId) { tabList, activeId ->
        tabList.firstOrNull { it.id == activeId } ?: tabList.firstOrNull() ?: initialTab
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialTab)

    // Active BottomSheet / Dialog
    private val _activeSheet = MutableStateFlow(ActiveSheet.NONE)
    val activeSheet: StateFlow<ActiveSheet> = _activeSheet.asStateFlow()

    // Search Engine
    private val _searchEngine = MutableStateFlow(SearchEngine.NOVA)
    val searchEngine: StateFlow<SearchEngine> = _searchEngine.asStateFlow()

    // Global Shields enabled
    private val _shieldsEnabled = MutableStateFlow(true)
    val shieldsEnabled: StateFlow<Boolean> = _shieldsEnabled.asStateFlow()

    // Force Dark Mode for Web Content (Global)
    private val _forceDarkMode = MutableStateFlow(false)
    val forceDarkMode: StateFlow<Boolean> = _forceDarkMode.asStateFlow()

    // Cookie & Privacy settings
    private val _blockThirdPartyCookies = MutableStateFlow(true)
    val blockThirdPartyCookies: StateFlow<Boolean> = _blockThirdPartyCookies.asStateFlow()

    private val _javascriptEnabled = MutableStateFlow(true)
    val javascriptEnabled: StateFlow<Boolean> = _javascriptEnabled.asStateFlow()

    private val _clearDataOnExit = MutableStateFlow(false)
    val clearDataOnExit: StateFlow<Boolean> = _clearDataOnExit.asStateFlow()

    // Find in Page state
    private val _isFindInPageVisible = MutableStateFlow(false)
    val isFindInPageVisible: StateFlow<Boolean> = _isFindInPageVisible.asStateFlow()

    private val _findQuery = MutableStateFlow("")
    val findQuery: StateFlow<String> = _findQuery.asStateFlow()

    private val _findMatchIndex = MutableStateFlow(0)
    val findMatchIndex: StateFlow<Int> = _findMatchIndex.asStateFlow()

    private val _findMatchTotal = MutableStateFlow(0)
    val findMatchTotal: StateFlow<Int> = _findMatchTotal.asStateFlow()

    // Omnibox state
    private val _isOmniboxEditing = MutableStateFlow(false)
    val isOmniboxEditing: StateFlow<Boolean> = _isOmniboxEditing.asStateFlow()

    private val _omniboxText = MutableStateFlow("")
    val omniboxText: StateFlow<String> = _omniboxText.asStateFlow()

    // WebView instances cache mapped by tabId
    private val webViewCache = mutableMapOf<String, WebView>()

    fun getOrCreateWebView(context: Context, tabId: String): WebView {
        return webViewCache.getOrPut(tabId) {
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = _javascriptEnabled.value
                    domStorageEnabled = true
                    databaseEnabled = true
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    mediaPlaybackRequiresUserGesture = false
                    allowFileAccess = false
                    allowContentAccess = true
                }
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, !_blockThirdPartyCookies.value)
            }
        }
    }

    fun openUrl(input: String) {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return

        val formattedUrl = resolveUrlOrSearch(trimmed)
        val currentActiveId = _activeTabId.value

        _tabs.value = _tabs.value.map { tab ->
            if (tab.id == currentActiveId) {
                tab.copy(url = formattedUrl, isLoading = true, progress = 10)
            } else tab
        }
        _isOmniboxEditing.value = false
        _omniboxText.value = ""

        // Trigger load in cached webview if present
        webViewCache[currentActiveId]?.loadUrl(formattedUrl)
    }

    private fun resolveUrlOrSearch(input: String): String {
        val isFullUrl = input.startsWith("http://", ignoreCase = true) ||
                input.startsWith("https://", ignoreCase = true) ||
                input.startsWith("nova://", ignoreCase = true) ||
                input.startsWith("about:", ignoreCase = true)

        if (isFullUrl) return input

        // Check if input looks like domain name e.g. "google.com" or "en.wikipedia.org/wiki"
        val domainPattern = Regex("^([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}(/.*)?$")
        if (domainPattern.matches(input)) {
            return "https://$input"
        }

        // Otherwise perform search query
        return _searchEngine.value.buildQuery(input)
    }

    fun newTab(url: String = "nova://start", isIncognito: Boolean = false) {
        val newTab = BrowserTab(
            url = url,
            isIncognito = isIncognito,
            title = if (url == "nova://start") "New Tab" else url
        )
        _tabs.value = _tabs.value + newTab
        _activeTabId.value = newTab.id
        _activeSheet.value = ActiveSheet.NONE
        _isOmniboxEditing.value = false
    }

    fun selectTab(tabId: String) {
        if (_tabs.value.any { it.id == tabId }) {
            _activeTabId.value = tabId
            _activeSheet.value = ActiveSheet.NONE
            _isOmniboxEditing.value = false
        }
    }

    fun closeTab(tabId: String) {
        val currentTabs = _tabs.value
        val tabToClose = currentTabs.firstOrNull { it.id == tabId } ?: return

        // Clean up webview instance
        webViewCache.remove(tabId)?.apply {
            stopLoading()
            loadUrl("about:blank")
            clearHistory()
            destroy()
        }

        val updatedTabs = currentTabs.filter { it.id != tabId }
        if (updatedTabs.isEmpty()) {
            // If all closed, create one fresh default tab
            val freshTab = BrowserTab()
            _tabs.value = listOf(freshTab)
            _activeTabId.value = freshTab.id
        } else {
            _tabs.value = updatedTabs
            if (_activeTabId.value == tabId) {
                // Switch to adjacent tab
                _activeTabId.value = updatedTabs.last().id
            }
        }
    }

    fun closeAllTabs(incognitoOnly: Boolean = false) {
        val currentTabs = _tabs.value
        val tabsToRemove = if (incognitoOnly) currentTabs.filter { it.isIncognito } else currentTabs
        tabsToRemove.forEach { tab ->
            webViewCache.remove(tab.id)?.apply {
                stopLoading()
                loadUrl("about:blank")
                clearHistory()
                destroy()
            }
        }

        val remainingTabs = if (incognitoOnly) currentTabs.filter { !it.isIncognito } else emptyList()
        if (remainingTabs.isEmpty()) {
            val freshTab = BrowserTab()
            _tabs.value = listOf(freshTab)
            _activeTabId.value = freshTab.id
        } else {
            _tabs.value = remainingTabs
            _activeTabId.value = remainingTabs.last().id
        }
        _activeSheet.value = ActiveSheet.NONE
    }

    fun updateTabState(
        tabId: String,
        url: String? = null,
        title: String? = null,
        canGoBack: Boolean? = null,
        canGoForward: Boolean? = null,
        isLoading: Boolean? = null,
        progress: Int? = null,
        sslSecure: Boolean? = null,
        incrementBlocked: Boolean = false
    ) {
        _tabs.value = _tabs.value.map { tab ->
            if (tab.id == tabId) {
                val newUrl = url ?: tab.url
                val newTitle = title ?: tab.title
                val newBlocked = if (incrementBlocked) tab.blockedCount + 1 else tab.blockedCount

                // Record history if finished loading and not incognito
                if (isLoading == false && !tab.isIncognito && newUrl.isNotBlank() && !tab.isStartPage) {
                    recordHistory(newTitle, newUrl)
                }

                tab.copy(
                    url = newUrl,
                    title = newTitle,
                    canGoBack = canGoBack ?: tab.canGoBack,
                    canGoForward = canGoForward ?: tab.canGoForward,
                    isLoading = isLoading ?: tab.isLoading,
                    progress = progress ?: tab.progress,
                    sslSecure = sslSecure ?: (newUrl.startsWith("https://")),
                    blockedCount = newBlocked
                )
            } else tab
        }

        if (incrementBlocked) {
            viewModelScope.launch {
                repository.recordBlockedEvent(trackers = 1, ads = 1, dataKb = 24, timeMs = 50)
            }
        }
    }

    fun toggleDesktopMode(tabId: String) {
        val tab = _tabs.value.firstOrNull { it.id == tabId } ?: return
        val newDesktop = !tab.isDesktopMode
        _tabs.value = _tabs.value.map {
            if (it.id == tabId) it.copy(isDesktopMode = newDesktop) else it
        }

        webViewCache[tabId]?.let { webView ->
            val desktopUa = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
            webView.settings.userAgentString = if (newDesktop) desktopUa else null
            webView.settings.useWideViewPort = newDesktop
            webView.reload()
        }
    }

    fun toggleReaderMode(tabId: String) {
        val tab = _tabs.value.firstOrNull { it.id == tabId } ?: return
        val newReader = !tab.isReaderMode
        _tabs.value = _tabs.value.map {
            if (it.id == tabId) it.copy(isReaderMode = newReader) else it
        }

        // Inject dark mode script
        webViewCache[tabId]?.evaluateJavascript(
            """
            (function() {
                var style = document.getElementById('nova-reader-style');
                if (style) {
                    style.remove();
                } else {
                    style = document.createElement('style');
                    style.id = 'nova-reader-style';
                    style.innerHTML = 'html { filter: invert(90%) hue-rotate(180deg) !important; background: #121212 !important; } img, video, canvas, svg { filter: invert(100%) hue-rotate(180deg) !important; }';
                    document.head.appendChild(style);
                }
            })()
            """.trimIndent(),
            null
        )
    }

    fun toggleGlobalShields() {
        _shieldsEnabled.value = !_shieldsEnabled.value
    }

    fun setSearchEngine(engine: SearchEngine) {
        _searchEngine.value = engine
    }

    fun openSheet(sheet: ActiveSheet) {
        _activeSheet.value = sheet
    }

    fun closeSheet() {
        _activeSheet.value = ActiveSheet.NONE
    }

    fun setOmniboxEditing(editing: Boolean, initialText: String = "") {
        _isOmniboxEditing.value = editing
        _omniboxText.value = initialText
    }

    fun updateOmniboxText(text: String) {
        _omniboxText.value = text
    }

    fun reloadCurrentTab() {
        val activeId = _activeTabId.value
        webViewCache[activeId]?.reload()
    }

    fun stopCurrentTabLoading() {
        val activeId = _activeTabId.value
        webViewCache[activeId]?.stopLoading()
        updateTabState(activeId, isLoading = false)
    }

    fun goBackCurrentTab() {
        val activeId = _activeTabId.value
        val webView = webViewCache[activeId]
        if (webView != null && webView.canGoBack()) {
            webView.goBack()
        }
    }

    fun goForwardCurrentTab() {
        val activeId = _activeTabId.value
        val webView = webViewCache[activeId]
        if (webView != null && webView.canGoForward()) {
            webView.goForward()
        }
    }

    fun toggleBookmarkCurrentTab() {
        val currentTab = activeTab.value
        if (currentTab.isStartPage || currentTab.url.isBlank()) return

        viewModelScope.launch {
            val isCurrentBookmarked = bookmarks.value.any { it.url == currentTab.url }
            if (isCurrentBookmarked) {
                repository.removeBookmarkByUrl(currentTab.url)
            } else {
                repository.addBookmark(
                    title = currentTab.displayTitle,
                    url = currentTab.url
                )
            }
        }
    }

    fun deleteBookmark(id: Long) {
        viewModelScope.launch {
            repository.removeBookmarkById(id)
        }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            repository.removeHistoryById(id)
        }
    }

    fun clearBrowsingHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun clearAllBrowsingData(context: Context) {
        viewModelScope.launch {
            repository.clearHistory()
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
            WebStorage.getInstance().deleteAllData()
            webViewCache.values.forEach { it.clearCache(true) }
        }
    }

    private fun recordHistory(title: String, url: String) {
        viewModelScope.launch {
            repository.addHistory(title, url)
        }
    }

    // Find in Page
    fun openFindInPage() {
        _isFindInPageVisible.value = true
        _findQuery.value = ""
        _findMatchIndex.value = 0
        _findMatchTotal.value = 0
    }

    fun closeFindInPage() {
        _isFindInPageVisible.value = false
        _findQuery.value = ""
        webViewCache[_activeTabId.value]?.clearMatches()
    }

    fun searchFindInPage(query: String) {
        _findQuery.value = query
        val webView = webViewCache[_activeTabId.value] ?: return
        if (query.isBlank()) {
            webView.clearMatches()
            _findMatchTotal.value = 0
            _findMatchIndex.value = 0
        } else {
            webView.setFindListener { activeMatchOrdinal, numberOfMatches, isDoneCounting ->
                _findMatchIndex.value = if (numberOfMatches > 0) activeMatchOrdinal + 1 else 0
                _findMatchTotal.value = numberOfMatches
            }
            webView.findAllAsync(query)
        }
    }

    fun findNextMatch(forward: Boolean) {
        webViewCache[_activeTabId.value]?.findNext(forward)
    }

    fun shareCurrentPage(context: Context) {
        val tab = activeTab.value
        if (tab.url.isBlank() || tab.isStartPage) return

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, tab.displayTitle)
            putExtra(Intent.EXTRA_TEXT, tab.url)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share via").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }

    fun toggleForceDarkMode(tabId: String? = null) {
        val targetId = tabId ?: _activeTabId.value
        val tab = _tabs.value.firstOrNull { it.id == targetId } ?: return
        val newForceDark = !tab.isForceDarkMode
        _tabs.value = _tabs.value.map {
            if (it.id == targetId) it.copy(isForceDarkMode = newForceDark) else it
        }
        if (newForceDark) {
            injectDarkMode(targetId)
        } else {
            removeDarkMode(targetId)
        }
    }

    fun toggleGlobalForceDark() {
        val newGlobal = !_forceDarkMode.value
        _forceDarkMode.value = newGlobal
        _tabs.value.forEach { tab ->
            if (newGlobal) injectDarkMode(tab.id) else removeDarkMode(tab.id)
        }
    }

    fun injectDarkMode(tabId: String) {
        webViewCache[tabId]?.evaluateJavascript(
            """
            (function() {
                var style = document.getElementById('nova-forcedark-style');
                if (!style) {
                    style = document.createElement('style');
                    style.id = 'nova-forcedark-style';
                    style.innerHTML = 'html { filter: invert(90%) hue-rotate(180deg) !important; background: #121212 !important; } img, video, canvas, svg, [style*="background-image"] { filter: invert(100%) hue-rotate(180deg) !important; }';
                    document.head.appendChild(style);
                }
            })()
            """.trimIndent(),
            null
        )
    }

    fun removeDarkMode(tabId: String) {
        webViewCache[tabId]?.evaluateJavascript(
            """
            (function() {
                var style = document.getElementById('nova-forcedark-style');
                if (style) style.remove();
            })()
            """.trimIndent(),
            null
        )
    }

    fun applyPageInjections(tabId: String) {
        val tab = _tabs.value.firstOrNull { it.id == tabId } ?: return
        if (tab.isForceDarkMode || _forceDarkMode.value || tab.isReaderMode) {
            injectDarkMode(tabId)
        }
        if (_shieldsEnabled.value && tab.url.contains("youtube.com", ignoreCase = true)) {
            webViewCache[tabId]?.evaluateJavascript(
                com.example.engine.AdBlocker.YOUTUBE_ADBLOCK_SCRIPT,
                null
            )
        }
    }

    fun toggleBlockThirdPartyCookies() {
        val newVal = !_blockThirdPartyCookies.value
        _blockThirdPartyCookies.value = newVal
        webViewCache.values.forEach {
            CookieManager.getInstance().setAcceptThirdPartyCookies(it, !newVal)
        }
    }

    fun toggleJavascript() {
        val newVal = !_javascriptEnabled.value
        _javascriptEnabled.value = newVal
        webViewCache.values.forEach {
            it.settings.javaScriptEnabled = newVal
            it.reload()
        }
    }

    fun toggleClearDataOnExit() {
        _clearDataOnExit.value = !_clearDataOnExit.value
    }

    fun startDownload(
        context: Context,
        downloadUrl: String,
        contentDisposition: String? = null,
        mimeType: String? = null,
        contentLength: Long = 0
    ) {
        val fileName = URLUtil.guessFileName(downloadUrl, contentDisposition, mimeType)
        Toast.makeText(context, "Starting download: $fileName", Toast.LENGTH_SHORT).show()
        try {
            val request = android.app.DownloadManager.Request(Uri.parse(downloadUrl)).apply {
                mimeType?.let { setMimeType(it) }
                setDescription("Downloading from Nova Browser…")
                setTitle(fileName)
                setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, fileName)
            }
            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? android.app.DownloadManager
            manager?.enqueue(request)

            viewModelScope.launch {
                repository.addDownload(
                    fileName = fileName,
                    url = downloadUrl,
                    mimeType = mimeType,
                    totalBytes = if (contentLength > 0) contentLength else 0,
                    status = "COMPLETED",
                    localFilePath = "${android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)}/$fileName"
                )
            }
        } catch (_: Exception) {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(browserIntent)
        }
    }

    fun deleteDownload(id: Long) {
        viewModelScope.launch {
            repository.removeDownloadById(id)
        }
    }

    fun clearAllDownloads() {
        viewModelScope.launch {
            repository.clearDownloads()
        }
    }

    fun savePageAsPdf(context: Context) {
        val activeId = _activeTabId.value
        val webView = webViewCache[activeId] ?: return
        val tab = activeTab.value
        if (tab.isStartPage || tab.url.isBlank()) {
            Toast.makeText(context, "Cannot export start page to PDF", Toast.LENGTH_SHORT).show()
            return
        }
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? android.print.PrintManager
        if (printManager != null) {
            val jobName = "Nova_${tab.displayTitle.take(20).replace(Regex("[^a-zA-Z0-9]"), "_")}"
            val printAdapter = webView.createPrintDocumentAdapter(jobName)
            printManager.print(jobName, printAdapter, android.print.PrintAttributes.Builder().build())
        } else {
            Toast.makeText(context, "PDF print spooler not available", Toast.LENGTH_SHORT).show()
        }
    }

    fun savePageForOffline(context: Context) {
        val activeId = _activeTabId.value
        val webView = webViewCache[activeId] ?: return
        val tab = activeTab.value
        if (tab.isStartPage || tab.url.isBlank()) {
            Toast.makeText(context, "Cannot save start page offline", Toast.LENGTH_SHORT).show()
            return
        }

        val safeTitle = tab.displayTitle.replace(Regex("[^a-zA-Z0-9.-]"), "_").take(25)
        val safeFileName = "${safeTitle}_${System.currentTimeMillis()}.mht"
        val downloadsDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        val file = java.io.File(downloadsDir, safeFileName)

        webView.saveWebArchive(file.absolutePath, false) { path ->
            if (path != null) {
                val fileSize = java.io.File(path).length()
                viewModelScope.launch {
                    repository.addDownload(
                        fileName = safeFileName,
                        url = tab.url,
                        mimeType = "multipart/related",
                        totalBytes = fileSize,
                        status = "COMPLETED",
                        localFilePath = path
                    )
                }
                Toast.makeText(context, "Saved offline archive: $safeFileName", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to save offline archive", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun clearBrowsingData(
        clearHistory: Boolean,
        clearCookies: Boolean,
        clearCache: Boolean,
        clearStorage: Boolean
    ) {
        viewModelScope.launch {
            if (clearHistory) {
                repository.clearHistory()
            }
            if (clearCookies) {
                CookieManager.getInstance().removeAllCookies(null)
                CookieManager.getInstance().flush()
            }
            if (clearCache) {
                webViewCache.values.forEach { it.clearCache(true) }
            }
            if (clearStorage) {
                WebStorage.getInstance().deleteAllData()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (_clearDataOnExit.value) {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
            WebStorage.getInstance().deleteAllData()
        }
        webViewCache.values.forEach { webView ->
            if (_clearDataOnExit.value) {
                webView.clearCache(true)
            }
            webView.stopLoading()
            webView.destroy()
        }
        webViewCache.clear()
    }
}
