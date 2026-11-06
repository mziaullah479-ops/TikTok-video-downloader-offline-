package com.example.ui.screens.setting

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.OfflineTikViewModel
import com.example.ui.theme.BrandGradient
import com.example.ui.theme.DividerDark
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
fun SettingScreen(
    viewModel: OfflineTikViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val storageStats by viewModel.storageStats.collectAsState()

    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showDeveloperInfoDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showRateDialog by remember { mutableStateOf(false) }
    var selectedThemeName by remember { mutableStateOf("Pure OLED Black") }

    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
            .testTag("setting_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Screen Title
            Text(
                text = "Settings",
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black
            )

            Spacer(modifier = Modifier.height(16.dp))

            // App Brand Header Card
            AppHeaderCard()

            Spacer(modifier = Modifier.height(20.dp))

            // Section 1: Storage Information & Maintenance
            SettingsSectionHeader(title = "Storage & Cache")
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SurfaceDark,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    val cacheMb = storageStats?.let {
                        String.format(Locale.US, "%.1f MB", it.cacheBytes / (1024.0 * 1024.0))
                    } ?: "0.0 MB"

                    val videoMb = storageStats?.let {
                        String.format(Locale.US, "%.1f MB", it.videoBytes / (1024.0 * 1024.0))
                    } ?: "0.0 MB"

                    val audioMb = storageStats?.let {
                        String.format(Locale.US, "%.1f MB", it.audioBytes / (1024.0 * 1024.0))
                    } ?: "0.0 MB"

                    SettingDetailItem(
                        icon = Icons.Filled.Storage,
                        iconTint = NeonCyan,
                        title = "Storage Breakdown",
                        subtitle = "Videos: $videoMb · Audio: $audioMb · Cache: $cacheMb"
                    )

                    Box(Modifier.fillMaxWidth().height(0.5.dp).background(DividerDark))

                    SettingClickableItem(
                        icon = Icons.Filled.CleaningServices,
                        iconTint = NeonPink,
                        title = "Clear Cache",
                        value = "Free $cacheMb",
                        onClick = { viewModel.clearCache() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 2: Appearance & Personalization
            SettingsSectionHeader(title = "Appearance")
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SurfaceDark,
                modifier = Modifier.fillMaxWidth()
            ) {
                SettingClickableItem(
                    icon = Icons.Filled.Palette,
                    iconTint = NeonPink,
                    title = "Theme Settings",
                    value = selectedThemeName,
                    onClick = { showThemeDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 3: Share & Feedback
            SettingsSectionHeader(title = "Share & Feedback")
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SurfaceDark,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SettingClickableItem(
                        icon = Icons.Filled.Share,
                        iconTint = NeonCyan,
                        title = "Share App",
                        subtitle = "Recommend OfflineTik to friends",
                        onClick = { shareApp(context) }
                    )

                    Box(Modifier.fillMaxWidth().height(0.5.dp).background(DividerDark))

                    SettingClickableItem(
                        icon = Icons.Filled.Star,
                        iconTint = Color(0xFFFFD700),
                        title = "Rate Us",
                        subtitle = "Leave a review on Google Play",
                        onClick = { showRateDialog = true }
                    )

                    Box(Modifier.fillMaxWidth().height(0.5.dp).background(DividerDark))

                    SettingClickableItem(
                        icon = Icons.Filled.Email,
                        iconTint = NeonPink,
                        title = "Contact Us",
                        subtitle = "Feedback, suggestions & support",
                        onClick = { contactSupport(context) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 4: About & Legal
            SettingsSectionHeader(title = "About & Legal")
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SurfaceDark,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SettingClickableItem(
                        icon = Icons.Filled.Code,
                        iconTint = NeonCyan,
                        title = "Developer Information",
                        subtitle = "OfflineTik Studio · Independent Engineering",
                        onClick = { showDeveloperInfoDialog = true }
                    )

                    Box(Modifier.fillMaxWidth().height(0.5.dp).background(DividerDark))

                    SettingClickableItem(
                        icon = Icons.Filled.PrivacyTip,
                        iconTint = Color(0xFF00E676),
                        title = "Privacy Policy",
                        subtitle = "100% on-device local storage, zero cloud tracking",
                        onClick = { showPrivacyPolicyDialog = true }
                    )

                    Box(Modifier.fillMaxWidth().height(0.5.dp).background(DividerDark))

                    SettingClickableItem(
                        icon = Icons.Filled.Description,
                        iconTint = TextSecondary,
                        title = "Terms of Service",
                        subtitle = "Personal offline use guidelines",
                        onClick = { showTermsDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Footer
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "OfflineTik v1.0.0 • Pure Offline Experience",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Privacy Policy Dialog
        if (showPrivacyPolicyDialog) {
            LegalDialog(
                title = "Privacy Policy",
                content = """
                    OfflineTik is committed to your complete personal privacy.

                    1. Zero Data Collection:
                    We do not collect, store, sell, or transmit any personal information, browsing habits, device identifiers, or media preferences to any external servers.

                    2. 100% Local Device Storage:
                    All downloaded short videos, audio tracks, custom playlists, and favorite statuses are indexed strictly on your local device using Android app-specific storage and an encrypted SQLite Room database.

                    3. Zero Accounts or Login:
                    OfflineTik functions completely offline without requiring sign-in, user accounts, email addresses, or phone numbers.

                    4. No Analytics or Third-Party SDKs:
                    OfflineTik contains no tracking telemetry, user-profiling trackers, or third-party behavioral analytics.
                """.trimIndent(),
                onDismiss = { showPrivacyPolicyDialog = false }
            )
        }

        // Terms of Service Dialog
        if (showTermsDialog) {
            LegalDialog(
                title = "Terms of Service",
                content = """
                    Welcome to OfflineTik.

                    1. Personal Fair Use:
                    OfflineTik is a personal media management utility designed to play, organize, and view legally saved short-form videos and audio offline.

                    2. Copyright Compliance:
                    Users agree to respect intellectual property and copyright laws. Do not use OfflineTik to distribute copyrighted material without authorization.

                    3. No DRM Circumvention:
                    OfflineTik does not bypass digital rights management (DRM), access private videos, or circumvent platform security restrictions.

                    4. Disclaimer:
                    OfflineTik is an independent offline media player and is not affiliated, endorsed, or associated with ByteDance, TikTok, or any social media network.
                """.trimIndent(),
                onDismiss = { showTermsDialog = false }
            )
        }

        // Developer Info Dialog
        if (showDeveloperInfoDialog) {
            LegalDialog(
                title = "Developer Information",
                content = """
                    OfflineTik Studio
                    
                    Crafted with Kotlin, Jetpack Compose, Media3 ExoPlayer, and Room Database.
                    
                    Engineered for high-performance offline vertical media playback, zero-tracking privacy, and native Android sharing integration.
                    
                    Contact: support@offlinetik.app
                    Version: 1.0.0 (Release Build)
                """.trimIndent(),
                onDismiss = { showDeveloperInfoDialog = false }
            )
        }

        // Theme Selector Dialog
        if (showThemeDialog) {
            val themes = listOf("Pure OLED Black", "Deep Space Purple", "Neon Cyberpunk")
            AlertDialog(
                onDismissRequest = { showThemeDialog = false },
                title = { Text("Select Theme", color = TextPrimary) },
                text = {
                    Column {
                        themes.forEach { themeName ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedThemeName = themeName
                                        showThemeDialog = false
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedThemeName == themeName,
                                    onClick = {
                                        selectedThemeName = themeName
                                        showThemeDialog = false
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = NeonPink)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(themeName, color = TextPrimary, fontSize = 15.sp)
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showThemeDialog = false }) {
                        Text("Close", color = TextMuted)
                    }
                },
                containerColor = SurfaceDark
            )
        }

        // Rate Us Dialog
        if (showRateDialog) {
            AlertDialog(
                onDismissRequest = { showRateDialog = false },
                title = { Text("Enjoying OfflineTik?", color = TextPrimary) },
                text = {
                    Text(
                        "If you love watching your favorite short videos and music completely offline without interruptions, please consider giving us a 5-star rating!",
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { showRateDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPink)
                    ) {
                        Text("Rate 5 Stars")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRateDialog = false }) {
                        Text("Maybe Later", color = TextMuted)
                    }
                },
                containerColor = SurfaceDark
            )
        }
    }
}

@Composable
fun AppHeaderCard() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceDark,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Logo
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(PureBlack)
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_offlinetik_logo),
                    contentDescription = "OfflineTik Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Offline",
                        color = TextPrimary,
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

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "v1.0.0 (Build 1) · Production Ready",
                    color = NeonCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Offline short-video player & audio vault",
                    color = TextMuted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        color = TextMuted,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingClickableItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String? = null,
    value: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
        }

        if (value != null) {
            Text(
                text = value,
                color = NeonCyan,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(end = 4.dp)
            )
        }

        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun SettingDetailItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = TextMuted,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun LegalDialog(
    title: String,
    content: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(content, color = TextSecondary, fontSize = 13.sp, lineHeight = 20.sp)
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = NeonPink)
            ) {
                Text("Got It")
            }
        },
        containerColor = SurfaceDark
    )
}

private fun shareApp(context: Context) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(
            Intent.EXTRA_TEXT,
            "Check out OfflineTik — the best offline short-video player and vault! Watch your saved videos without internet."
        )
    }
    context.startActivity(Intent.createChooser(intent, "Share OfflineTik via"))
}

private fun contactSupport(context: Context) {
    try {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:support@offlinetik.app")
            putExtra(Intent.EXTRA_SUBJECT, "OfflineTik Feedback & Support")
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        // Handle no mail client
    }
}
