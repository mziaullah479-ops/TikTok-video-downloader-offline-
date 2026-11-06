package com.example.ui.components

import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.downloader.DownloadState
import com.example.ui.OfflineTikViewModel
import com.example.ui.navigation.NavigationTab
import com.example.ui.theme.BrandGradient
import com.example.ui.theme.CyanGradient
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
import java.util.Locale

@Composable
fun DownloadDialog(
    viewModel: OfflineTikViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val downloadState by viewModel.downloadState.collectAsState()
    var inputUrl by remember { mutableStateOf("") }

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.importMediaFromUri(uri, isMusic = false)
            onDismiss()
        }
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.importMediaFromUri(uri, isMusic = true)
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (downloadState !is DownloadState.Downloading) {
                onDismiss()
            }
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Download,
                        contentDescription = null,
                        tint = NeonPink,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "TikTok Downloader",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = TextMuted)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                when (val state = downloadState) {
                    is DownloadState.Idle -> {
                        Text(
                            text = "Paste a TikTok link to download high-definition video or extract the sound track for offline listening:",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // URL input field with paste button
                        OutlinedTextField(
                            value = inputUrl,
                            onValueChange = { inputUrl = it },
                            placeholder = { Text("https://www.tiktok.com/@... or link", color = TextMuted, fontSize = 12.sp) },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Filled.Link, contentDescription = null, tint = NeonCyan) },
                            trailingIcon = {
                                IconButton(onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    val clip = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()
                                    if (!clip.isNullOrBlank()) {
                                        inputUrl = clip
                                    }
                                }) {
                                    Icon(Icons.Filled.ContentPaste, contentDescription = "Paste", tint = TextMuted)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonPink,
                                unfocusedBorderColor = SurfaceVariantDark,
                                focusedContainerColor = SurfaceVariantDark,
                                unfocusedContainerColor = SurfaceVariantDark,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("download_dialog_url_input")
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Alternative local imports
                        Text("OR IMPORT OFFLINE MEDIA FROM DEVICE:", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                onClick = {
                                    mediaPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = SurfaceVariantDark,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.Videocam, contentDescription = null, tint = NeonPink, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Pick Video", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            Surface(
                                onClick = { audioPickerLauncher.launch("audio/*") },
                                shape = RoundedCornerShape(10.dp),
                                color = SurfaceVariantDark,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.MusicNote, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Pick Audio", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    is DownloadState.Validating -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = NeonPink, modifier = Modifier.size(44.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Analyzing TikTok Media Streams...", color = TextPrimary, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(state.url, color = TextMuted, fontSize = 12.sp, maxLines = 1)
                        }
                    }

                    is DownloadState.MediaReady -> {
                        val info = state.info
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Media Preview Card
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SurfaceElevatedDark,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = info.title,
                                        color = TextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "By ${info.author}",
                                        color = NeonCyan,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Sound details
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.MusicNote,
                                            contentDescription = null,
                                            tint = NeonPink,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${info.soundTitle} · ${info.soundAuthor}",
                                            color = TextSecondary,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "Choose Download Format:",
                                color = TextMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )

                            // Choice 1: Download Full Video (MP4)
                            Surface(
                                onClick = { viewModel.downloadMedia(info, isAudioOnly = false) },
                                shape = RoundedCornerShape(12.dp),
                                color = SurfaceVariantDark,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(BrandGradient),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Videocam,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Download Full Video", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("HD MP4 with sound • For Home feed", color = TextMuted, fontSize = 11.sp)
                                    }
                                    Icon(Icons.Filled.Download, contentDescription = null, tint = NeonPink)
                                }
                            }

                            // Choice 2: Download Audio / Sound Only (Music)
                            Surface(
                                onClick = { viewModel.downloadMedia(info, isAudioOnly = true) },
                                shape = RoundedCornerShape(12.dp),
                                color = SurfaceVariantDark,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(CyanGradient),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Headphones,
                                            contentDescription = null,
                                            tint = PureBlack,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Download Audio Only", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("MP3 / M4A Sound track • For Music player", color = TextMuted, fontSize = 11.sp)
                                    }
                                    Icon(Icons.Filled.Download, contentDescription = null, tint = NeonCyan)
                                }
                            }
                        }
                    }

                    is DownloadState.Downloading -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (state.isAudioOnly) "Downloading Sound Track (Audio)" else "Downloading Full Video (MP4)",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            LinearProgressIndicator(
                                progress = { (state.progressPercent / 100f).coerceIn(0f, 1f) },
                                color = if (state.isAudioOnly) NeonCyan else NeonPink,
                                trackColor = SurfaceVariantDark,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            val mbDownloaded = String.format(Locale.US, "%.1f MB", state.bytesDownloaded / (1024.0 * 1024.0))
                            val totalMb = if (state.totalBytes > 0) {
                                String.format(Locale.US, "%.1f MB", state.totalBytes / (1024.0 * 1024.0))
                            } else "..."

                            Text(
                                text = "${state.progressPercent}% · $mbDownloaded / $totalMb",
                                color = if (state.isAudioOnly) NeonCyan else NeonPink,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    is DownloadState.Success -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF00E676),
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (state.isAudioOnly) "Saved to Offline Music!" else "Saved to Offline Videos!",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = state.mediaItem.title,
                                color = TextSecondary,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            if (state.isAudioOnly) {
                                Button(
                                    onClick = {
                                        viewModel.audioPlayerManager.playTrack(state.mediaItem, listOf(state.mediaItem))
                                        viewModel.selectTab(NavigationTab.AUDIO)
                                        onDismiss()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Filled.Headphones, contentDescription = null, tint = PureBlack)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Listen in Audio Player", color = PureBlack, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(
                                    onClick = {
                                        viewModel.jumpToHomeVideo(state.mediaItem)
                                        onDismiss()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonPink),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Watch in Video Feed", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    is DownloadState.Error -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = HeartRed)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(state.title, color = HeartRed, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(state.message, color = TextSecondary, fontSize = 13.sp, lineHeight = 18.sp)

                            if (state.canImportLocally) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        mediaPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Filled.FileUpload, contentDescription = null, tint = PureBlack)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Pick Saved Video from Gallery", color = PureBlack, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (downloadState is DownloadState.Idle) {
                Button(
                    onClick = { viewModel.resolveUrl(inputUrl) },
                    enabled = inputUrl.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPink),
                    modifier = Modifier.testTag("download_dialog_submit_button")
                ) {
                    Text("Analyze Link", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            if (downloadState !is DownloadState.Downloading) {
                TextButton(onClick = onDismiss) {
                    Text("Close", color = TextMuted)
                }
            }
        },
        containerColor = SurfaceDark
    )
}
