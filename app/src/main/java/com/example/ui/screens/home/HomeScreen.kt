@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.media3.common.util.UnstableApi::class)

package com.example.ui.screens.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.model.VideoItem
import com.example.ui.OfflineTikViewModel
import com.example.ui.theme.BrandGradient
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.PureBlack
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: OfflineTikViewModel,
    videos: List<VideoItem>,
    targetedIndex: Int?,
    onClearTargetedIndex: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    val filteredVideos = remember(videos, searchQuery) {
        if (searchQuery.isBlank()) videos
        else videos.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.author.contains(searchQuery, ignoreCase = true) ||
            it.soundTitle.contains(searchQuery, ignoreCase = true) ||
            it.description.contains(searchQuery, ignoreCase = true)
        }
    }

    var selectedVideoForInfo by remember { mutableStateOf<VideoItem?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
            .testTag("home_screen")
    ) {
        if (filteredVideos.isEmpty()) {
            EmptyVideoState(
                searchQuery = searchQuery,
                onImportClick = { viewModel.openDownloadDialog() }
            )
        } else {
            val pagerState = rememberPagerState(
                initialPage = (targetedIndex ?: 0).coerceIn(0, (filteredVideos.size - 1).coerceAtLeast(0)),
                pageCount = { filteredVideos.size }
            )

            LaunchedEffect(targetedIndex) {
                if (targetedIndex != null && targetedIndex in filteredVideos.indices) {
                    pagerState.scrollToPage(targetedIndex)
                    onClearTargetedIndex()
                }
            }

            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1
            ) { page ->
                val video = filteredVideos[page]
                val isCurrentPage = page == pagerState.currentPage

                VideoFeedItem(
                    video = video,
                    isCurrentPage = isCurrentPage,
                    onSaveToGallery = { viewModel.saveVideoToGallery(video) },
                    onShareClick = { shareVideo(context, video) },
                    onInfoClick = { selectedVideoForInfo = video }
                )
            }
        }

        // Top Overlay Bar: Search & OfflineTik Branding
        TopOverlayHeader(
            searchQuery = searchQuery,
            isSearchActive = isSearchActive,
            onSearchQueryChange = { searchQuery = it },
            onToggleSearch = { isSearchActive = !isSearchActive },
            onOpenDownloader = { viewModel.openDownloadDialog() }
        )

        // Video Details Bottom Sheet (Prominently shows author/profile origin and save to gallery)
        if (selectedVideoForInfo != null) {
            VideoDetailsBottomSheet(
                video = selectedVideoForInfo!!,
                onDismiss = { selectedVideoForInfo = null },
                onShare = { shareVideo(context, selectedVideoForInfo!!) },
                onSaveToGallery = { viewModel.saveVideoToGallery(selectedVideoForInfo!!) }
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun VideoFeedItem(
    video: VideoItem,
    isCurrentPage: Boolean,
    onSaveToGallery: () -> Unit,
    onShareClick: () -> Unit,
    onInfoClick: () -> Unit
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableStateOf(0L) }
    var totalDurationMs by remember { mutableStateOf(0L) }
    var showPlayPauseIndicator by remember { mutableStateOf(false) }

    val exoPlayer = remember(video.id) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            val file = File(video.filePath)
            val uri = if (file.exists()) Uri.fromFile(file) else Uri.parse(video.filePath)
            val mediaItem = MediaItem.fromUri(uri)
            setMediaItem(mediaItem)
            prepare()
        }
    }

    DisposableEffect(video.id) {
        onDispose {
            exoPlayer.release()
        }
    }

    LaunchedEffect(isCurrentPage, isPlaying) {
        if (isCurrentPage) {
            exoPlayer.playWhenReady = isPlaying
            while (isPlaying) {
                currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                totalDurationMs = exoPlayer.duration.coerceAtLeast(0L)
                kotlinx.coroutines.delay(250)
            }
        } else {
            exoPlayer.playWhenReady = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(video.id) {
                detectTapGestures(
                    onTap = {
                        isPlaying = !isPlaying
                        exoPlayer.playWhenReady = isPlaying
                        showPlayPauseIndicator = true
                    }
                )
            }
    ) {
        // Full screen video surface
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top and bottom dark gradient scrims for readability
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(PureBlack.copy(alpha = 0.8f), Color.Transparent)
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, PureBlack.copy(alpha = 0.9f))
                    )
                )
        )

        // Play/Pause Center Indicator
        LaunchedEffect(showPlayPauseIndicator) {
            if (showPlayPauseIndicator) {
                kotlinx.coroutines.delay(650)
                showPlayPauseIndicator = false
            }
        }

        AnimatedVisibility(
            visible = showPlayPauseIndicator,
            enter = fadeIn(tween(150)) + scaleIn(spring(dampingRatio = 0.6f)),
            exit = fadeOut(tween(250)) + scaleOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(PureBlack.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                    contentDescription = if (isPlaying) "Playing" else "Paused",
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        // Right side interaction controls (Personal Vault: Save to Gallery, Share, Info, Mute, Vinyl)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Save to Phone Gallery Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onSaveToGallery() }
                    .testTag("home_save_gallery_button")
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(PureBlack.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Download,
                        contentDescription = "Save to Phone Gallery",
                        tint = NeonCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = "Gallery",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // 2. Share Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onShareClick() }
                    .testTag("home_share_button")
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(PureBlack.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = "Share",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = "Share",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // 3. Information Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onInfoClick() }
                    .testTag("home_info_button")
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(PureBlack.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = "Video Information",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = "Info",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // 4. Audio Mute / Unmute Toggle Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable {
                    isMuted = !isMuted
                    exoPlayer.volume = if (isMuted) 0f else 1f
                }
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(PureBlack.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                        contentDescription = if (isMuted) "Unmute" else "Mute",
                        tint = if (isMuted) NeonPink else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = if (isMuted) "Muted" else "Sound",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // 5. Rotating Vinyl Sound Disc
            SpinningSoundDisc(soundTitle = video.soundTitle, isPlaying = isPlaying)
        }

        // Bottom Details (Clean Video Title, Description, Sound Info - No Fake Social Profiles)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.72f)
                .padding(start = 16.dp, bottom = 28.dp)
        ) {
            // Video Title
            Text(
                text = video.title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (video.description.isNotBlank() && video.description != video.title) {
                Text(
                    text = video.description,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sound Marquee Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(PureBlack.copy(alpha = 0.45f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (video.soundTitle.isNotBlank()) "${video.soundTitle} · ${video.soundAuthor}" else "Original Soundtrack",
                    color = Color.White,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Bottom playback progress indicator
        val videoProgress = if (totalDurationMs > 0) {
            (currentPositionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
        } else 0f

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.5.dp)
                .align(Alignment.BottomCenter)
                .background(Color.White.copy(alpha = 0.2f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(videoProgress)
                    .fillMaxHeight()
                    .background(BrandGradient)
            )
        }
    }
}

@Composable
fun SpinningSoundDisc(soundTitle: String, isPlaying: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "discRotate")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .size(48.dp)
            .rotate(if (isPlaying) rotation else 0f)
            .clip(CircleShape)
            .background(PureBlack)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer vinyl groove ring
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(SurfaceVariantDark),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(BrandGradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
fun TopOverlayHeader(
    searchQuery: String,
    isSearchActive: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onToggleSearch: () -> Unit,
    onOpenDownloader: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Brand Name with gradient
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Offline",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Tik",
                    color = NeonPink,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
            }

            // Top action buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onToggleSearch,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(PureBlack.copy(alpha = 0.5f))
                        .testTag("home_search_toggle")
                ) {
                    Icon(
                        imageVector = if (isSearchActive) Icons.Filled.Close else Icons.Filled.Search,
                        contentDescription = "Search",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onOpenDownloader,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(BrandGradient)
                        .testTag("home_add_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add or Download Video",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Expandable search field
        AnimatedVisibility(visible = isSearchActive) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search offline videos, titles, sounds...", color = TextMuted, fontSize = 13.sp) },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = SurfaceDark.copy(alpha = 0.9f),
                    unfocusedContainerColor = SurfaceDark.copy(alpha = 0.8f),
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = TextMuted)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear", tint = TextMuted)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .testTag("home_search_input")
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoDetailsBottomSheet(
    video: VideoItem,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onSaveToGallery: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sdf = remember { SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault()) }
    val formattedDate = remember(video.dateAdded) { sdf.format(Date(video.dateAdded)) }
    val formattedSize = remember(video.fileSizeBytes) {
        String.format("%.2f MB", video.fileSizeBytes / (1024.0 * 1024.0))
    }
    val formattedDuration = remember(video.durationMs) {
        val totalSec = video.durationMs / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        String.format("%02d:%02d", min, sec)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceDark,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Video Details",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Prominently display which profile/account the video was downloaded from
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceVariantDark,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(BrandGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = video.author.take(2).uppercase().replace("@", ""),
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Downloaded from Profile",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = video.author,
                            color = NeonCyan,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            InfoRow(label = "Title", value = video.title)
            InfoRow(label = "Sound Track", value = "${video.soundTitle} · ${video.soundAuthor}")
            InfoRow(label = "Duration", value = formattedDuration)
            InfoRow(label = "File Size", value = formattedSize)
            InfoRow(label = "Date Added", value = formattedDate)
            InfoRow(label = "Storage Location", value = video.filePath, isPath = true)

            if (!video.sourceUrl.isNullOrBlank()) {
                InfoRow(label = "Source URL", value = video.sourceUrl, isPath = true)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons: Save to Phone Gallery and Share Video
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        onSaveToGallery()
                        onDismiss()
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan,
                        contentColor = PureBlack
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Save to Gallery",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Surface(
                    onClick = onShare,
                    shape = RoundedCornerShape(14.dp),
                    color = SurfaceVariantDark,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Share Video",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String, isPath: Boolean = false) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = label, color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Text(
            text = value,
            color = TextPrimary,
            fontSize = if (isPath) 11.sp else 13.sp,
            fontWeight = if (isPath) FontWeight.Normal else FontWeight.SemiBold,
            maxLines = if (isPath) 2 else 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun EmptyVideoState(
    searchQuery: String,
    onImportClick: () -> Unit
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
                .size(80.dp)
                .clip(CircleShape)
                .background(SurfaceDark),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Download,
                contentDescription = null,
                tint = NeonPink,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (searchQuery.isNotBlank()) "No videos match \"$searchQuery\"" else "No Offline Videos Yet",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (searchQuery.isNotBlank()) "Try another search term" else "Import short videos or paste a TikTok link to watch offline.",
            color = TextMuted,
            fontSize = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(BrandGradient)
                .clickable { onImportClick() }
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Add / Import Videos",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

private fun shareVideo(context: Context, video: VideoItem) {
    val file = File(video.filePath)
    if (file.exists()) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = if (video.isMusic) "audio/*" else "video/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "Watch offline on OfflineTik: ${video.title}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Media via"))
            return
        } catch (e: Exception) {
            // Fallback to text share
        }
    }

    val textIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "OfflineTik: ${video.title} by ${video.author}")
    }
    context.startActivity(Intent.createChooser(textIntent, "Share Media"))
}
