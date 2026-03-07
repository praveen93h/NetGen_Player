# NextGen Media Player — v1.2 Competitive Roadmap

Competitive analysis and feature plan to stand out against VLC, MX Player, mpv, Nova Player, and other top Android media players on Google Play Store.

---

## Current Feature Summary (v1.0 + v1.1)

- Core playback (MP4, MKV, AVI, MOV, FLV, WebM, TS) with HW acceleration
- Subtitle engine (SRT, ASS, SSA) with sync adjustment
- Audio boost (300%), audio delay sync
- Gesture controls (brightness, volume, seek, zoom, double-tap, long-press 2x)
- A-B repeat, playback speed (0.25x–4x)
- Local media library (folders, recent, search, sort, grid/list)
- Background playback via MediaSession
- Sync & Audio bottom sheet UI
- Dark Material 3 theme, string resources

---

## v1.2 — Polish & Essential Missing Features

### 1. Picture-in-Picture (PiP) Mode
- Auto-enter PiP on home press during video playback
- Compact controls in PiP window (play/pause, seek)
- Configurable auto-PiP behavior in settings
- **Why**: MX Player & VLC both offer this — users expect it

### 2. Resume Playback Prompt
- Show "Resume from XX:XX?" dialog when reopening a partially watched video
- Option to "Start Over" or "Resume"
- Save position per-video in Room DB
- **Why**: MX Player Pro does this and users frequently cite it as a must-have

### 3. Video Thumbnails & Metadata Display
- Generate video thumbnails using MediaMetadataRetriever or Coil/Glide
- Show resolution badge (720p/1080p/4K), codec info, file size in library
- Duration overlay on thumbnail corners
- **Why**: Visual browsing is critical — bare file lists drive users away

### 4. Lock Screen Controls
- Full lock screen & notification media controls via MediaSession
- Album art / video thumbnail in notification
- Play/pause, next, previous, seek bar
- **Why**: Standard expectation for any media player

### 5. Screen Lock During Playback
- Lock touch controls button to prevent accidental touches
- Unlock via long-press or swipe gesture
- **Why**: Top feature request in MX Player reviews

### 6. Rotation Lock / Auto-Rotate
- Per-video rotation lock (portrait / landscape / sensor)
- Force rotation for oddly-encoded videos
- Quick toggle in player controls
- **Why**: VLC and MX Player both offer this — essential for mobile viewing

### 7. Night / Blue Light Filter
- Overlay warm color filter over video
- Adjustable intensity slider
- Quick toggle in player controls
- **Why**: Unique differentiator vs most players that lack this

### 8. Swipe-to-Delete & File Operations
- Long press on media item → Delete, Rename, Share, File Info
- Confirmation dialog for destructive actions
- **Why**: Users shouldn't need a file manager to manage their media

---

## v1.3 — Advanced Playback & Audio

### 9. Equalizer & Audio Effects
- 10-band graphic equalizer
- Presets (Flat, Bass Boost, Vocal, Cinema, Custom)
- Virtualizer & bass boost sliders
- Save per-video or global presets
- **Why**: MX Player Pro and Poweramp users love EQ — strong differentiator

### 10. Multi-Audio Track Selection
- UI for selecting between multiple audio tracks (language, commentary)
- External audio file loading (load separate .mp3/.aac alongside video)
- Display track info (codec, language, channels, bitrate)
- **Why**: Essential for MKV files with multiple tracks

### 11. Video Filters & Adjustments
- Real-time brightness, contrast, saturation, hue, gamma controls
- Shader-based or OpenGL-based pipeline
- Per-video or global settings
- **Why**: VLC has this; none of the lightweight players do

### 12. Playback Queue / Playlist
- "Play All" from folder
- Drag-to-reorder queue
- Next/Previous controls
- Auto-play next video in folder
- Shuffle & repeat modes
- **Why**: Most file-based players lack a proper queue — major gap

### 13. Skip Silence / Skip Intro
- Detect and skip silent segments (like podcast players)
- Manual "Skip 85s" button for series intros
- **Why**: Innovative feature — almost no video player offers this

### 14. Variable Speed with Pitch Correction
- Speed changes from 0.25x to 4x without chipmunk effect
- Sonic or RubberBand-like audio processing
- **Why**: Lecture/podcast viewers need this; most players distort audio at speed

---

## v1.4 — Network & Streaming

### 15. Network Streaming (SMB/FTP/SFTP/WebDAV)
- Browse and play from NAS, shared folders, servers
- Save server bookmarks with credentials (encrypted storage)
- Buffer/cache settings for network streams
- **Why**: VLC and Nova Player dominate here — this opens the NAS user market

