# 🎬 OfflineTik — Offline Short Video Player & Audio Vault

**OfflineTik** is a premium, high-performance Android application inspired by TikTok's immersive vertical scrolling experience. It allows users to save legally downloadable videos and sounds locally on their Android devices and watch or listen completely offline without requiring an active internet connection.

> 🔒 **Privacy & Vault Architecture**: This is not a social network. There are no public accounts, login gates, comments, or fake followers. It is a personal offline vault for your media.

---

## ✨ Features

### 1. 📱 Home — Vertical Video Feed
- **TikTok-Style Vertical Swipe**: Seamless full-screen video player with auto-looping, auto-play on swipe, and pause on exit.
- **Save to Gallery**: Direct one-tap export to the device's native Gallery (`Movies/OfflineTik`) using scoped Android `MediaStore` APIs.
- **Original Source & Profile Info**: Transparent metadata modal displaying the downloaded creator's handle (`@username`), soundtrack, file size, duration, and local path.
- **Native Sharing**: Share video files directly via Android's `ACTION_SEND` intent.
- **Mute / Audio Toggle & Spinning Vinyl**: Authentic TikTok aesthetics with spinning vinyl album disc and sound toggles.

### 2. ⚡ Download — Direct Paste & Library Manager
- **Inline Link Downloader**: Prominent top card to paste TikTok or direct media URLs with a 1-tap **"Paste from Clipboard"** button.
- **Dual Format Choice**:
  - **Save Video (MP4)**: Saves video directly to your local feed and download library.
  - **Save Audio (Music)**: Extracts the soundtrack directly for background audio playback.
- **Live Download Progress**: Real-time progress bar, stream validation, and byte stats.
- **Offline Storage Breakdown**: Real-time storage stats for videos, audio, cache, and free device storage.
- **Local Importer**: Zero-permission import of videos and audio files directly from your device.

### 3. 🎵 Audio — Full Offline Music Player
- **Background & Lock-Screen Playback**: Powered by Media3 ExoPlayer for background playback when the screen is off.
- **Full Player & Mini Bar**: Persistent bottom mini player and expandable full-screen player with seekbar and vinyl rotation.
- **Playlists & Queues**: Create custom playlists, shuffle, and repeat modes (`OFF`, `ALL`, `ONE`).
- **Save to Device Music**: Export audio tracks directly to the phone's `Music/OfflineTik` folder.

### 4. ⚙️ Settings — Maintenance & Personalization
- **Cache Cleaner**: One-tap cache cleanup with freed MB calculations.
- **TikTok Signature Palette**: Modern dark aesthetic with TikTok Red (`#FE2C55`) and TikTok Cyan (`#25F4EE`).
- **Developer Info & Contact**: Built-in support and feedback channels.

### 5. 🔄 TikTok Share Integration
- Register as an Android share target for TikTok links. When you tap "Share" in TikTok and select **OfflineTik**, the app automatically resolves the downloadable stream and saves it locally.

---

## 🚀 Automated GitHub Actions APK Build

Every time you push commits to GitHub, the included GitHub Actions workflow (`.github/workflows/build-apk.yml`) automatically builds the Android APK.

### How to download the APK from GitHub:
1. Go to your GitHub repository in your browser.
2. Click on the **Actions** tab at the top.
3. Click on the latest workflow run: **Build OfflineTik Android APK**.
4. Scroll down to the **Artifacts** section at the bottom.
5. Download **OfflineTik-Android-APK** (ZIP containing `OfflineTik-latest.apk`) and install it directly on your Android phone!

---

## 🛠️ Tech Stack & Architecture
- **Language**: Kotlin 2.1.0
- **UI Framework**: Jetpack Compose (Material 3)
- **Architecture**: MVVM with Kotlin Coroutines & StateFlow
- **Media Engine**: AndroidX Media3 ExoPlayer 1.5.1
- **Local Database**: Room 2.7.0 (KSP)
- **Networking**: OkHttp 4.10.0
- **Image Loading**: Coil Compose 3.0.4
- **Minimum SDK**: Android 7.0 (API 24)
- **Target SDK**: Android 15 / 16 (API 36)

---

## 💻 Local Building

To build the project locally using Gradle:

```bash
# Clone the repository
git clone https://github.com/your-username/OfflineTik.git
cd OfflineTik

# Run unit tests
./gradlew testDebugUnitTest

# Assemble Debug APK
./gradlew assembleDebug
```

The output APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`
