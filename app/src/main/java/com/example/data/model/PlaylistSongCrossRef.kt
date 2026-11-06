package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "playlist_songs",
    indices = [
        Index(value = ["playlistId", "videoId"], unique = true)
    ]
)
data class PlaylistSongCrossRef(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val playlistId: Long,
    val videoId: Long,
    val addedAt: Long = System.currentTimeMillis()
)
