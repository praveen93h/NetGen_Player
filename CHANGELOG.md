# Changelog

All notable changes to NextGen Media Player will be documented in this file.

---

## [1.4.0] — 2026-03-09

### Added
- **SMB/CIFS Browsing** — Browse and play media from network shares (NAS, Windows) via smbj with SMB2/3 support
- **FTP Browsing** — Connect to FTP servers with passive mode and binary transfer
- **SFTP Browsing** — Secure file transfer via JSch SSH library
- **WebDAV Browsing** — Browse WebDAV servers using OkHttp PROPFIND requests
- **DLNA/UPnP Discovery** — Discover media servers on local network via SSDP multicast, browse ContentDirectory via SOAP
- **URL Streaming** — Play HTTP, HTTPS, RTSP, HLS, and DASH streams directly by URL
- **M3U/M3U8 Parser** — Parse playlist files with auto-detection of HLS/DASH streams
- **Server Bookmarks** — Save, edit, and delete network server connections with Room DB persistence
- **Recent URLs** — History of recently played stream URLs
- **Custom ExoPlayer DataSources** — SMB and FTP DataSources with seeking support for direct network playback
- **Network UI** — Three-tab interface (Servers / Stream / DLNA) with file browser, add/edit server dialog, and URL input dialog
- **Network Button** — Quick access to network features from library top bar

### Changed
- Room database migrated to version 2 (added `server_bookmarks` table)
- Navigation graph extended with network browsing routes
- New `feature-network` module with full Hilt DI integration
- Version bumped to 1.4.0 (versionCode 5)

### Dependencies Added
- smbj 0.13.0, Commons Net 3.11.1, JSch 0.2.18, OkHttp 4.12.0

---

## [1.3.0] — 2026-03-07

### Added
- **10-Band Graphic Equalizer** — Full equalizer with adjustable frequency bands, bass boost, and virtualizer effect
- **EQ Presets** — 10 built-in presets: Flat, Bass Boost, Vocal, Cinema, Treble Boost, Rock, Pop, Jazz, Classical, Custom
- **Video Filters** — Real-time brightness, contrast, saturation, hue, and gamma adjustments via color matrix
- **Shuffle Mode** — Randomize playback queue while keeping current video playing
- **Repeat Modes** — Cycle between Off, Repeat All, and Repeat One
- **Skip Silence** — Automatically skip silent segments during playback (uses ExoPlayer's silence detection)
- **Queue Position Display** — Shows current position (e.g., "3 / 12") in the player controls

### Changed
- Extended player controls bar with shuffle, repeat, skip silence, equalizer, and video filter buttons
- Equalizer and video filter settings accessible via bottom sheets
- Playback completion now respects repeat mode (loop, wrap, or stop)

---

## [1.2.0] — 2026-03-06

### Added
- **Picture-in-Picture (PiP)** — Auto-enters PiP on home press during video playback, with configurable auto-PiP in settings
- **Resume Playback** — Prompts to resume or start over when reopening partially watched videos; positions saved in Room DB
- **Rotation Lock** — Per-video rotation control (auto / landscape / portrait) with quick toggle in player
- **Night Filter** — Blue light filter overlay with adjustable intensity slider for nighttime viewing
- **Screen Lock** — Lock touch controls during playback to prevent accidental input; unlock via long-press
- **Playback Queue** — Auto-play next video in folder with next/previous navigation
- **Play All** — Button in folder view to play all videos in the folder
- **Multi-Audio Track Selection** — Switch between audio tracks in multi-track video files

---

## [1.1.0] — 2026-03-05

### Added
- **Background Playback** — Continuous playback via MediaSessionService with persistent notification controls (play/pause, next, previous, seek)
- **Audio Boost** — Volume amplification up to 300% via AudioEngine
- **Audio Delay Sync** — Adjust audio delay from −1.0s to +1.0s
- **Subtitle Sync** — Adjust subtitle offset for out-of-sync subtitles
- **Sync & Audio Bottom Sheet** — Unified UI panel for audio boost, audio delay, and subtitle sync controls

### Changed
- Moved all hardcoded UI strings to string resources for localization readiness
- Registered PlaybackService in AndroidManifest with foreground service permissions

---

## [1.0.0] — 2026-03-04

### Added
- **Core Video Playback** — MP4, MKV, AVI, MOV, FLV, WebM, TS with hardware-accelerated decoding via Media3/ExoPlayer
- **4K & HDR** — High-resolution playback support
- **Subtitle Engine** — Parser for SRT, ASS, SSA formats with styled rendering (colors, outline, shadow, positioning)
- **Gesture Controls** — Brightness (left swipe), volume (right swipe), horizontal seek, pinch-to-zoom, double-tap skip, long-press 2x speed
- **A-B Repeat** — Loop playback between two user-defined points
- **Variable Speed** — Playback speed control from 0.25x to 4x
- **Media Library** — Folder browsing, recent files, search, sort by name/date/size, grid and list view toggle
- **Dark Theme** — Material 3 dark theme with orange accent color
- **Multi-Module Architecture** — Modular project structure (app, core-player, core-ui, data-local, feature-library, feature-subtitle)
- **Hilt Dependency Injection** — Singleton-scoped engines with proper lifecycle management
- **Room Database** — Local storage for media metadata and playback positions
