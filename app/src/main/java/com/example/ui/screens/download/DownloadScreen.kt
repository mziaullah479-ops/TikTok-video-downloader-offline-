package com.example.ui.screens.download

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.model.VideoItem
import com.example.data.repository.StorageStats
import com.example.downloader.DownloadState
import com.example.ui.OfflineTikViewModel
import com.example.ui.navigation.NavigationTab
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class MediaFilter {
    ALL, VIDEOS, MUSIC
}

@Composable
fun DownloadScreen(
    viewModel: OfflineTikViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val allMedia by viewModel.allMedia.collectAsState()
    val storageStats by viewModel.storageStats.collectAsState()
    val downloadState by viewModel.downloadState.collectAsState()

    var pasteUrlInput by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(MediaFilter.ALL) }
    var videoToDelete by remember { mutableStateOf<VideoItem?>(null) }
    var showImportChoiceDialog by remember { mutableStateOf(false) }

    // Modern zero-permission Android Photo/Video Picker
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importMediaFromUri(uri, isMusic = false)
        }
    }

    // Audio Document Picker
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importMediaFromUri(uri, isMusic = true)
        }
    }

    val filteredList = remember(allMedia, selectedFilter, searchQuery) {
        allMedia.filter { item ->
            val matchesFilter = when (selectedFilter) {
                MediaFilter.ALL -> true
                MediaFilter.VIDEOS -> !item.isMusic
                MediaFilter.MUSIC -> item.isMusic
            }
            val matchesSearch = searchQuery.isBlank() ||
                    item.title.contains(searchQuery, ignoreCase = true) ||
                    item.author.contains(searchQuery, ignoreCase = true) ||
                    item.soundTitle.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesSearch
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
            .testTag("download_screen")
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
                        text = "Downloads & Library",
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "${allMedia.size} items ready for offline viewing",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }

                IconButton(
                    onClick = { showImportChoiceDialog = true },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(BrandGradient)
                        .testTag("download_add_fab")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Import or Download Media",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Quick Paste & Download Card (Primary User Workflow)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceDark,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .testTag("paste_download_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Link,
                            contentDescription = null,
                            tint = NeonPink,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Paste TikTok / Video Link",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.weight(1f))

                        // Paste from Clipboard Button
                        Surface(
                            onClick = {
                                val clipText = clipboardManager.getText()?.text
                                if (!clipText.isNullOrBlank()) {
                                    pasteUrlInput = clipText.trim()
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = SurfaceElevatedDark
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ContentPaste,
                                    contentDescription = "Paste",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Paste",
                                    color = NeonCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Text Field for URL
                    OutlinedTextField(
                        value = pasteUrlInput,
                        onValueChange = { pasteUrlInput = it },
                        placeholder = { Text("https://vt.tiktok.com/... or media link", color = TextMuted, fontSize = 12.sp) },
                        singleLine = true,
                        trailingIcon = {
                            if (pasteUrlInput.isNotBlank()) {
                                IconButton(onClick = { pasteUrlInput = "" }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Clear", tint = TextMuted, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = DividerDark,
                            focusedContainerColor = SurfaceVariantDark,
                            unfocusedContainerColor = SurfaceVariantDark,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("paste_url_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Download Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (pasteUrlInput.isNotBlank()) {
                                    viewModel.downloadDirectUrl(pasteUrlInput, isAudioOnly = false)
                                }
                            },
                            enabled = pasteUrlInput.isNotBlank() &&
                                    downloadState !is DownloadState.Downloading &&
                                    downloadState !is DownloadState.Validating,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonPink,
                                contentColor = Color.White,
                                disabledContainerColor = SurfaceElevatedDark
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_download_video")
                        ) {
                            Icon(Icons.Filled.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Video", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                if (pasteUrlInput.isNotBlank()) {
                                    viewModel.downloadDirectUrl(pasteUrlInput, isAudioOnly = true)
                                }
                            },
                            enabled = pasteUrlInput.isNotBlank() &&
                                    downloadState !is DownloadState.Downloading &&
                                    downloadState !is DownloadState.Validating,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonCyan,
                                contentColor = PureBlack,
                                disabledContainerColor = SurfaceElevatedDark
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_download_audio")
                        ) {
                            Icon(Icons.Filled.MusicNote, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Audio", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Live Inline Download Status
                    AnimatedVisibility(visible = downloadState !is DownloadState.Idle) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                        ) {
                            when (val state = downloadState) {
                                is DownloadState.Validating -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = NeonCyan,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Analyzing link & resolving stream...",
                                            color = TextSecondary,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                is DownloadState.Downloading -> {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = if (state.isAudioOnly) "Downloading Audio..." else "Downloading Video...",
                                                color = TextPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = "${state.progressPercent}%",
                                                color = NeonCyan,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LinearProgressIndicator(
                                            progress = { state.progressPercent / 100f },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = if (state.isAudioOnly) NeonCyan else NeonPink,
                                            trackColor = SurfaceElevatedDark
                                        )
                                    }
                                }

                                is DownloadState.Success -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = null,
                                            tint = NeonCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Saved! Check ${if (state.isAudioOnly) "Audio tab" else "Home feed & list below"}.",
                                            color = NeonCyan,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                is DownloadState.Error -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.ErrorOutline,
                                            contentDescription = null,
                                            tint = HeartRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = state.message,
                                            color = HeartRed,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                else -> {}
                            }
                        }
                    }
                }
            }

            // Storage Overview Banner
            storageStats?.let { stats ->
                StorageOverviewBanner(stats = stats)
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search downloaded videos & audio...", color = TextMuted, fontSize = 13.sp) },
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
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .testTag("download_search_input")
            )

            // Filter Chips: All, Videos, Music
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val videosCount = allMedia.count { !it.isMusic }
                val musicCount = allMedia.count { it.isMusic }

                FilterChip(
                    selected = selectedFilter == MediaFilter.ALL,
                    onClick = { selectedFilter = MediaFilter.ALL },
                    label = { Text("All (${allMedia.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonPink,
                        selectedLabelColor = Color.White,
                        containerColor = SurfaceDark,
                        labelColor = TextMuted
                    )
                )

                FilterChip(
                    selected = selectedFilter == MediaFilter.VIDEOS,
                    onClick = { selectedFilter = MediaFilter.VIDEOS },
                    label = { Text("Videos ($videosCount)") },
                    leadingIcon = { Icon(Icons.Filled.Videocam, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonCyan,
                        selectedLabelColor = PureBlack,
                        containerColor = SurfaceDark,
                        labelColor = TextMuted
                    )
                )

                FilterChip(
                    selected = selectedFilter == MediaFilter.MUSIC,
                    onClick = { selectedFilter = MediaFilter.MUSIC },
                    label = { Text("Audio ($musicCount)") },
                    leadingIcon = { Icon(Icons.Filled.MusicNote, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonPink,
                        selectedLabelColor = Color.White,
                        containerColor = SurfaceDark,
                        labelColor = TextMuted
                    )
                )
            }

            // Media List
            if (filteredList.isEmpty()) {
                EmptyDownloadState(
                    searchQuery = searchQuery,
                    onImportClick = { showImportChoiceDialog = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList, key = { it.id }) { item ->
                        DownloadedMediaCard(
                            item = item,
                            onPlay = {
                                if (item.isMusic) {
                                    viewModel.audioPlayerManager.playTrack(item, filteredList)
                                    viewModel.selectTab(NavigationTab.AUDIO)
                                } else {
                                    viewModel.jumpToHomeVideo(item)
                                }
                            },
                            onSaveToGallery = {
                                if (item.isMusic) {
                                    viewModel.saveAudioToMusic(item)
                                } else {
                                    viewModel.saveVideoToGallery(item)
                                }
                            },
                            onShare = { shareMedia(context, item) },
                            onDelete = { videoToDelete = item }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }

        // Delete Confirmation Dialog
        if (videoToDelete != null) {
            AlertDialog(
                onDismissRequest = { videoToDelete = null },
                title = { Text("Delete Media?", color = TextPrimary) },
                text = {
                    Text(
                        "Are you sure you want to permanently delete \"${videoToDelete!!.title}\" from your offline storage?",
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteMedia(videoToDelete!!)
                            videoToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HeartRed)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { videoToDelete = null }) {
                        Text("Cancel", color = TextMuted)
                    }
                },
                containerColor = SurfaceDark
            )
        }

        // Choice Dialog: Import Local Video vs Download from URL
        if (showImportChoiceDialog) {
            AlertDialog(
                onDismissRequest = { showImportChoiceDialog = false },
                title = { Text("Add Offline Media", color = TextPrimary) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Choose how you want to add content to your vault:", color = TextSecondary, fontSize = 14.sp)

                        // Option 1: Download from URL
                        Surface(
                            onClick = {
                                showImportChoiceDialog = false
                                viewModel.openDownloadDialog()
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = SurfaceVariantDark,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Link, contentDescription = null, tint = NeonPink)
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text("Download from TikTok / Web", color = TextPrimary, fontWeight = FontWeight.Bold)
                                    Text("Paste video link to fetch offline", color = TextMuted, fontSize = 12.sp)
                                }
                            }
                        }

                        // Option 2: Import Video from Device Gallery
                        Surface(
                            onClick = {
                                showImportChoiceDialog = false
                                mediaPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = SurfaceVariantDark,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.FileUpload, contentDescription = null, tint = NeonCyan)
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text("Import Video from Device", color = TextPrimary, fontWeight = FontWeight.Bold)
                                    Text("Select MP4 / video files from gallery", color = TextMuted, fontSize = 12.sp)
                                }
                            }
                        }

                        // Option 3: Import Audio file
                        Surface(
                            onClick = {
                                showImportChoiceDialog = false
                                audioPickerLauncher.launch("audio/*")
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = SurfaceVariantDark,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.MusicNote, contentDescription = null, tint = NeonCyan)
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text("Import Audio File", color = TextPrimary, fontWeight = FontWeight.Bold)
                                    Text("Select MP3 / M4A sound tracks", color = TextMuted, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showImportChoiceDialog = false }) {
                        Text("Cancel", color = TextMuted)
                    }
                },
                containerColor = SurfaceDark
            )
        }
    }
}

@Composable
fun StorageOverviewBanner(stats: StorageStats) {
    val devAvailGb = String.format("%.1f GB", stats.deviceAvailableBytes / (1024.0 * 1024.0 * 1024.0))

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceDark,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Offline Storage Used",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = formatMb(stats.totalAppBytes),
                    color = NeonCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            val fractionUsed = if (stats.deviceTotalBytes > 0) {
                (stats.totalAppBytes.toFloat() / stats.deviceTotalBytes.toFloat()).coerceIn(0.02f, 1f)
            } else 0.05f

            LinearProgressIndicator(
                progress = { fractionUsed },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = NeonPink,
                trackColor = SurfaceElevatedDark
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Videos: ${formatMb(stats.videoBytes)} · Audio: ${formatMb(stats.audioBytes)}",
                    color = TextMuted,
                    fontSize = 11.sp
                )
                Text(
                    text = "$devAvailGb free on phone",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun DownloadedMediaCard(
    item: VideoItem,
    onPlay: () -> Unit,
    onSaveToGallery: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val formattedDuration = remember(item.durationMs) {
        val totalSec = item.durationMs / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        String.format(Locale.US, "%02d:%02d", min, sec)
    }

    val formattedSize = remember(item.fileSizeBytes) {
        formatMb(item.fileSizeBytes)
    }

    val sdf = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val formattedDate = remember(item.dateAdded) { sdf.format(Date(item.dateAdded)) }

    Surface(
        onClick = onPlay,
        shape = RoundedCornerShape(14.dp),
        color = SurfaceDark,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Video / Music Icon Thumbnail with Duration pill
            Box(
                modifier = Modifier
                    .size(width = 68.dp, height = 68.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (item.isMusic) SurfaceElevatedDark else SurfaceVariantDark),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (item.isMusic) Icons.Filled.MusicNote else Icons.Filled.Videocam,
                    contentDescription = null,
                    tint = if (item.isMusic) NeonCyan else NeonPink,
                    modifier = Modifier.size(30.dp)
                )

                // Duration tag in bottom corner
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(PureBlack.copy(alpha = 0.75f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = formattedDuration,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = item.author,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = formattedSize,
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(text = "•", color = TextMuted, fontSize = 10.sp)
                    Text(
                        text = formattedDate,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            // Quick Actions: Save to Gallery, Share, Delete
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onSaveToGallery,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Download,
                        contentDescription = if (item.isMusic) "Save to Phone Music" else "Save to Phone Gallery",
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onShare,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = "Share",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyDownloadState(
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
                .size(72.dp)
                .clip(CircleShape)
                .background(SurfaceDark),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Download, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(36.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (searchQuery.isNotBlank()) "No media matches \"$searchQuery\"" else "Library is Empty",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Paste a TikTok link above or import media from your device to watch completely offline.",
            color = TextMuted,
            fontSize = 13.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onImportClick,
            colors = ButtonDefaults.buttonColors(containerColor = NeonPink)
        ) {
            Text("Add Offline Media", fontWeight = FontWeight.Bold)
        }
    }
}

private fun formatMb(bytes: Long): String {
    return String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
}

private fun shareMedia(context: Context, item: VideoItem) {
    val file = File(item.filePath)
    if (file.exists()) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = if (item.isMusic) "audio/*" else "video/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "Watch offline on OfflineTik: ${item.title}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Media via"))
            return
        } catch (e: Exception) {
            // fallback
        }
    }

    val textIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "OfflineTik: ${item.title} by ${item.author}")
    }
    context.startActivity(Intent.createChooser(textIntent, "Share Media"))
}
