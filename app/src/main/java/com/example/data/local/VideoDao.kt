package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.VideoItem
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    @Query("SELECT * FROM videos ORDER BY dateAdded DESC")
    fun getAllMedia(): Flow<List<VideoItem>>

    @Query("SELECT * FROM videos WHERE isMusic = 0 ORDER BY dateAdded DESC")
    fun getVideos(): Flow<List<VideoItem>>

    @Query("SELECT * FROM videos WHERE isMusic = 1 ORDER BY dateAdded DESC")
    fun getAudioTracks(): Flow<List<VideoItem>>

    @Query("SELECT * FROM videos WHERE isFavorite = 1 ORDER BY dateAdded DESC")
    fun getFavorites(): Flow<List<VideoItem>>

    @Query("SELECT * FROM videos WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): VideoItem?

    @Query("SELECT * FROM videos WHERE filePath = :filePath LIMIT 1")
    suspend fun findByFilePath(filePath: String): VideoItem?

    @Query("SELECT * FROM videos WHERE sourceUrl = :url LIMIT 1")
    suspend fun findBySourceUrl(url: String): VideoItem?

    @Query("SELECT COUNT(*) FROM videos")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: VideoItem): Long

    @Update
    suspend fun update(item: VideoItem)

    @Delete
    suspend fun delete(item: VideoItem)

    @Query("DELETE FROM videos WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE videos SET isFavorite = :isFavorite, likeCount = :likeCount WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean, likeCount: Long)
}
