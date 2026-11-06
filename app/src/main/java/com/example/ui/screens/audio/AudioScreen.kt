package com.example.ui.screens.audio

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode as AnimRepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.model.PlaylistItem
import com.example.data.model.VideoItem
import com.example.player.AudioPlaybackState
import com.example.player.AudioPlayerManager
import com.example.player.RepeatMode
import com.example.ui.OfflineTikViewModel
import com.example.ui.theme.BrandGradient
import com.example.ui.theme.DividerDark
import com.example.ui.theme.HeartRed
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.PureBlack
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevatedDark
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioScreen(
    viewModel: OfflineTikViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val audioTracks by viewModel.audioTracks.collectAsState()
    val allMedia by viewModel.allMedia.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val playbackState by viewModel.audioPlaybackState.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var showNewPlaylistDialog by remember { mutableStateOf(false) }
    var showExpandedPlayer by remember { mutableStateOf(false) }
    var songForPlaylistSelection by remember { mutableStateOf<VideoItem?>(null) }
    var selectedPlaylistForDetail by remember { mutableStateOf<PlaylistItem?>(null) }

    // Combined audio pool: explicit audio tracks plus any videos (videos have audio tracks too)
    val combinedAudioPool = remember(audioTracks, allMedia) {
        val list = mutableListOf<VideoItem>()
        list.addAll(audioTracks)
        allMedia.filter { !it.isMusic }.forEach { vid ->
            if (list.none { it.id == vid.id }) {
                list.add(vid)
            }
        }
        list
    }

    val filteredTracks = remember(combinedAudioPool, searchQuery) {
        if (searchQuery.isBlank()) combinedAudioPool
        else combinedAudioPool.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.author.contains(searchQuery, ignoreCase = true) ||
            it.soundTitle.contains(searchQuery, ignoreCase = true)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
            .testTag("audio_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Offline Music",
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "${combinedAudioPool.size} offline audio files available",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }

                IconButton(
                    onClick = { showNewPlaylistDialog = true },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SurfaceVariantDark)
                        .testTag("new_playlist_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlaylistAdd,
                        contentDescription = "New Playlist",
                        tint = NeonCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search songs, artists, audio...", color = TextMuted, fontSize = 13.sp) },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = TextMuted) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear", tint = TextMuted)
                        }
                    }
                },
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .testTag("audio_search_input")
            )

            // Tabs: All Songs, Playlists, Favorites
            val tabTitles = listOf("All Tracks", "Playlists", "Favorites")
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = PureBlack,
                contentColor = NeonPink,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = NeonPink
                    )
                },
                divider = { Box(Modifier.fillMaxWidth().height(1.dp).background(DividerDark)) }
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                color = if (selectedTabIndex == index) TextPrimary else TextMuted,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        }
                    )
                }
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedTabIndex) {
                    0 -> {
                        // All Tracks
                        if (filteredTracks.isEmpty()) {
                            EmptyAudioView(message = if (searchQuery.isNotBlank()) "No songs match \"$searchQuery\"" else "No audio files available")
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = if (playbackState.currentTrack != null) 72.dp else 8.dp)
                            ) {
                                items(filteredTracks, key = { it.id }) { track ->
                                    val isCurrent = playbackState.currentTrack?.id == track.id
                                        AudioTrackListItem(
                                            track = track,
                                            isCurrent = isCurrent,
                                            isPlaying = isCurrent && playbackState.isPlaying,
                                            onTrackClick = {
                                                viewModel.audioPlayerManager.playTrack(track, filteredTracks)
                                            },
                                            onAddToPlaylist = { songForPlaylistSelection = track },
                                            onSaveToMusic = { viewModel.saveAudioToMusic(track) },
                                            onShare = { shareAudioFile(context, track) },
                                            onDelete = { viewModel.deleteMedia(track) }
                                        )
                                }
                            }
                        }
                    }
                    1 -> {
                        // Playlists Tab
                        if (playlists.isEmpty()) {
                            EmptyAudioView(
                                message = "No playlists created yet",
                                actionLabel = "Create First Playlist",
                                onAction = { showNewPlaylistDialog = true }
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp, vertical = 12.dp)
                                    .padding(bottom = if (playbackState.currentTrack != null) 72.dp else 8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(playlists, key = { it.id }) { playlist ->
                                    PlaylistItemCard(
                                        playlist = playlist,
                                        onClick = { selectedPlaylistForDetail = playlist },
                                        onDelete = { viewModel.deletePlaylist(playlist) }
                                    )
                                }
                            }
                        }
                    }
                    2 -> {
                        // Favorites Tab
                        val favoriteTracks = filteredTracks.filter { it.isFavorite }
                        if (favoriteTracks.isEmpty()) {
                            EmptyAudioView(message = "No favorite audio tracks yet")
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = if (playbackState.currentTrack != null) 72.dp else 8.dp)
                            ) {
                                items(favoriteTracks, key = { it.id }) { track ->
                                    val isCurrent = playbackState.currentTrack?.id == track.id
                                    AudioTrackListItem(
                                        track = track,
                                        isCurrent = isCurrent,
                                        isPlaying = isCurrent && playbackState.isPlaying,
                                        onTrackClick = {
                                            viewModel.audioPlayerManager.playTrack(track, favoriteTracks)
                                        },
                                        onAddToPlaylist = { songForPlaylistSelection = track },
                                        onSaveToMusic = { viewModel.saveAudioToMusic(track) },
                                        onShare = { shareAudioFile(context, track) },
                                        onDelete = { viewModel.deleteMedia(track) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Persistent Mini Player above Bottom Bar
        AnimatedVisibility(
            visible = playbackState.currentTrack != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            playbackState.currentTrack?.let { track ->
                MiniPlayerBar(
                    track = track,
                    playbackState = playbackState,
                    onPlayPause = { viewModel.audioPlayerManager.togglePlayPause() },
                    onNext = { viewModel.audioPlayerManager.playNext() },
                    onExpand = { showExpandedPlayer = true }
                )
            }
        }

        // Expanded Audio Player Modal Bottom Sheet
        if (showExpandedPlayer && playbackState.currentTrack != null) {
            ExpandedPlayerBottomSheet(
                playbackState = playbackState,
                audioPlayerManager = viewModel.audioPlayerManager,
                onDismiss = { showExpandedPlayer = false },
                onAddToPlaylist = { songForPlaylistSelection = playbackState.currentTrack }
            )
        }

        // Dialog: Create New Playlist
        if (showNewPlaylistDialog) {
            CreatePlaylistDialog(
                onDismiss = { showNewPlaylistDialog = false },
                onCreate = { name ->
                    viewModel.createPlaylist(name)
                    showNewPlaylistDialog = false
                }
            )
        }

        // Dialog: Add Song to Playlist Selector
        if (songForPlaylistSelection != null) {
            AddToPlaylistDialog(
                playlists = playlists,
                onDismiss = { songForPlaylistSelection = null },
                onSelectPlaylist = { playlistId ->
                    viewModel.addSongToPlaylist(playlistId, songForPlaylistSelection!!.id)
                    songForPlaylistSelection = null
                },
                onCreateNewPlaylist = {
                    showNewPlaylistDialog = true
                }
            )
        }

        // Playlist Detail View
        if (selectedPlaylistForDetail != null) {
            PlaylistDetailBottomSheet(
                playlist = selectedPlaylistForDetail!!,
                viewModel = viewModel,
                onDismiss = { selectedPlaylistForDetail = null },
                onPlayTrack = { track, queue ->
                    viewModel.audioPlayerManager.playTrack(track, queue)
                }
            )
        }
    }
}

@Composable
fun AudioTrackListItem(
    track: VideoItem,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onTrackClick: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onSaveToMusic: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val formattedDuration = remember(track.durationMs) {
        val totalSec = track.durationMs / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        String.format(Locale.US, "%d:%02d", min, sec)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTrackClick() }
            .background(if (isCurrent) SurfaceVariantDark.copy(alpha = 0.6f) else Color.Transparent)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val thumbBgModifier = if (isCurrent) {
            Modifier.background(BrandGradient)
        } else {
            Modifier.background(SurfaceElevatedDark)
        }

        // Icon / Vinyl thumbnail
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(8.dp))
                .then(thumbBgModifier),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.PlayArrow else Icons.Filled.MusicNote,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Title & Artist
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = if (isCurrent) NeonCyan else TextPrimary,
                fontSize = 15.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${track.author} · $formattedDuration",
                color = TextMuted,
                fontSize = 12.sp,
                maxLines = 1
            )
        }

        // More Options
        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "Options",
                    tint = TextMuted
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(SurfaceDark)
            ) {
                DropdownMenuItem(
                    text = { Text("Add to Playlist", color = TextPrimary) },
                    leadingIcon = { Icon(Icons.Filled.PlaylistAdd, contentDescription = null, tint = NeonCyan) },
                    onClick = {
                        showMenu = false
                        onAddToPlaylist()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Save to Phone Music", color = TextPrimary) },
                    leadingIcon = { Icon(Icons.Filled.Download, contentDescription = null, tint = NeonCyan) },
                    onClick = {
                        showMenu = false
                        onSaveToMusic()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Share Track", color = TextPrimary) },
                    leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null, tint = NeonPink) },
                    onClick = {
                        showMenu = false
                        onShare()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete Track", color = HeartRed) },
                    leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = HeartRed) },
                    onClick = {
                        showMenu = false
                        onDelete()
                    }
                )
            }
        }
    }
}

