package com.example.data.repository

import com.example.data.local.BookmarkDao
import com.example.data.local.BookmarkEntity
import com.example.data.local.DownloadDao
import com.example.data.local.DownloadEntity
import com.example.data.local.HistoryDao
import com.example.data.local.HistoryEntity
import com.example.data.local.ShieldDao
import com.example.data.local.ShieldStatsEntity
import kotlinx.coroutines.flow.Flow

class BrowserRepository(
    private val bookmarkDao: BookmarkDao,
    private val historyDao: HistoryDao,
    private val shieldDao: ShieldDao,
    private val downloadDao: DownloadDao
) {
    val bookmarks: Flow<List<BookmarkEntity>> = bookmarkDao.getAllBookmarks()
    val history: Flow<List<HistoryEntity>> = historyDao.getAllHistory()
    val shieldStats: Flow<ShieldStatsEntity?> = shieldDao.getShieldStats()
    val downloads: Flow<List<DownloadEntity>> = downloadDao.getAllDownloads()

    fun isBookmarked(url: String): Flow<Boolean> = bookmarkDao.isBookmarked(url)

    suspend fun addBookmark(title: String, url: String) {
        if (url.isBlank()) return
        bookmarkDao.insertBookmark(
            BookmarkEntity(
                title = title.ifBlank { url },
                url = url
            )
        )
    }

    suspend fun removeBookmarkByUrl(url: String) {
        bookmarkDao.deleteBookmarkByUrl(url)
    }

    suspend fun removeBookmarkById(id: Long) {
        bookmarkDao.deleteBookmarkById(id)
    }

    suspend fun addHistory(title: String, url: String) {
        if (url.isBlank() || url.startsWith("about:") || url.startsWith("nova:")) return
        historyDao.insertHistory(
            HistoryEntity(
                title = title.ifBlank { url },
                url = url,
                visitedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun removeHistoryById(id: Long) {
        historyDao.deleteHistoryById(id)
    }

    suspend fun clearHistory() {
        historyDao.clearAllHistory()
    }

    suspend fun recordBlockedEvent(trackers: Long, ads: Long, dataKb: Long = 18, timeMs: Long = 45) {
        shieldDao.addBlockedStats(trackers, ads, dataKb, timeMs)
    }

    suspend fun addDownload(
        fileName: String,
        url: String,
        mimeType: String? = null,
        totalBytes: Long = 0,
        status: String = "COMPLETED",
        localFilePath: String? = null
    ): Long {
        return downloadDao.insertDownload(
            DownloadEntity(
                fileName = fileName,
                url = url,
                mimeType = mimeType,
                totalBytes = totalBytes,
                downloadedBytes = totalBytes,
                status = status,
                localFilePath = localFilePath
            )
        )
    }

    suspend fun removeDownloadById(id: Long) {
        downloadDao.deleteDownloadById(id)
    }

    suspend fun clearDownloads() {
        downloadDao.clearAllDownloads()
    }
}
