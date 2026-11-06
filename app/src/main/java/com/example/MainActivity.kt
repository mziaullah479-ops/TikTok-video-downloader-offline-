package com.example

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.ui.OfflineTikViewModel
import com.example.ui.components.BottomNavBar
import com.example.ui.components.DownloadDialog
import com.example.ui.navigation.NavigationTab
import com.example.ui.screens.audio.AudioScreen
import com.example.ui.screens.download.DownloadScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.setting.SettingScreen
import com.example.ui.theme.OfflineTikTheme
import com.example.ui.theme.PureBlack
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    private val viewModel: OfflineTikViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIncomingIntent(intent)

        setContent {
            OfflineTikTheme {
                MainContent(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        val type = intent.type ?: ""

        if (action == Intent.ACTION_SEND) {
            when {
                type == "text/plain" -> {
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                    if (!sharedText.isNullOrBlank()) {
                        viewModel.processUrl(sharedText)
                    }
                }
                type.startsWith("video/") -> {
                    val streamUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(Intent.EXTRA_STREAM)
                    } ?: intent.data

                    if (streamUri != null) {
                        viewModel.importMediaFromUri(streamUri, isMusic = false)
                    }
                }
                type.startsWith("audio/") -> {
                    val streamUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(Intent.EXTRA_STREAM)
                    } ?: intent.data

                    if (streamUri != null) {
                        viewModel.importMediaFromUri(streamUri, isMusic = true)
                    }
                }
            }
        }
    }
}

@Composable
fun MainContent(viewModel: OfflineTikViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val videos by viewModel.videos.collectAsState()
    val targetedIndex by viewModel.targetedVideoIndex.collectAsState()
    val showDownloadDialog by viewModel.showDownloadDialog.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Collect user toast/snackbar messages
    LaunchedEffect(Unit) {
        viewModel.userMessage.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Android Back Handler: return to Home tab if on another screen
    BackHandler(enabled = currentTab != NavigationTab.HOME) {
        viewModel.selectTab(NavigationTab.HOME)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack),
        containerColor = PureBlack,
        bottomBar = {
            BottomNavBar(
                currentTab = currentTab,
                onTabSelected = { viewModel.selectTab(it) }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            Crossfade(
                targetState = currentTab,
                label = "tab_crossfade"
            ) { tab ->
                when (tab) {
                    NavigationTab.HOME -> {
                        HomeScreen(
                            viewModel = viewModel,
                            videos = videos,
                            targetedIndex = targetedIndex,
                            onClearTargetedIndex = { viewModel.clearTargetedVideoIndex() }
                        )
                    }
                    NavigationTab.AUDIO -> {
                        AudioScreen(viewModel = viewModel)
                    }
                    NavigationTab.DOWNLOAD -> {
                        DownloadScreen(viewModel = viewModel)
                    }
                    NavigationTab.SETTING -> {
                        SettingScreen(viewModel = viewModel)
                    }
                }
            }

            // Downloader Dialog (for URL downloads & TikTok links)
            if (showDownloadDialog) {
                DownloadDialog(
                    viewModel = viewModel,
                    onDismiss = { viewModel.closeDownloadDialog() }
                )
            }
        }
    }
}