@Composable
fun MiniPlayerBar(
    track: VideoItem,
    playbackState: AudioPlaybackState,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onExpand: () -> Unit
) {
    val progress = if (playbackState.totalDurationMs > 0) {
        (playbackState.currentPositionMs.toFloat() / playbackState.totalDurationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExpand() }
            .background(SurfaceElevatedDark.copy(alpha = 0.98f))
            .testTag("audio_mini_player")
    ) {
        // Progress Line
        LinearProgressIndicator(
            progress = { progress },
            color = NeonPink,
            trackColor = DividerDark,
            modifier = Modifier.fillMaxWidth().height(2.5.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(BrandGradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.author,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }

            IconButton(onClick = onPlayPause) {
                Icon(
                    imageVector = if (playbackState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                    tint = TextPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }

            IconButton(onClick = onNext) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "Next",
                    tint = TextPrimary,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandedPlayerBottomSheet(
    playbackState: AudioPlaybackState,
    audioPlayerManager: AudioPlayerManager,
    onDismiss: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    val track = playbackState.currentTrack ?: return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val infiniteTransition = rememberInfiniteTransition(label = "vinylSpin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = AnimRepeatMode.Restart
        ),
        label = "rotation"
    )

    val currentFormatted = remember(playbackState.currentPositionMs) {
        val totalSec = playbackState.currentPositionMs / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        String.format(Locale.US, "%d:%02d", min, sec)
    }

    val totalFormatted = remember(playbackState.totalDurationMs) {
        val totalSec = playbackState.totalDurationMs / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        String.format(Locale.US, "%d:%02d", min, sec)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PureBlack,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Collapse", tint = TextMuted, modifier = Modifier.size(28.dp))
                }

                Text(
                    text = "OFFLINE PLAYER",
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                IconButton(onClick = onAddToPlaylist) {
                    Icon(Icons.Filled.PlaylistAdd, contentDescription = "Add to playlist", tint = NeonCyan)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Large Spinning Vinyl Disc
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .rotate(if (playbackState.isPlaying) rotation else 0f)
                    .clip(CircleShape)
                    .background(SurfaceDark)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(SurfaceElevatedDark),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(BrandGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MusicNote,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Track info
            Text(
                text = track.title,
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = track.author,
                color = TextSecondary,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Progress Slider
            val maxSlider = if (playbackState.totalDurationMs > 0) playbackState.totalDurationMs.toFloat() else 1f
            val currentSlider = playbackState.currentPositionMs.toFloat().coerceIn(0f, maxSlider)

            Slider(
                value = currentSlider,
                onValueChange = { audioPlayerManager.seekTo(it.toLong()) },
                valueRange = 0f..maxSlider,
                colors = SliderDefaults.colors(
                    thumbColor = NeonPink,
                    activeTrackColor = NeonPink,
                    inactiveTrackColor = DividerDark
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = currentFormatted, color = TextMuted, fontSize = 12.sp)
                Text(text = totalFormatted, color = TextMuted, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Playback Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle
                IconButton(onClick = { audioPlayerManager.toggleShuffle() }) {
                    Icon(
                        imageVector = Icons.Filled.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (playbackState.isShuffle) NeonPink else TextMuted
                    )
                }

                // Previous
                IconButton(
                    onClick = { audioPlayerManager.playPrevious() },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = "Previous",
                        tint = TextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Play / Pause FAB
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(BrandGradient)
                        .clickable { audioPlayerManager.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Next
                IconButton(
                    onClick = { audioPlayerManager.playNext() },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Next",
                        tint = TextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Repeat Mode
                IconButton(onClick = { audioPlayerManager.toggleRepeat() }) {
                    val repeatIcon = when (playbackState.repeatMode) {
                        RepeatMode.ONE -> Icons.Filled.RepeatOne
                        else -> Icons.Filled.Repeat
                    }
                    val tint = if (playbackState.repeatMode != RepeatMode.OFF) NeonCyan else TextMuted
                    Icon(
                        imageVector = repeatIcon,
                        contentDescription = "Repeat",
                        tint = tint
                    )
                }
            }
        }
    }
}

@Composable
fun PlaylistItemCard(
    playlist: PlaylistItem,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = SurfaceDark,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceElevatedDark),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.QueueMusic,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Tap to view tracks",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete Playlist",
                    tint = TextMuted
                )
            }
        }
    }
}

@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Playlist", color = TextPrimary) },
        text = {
            Column {
                Text("Enter a name for your playlist:", color = TextSecondary, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("e.g. Synthwave Night", color = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonPink,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = NeonPink)
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        },
        containerColor = SurfaceDark
    )
}

@Composable
fun AddToPlaylistDialog(
    playlists: List<PlaylistItem>,
    onDismiss: () -> Unit,
    onSelectPlaylist: (Long) -> Unit,
    onCreateNewPlaylist: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Track to Playlist", color = TextPrimary) },
        text = {
            if (playlists.isEmpty()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("You don't have any playlists yet.", color = TextSecondary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            onDismiss()
                            onCreateNewPlaylist()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                    ) {
                        Text("Create Playlist")
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.height(200.dp)) {
                    items(playlists) { pl ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectPlaylist(pl.id) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.QueueMusic, contentDescription = null, tint = NeonCyan)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = pl.name, color = TextPrimary, fontSize = 15.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextMuted)
            }
        },
        containerColor = SurfaceDark
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailBottomSheet(
    playlist: PlaylistItem,
    viewModel: OfflineTikViewModel,
    onDismiss: () -> Unit,
    onPlayTrack: (VideoItem, List<VideoItem>) -> Unit
) {
    val songs by viewModel.repository.getSongsInPlaylist(playlist.id).collectAsState(initial = emptyList())
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceDark,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = playlist.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(text = "${songs.size} tracks", fontSize = 12.sp, color = TextMuted)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (songs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No tracks added to this playlist yet.", color = TextMuted)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                    items(songs, key = { it.id }) { track ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPlayTrack(track, songs) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.MusicNote, contentDescription = null, tint = NeonPink)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(track.title, color = TextPrimary, fontSize = 14.sp, maxLines = 1)
                                Text(track.author, color = TextMuted, fontSize = 12.sp, maxLines = 1)
                            }
                            IconButton(onClick = { viewModel.removeSongFromPlaylist(playlist.id, track.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = TextMuted)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyAudioView(
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(SurfaceElevatedDark),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.MusicNote, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(32.dp))
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(text = message, color = TextMuted, fontSize = 15.sp)

        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text(actionLabel, color = PureBlack, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun shareAudioFile(context: Context, track: VideoItem) {
    val file = File(track.filePath)
    if (file.exists()) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "OfflineTik Music: ${track.title} by ${track.author}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Audio via"))
            return
        } catch (e: Exception) {
            // fallback
        }
    }

    val textIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "OfflineTik Music: ${track.title} by ${track.author}")
    }
    context.startActivity(Intent.createChooser(textIntent, "Share Audio"))
}
