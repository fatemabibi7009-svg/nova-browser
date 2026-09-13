package com.example.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.DownloadListener
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.engine.ActiveSheet
import com.example.engine.AdBlocker
import com.example.ui.components.BookmarksSheet
import com.example.ui.components.BrowserBottomBar
import com.example.ui.components.ClearDataSheet
import com.example.ui.components.DownloadsSheet
import com.example.ui.components.FindInPageBar
import com.example.ui.components.HistorySheet
import com.example.ui.components.NewTabPage
import com.example.ui.components.NovaSearchResultsView
import com.example.ui.components.Omnibox
import com.example.ui.components.QrScannerSheet
import com.example.ui.components.SettingsSheet
import com.example.ui.components.ShieldsSheet
import com.example.ui.components.TabGridSheet
import com.example.ui.viewmodel.BrowserViewModel

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val tabs by viewModel.tabs.collectAsStateWithLifecycle()
    val activeTabId by viewModel.activeTabId.collectAsStateWithLifecycle()
    val activeSheet by viewModel.activeSheet.collectAsStateWithLifecycle()
    val searchEngine by viewModel.searchEngine.collectAsStateWithLifecycle()
    val shieldsEnabled by viewModel.shieldsEnabled.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val shieldStats by viewModel.shieldStats.collectAsStateWithLifecycle()
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val forceDarkMode by viewModel.forceDarkMode.collectAsStateWithLifecycle()
    val blockThirdPartyCookies by viewModel.blockThirdPartyCookies.collectAsStateWithLifecycle()
    val javascriptEnabled by viewModel.javascriptEnabled.collectAsStateWithLifecycle()
    val clearDataOnExit by viewModel.clearDataOnExit.collectAsStateWithLifecycle()

    val isOmniboxEditing by viewModel.isOmniboxEditing.collectAsStateWithLifecycle()
    val omniboxText by viewModel.omniboxText.collectAsStateWithLifecycle()

    val isFindInPageVisible by viewModel.isFindInPageVisible.collectAsStateWithLifecycle()
    val findQuery by viewModel.findQuery.collectAsStateWithLifecycle()
    val findMatchIndex by viewModel.findMatchIndex.collectAsStateWithLifecycle()
    val findMatchTotal by viewModel.findMatchTotal.collectAsStateWithLifecycle()

    val isCurrentBookmarked = remember(bookmarks, activeTab.url) {
        bookmarks.any { it.url == activeTab.url }
    }

    // Fullscreen video container state
    var customFullscreenView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    // Intercept back button gracefully
    BackHandler(enabled = true) {
        when {
            customFullscreenView != null -> {
                customViewCallback?.onCustomViewHidden()
                customFullscreenView = null
                customViewCallback = null
            }
            activeSheet != ActiveSheet.NONE -> {
                viewModel.closeSheet()
            }
            isFindInPageVisible -> {
                viewModel.closeFindInPage()
            }
            isOmniboxEditing -> {
                viewModel.setOmniboxEditing(false)
            }
            activeTab.canGoBack -> {
                viewModel.goBackCurrentTab()
            }
            !activeTab.isStartPage -> {
                viewModel.openUrl("nova://start")
            }
            tabs.size > 1 -> {
                viewModel.closeTab(activeTab.id)
            }
            else -> {
                (context as? android.app.Activity)?.finish()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Omnibox Header
            Omnibox(
                tab = activeTab,
                isEditing = isOmniboxEditing,
                textInput = omniboxText,
                searchEngine = searchEngine,
                bookmarks = bookmarks,
                history = history,
                onEditStart = {
                    val initial = if (activeTab.isStartPage) "" else activeTab.url
                    viewModel.setOmniboxEditing(true, initial)
                },
                onEditCancel = {
                    viewModel.setOmniboxEditing(false)
                },
                onTextChanged = { viewModel.updateOmniboxText(it) },
                onSubmitUrl = { viewModel.openUrl(it) },
                onReload = { viewModel.reloadCurrentTab() },
                onStop = { viewModel.stopCurrentTabLoading() },
                onShieldClick = { viewModel.openSheet(ActiveSheet.SHIELDS) },
                onQrScannerClick = { viewModel.openSheet(ActiveSheet.QR_SCANNER) },
                modifier = Modifier.statusBarsPadding()
            )

            // Find In Page Bar if active
            if (isFindInPageVisible) {
                FindInPageBar(
                    query = findQuery,
                    matchIndex = findMatchIndex,
                    matchTotal = findMatchTotal,
                    onQueryChanged = { viewModel.searchFindInPage(it) },
                    onNext = { viewModel.findNextMatch(true) },
                    onPrevious = { viewModel.findNextMatch(false) },
                    onClose = { viewModel.closeFindInPage() }
                )
            }

            // Web View or Start Page
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (activeTab.isStartPage) {
                    NewTabPage(
                        tab = activeTab,
                        searchEngine = searchEngine,
                        shieldStats = shieldStats,
                        bookmarks = bookmarks,
                        onOpenUrl = { viewModel.openUrl(it) },
                        onFocusSearch = {
                            viewModel.setOmniboxEditing(true, "")
                        }
                    )
                } else if (activeTab.isNovaSearchPage) {
                    NovaSearchResultsView(
                        query = activeTab.novaSearchQuery,
                        onOpenUrl = { viewModel.openUrl(it) },
                        onSearchAgain = { viewModel.openUrl(searchEngine.buildQuery(it)) },
                        onFocusSearch = {
                            viewModel.setOmniboxEditing(true, activeTab.novaSearchQuery)
                        }
                    )
                } else {
                    // Real working Android WebView
                    val currentTabId = activeTab.id
                    val webView = remember(currentTabId) {
                        viewModel.getOrCreateWebView(context, currentTabId).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )

                            // Setup clients
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val url = request?.url?.toString() ?: return false
                                    if (url.startsWith("http://") || url.startsWith("https://")) {
                                        return false // Load in WebView
                                    }
                                    return try {
                                        // Handle external schemes like mailto:, tel:, market:
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(intent)
                                        true
                                    } catch (_: Exception) {
                                        true
                                    }
                                }

                                override fun shouldInterceptRequest(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): WebResourceResponse? {
                                    val reqUrl = request?.url?.toString() ?: return null
                                    if (shieldsEnabled && AdBlocker.isAdOrTracker(reqUrl)) {
                                        viewModel.updateTabState(currentTabId, incrementBlocked = true)
                                        return AdBlocker.createEmptyResponse()
                                    }
                                    return super.shouldInterceptRequest(view, request)
                                }

                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    viewModel.updateTabState(
                                        tabId = currentTabId,
                                        url = url,
                                        isLoading = true,
                                        progress = 15,
                                        canGoBack = view?.canGoBack(),
                                        canGoForward = view?.canGoForward()
                                    )
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    viewModel.updateTabState(
                                        tabId = currentTabId,
                                        url = url,
                                        title = view?.title,
                                        isLoading = false,
                                        progress = 100,
                                        canGoBack = view?.canGoBack(),
                                        canGoForward = view?.canGoForward(),
                                        sslSecure = url?.startsWith("https://") == true
                                    )
                                    viewModel.applyPageInjections(currentTabId)
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    super.onProgressChanged(view, newProgress)
                                    viewModel.updateTabState(
                                        tabId = currentTabId,
                                        progress = newProgress,
                                        isLoading = newProgress < 100
                                    )
                                }

                                override fun onReceivedTitle(view: WebView?, title: String?) {
                                    super.onReceivedTitle(view, title)
                                    if (!title.isNullOrBlank()) {
                                        viewModel.updateTabState(tabId = currentTabId, title = title)
                                    }
                                }

                                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                                    super.onShowCustomView(view, callback)
                                    customFullscreenView = view
                                    customViewCallback = callback
                                }

                                override fun onHideCustomView() {
                                    super.onHideCustomView()
                                    customFullscreenView = null
                                    customViewCallback = null
                                }
                            }

                            setDownloadListener { downloadUrl, _, contentDisposition, mimeType, contentLength ->
                                viewModel.startDownload(
                                    context = context,
                                    downloadUrl = downloadUrl,
                                    contentDisposition = contentDisposition,
                                    mimeType = mimeType,
                                    contentLength = contentLength
                                )
                            }
                        }
                    }

                    // Load URL if not yet loaded
                    LaunchedEffect(activeTab.url) {
                        if (!activeTab.isStartPage && !activeTab.isNovaSearchPage && webView.url != activeTab.url) {
                            webView.loadUrl(activeTab.url)
                        }
                    }

                    AndroidView(
                        factory = {
                            (webView.parent as? ViewGroup)?.removeView(webView)
                            webView
                        },
                        update = { /* webView is reactive to tabId */ },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Bottom Navigation Toolbar
            BrowserBottomBar(
                tab = activeTab,
                tabCount = tabs.size,
                isBookmarked = isCurrentBookmarked,
                onBack = { viewModel.goBackCurrentTab() },
                onForward = { viewModel.goForwardCurrentTab() },
                onHome = { viewModel.openUrl("nova://start") },
                onTabsClick = { viewModel.openSheet(ActiveSheet.TAB_GRID) },
                onOpenSheet = { viewModel.openSheet(it) },
                onToggleBookmark = { viewModel.toggleBookmarkCurrentTab() },
                onToggleDesktop = { viewModel.toggleDesktopMode(activeTab.id) },
                onToggleReader = { viewModel.toggleReaderMode(activeTab.id) },
                onToggleForceDark = { viewModel.toggleForceDarkMode(activeTab.id) },
                onSavePdf = { viewModel.savePageAsPdf(context) },
                onSaveOffline = { viewModel.savePageForOffline(context) },
                onShare = { viewModel.shareCurrentPage(context) },
                onNewTab = { isIncognito -> viewModel.newTab(isIncognito = isIncognito) },
                onOpenFindInPage = { viewModel.openFindInPage() }
            )
        }

        // Fullscreen Video View Overlay (e.g. YouTube fullscreen)
        customFullscreenView?.let { fullView ->
            AndroidView(
                factory = {
                    FrameLayout(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        addView(fullView)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Sheets
        when (activeSheet) {
            ActiveSheet.TAB_GRID -> {
                TabGridSheet(
                    tabs = tabs,
                    activeTabId = activeTabId,
                    onSelectTab = { viewModel.selectTab(it) },
                    onCloseTab = { viewModel.closeTab(it) },
                    onCloseAll = { incognitoOnly -> viewModel.closeAllTabs(incognitoOnly) },
                    onNewTab = { isIncognito -> viewModel.newTab(isIncognito = isIncognito) },
                    onDismiss = { viewModel.closeSheet() }
                )
            }
            ActiveSheet.SHIELDS -> {
                ShieldsSheet(
                    tab = activeTab,
                    shieldStats = shieldStats,
                    isGlobalShieldEnabled = shieldsEnabled,
                    onToggleGlobalShield = { viewModel.toggleGlobalShields() },
                    onDismiss = { viewModel.closeSheet() }
                )
            }
            ActiveSheet.BOOKMARKS -> {
                BookmarksSheet(
                    bookmarks = bookmarks,
                    currentTab = activeTab,
                    onSelectUrl = { viewModel.openUrl(it) },
                    onAddCurrentTab = { viewModel.toggleBookmarkCurrentTab() },
                    onDeleteBookmark = { viewModel.deleteBookmark(it) },
                    onDismiss = { viewModel.closeSheet() }
                )
            }
            ActiveSheet.HISTORY -> {
                HistorySheet(
                    history = history,
                    onSelectUrl = { viewModel.openUrl(it) },
                    onDeleteHistoryItem = { viewModel.deleteHistoryItem(it) },
                    onClearAll = { viewModel.clearBrowsingHistory() },
                    onDismiss = { viewModel.closeSheet() }
                )
            }
            ActiveSheet.DOWNLOADS -> {
                DownloadsSheet(
                    downloads = downloads,
                    onClose = { viewModel.closeSheet() },
                    onDeleteDownload = { viewModel.deleteDownload(it) },
                    onClearAll = { viewModel.clearAllDownloads() }
                )
            }
            ActiveSheet.QR_SCANNER -> {
                QrScannerSheet(
                    onClose = { viewModel.closeSheet() },
                    onUrlScanned = { viewModel.openUrl(it) }
                )
            }
            ActiveSheet.CLEAR_DATA -> {
                ClearDataSheet(
                    onClose = { viewModel.closeSheet() },
                    onClearData = { ch, cc, ca, cs ->
                        viewModel.clearBrowsingData(ch, cc, ca, cs)
                    }
                )
            }
            ActiveSheet.SETTINGS -> {
                SettingsSheet(
                    currentEngine = searchEngine,
                    shieldsEnabled = shieldsEnabled,
                    forceDarkMode = forceDarkMode,
                    blockThirdPartyCookies = blockThirdPartyCookies,
                    javascriptEnabled = javascriptEnabled,
                    clearDataOnExit = clearDataOnExit,
                    onSelectEngine = { viewModel.setSearchEngine(it) },
                    onToggleShields = { viewModel.toggleGlobalShields() },
                    onToggleForceDark = { viewModel.toggleGlobalForceDark() },
                    onToggleThirdPartyCookies = { viewModel.toggleBlockThirdPartyCookies() },
                    onToggleJavascript = { viewModel.toggleJavascript() },
                    onToggleClearDataOnExit = { viewModel.toggleClearDataOnExit() },
                    onOpenDownloads = { viewModel.openSheet(ActiveSheet.DOWNLOADS) },
                    onSavePdf = { viewModel.savePageAsPdf(context) },
                    onSaveOffline = { viewModel.savePageForOffline(context) },
                    onOpenClearData = { viewModel.openSheet(ActiveSheet.CLEAR_DATA) },
                    onDismiss = { viewModel.closeSheet() }
                )
            }
            ActiveSheet.NONE -> {}
        }
    }
}
