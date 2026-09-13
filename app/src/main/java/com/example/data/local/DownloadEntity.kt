package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val url: String,
    val mimeType: String? = null,
    val totalBytes: Long = 0,
    val downloadedBytes: Long = 0,
    val status: String = "COMPLETED", // "DOWNLOADING", "COMPLETED", "FAILED"
    val localFilePath: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
