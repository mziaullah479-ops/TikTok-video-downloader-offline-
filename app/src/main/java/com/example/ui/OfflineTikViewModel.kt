package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.PlaylistItem
import com.example.data.model.VideoItem
import com.example.data.repository.OfflineTikRepository
import com.example.data.repository.StorageStats
import com.example.downloader.DownloadState
import com.example.downloader.VideoDownloader
import com.example.player.AudioPlaybackState
import com.example.player.AudioPlayerManager
import com.example.ui.navigation.NavigationTab
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class OfflineTikViewModel(application: Application) : AndroidViewModel(application) {
    val repository = OfflineTikRepository(application)
    val audioPlayerManager = AudioPlayerManager(application)
    private val downloader = VideoDownloader(application, repository)

    // Current navigation tab
    private val _currentTab = MutableStateFlow(NavigationTab.HOME)
    val currentTab: StateFlow<NavigationTab> = _currentTab.asStateFlow()

    // Active media lists from Room
    val videos: StateFlow<List<VideoItem>> = repository.videos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val audioTracks: StateFlow<List<VideoItem>> = repository.audioTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMedia: StateFlow<List<VideoItem>> = repository.allMedia
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<VideoItem>> = repository.favorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<PlaylistItem>> = repository.playlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Downloader state
    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    // Download modal visibility
    private val _showDownloadDialog = MutableStateFlow(false)
    val showDownloadDialog: StateFlow<Boolean> = _showDownloadDialog.asStateFlow()

    // Storage stats
    private val _storageStats = MutableStateFlow<StorageStats?>(null)
    val storageStats: StateFlow<StorageStats?> = _storageStats.asStateFlow()

    // User feedback messages (Toast / Snackbar)
    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    // Audio Player State from manager
    val audioPlaybackState: StateFlow<AudioPlaybackState> = audioPlayerManager.playbackState

    // Video feed targeted initial index (when jumping from downloads to home)
    private val _targetedVideoIndex = MutableStateFlow<Int?>(null)
    val targetedVideoIndex: StateFlow<Int?> = _targetedVideoIndex.asStateFlow()

    init {
        viewModelScope.launch {
            repository.checkAndSeedInitialSamples()
            refreshStorageStats()
        }
    }

    fun selectTab(tab: NavigationTab) {
        _currentTab.value = tab
    }

    fun jumpToHomeVideo(video: VideoItem) {
        val currentVideos = videos.value
        val index = currentVideos.indexOfFirst { it.id == video.id }
        if (index >= 0) {
            _targetedVideoIndex.value = index
        }
        _currentTab.value = NavigationTab.HOME
    }

    fun clearTargetedVideoIndex() {
        _targetedVideoIndex.value = null
    }

    fun toggleFavorite(video: VideoItem) {
        viewModelScope.launch {
            repository.toggleFavorite(video)
        }
    }

    fun deleteMedia(video: VideoItem) {
        viewModelScope.launch {
            repository.deleteMedia(video)
            refreshStorageStats()
            _userMessage.emit("Deleted \"${video.title}\"")
        }
    }

    fun importMediaFromUri(uri: Uri, isMusic: Boolean = false) {
        viewModelScope.launch {
            val result = repository.importMediaFromUri(uri, isMusic)
            result.onSuccess { item ->
                refreshStorageStats()
                _userMessage.emit("Successfully imported: ${item.title}")
            }.onFailure { err ->
                _userMessage.emit("Failed to import file: ${err.localizedMessage}")
            }
        }
    }

    fun openDownloadDialog() {
        _showDownloadDialog.value = true
        _downloadState.value = DownloadState.Idle
    }

    fun closeDownloadDialog() {
        _showDownloadDialog.value = false
        _downloadState.value = DownloadState.Idle
    }

    fun processUrl(rawInput: String) {
        resolveUrl(rawInput)
    }

    fun resolveUrl(rawInput: String) {
        _showDownloadDialog.value = true
        viewModelScope.launch {
            downloader.resolveMediaInfo(rawInput) { state ->
                _downloadState.value = state
            }
        }
    }

    fun downloadMedia(info: com.example.downloader.DownloadableMediaInfo, isAudioOnly: Boolean) {
        viewModelScope.launch {
            downloader.downloadMedia(info, isAudioOnly) { state ->
                _downloadState.value = state
                if (state is DownloadState.Success) {
                    refreshStorageStats()
                }
            }
        }
    }

    fun downloadDirectUrl(rawInput: String, isAudioOnly: Boolean) {
        viewModelScope.launch {
            downloader.resolveMediaInfo(rawInput) { state ->
                _downloadState.value = state
                if (state is DownloadState.MediaReady) {
                    downloadMedia(state.info, isAudioOnly)
                }
            }
        }
    }

    fun saveVideoToGallery(video: VideoItem) {
        viewModelScope.launch {
            val result = repository.saveVideoToDeviceGallery(video)
            result.onSuccess {
                _userMessage.emit("Saved to Phone Gallery (Movies/OfflineTik)")
            }.onFailure { err ->
                _userMessage.emit("Failed to save to gallery: ${err.localizedMessage}")
            }
        }
    }

    fun saveAudioToMusic(audio: VideoItem) {
        viewModelScope.launch {
            val result = repository.saveAudioToDeviceMusic(audio)
            result.onSuccess {
                _userMessage.emit("Saved to Phone Music (Music/OfflineTik)")
            }.onFailure { err ->
                _userMessage.emit("Failed to save to music: ${err.localizedMessage}")
            }
        }
    }

    fun refreshStorageStats() {
        viewModelScope.launch {
            _storageStats.value = repository.getStorageStats()
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            val freedBytes = repository.clearCache()
            val freedMb = String.format("%.1f", freedBytes / (1024.0 * 1024.0))
            refreshStorageStats()
            _userMessage.emit("Cleaned $freedMb MB of cached files")
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                repository.createPlaylist(name)
                _userMessage.emit("Created playlist \"$name\"")
            }
        }
    }

    fun deletePlaylist(playlist: PlaylistItem) {
        viewModelScope.launch {
            repository.deletePlaylist(playlist)
            _userMessage.emit("Deleted playlist \"${playlist.name}\"")
        }
    }

    fun addSongToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            repository.addSongToPlaylist(playlistId, songId)
            _userMessage.emit("Added to playlist")
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            repository.removeSongFromPlaylist(playlistId, songId)
            _userMessage.emit("Removed from playlist")
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayerManager.release()
    }
}