### 16. HTTP/HTTPS/RTSP Stream Playback
- Open URL dialog for direct stream playback
- M3U/M3U8 playlist support
- HLS & DASH adaptive streaming
- **Why**: IPTV and streaming users need direct URL playback

### 17. DLNA / UPnP Client
- Discover and browse DLNA media servers on local network
- Stream directly from compatible devices (smart TVs, PCs, NAS)
- **Why**: Key feature for home network users

### 18. Chromecast Support
- Cast video to Chromecast / Google TV devices
- Control playback from phone while casting
- Subtitle forwarding to cast device
- **Why**: Users expect cast support — major competitive disadvantage without it

---

## v1.5 — UI/UX & Personalization

### 19. Material You / Dynamic Color
- Support Android 12+ dynamic color theming
- Accent color picker for older devices
- Multiple theme options (Pure Black, Dark, Midnight Blue)
- **Why**: Modern Android apps are expected to support Material You

### 20. Custom Gesture Mapping
- Let users assign gestures: double-tap sides for skip ±10s, swipe distance sensitivity
- Configurable seek duration (5s/10s/15s/30s)
- Bottom swipe areas for brightness/volume or off
- **Why**: Power users want control — this is a premium differentiator

### 21. Floating Video (Pop-up Player)
- Floating window overlay outside the app
- Resize and reposition freely
- Mini controls (play/pause, close)
- **Why**: Samsung and MX Player both offer this — very popular feature

### 22. Favorites & Watch History
- Heart/star videos to add to favorites
- Detailed watch history with timestamps
- "Continue Watching" section on library home
- Clear history option for privacy
- **Why**: Makes the app feel like a full media center, not just a file opener

### 23. Batch Operations
- Select multiple files → Delete, Move, Share
- "Select All in Folder" support
- **Why**: File management without leaving the app

---

## v1.6 — Subtitle Powerhouse

### 24. Online Subtitle Download
- Integrate OpenSubtitles API
- Auto-match subtitles by video hash + filename
- Language preference and download management
- **Why**: MX Player's #1 feature — absolutely essential for competitive parity

### 25. Subtitle Appearance Customization
- Font family, size, color, outline, shadow, background
- Position (top/bottom/custom Y offset)
- Preview in real-time
- **Why**: VLC and MX Player both offer deep subtitle customization

### 26. Dual Subtitle Display
- Show two subtitle tracks simultaneously (e.g., English + native language)
- Independent positioning (top + bottom)
- **Why**: Language learners love this — unique competitive advantage

### 27. Embedded Subtitle Track Selection
- UI to switch between embedded subtitle tracks in MKV/MP4
- Support for PGS (image-based) subtitles
- **Why**: MKV files frequently have multiple embedded tracks

---

## v1.7 — Performance & Codec

### 28. Software Decoding Fallback (FFmpeg)
- Integrate FFmpeg-based decoder as fallback when hardware decoder fails
- Support obscure codecs: HEVC, AV1, VP9, MPEG-2, WMV, DivX, Xvid
- AC3, DTS, FLAC, Opus, Vorbis audio codec support
- **Why**: VLC's biggest strength — plays everything

### 29. HDR / Dolby Vision / HDR10+ Support
- Proper tone mapping for non-HDR displays
- HDR metadata passthrough for HDR displays
- **Why**: 4K content increasingly uses HDR — essential for future-proofing

### 30. Hardware Decode Reporting
- Show currently used decoder (HW/SW) in video info
- Log codec negotiation for troubleshooting
- **Why**: Power users and reviewers value transparency

---

## v1.8 — Smart Features & AI

### 31. Smart Auto-Brightness
- Analyze video scene brightness and adjust screen brightness dynamically
- Dark scene boost for nighttime viewing
- **Why**: Unique feature no competitor offers

### 32. Video Bookmark / Scene Markers
- Tap to bookmark a timestamp during playback
- Named bookmarks with optional screenshot
- Jump to any bookmark
- **Why**: Great for lectures, tutorials, long videos

### 33. Frame-by-Frame Navigation
- Step forward/backward one frame at a time
- Useful for video analysis, sports replays
- **Why**: VLC desktop has this — rare on mobile

### 34. Audio-Only Mode
- Play video files as audio-only for music videos
- Background audio without video rendering to save battery
- **Why**: Users play music videos as audio frequently

---

## v1.9 — Ecosystem & Platform

### 35. Android TV / Leanback Support
- Optimized TV interface with D-pad navigation
- Leanback recommendation row
- Voice search integration
- **Why**: Expand market to TV users — very few quality free TV players

### 36. Android Auto Support
- Audio playback integration for Android Auto
- Album art and controls on car display
- **Why**: Audio-only mode + Auto = great for commuters

