package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        BookmarkEntity::class,
        HistoryEntity::class,
        ShieldStatsEntity::class,
        DownloadEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class BrowserDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun historyDao(): HistoryDao
    abstract fun shieldDao(): ShieldDao
    abstract fun downloadDao(): DownloadDao

    companion object {
        @Volatile
        private var INSTANCE: BrowserDatabase? = null

        fun getInstance(context: Context): BrowserDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BrowserDatabase::class.java,
                    "nova_browser.db"
                ).fallbackToDestructiveMigration()
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            val database = getInstance(context)
                            database.shieldDao().ensureInitialized()
                            // Seed essential default bookmarks
                            val defaultBookmarks = listOf(
                                BookmarkEntity(title = "DuckDuckGo", url = "https://duckduckgo.com"),
                                BookmarkEntity(title = "Wikipedia", url = "https://en.wikipedia.org"),
                                BookmarkEntity(title = "GitHub", url = "https://github.com"),
                                BookmarkEntity(title = "Google", url = "https://www.google.com"),
                                BookmarkEntity(title = "Reddit", url = "https://www.reddit.com"),
                                BookmarkEntity(title = "BBC News", url = "https://www.bbc.com/news")
                            )
                            defaultBookmarks.forEach { database.bookmarkDao().insertBookmark(it) }
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
