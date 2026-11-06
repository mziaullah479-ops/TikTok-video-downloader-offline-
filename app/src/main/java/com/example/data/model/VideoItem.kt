package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "videos")
data class VideoItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val author: String = "@creator",
    val description: String = "",
    val filePath: String,
    val thumbnailPath: String? = null,
    val durationMs: Long = 0L,
    val fileSizeBytes: Long = 0L,
    val soundTitle: String = "Original Sound",
    val soundAuthor: String = "OfflineTik Audio",
    val isFavorite: Boolean = false,
    val likeCount: Long = 0L,
    val sourceUrl: String? = null,
    val dateAdded: Long = System.currentTimeMillis(),
    val isMusic: Boolean = false
)
