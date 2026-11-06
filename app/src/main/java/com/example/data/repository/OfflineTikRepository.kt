package com.example.data.repository

import android.content.ContentValues
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import android.util.Log
import com.example.data.local.OfflineTikDatabase
import com.example.data.model.PlaylistItem
import com.example.data.model.PlaylistSongCrossRef
import com.example.data.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

data class StorageStats(
    val videoBytes: Long,
    val audioBytes: Long,
    val cacheBytes: Long,
    val totalAppBytes: Long,
    val deviceAvailableBytes: Long,
    val deviceTotalBytes: Long
)

class OfflineTikRepository(private val context: Context) {
    private val database = OfflineTikDatabase.getDatabase(context)
    private val videoDao = database.videoDao()
    private val playlistDao = database.playlistDao()

    val videos: Flow<List<VideoItem>> = videoDao.getVideos()
    val audioTracks: Flow<List<VideoItem>> = videoDao.getAudioTracks()
    val allMedia: Flow<List<VideoItem>> = videoDao.getAllMedia()
    val favorites: Flow<List<VideoItem>> = videoDao.getFavorites()
    val playlists: Flow<List<PlaylistItem>> = playlistDao.getAllPlaylists()

    fun getSongsInPlaylist(playlistId: Long): Flow<List<VideoItem>> {
        return playlistDao.getSongsInPlaylist(playlistId)
    }

    suspend fun checkAndSeedInitialSamples() = withContext(Dispatchers.IO) {
        try {
            val count = videoDao.getCount()
            if (count > 0) return@withContext

            val videosDir = File(context.filesDir, "videos").apply { mkdirs() }
            val audioDir = File(context.filesDir, "audio").apply { mkdirs() }

            // 1. Neon Pulse
            val neonFile = copyAssetToFile("samples/sample_neon.mp4", File(videosDir, "sample_neon.mp4"))
            if (neonFile != null && neonFile.exists()) {
                val dur = getMediaDuration(neonFile.absolutePath, 8000L)
                videoDao.insert(
                    VideoItem(
                        title = "Electric Cyber Pulse",
                        author = "@neon_dreams",
                        description = "Neon cyber aesthetics in the night city ⚡ #cyberpunk #neon #glow",
                        filePath = neonFile.absolutePath,
                        durationMs = dur,
                        fileSizeBytes = neonFile.length(),
                        soundTitle = "Cyber Wave - Original Mix",
                        soundAuthor = "Neon Dreams",
                        isFavorite = true,
                        likeCount = 1420L,
                        isMusic = false
                    )
                )
            }

            // 2. Sunset Glow
            val sunsetFile = copyAssetToFile("samples/sample_sunset.mp4", File(videosDir, "sample_sunset.mp4"))
            if (sunsetFile != null && sunsetFile.exists()) {
                val dur = getMediaDuration(sunsetFile.absolutePath, 8000L)
                videoDao.insert(
                    VideoItem(
                        title = "Golden Hour Coast",
                        author = "@sunset_chaser",
                        description = "Catching the golden rays on the ocean drive 🌅 #sunset #vibes #coast",
                        filePath = sunsetFile.absolutePath,
                        durationMs = dur,
                        fileSizeBytes = sunsetFile.length(),
                        soundTitle = "Lofi Breeze - Acoustic Vibes",
                        soundAuthor = "Acoustic Coast",
                        isFavorite = false,
                        likeCount = 890L,
                        isMusic = false
                    )
                )
            }

            // 3. Cyber Teal Bass
            val bassFile = copyAssetToFile("samples/sample_bass.mp4", File(videosDir, "sample_bass.mp4"))
            if (bassFile != null && bassFile.exists()) {
                val dur = getMediaDuration(bassFile.absolutePath, 8000L)
                videoDao.insert(
                    VideoItem(
                        title = "Sub-Zero Future Beat",
                        author = "@sound_architect",
                        description = "Heavy bass drops and future cyber visualizer 🚀 #bass #future #synth",
                        filePath = bassFile.absolutePath,
                        durationMs = dur,
                        fileSizeBytes = bassFile.length(),
                        soundTitle = "Future Drift - Club Edit",
                        soundAuthor = "Sound Architect",
                        isFavorite = true,
                        likeCount = 2310L,
                        isMusic = false
                    )
                )
            }

            // 4. Audio Track 1
            val midnightAudio = copyAssetToFile("samples/sample_midnight.m4a", File(audioDir, "sample_midnight.m4a"))
            var audioId1: Long = 0
            if (midnightAudio != null && midnightAudio.exists()) {
                val dur = getMediaDuration(midnightAudio.absolutePath, 15000L)
                audioId1 = videoDao.insert(
                    VideoItem(
                        title = "Midnight Echoes",
                        author = "Aura Soundscape",
                        description = "Atmospheric chill ambient tones for nighttime focus",
                        filePath = midnightAudio.absolutePath,
                        durationMs = dur,
                        fileSizeBytes = midnightAudio.length(),
                        soundTitle = "Midnight Echoes",
                        soundAuthor = "Aura Soundscape",
                        isFavorite = true,
                        likeCount = 540L,
                        isMusic = true
                    )
                )
            }

            // 5. Audio Track 2
            val retrogradeAudio = copyAssetToFile("samples/sample_retrograde.m4a", File(audioDir, "sample_retrograde.m4a"))
            var audioId2: Long = 0
            if (retrogradeAudio != null && retrogradeAudio.exists()) {
                val dur = getMediaDuration(retrogradeAudio.absolutePath, 15000L)
                audioId2 = videoDao.insert(
                    VideoItem(
                        title = "Retrograde Motion",
                        author = "Neon Pulse",
                        description = "Energetic retro synthwave groove",
                        filePath = retrogradeAudio.absolutePath,
                        durationMs = dur,
                        fileSizeBytes = retrogradeAudio.length(),
                        soundTitle = "Retrograde Motion",
                        soundAuthor = "Neon Pulse",
                        isFavorite = false,
                        likeCount = 312L,
                        isMusic = true
                    )
                )
            }

            // Seed initial Playlist
            val playlistId = playlistDao.insertPlaylist(
                PlaylistItem(name = "Night Drive & Chill")
            )
            if (audioId1 > 0) {
                playlistDao.addSongToPlaylist(PlaylistSongCrossRef(playlistId = playlistId, videoId = audioId1))
            }
            if (audioId2 > 0) {
                playlistDao.addSongToPlaylist(PlaylistSongCrossRef(playlistId = playlistId, videoId = audioId2))
            }

        } catch (e: Exception) {
            Log.e("OfflineTikRepo", "Error seeding sample assets", e)
        }
    }

