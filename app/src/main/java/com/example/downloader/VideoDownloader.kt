package com.example.downloader

import android.content.Context
import android.util.Log
import com.example.data.model.VideoItem
import com.example.data.repository.OfflineTikRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class DownloadableMediaInfo(
    val title: String,
    val author: String,
    val description: String,
    val videoUrl: String?,
    val audioUrl: String?,
    val soundTitle: String,
    val soundAuthor: String,
    val coverThumbnailUrl: String?,
    val durationSeconds: Int,
    val originalSourceUrl: String
)

sealed class DownloadState {
    object Idle : DownloadState()
    data class Validating(val url: String) : DownloadState()
    data class MediaReady(val info: DownloadableMediaInfo) : DownloadState()
    data class Downloading(
        val isAudioOnly: Boolean,
        val progressPercent: Int,
        val bytesDownloaded: Long,
        val totalBytes: Long
    ) : DownloadState()
    data class Success(val mediaItem: VideoItem, val isAudioOnly: Boolean) : DownloadState()
    data class Error(val title: String, val message: String, val canImportLocally: Boolean = true) : DownloadState()
}

class VideoDownloader(
    private val context: Context,
    private val repository: OfflineTikRepository
) {
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    companion object {
        private val URL_REGEX = Pattern.compile("https?://[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]+")

        fun extractUrl(text: String): String? {
            val matcher = URL_REGEX.matcher(text)
            return if (matcher.find()) {
                matcher.group(0)?.trim()
            } else null
        }

        fun isTikTokUrl(url: String): Boolean {
            val lower = url.lowercase()
            return lower.contains("tiktok.com") || lower.contains("tiktokv.com")
        }

        fun isDirectMediaUrl(url: String): Boolean {
            val lower = url.lowercase()
            return lower.endsWith(".mp4") || lower.endsWith(".webm") ||
                    lower.endsWith(".mov") || lower.endsWith(".m4a") ||
                    lower.endsWith(".mp3")
        }
    }

    suspend fun resolveMediaInfo(
        rawInput: String,
        onProgress: (DownloadState) -> Unit
    ): DownloadableMediaInfo? = withContext(Dispatchers.IO) {
        val extractedUrl = extractUrl(rawInput)
        if (extractedUrl == null) {
            val error = DownloadState.Error(
                title = "Invalid URL",
                message = "Could not find a valid web or video link in the text.",
                canImportLocally = true
            )
            onProgress(error)
            return@withContext null
        }

        onProgress(DownloadState.Validating(extractedUrl))

        try {
            if (isDirectMediaUrl(extractedUrl)) {
                val fileName = extractedUrl.substringAfterLast("/").substringBefore("?").ifBlank { "Media File" }
                val isAudio = extractedUrl.endsWith(".m4a") || extractedUrl.endsWith(".mp3")
                val info = DownloadableMediaInfo(
                    title = fileName,
                    author = "@direct_source",
                    description = "Direct media stream from $extractedUrl",
                    videoUrl = if (isAudio) null else extractedUrl,
                    audioUrl = if (isAudio) extractedUrl else null,
                    soundTitle = "$fileName Sound",
                    soundAuthor = "Original Audio",
                    coverThumbnailUrl = null,
                    durationSeconds = 0,
                    originalSourceUrl = extractedUrl
                )
                onProgress(DownloadState.MediaReady(info))
                return@withContext info
            }

            if (isTikTokUrl(extractedUrl)) {
                // Method 1: Try public TikTok resolver API (TikWM) for high-quality video & audio separation
                val encodedUrl = URLEncoder.encode(extractedUrl, "UTF-8")
                val apiUrl = "https://www.tikwm.com/api/?url=$encodedUrl"

                try {
                    val apiRequest = Request.Builder()
                        .url(apiUrl)
                        .header("User-Agent", "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36")
                        .build()

                    val apiResponse = client.newCall(apiRequest).execute()
                    if (apiResponse.isSuccessful) {
                        val body = apiResponse.body?.string() ?: ""
                        if (body.isNotBlank()) {
                            val json = JSONObject(body)
                            if (json.optInt("code") == 0) {
                                val data = json.optJSONObject("data")
                                if (data != null) {
                                    val videoPlayUrl = data.optString("play").ifBlank { data.optString("wmplay") }
                                    val musicPlayUrl = data.optString("music")
                                    val title = data.optString("title").ifBlank { "TikTok Video" }
                                    val authorNick = data.optJSONObject("author")?.optString("nickname")?.ifBlank { "@creator" } ?: "@creator"
                                    val soundTitle = data.optJSONObject("music_info")?.optString("title")?.ifBlank { "Original Sound" } ?: "Original Sound"
                                    val soundAuthor = data.optJSONObject("music_info")?.optString("author")?.ifBlank { authorNick } ?: authorNick
                                    val cover = data.optString("cover")
                                    val duration = data.optInt("duration", 0)

                                    val info = DownloadableMediaInfo(
                                        title = title,
                                        author = authorNick,
                                        description = title,
                                        videoUrl = videoPlayUrl.ifBlank { null },
                                        audioUrl = musicPlayUrl.ifBlank { null },
                                        soundTitle = soundTitle,
                                        soundAuthor = soundAuthor,
                                        coverThumbnailUrl = cover.ifBlank { null },
                                        durationSeconds = duration,
                                        originalSourceUrl = extractedUrl
                                    )
                                    onProgress(DownloadState.MediaReady(info))
                                    return@withContext info
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w("VideoDownloader", "API resolver fallback: ${e.message}")
                }

                // Method 2: Public TikTok oEmbed fallback
                var oembedTitle = "TikTok Video"
                var oembedAuthor = "@creator"
                var oembedThumb: String? = null

                try {
                    val oembedUrl = "https://www.tiktok.com/oembed?url=$encodedUrl"
                    val oembedReq = Request.Builder().url(oembedUrl).build()
                    val oembedResp = client.newCall(oembedReq).execute()
                    if (oembedResp.isSuccessful) {
                        val oembedBody = oembedResp.body?.string() ?: ""
                        if (oembedBody.isNotBlank()) {
                            val oembedJson = JSONObject(oembedBody)
                            oembedTitle = oembedJson.optString("title", oembedTitle)
                            oembedAuthor = oembedJson.optString("author_name", oembedAuthor)
                            oembedThumb = oembedJson.optString("thumbnail_url", null)
                        }
                    }
                } catch (e: Exception) {
                    Log.w("VideoDownloader", "oEmbed error: ${e.message}")
                }

                // Method 3: OpenGraph direct web page parsing
                val pageRequest = Request.Builder()
                    .url(extractedUrl)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                    .build()

                val response = client.newCall(pageRequest).execute()
                val pageBody = response.body?.string() ?: ""

                var mediaUrl: String? = null
                val ogVideoMatcher = Pattern.compile("<meta property=\"og:video\" content=\"([^\"]+)\"").matcher(pageBody)
                val ogVideoSecureMatcher = Pattern.compile("<meta property=\"og:video:secure_url\" content=\"([^\"]+)\"").matcher(pageBody)

                if (ogVideoSecureMatcher.find()) {
                    mediaUrl = ogVideoSecureMatcher.group(1)
                } else if (ogVideoMatcher.find()) {
                    mediaUrl = ogVideoMatcher.group(1)
                }

                if (mediaUrl.isNullOrBlank()) {
                    // Check video URL in script JSON tags
                    val scriptMatcher = Pattern.compile("\"playAddr\":\"([^\"]+)\"").matcher(pageBody)
                    if (scriptMatcher.find()) {
                        mediaUrl = scriptMatcher.group(1)?.replace("\\u002F", "/")
                    }
                }

                if (!mediaUrl.isNullOrBlank()) {
                    val info = DownloadableMediaInfo(
                        title = oembedTitle,
                        author = oembedAuthor,
                        description = oembedTitle,
                        videoUrl = mediaUrl,
                        audioUrl = null, // audio will be extracted from video if requested
                        soundTitle = "$oembedTitle (Sound)",
                        soundAuthor = oembedAuthor,
                        coverThumbnailUrl = oembedThumb,
                        durationSeconds = 0,
                        originalSourceUrl = extractedUrl
                    )
                    onProgress(DownloadState.MediaReady(info))
                    return@withContext info
                }

                // If platform protection prevents direct web stream
                val error = DownloadState.Error(
                    title = "Platform Protected Video",
                    message = "TikTok has protected this link with DRM or anti-scraping. You can save this video in TikTok, then tap 'Import Video from Gallery' below to add it to your offline vault.",
                    canImportLocally = true
                )
                onProgress(error)
                return@withContext null
            }

            // General web link
            val info = DownloadableMediaInfo(
                title = "Web Video",
                author = "@web_source",
                description = "Media link: $extractedUrl",
                videoUrl = extractedUrl,
                audioUrl = null,
                soundTitle = "Web Audio",
                soundAuthor = "Online Audio",
                coverThumbnailUrl = null,
                durationSeconds = 0,
                originalSourceUrl = extractedUrl
            )
            onProgress(DownloadState.MediaReady(info))
            return@withContext info

        } catch (e: Exception) {
            Log.e("VideoDownloader", "Resolve error", e)
            val error = DownloadState.Error(
                title = "Resolution Failed",
                message = e.localizedMessage ?: "Unable to resolve media from this link.",
                canImportLocally = true
            )
            onProgress(error)
            return@withContext null
        }
    }

    suspend fun downloadMedia(
        info: DownloadableMediaInfo,
        isAudioOnly: Boolean,
        onProgress: (DownloadState) -> Unit
    ): DownloadState = withContext(Dispatchers.IO) {
        val streamUrl = if (isAudioOnly) {
            info.audioUrl ?: info.videoUrl
        } else {
            info.videoUrl ?: info.audioUrl
        }

        if (streamUrl.isNullOrBlank()) {
            val error = DownloadState.Error(
                title = "No Stream Available",
                message = "Could not locate a valid downloadable stream for this media.",
                canImportLocally = true
            )
            onProgress(error)
            return@withContext error
        }

        onProgress(DownloadState.Downloading(isAudioOnly, 0, 0L, 0L))

        try {
            val downloadRequest = Request.Builder()
                .url(streamUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36")
                .header("Referer", "https://www.tiktok.com/")
                .build()

            val downloadResponse = client.newCall(downloadRequest).execute()
            if (!downloadResponse.isSuccessful) {
                val error = DownloadState.Error(
                    title = "Download Failed",
                    message = "Server returned HTTP ${downloadResponse.code} while streaming media content.",
                    canImportLocally = true
                )
                onProgress(error)
                return@withContext error
            }

            val responseBody = downloadResponse.body
            if (responseBody == null) {
                val error = DownloadState.Error(
                    title = "Empty Content",
                    message = "The server returned an empty media stream.",
                    canImportLocally = true
                )
                onProgress(error)
                return@withContext error
            }

            val totalBytes = responseBody.contentLength()
            val isDirectAudioStream = isAudioOnly && info.audioUrl != null && streamUrl == info.audioUrl
            val tempExtension = if (isDirectAudioStream) "m4a" else "mp4"
            val tempFile = File(context.cacheDir, "temp_dl_${System.currentTimeMillis()}.$tempExtension")

            responseBody.byteStream().use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead = 0L
                    var lastPercent = 0

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (totalBytes > 0) {
                            val percent = ((totalRead * 100) / totalBytes).toInt()
                            if (percent != lastPercent) {
                                lastPercent = percent
                                onProgress(DownloadState.Downloading(isAudioOnly, percent, totalRead, totalBytes))
                            }
                        } else {
                            onProgress(DownloadState.Downloading(isAudioOnly, 50, totalRead, -1L))
                        }
                    }
                }
            }

            if (isAudioOnly) {
                val finalAudioFile: File
                if (isDirectAudioStream) {
                    finalAudioFile = tempFile
                } else {
                    // Extract audio track from the downloaded video using MediaExtractor & MediaMuxer
                    val extractedFile = File(context.cacheDir, "extracted_${System.currentTimeMillis()}.m4a")
                    val extracted = repository.extractAudioFromVideoFile(tempFile, extractedFile)
                    tempFile.delete()
                    finalAudioFile = if (extracted && extractedFile.exists()) extractedFile else {
                        // Fallback: save file as audio
                        tempFile
                    }
                }

                val savedAudio = repository.saveDownloadedAudio(
                    title = info.soundTitle.ifBlank { "${info.title} (Audio)" },
                    author = info.soundAuthor.ifBlank { info.author },
                    soundTitle = info.soundTitle,
                    soundAuthor = info.soundAuthor,
                    tempFile = finalAudioFile,
                    sourceUrl = info.originalSourceUrl
                )
                val success = DownloadState.Success(savedAudio, isAudioOnly = true)
                onProgress(success)
                return@withContext success
            } else {
                // Save Full Video
                val savedVideo = repository.saveDownloadedVideo(
                    title = info.title,
                    author = info.author,
                    description = info.description,
                    tempFile = tempFile,
                    sourceUrl = info.originalSourceUrl
                )
                val success = DownloadState.Success(savedVideo, isAudioOnly = false)
                onProgress(success)
                return@withContext success
            }

        } catch (e: Exception) {
            Log.e("VideoDownloader", "Download execution error", e)
            val error = DownloadState.Error(
                title = "Download Error",
                message = e.localizedMessage ?: "Failed while saving media file.",
                canImportLocally = true
            )
            onProgress(error)
            return@withContext error
        }
    }
}
