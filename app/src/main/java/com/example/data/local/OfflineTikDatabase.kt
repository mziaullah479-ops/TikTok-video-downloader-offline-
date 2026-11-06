package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.PlaylistItem
import com.example.data.model.PlaylistSongCrossRef
import com.example.data.model.VideoItem

@Database(
    entities = [
        VideoItem::class,
        PlaylistItem::class,
        PlaylistSongCrossRef::class
    ],
    version = 1,
    exportSchema = false
)
abstract class OfflineTikDatabase : RoomDatabase() {
    abstract fun videoDao(): VideoDao
    abstract fun playlistDao(): PlaylistDao

    companion object {
        @Volatile
        private var INSTANCE: OfflineTikDatabase? = null

        fun getDatabase(context: Context): OfflineTikDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OfflineTikDatabase::class.java,
                    "offlinetik_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
