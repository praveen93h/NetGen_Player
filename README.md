# NextGen Media Player

A modern, ad-free, high-performance media player for Android — built with Kotlin, Jetpack Compose, and Media3/ExoPlayer.

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)]()
[![Min SDK](https://img.shields.io/badge/Min%20SDK-24-blue.svg)]()
[![Version](https://img.shields.io/badge/Version-1.4.0-orange.svg)]()

---

## Table of Contents

- [Screenshots](#screenshots)
- [Features](#features)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Building](#building)
- [Roadmap](#roadmap)
- [Changelog](#changelog)
- [License](#license)

---

## Screenshots

> *Coming soon*

---

## Features

### Playback
- Supports **MP4, MKV, AVI, MOV, FLV, WebM, TS**
- 4K & HDR playback with hardware acceleration
- Variable speed playback (0.25x – 4x)
- A-B repeat for looping sections
- Picture-in-Picture (PiP) mode
- Resume playback from where you left off
- Background playback via MediaSession with notification controls
- Skip silence detection

### Audio
- 10-band graphic equalizer with presets (Flat, Bass Boost, Vocal, Cinema, Rock, Pop, Jazz, Classical, etc.)
- Bass boost & virtualizer effects
- Audio boost up to 300%
- Audio delay sync adjustment
- Multi-audio track selection

### Video
- Real-time video filters (brightness, contrast, saturation, hue, gamma)
- Night / blue light filter
- Rotation lock (portrait / landscape / sensor)
- Screen lock to prevent accidental touches

### Subtitles
- Subtitle support (SRT, ASS, SSA, SUB)
- Subtitle sync adjustment
- Styled rendering with color, outline, shadow

### Gestures
- Swipe for brightness & volume control
- Horizontal swipe to seek
- Pinch to zoom
- Double-tap to skip forward/backward
- Long-press for 2x speed

### Network & Streaming
- Browse **SMB/CIFS** network shares (NAS, Windows shares)
- Browse **FTP** and **SFTP** servers
- Browse **WebDAV** servers
- **DLNA/UPnP** device discovery and content browsing
- **URL streaming** — HTTP, HTTPS, RTSP, HLS, DASH
- **M3U/M3U8** playlist parsing
- Server bookmarks with connection management
- Recent URL history

### Library
- Browse by folders, recent files, favorites
- Play All from folder
- Playback queue with shuffle & repeat modes (Off / All / One)
- Auto-play next video in folder

### Privacy
- 100% ad-free, no trackers, no analytics
- All data stored locally on device

---

## Architecture

Multi-module MVVM / Clean Architecture:

```
app/                  → Main app, navigation, ViewModels, UI screens
core-player/          → PlayerEngine, AudioEngine, EqualizerEngine, GestureController
core-ui/              → Theme, colors, typography, shared components
data-local/           → Room database, DAOs, MediaScanner, repositories
feature-library/      → Library browsing UI (folders, recent, search)
feature-network/      → Network browsing (SMB/FTP/SFTP/WebDAV/DLNA), URL streaming
feature-subtitle/     → Subtitle parsing (SRT/ASS/SSA), rendering, sync
```

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | **Kotlin** |
| UI | **Jetpack Compose** + **Material 3** |
| Playback | **Media3 / ExoPlayer** |
| DI | **Hilt** (Dagger) |
| Database | **Room** |
| Preferences | **DataStore** |
| Async | **Kotlin Coroutines** + **Flow** |
| Networking | **smbj** (SMB), **Commons Net** (FTP), **JSch** (SFTP), **OkHttp** (WebDAV/HTTP) |
| Build | **Gradle KTS** + **Version Catalog** |

---

## Project Structure

```
MediaPlayer/
├── app/                          # Main application module
│   └── src/main/
│       ├── java/.../player/
│       │   ├── MainActivity.kt        # Entry point
│       │   ├── PlayerActivity.kt      # Player host activity
│       │   ├── ui/
│       │   │   ├── PlayerScreen.kt    # Player composable with controls
│       │   │   ├── PlayerViewModel.kt # Player state management
│       │   │   ├── SettingsScreen.kt  # App settings
│       │   │   └── SettingsViewModel.kt
│       │   ├── navigation/NavGraph.kt
│       │   ├── service/PlaybackService.kt
│       │   └── di/AppModule.kt
│       └── res/
├── core-player/                  # Playback engine module
│   └── src/main/java/.../player/
│       ├── PlayerEngine.kt            # ExoPlayer wrapper
│       ├── VideoFilterState.kt        # Color matrix filters
│       ├── audio/
│       │   ├── AudioEngine.kt         # Audio boost & delay
│       │   └── EqualizerEngine.kt     # 10-band EQ, bass boost, virtualizer
│       ├── gesture/GestureController.kt
│       └── di/PlayerModule.kt
├── core-ui/                      # Shared design system
│   └── src/main/java/.../ui/
│       ├── theme/ (Color, Theme, Type)
│       └── components/PlayerIcons.kt
├── data-local/                   # Local data layer
│   └── src/main/java/.../data/local/
│       ├── AppDatabase.kt
│       ├── dao/, entity/, repository/
│       └── scanner/
├── feature-library/              # Media library UI
│   └── src/main/java/.../library/
│       ├── ui/ (LibraryScreen, FolderScreen, etc.)
│       └── viewmodel/
├── feature-network/              # Network streaming module
│   └── src/main/java/.../network/
│       ├── client/
│       │   ├── SmbClient.kt           # SMB2/3 browsing via smbj
│       │   ├── FtpClient.kt           # FTP browsing via Commons Net
│       │   ├── SftpClient.kt          # SFTP browsing via JSch
│       │   ├── WebDavClient.kt        # WebDAV via OkHttp PROPFIND
│       │   └── DlnaClient.kt          # SSDP discovery + SOAP browsing
│       ├── datasource/
│       │   ├── SmbDataSource.kt       # ExoPlayer SMB DataSource
│       │   └── FtpDataSource.kt       # ExoPlayer FTP DataSource
│       ├── parser/M3uParser.kt        # M3U/M3U8 playlist parser
│       ├── model/NetworkModels.kt
│       ├── ui/ (NetworkScreen, ServerBrowserScreen, dialogs)
│       ├── viewmodel/NetworkViewModel.kt
│       └── di/NetworkModule.kt
├── feature-subtitle/             # Subtitle engine
│   └── src/main/java/.../subtitle/
│       ├── SubtitleParser.kt
│       ├── SubtitleRenderer.kt
│       └── SubtitleSyncManager.kt
└── gradle/
    └── libs.versions.toml        # Version catalog
```

---

## Building

### Prerequisites
- **Android Studio** Ladybug (2024.2+) or newer
- **JDK 17**
- **Android SDK** with API 35

### Debug Build
```bash
./gradlew assembleDebug
```
APK output: `app/build/outputs/apk/debug/app-debug.apk`

### Release Build
```bash
./gradlew assembleRelease
```

---

## Roadmap

| Version | Status | Focus |
|---------|--------|-------|
| v1.0 | ✅ Done | Core playback, subtitles, gestures, library |
| v1.1 | ✅ Done | Background playback, audio sync, string resources |
| v1.2 | ✅ Done | PiP, resume playback, rotation lock, night filter, screen lock, queue |
| v1.3 | ✅ Done | Equalizer, video filters, shuffle/repeat, skip silence |
| v1.4 | ✅ Done | Network streaming (SMB/FTP/SFTP/WebDAV), URL playback, DLNA/UPnP |
| v1.5 | Planned | Material You, custom gestures, floating video, favorites, watch history |
| v1.6 | Planned | Online subtitle download, subtitle customization, dual subtitles |
| v1.7 | Planned | FFmpeg fallback, HDR/Dolby Vision, decoder reporting |
| v1.8 | Planned | Smart auto-brightness, video bookmarks, frame-by-frame, audio-only mode |
| v1.9 | Planned | Android TV support, tablet layout, Wear OS remote |

---

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for a detailed version history.

---

## License

Proprietary – All rights reserved.
