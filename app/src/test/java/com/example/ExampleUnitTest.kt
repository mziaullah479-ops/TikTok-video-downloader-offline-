package com.example

import com.example.data.model.PlaylistItem
import com.example.data.model.VideoItem
import com.example.data.repository.StorageStats
import com.example.downloader.VideoDownloader
import com.example.player.AudioPlaybackState
import com.example.player.RepeatMode
import com.example.ui.navigation.NavigationTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testNavigationTabCountAndTitles() {
        assertEquals(4, NavigationTab.values().size)
        assertEquals("Home", NavigationTab.HOME.title)
        assertEquals("Audio", NavigationTab.AUDIO.title)
        assertEquals("Download", NavigationTab.DOWNLOAD.title)
        assertEquals("Setting", NavigationTab.SETTING.title)
    }

    @Test
    fun testUrlExtractionFromSharedText() {
        val sharedText = "Hey check out this video: https://www.tiktok.com/@creator/video/7123456789012345678?is_copy_url=1"
        val extracted = VideoDownloader.extractUrl(sharedText)
        assertEquals("https://www.tiktok.com/@creator/video/7123456789012345678?is_copy_url=1", extracted)
        assertTrue(VideoDownloader.isTikTokUrl(extracted!!))
    }

    @Test
    fun testShortTikTokUrlExtraction() {
        val shortText = "Look at this https://vm.tiktok.com/ZM8abc123/ awesome clip"
        val extracted = VideoDownloader.extractUrl(shortText)
        assertNotNull(extracted)
        assertTrue(VideoDownloader.isTikTokUrl(extracted!!))
    }

    @Test
    fun testDirectMediaUrlDetection() {
        assertTrue(VideoDownloader.isDirectMediaUrl("https://example.com/stream/sample.mp4"))
        assertTrue(VideoDownloader.isDirectMediaUrl("https://example.com/audio.m4a"))
        assertTrue(VideoDownloader.isDirectMediaUrl("https://example.com/video.webm"))
        assertFalse(VideoDownloader.isDirectMediaUrl("https://www.tiktok.com/@user/video/123"))
        assertFalse(VideoDownloader.isDirectMediaUrl("https://example.com/page.html"))
    }

    @Test
    fun testEmptyOrInvalidUrl() {
        assertNull(VideoDownloader.extractUrl("This is just plain text without any links"))
        assertNull(VideoDownloader.extractUrl(""))
    }

    @Test
    fun testVideoItemModel() {
        val item = VideoItem(
            id = 1L,
            title = "Test Neon Video",
            author = "@cyber_hero",
            filePath = "/data/user/0/com.example/files/videos/test.mp4",
            durationMs = 8500L,
            fileSizeBytes = 2048576L,
            isFavorite = true,
            likeCount = 42L,
            isMusic = false
        )
        assertEquals("Test Neon Video", item.title)
        assertEquals("@cyber_hero", item.author)
        assertTrue(item.isFavorite)
        assertEquals(42L, item.likeCount)
        assertFalse(item.isMusic)
    }

    @Test
    fun testAudioPlaybackState() {
        val state = AudioPlaybackState(
            isPlaying = true,
            isShuffle = true,
            repeatMode = RepeatMode.ALL
        )
        assertTrue(state.isPlaying)
        assertTrue(state.isShuffle)
        assertEquals(RepeatMode.ALL, state.repeatMode)
    }

    @Test
    fun testPlaylistItemModel() {
        val pl = PlaylistItem(id = 5L, name = "Night Ambient")
        assertEquals(5L, pl.id)
        assertEquals("Night Ambient", pl.name)
    }

    @Test
    fun testStorageStats() {
        val stats = StorageStats(
            videoBytes = 1000L,
            audioBytes = 500L,
            cacheBytes = 200L,
            totalAppBytes = 1700L,
            deviceAvailableBytes = 10_000_000L,
            deviceTotalBytes = 64_000_000L
        )
        assertEquals(1700L, stats.totalAppBytes)
        assertEquals(10_000_000L, stats.deviceAvailableBytes)
    }

    @Test
    fun testDownloadableMediaInfoModel() {
        val info = com.example.downloader.DownloadableMediaInfo(
            title = "Awesome Dance Clip",
            author = "@dancer",
            description = "Check this dance move #dance",
            videoUrl = "https://example.com/video.mp4",
            audioUrl = "https://example.com/music.mp3",
            soundTitle = "Trending Beat",
            soundAuthor = "@beatmaker",
            coverThumbnailUrl = "https://example.com/thumb.jpg",
            durationSeconds = 15,
            originalSourceUrl = "https://www.tiktok.com/@dancer/video/12345"
        )
        assertEquals("Awesome Dance Clip", info.title)
        assertEquals("Trending Beat", info.soundTitle)
        assertNotNull(info.videoUrl)
        assertNotNull(info.audioUrl)
        assertEquals(15, info.durationSeconds)
    }
}