    private fun copyAssetToFile(assetName: String, destFile: File): File? {
        return try {
            context.assets.open(assetName).use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            destFile
        } catch (e: Exception) {
            Log.w("OfflineTikRepo", "Could not copy asset $assetName: ${e.message}")
            null
        }
    }

    private fun getMediaDuration(filePath: String, fallback: Long): Long {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(filePath)
            val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            time?.toLongOrNull() ?: fallback
        } catch (e: Exception) {
            fallback
        }
    }

    suspend fun toggleFavorite(video: VideoItem) = withContext(Dispatchers.IO) {
        val newFav = !video.isFavorite
        val newCount = if (newFav) video.likeCount + 1 else (video.likeCount - 1).coerceAtLeast(0)
        videoDao.updateFavorite(video.id, newFav, newCount)
    }

    suspend fun deleteMedia(video: VideoItem) = withContext(Dispatchers.IO) {
        try {
            val file = File(video.filePath)
            if (file.exists()) {
                file.delete()
            }
            if (video.thumbnailPath != null) {
                val thumb = File(video.thumbnailPath)
                if (thumb.exists()) thumb.delete()
            }
        } catch (e: Exception) {
            Log.e("OfflineTikRepo", "Error deleting file: ${video.filePath}", e)
        }
        videoDao.deleteById(video.id)
    }

    suspend fun importMediaFromUri(uri: Uri, isMusic: Boolean = false): Result<VideoItem> = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val mimeType = resolver.getType(uri) ?: if (isMusic) "audio/mp4" else "video/mp4"
            val isAudio = isMusic || mimeType.startsWith("audio")

            val targetDir = if (isAudio) {
                File(context.filesDir, "audio").apply { mkdirs() }
            } else {
                File(context.filesDir, "videos").apply { mkdirs() }
            }

            val extension = when {
                mimeType.contains("mp4") -> "mp4"
                mimeType.contains("m4a") -> "m4a"
                mimeType.contains("mp3") -> "mp3"
                mimeType.contains("webm") -> "webm"
                mimeType.contains("ogg") -> "ogg"
                mimeType.contains("wav") -> "wav"
                isAudio -> "m4a"
                else -> "mp4"
            }

            val fileName = "imported_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.$extension"
            val targetFile = File(targetDir, fileName)

            resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(Exception("Failed to open input stream for Uri: $uri"))

            // Extract real metadata
            var extractedTitle = if (isAudio) "Imported Audio Track" else "Imported Video"
            var extractedAuthor = "@local_import"
            var extractedDuration = 0L

            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(targetFile.absolutePath)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.let {
                    if (it.isNotBlank()) extractedTitle = it
                }
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.let {
                    if (it.isNotBlank()) extractedAuthor = it
                }
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.let {
                    extractedDuration = it
                }
                retriever.release()
            } catch (e: Exception) {
                Log.w("OfflineTikRepo", "Retriever failed on imported file: ${e.message}")
            }

            val item = VideoItem(
                title = extractedTitle,
                author = extractedAuthor,
                description = "Locally imported media file (${targetFile.name})",
                filePath = targetFile.absolutePath,
                durationMs = extractedDuration,
                fileSizeBytes = targetFile.length(),
                soundTitle = extractedTitle,
                soundAuthor = extractedAuthor,
                isFavorite = false,
                likeCount = 0L,
                isMusic = isAudio
            )

            val id = videoDao.insert(item)
            val created = item.copy(id = id)
            Result.success(created)
        } catch (e: Exception) {
            Log.e("OfflineTikRepo", "Failed to import media", e)
            Result.failure(e)
        }
    }

    suspend fun saveDownloadedVideo(
        title: String,
        author: String,
        description: String,
        tempFile: File,
        sourceUrl: String?
    ): VideoItem = withContext(Dispatchers.IO) {
        val videosDir = File(context.filesDir, "videos").apply { mkdirs() }
        val extension = tempFile.extension.ifBlank { "mp4" }
        val finalFile = File(videosDir, "saved_${System.currentTimeMillis()}.$extension")
        tempFile.copyTo(finalFile, overwrite = true)
        tempFile.delete()

        val dur = getMediaDuration(finalFile.absolutePath, 0L)
        val item = VideoItem(
            title = title,
            author = author,
            description = description,
            filePath = finalFile.absolutePath,
            durationMs = dur,
            fileSizeBytes = finalFile.length(),
            soundTitle = "$title (Sound)",
            soundAuthor = author,
            isFavorite = false,
            likeCount = 0L,
            sourceUrl = sourceUrl,
            isMusic = false
        )
        val id = videoDao.insert(item)
        item.copy(id = id)
    }

    suspend fun saveDownloadedAudio(
        title: String,
        author: String,
        soundTitle: String,
        soundAuthor: String,
        tempFile: File,
        sourceUrl: String?
    ): VideoItem = withContext(Dispatchers.IO) {
        val audioDir = File(context.filesDir, "audio").apply { mkdirs() }
        val extension = tempFile.extension.ifBlank { "m4a" }
        val finalFile = File(audioDir, "audio_${System.currentTimeMillis()}.$extension")
        tempFile.copyTo(finalFile, overwrite = true)
        tempFile.delete()

        val dur = getMediaDuration(finalFile.absolutePath, 0L)
        val item = VideoItem(
            title = title,
            author = author,
            description = "Offline TikTok sound from $soundAuthor",
            filePath = finalFile.absolutePath,
            durationMs = dur,
            fileSizeBytes = finalFile.length(),
            soundTitle = soundTitle,
            soundAuthor = soundAuthor,
            isFavorite = false,
            likeCount = 0L,
            sourceUrl = sourceUrl,
            isMusic = true
        )
        val id = videoDao.insert(item)
        item.copy(id = id)
    }

    suspend fun extractAudioFromVideoFile(videoFile: File, outputFile: File): Boolean = withContext(Dispatchers.IO) {
        var extractor: android.media.MediaExtractor? = null
        var muxer: android.media.MediaMuxer? = null
        try {
            extractor = android.media.MediaExtractor()
            extractor.setDataSource(videoFile.absolutePath)
            var audioTrackIndex = -1
            var audioFormat: android.media.MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(android.media.MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = format
                    break
                }
            }
            if (audioTrackIndex == -1 || audioFormat == null) {
                return@withContext false
            }

            extractor.selectTrack(audioTrackIndex)
            muxer = android.media.MediaMuxer(outputFile.absolutePath, android.media.MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val muxerTrackIndex = muxer.addTrack(audioFormat)
            muxer.start()

            val maxBufferSize = try {
                audioFormat.getInteger(android.media.MediaFormat.KEY_MAX_INPUT_SIZE)
            } catch (e: Exception) {
                64 * 1024
            }
            val buffer = java.nio.ByteBuffer.allocate(maxBufferSize)
            val bufferInfo = android.media.MediaCodec.BufferInfo()

            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) {
                    bufferInfo.size = 0
                    break
                } else {
                    bufferInfo.presentationTimeUs = extractor.sampleTime
                    bufferInfo.flags = extractor.sampleFlags
                    muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
                    extractor.advance()
                }
            }
            muxer.stop()
            true
        } catch (e: Exception) {
            Log.e("OfflineTikRepo", "Error extracting audio track", e)
            false
        } finally {
            try { muxer?.release() } catch (_: Exception) {}
            try { extractor?.release() } catch (_: Exception) {}
        }
    }

    suspend fun saveVideoToDeviceGallery(video: VideoItem): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(video.filePath)
            if (!sourceFile.exists()) {
                return@withContext Result.failure(Exception("Source video file not found on disk."))
            }

            val fileName = "OfflineTik_${System.currentTimeMillis()}_${sourceFile.name}"
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/OfflineTik")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
            }

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }

            val itemUri = resolver.insert(collection, contentValues)
                ?: return@withContext Result.failure(Exception("Could not create MediaStore gallery entry."))

            resolver.openOutputStream(itemUri)?.use { out ->
                sourceFile.inputStream().use { input ->
                    input.copyTo(out)
                }
            } ?: return@withContext Result.failure(Exception("Could not write to MediaStore stream."))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(itemUri, contentValues, null, null)
            }

            Result.success(itemUri)
        } catch (e: Exception) {
            Log.e("OfflineTikRepo", "Error saving video to gallery", e)
            Result.failure(e)
        }
    }

    suspend fun saveAudioToDeviceMusic(audio: VideoItem): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(audio.filePath)
            if (!sourceFile.exists()) {
                return@withContext Result.failure(Exception("Source audio file not found on disk."))
            }

            val fileName = "OfflineTik_${System.currentTimeMillis()}_${sourceFile.name}"
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/OfflineTik")
                    put(MediaStore.Audio.Media.IS_PENDING, 1)
                }
            }

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

            val itemUri = resolver.insert(collection, contentValues)
                ?: return@withContext Result.failure(Exception("Could not create MediaStore audio entry."))

            resolver.openOutputStream(itemUri)?.use { out ->
                sourceFile.inputStream().use { input ->
                    input.copyTo(out)
                }
            } ?: return@withContext Result.failure(Exception("Could not write to MediaStore stream."))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
                resolver.update(itemUri, contentValues, null, null)
            }

            Result.success(itemUri)
        } catch (e: Exception) {
            Log.e("OfflineTikRepo", "Error saving audio to music", e)
            Result.failure(e)
        }
    }

    suspend fun createPlaylist(name: String): Long = withContext(Dispatchers.IO) {
        playlistDao.insertPlaylist(PlaylistItem(name = name.trim()))
    }

    suspend fun deletePlaylist(playlist: PlaylistItem) = withContext(Dispatchers.IO) {
        playlistDao.deletePlaylist(playlist)
    }

    suspend fun addSongToPlaylist(playlistId: Long, songId: Long) = withContext(Dispatchers.IO) {
        playlistDao.addSongToPlaylist(PlaylistSongCrossRef(playlistId = playlistId, videoId = songId))
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) = withContext(Dispatchers.IO) {
        playlistDao.removeSongFromPlaylist(playlistId, songId)
    }

    suspend fun getStorageStats(): StorageStats = withContext(Dispatchers.IO) {
        val videosDir = File(context.filesDir, "videos")
        val audioDir = File(context.filesDir, "audio")
        val cacheDir = context.cacheDir

        fun dirSize(dir: File): Long {
            if (!dir.exists()) return 0L
            return dir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
        }

        val videoBytes = dirSize(videosDir)
        val audioBytes = dirSize(audioDir)
        val cacheBytes = dirSize(cacheDir)
        val totalAppBytes = videoBytes + audioBytes + cacheBytes

        var devAvail = 0L
        var devTotal = 0L
        try {
            val stat = StatFs(context.filesDir.path)
            devAvail = stat.availableBlocksLong * stat.blockSizeLong
            devTotal = stat.blockCountLong * stat.blockSizeLong
        } catch (e: Exception) {
            devAvail = 10L * 1024 * 1024 * 1024
            devTotal = 64L * 1024 * 1024 * 1024
        }

        StorageStats(
            videoBytes = videoBytes,
            audioBytes = audioBytes,
            cacheBytes = cacheBytes,
            totalAppBytes = totalAppBytes,
            deviceAvailableBytes = devAvail,
            deviceTotalBytes = devTotal
        )
    }

    suspend fun clearCache(): Long = withContext(Dispatchers.IO) {
        var freed = 0L
        try {
            val cacheDir = context.cacheDir
            if (cacheDir.exists()) {
                cacheDir.listFiles()?.forEach { file ->
                    freed += file.length()
                    file.deleteRecursively()
                }
            }
        } catch (e: Exception) {
            Log.e("OfflineTikRepo", "Error clearing cache", e)
        }
        freed
    }
}
