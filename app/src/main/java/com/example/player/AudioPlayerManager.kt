package com.example.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.model.VideoItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

enum class RepeatMode {
    OFF, ALL, ONE
}

data class AudioPlaybackState(
    val currentTrack: VideoItem? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val isShuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.ALL,
    val queue: List<VideoItem> = emptyList()
)

class AudioPlayerManager(private val context: Context) {
    private var exoPlayer: ExoPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null

    private val _playbackState = MutableStateFlow(AudioPlaybackState())
    val playbackState: StateFlow<AudioPlaybackState> = _playbackState.asStateFlow()

    init {
        initPlayer()
    }

    private fun initPlayer() {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(context).build().apply {
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        when (state) {
                            Player.STATE_BUFFERING -> {
                                _playbackState.value = _playbackState.value.copy(isBuffering = true)
                            }
                            Player.STATE_READY -> {
                                _playbackState.value = _playbackState.value.copy(
                                    isBuffering = false,
                                    totalDurationMs = duration.coerceAtLeast(0L)
                                )
                            }
                            Player.STATE_ENDED -> {
                                handleTrackEnded()
                            }
                            Player.STATE_IDLE -> {
                                _playbackState.value = _playbackState.value.copy(isBuffering = false)
                            }
                        }
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _playbackState.value = _playbackState.value.copy(isPlaying = isPlaying)
                        if (isPlaying) {
                            startProgressUpdates()
                        } else {
                            progressJob?.cancel()
                        }
                    }
                })
            }
        }
    }

    fun playTrack(track: VideoItem, queue: List<VideoItem>) {
        initPlayer()
        val player = exoPlayer ?: return

        _playbackState.value = _playbackState.value.copy(
            currentTrack = track,
            queue = queue,
            currentPositionMs = 0L,
            totalDurationMs = track.durationMs
        )

        val file = File(track.filePath)
        val uri = if (file.exists()) Uri.fromFile(file) else Uri.parse(track.filePath)
        val mediaItem = MediaItem.fromUri(uri)

        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true
    }

    fun togglePlayPause() {
        val player = exoPlayer ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            if (_playbackState.value.currentTrack != null) {
                player.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
        _playbackState.value = _playbackState.value.copy(currentPositionMs = positionMs)
    }

    fun playNext() {
        val currentState = _playbackState.value
        val queue = currentState.queue
        if (queue.isEmpty() || currentState.currentTrack == null) return

        val currentIndex = queue.indexOfFirst { it.id == currentState.currentTrack.id }
        if (currentIndex == -1) return

        val nextIndex = if (currentState.isShuffle) {
            queue.indices.filter { it != currentIndex }.randomOrNull() ?: currentIndex
        } else {
            (currentIndex + 1) % queue.size
        }

        playTrack(queue[nextIndex], queue)
    }

    fun playPrevious() {
        val currentState = _playbackState.value
        val queue = currentState.queue
        if (queue.isEmpty() || currentState.currentTrack == null) return

        val currentIndex = queue.indexOfFirst { it.id == currentState.currentTrack.id }
        if (currentIndex == -1) return

        // If played more than 3 seconds, restart current track
        val player = exoPlayer
        if (player != null && player.currentPosition > 3000L) {
            player.seekTo(0)
            return
        }

        val prevIndex = if (currentIndex - 1 < 0) queue.size - 1 else currentIndex - 1
        playTrack(queue[prevIndex], queue)
    }

    fun toggleShuffle() {
        _playbackState.value = _playbackState.value.copy(isShuffle = !_playbackState.value.isShuffle)
    }

    fun toggleRepeat() {
        val nextMode = when (_playbackState.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _playbackState.value = _playbackState.value.copy(repeatMode = nextMode)
    }

    private fun handleTrackEnded() {
        when (_playbackState.value.repeatMode) {
            RepeatMode.ONE -> {
                exoPlayer?.seekTo(0)
                exoPlayer?.play()
            }
            RepeatMode.ALL -> {
                playNext()
            }
            RepeatMode.OFF -> {
                val queue = _playbackState.value.queue
                val curr = _playbackState.value.currentTrack
                if (queue.isNotEmpty() && curr != null && queue.last().id != curr.id) {
                    playNext()
                } else {
                    exoPlayer?.pause()
                    _playbackState.value = _playbackState.value.copy(isPlaying = false, currentPositionMs = 0L)
                }
            }
        }
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                exoPlayer?.let { player ->
                    _playbackState.value = _playbackState.value.copy(
                        currentPositionMs = player.currentPosition.coerceAtLeast(0L),
                        totalDurationMs = player.duration.coerceAtLeast(0L)
                    )
                }
                delay(300L)
            }
        }
    }

    fun release() {
        progressJob?.cancel()
        exoPlayer?.release()
        exoPlayer = null
    }
}