### 37. Tablet & Foldable Optimization
- Adaptive layouts for large screens
- Split-screen support
- Fold-aware playback (continue on unfold)
- **Why**: Growing foldable market with few optimized players

### 38. Wear OS Companion
- Remote control from smartwatch
- Play/pause, volume, seek
- **Why**: Nice-to-have that sets the app apart in reviews

---

## v2.0 — Premium & Monetization

### 39. Cloud Backup & Sync
- Backup settings, watch history, bookmarks to Google Drive
- Sync across devices
- **Why**: Premium feature — users hate losing their history

### 40. Widgets
- Home screen widget: recently played, quick resume
- Now playing widget with controls
- **Why**: Convenience feature that increases daily engagement

### 41. App Lock / Private Folder
- PIN/biometric lock for the app or specific folders
- Hidden media folders
- **Why**: Privacy is a top concern — MX Player offers this in Pro

### 42. In-App Video Trimmer / Editor
- Basic trim, cut, merge operations
- Extract audio from video
- No re-encoding for supported formats (fast trim)
- **Why**: Users want quick edits without a separate editor app

---

## Priority Matrix

| Priority | Features | Impact | Effort |
|----------|----------|--------|--------|
| **P0 — Ship First** | PiP, Resume, Thumbnails, Lock Screen, Screen Lock, Rotation | High | Low-Medium |
| **P1 — Core Competitive** | EQ, Multi-Track Audio, Playlists, Subtitle Download, Subtitle Customization | High | Medium |
| **P2 — Differentiators** | Night Filter, Dual Subtitles, Video Filters, Custom Gestures, Floating Player | Medium-High | Medium |
| **P3 — Power Users** | FFmpeg, HDR, Frame-by-Frame, Bookmarks, Network Streaming | Medium | High |
| **P4 — Platform Expansion** | Android TV, Chromecast, DLNA, Tablet/Foldable | Medium | High |
| **P5 — Premium** | Cloud Sync, App Lock, Video Editor, Widgets | Low-Medium | Medium-High |

---

## Competitive Landscape

| Feature | NextGen (v1.1) | MX Player Pro | VLC | Nova Player | mpv |
|---------|---------------|---------------|-----|-------------|-----|
| HW Acceleration | ✅ | ✅ | ✅ | ✅ | ✅ |
| SW Decoding (FFmpeg) | ❌ | ✅ | ✅ | ✅ | ✅ |
| Gesture Controls | ✅ | ✅ | ✅ | ✅ | ❌ |
| Subtitle Sync | ✅ | ✅ | ✅ | ✅ | ✅ |
| Online Subtitle DL | ❌ | ✅ | ✅ | ❌ | ❌ |
| Audio Boost | ✅ | ✅ | ❌ | ❌ | ❌ |
| Equalizer | ❌ | ✅ | ✅ | ❌ | ❌ |
| PiP | ❌ | ✅ | ✅ | ✅ | ❌ |
| Chromecast | ❌ | ✅ | ❌ | ✅ | ❌ |
| Network Play | ❌ | ✅ | ✅ | ✅ | ❌ |
| Floating Player | ❌ | ✅ | ❌ | ❌ | ❌ |
| Screen Lock | ❌ | ✅ | ✅ | ✅ | ❌ |
| Android TV | ❌ | ❌ | ✅ | ✅ | ❌ |
| Ad-Free | ✅ | ✅ (paid) | ✅ | ❌ | ✅ |
| Material 3 / Modern UI | ✅ | ❌ | ❌ | ✅ | ❌ |
| Dual Subtitles | ❌ | ❌ | ❌ | ❌ | ✅ |

---

## Key Differentiators to Focus On

1. **Modern Material 3 UI** — Most competitors look dated. Keep the UI polished and modern.
2. **Ad-Free & Privacy-First** — This is the #1 complaint about MX Player (free). Own this positioning.
3. **Dual Subtitles** — Almost no Android player does this well. Target language learners.
4. **Night/Blue Light Filter** — Unique feature for nighttime viewing.
5. **Smart Auto-Brightness** — Scene-aware brightness is genuinely novel.
6. **Zero-bloat philosophy** — No news feeds, no video recommendations, no social features. Just a player.

---

## Recommended v1.2 Implementation Order

1. PiP Mode (high impact, low effort)
2. Resume Playback Prompt (high impact, low effort)
3. Screen Lock Button (high impact, low effort)
4. Rotation Lock Toggle (medium impact, low effort)
5. Video Thumbnails in Library (high impact, medium effort)
6. Playback Queue / Auto-Next (high impact, medium effort)
7. Multi-Audio Track Selection UI (high impact, medium effort)
8. Night Filter (medium impact, low effort)
