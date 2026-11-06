package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.PlaylistItem
import com.example.data.model.PlaylistSongCrossRef
import com.example.data.model.VideoItem
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistItem): Long

    @Delete
    suspend fun deletePlaylist(playlist: PlaylistItem)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylistById(id: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addSongToPlaylist(ref: PlaylistSongCrossRef): Long

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND videoId = :videoId")
    suspend fun removeSongFromPlaylist(playlistId: Long, videoId: Long)

    @Query("""
        SELECT v.* FROM videos v
        INNER JOIN playlist_songs ps ON v.id = ps.videoId
        WHERE ps.playlistId = :playlistId
        ORDER BY ps.addedAt DESC
    """)
    fun getSongsInPlaylist(playlistId: Long): Flow<List<VideoItem>>
}
