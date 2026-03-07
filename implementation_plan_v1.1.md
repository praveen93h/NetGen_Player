# NextGen Media Player — Final Excellence Pass (v1.1)

This plan addresses the final high-impact features required for a professional-grade media player, focusing on background playback, synchronization controls, and localized resources.

## User Review Required

> [!IMPORTANT]
> **Background Playback**: Currently, playback may stop or the app may be killed when backgrounded. This plan adds a `MediaSessionService` to ensure continuous playback and persistent notification controls.

---

## Proposed Changes

### Background Playback & MediaSession

#### [NEW] [PlaybackService.kt](file:///c:/games/MediaPlayer/app/src/main/java/com/nextgen/player/service/PlaybackService.kt)
- Implement `MediaSessionService` using Media3.
- Initialize and manage `MediaSession` wrapping the [PlayerEngine](file:///c:/games/MediaPlayer/core-player/src/main/java/com/nextgen/player/player/PlayerEngine.kt#42-308).
- Handle `onGetSession` to provide the session to the system.

#### [MODIFY] [AndroidManifest.xml](file:///c:/games/MediaPlayer/app/src/main/AndroidManifest.xml)
- Register `PlaybackService` with `android:foregroundServiceType="mediaPlayback"`.
- Add `android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK`.

#### [MODIFY] [PlayerEngine.kt](file:///c:/games/MediaPlayer/core-player/src/main/java/com/nextgen/player/player/PlayerEngine.kt)
- Add `MediaSession` support.
- Ensure the `ExoPlayer` instance is correctly shared with the service.

---

### Player Power Features (Sync & Audio)

#### [MODIFY] [PlayerScreen.kt](file:///c:/games/MediaPlayer/app/src/main/java/com/nextgen/player/ui/PlayerScreen.kt)
- Add a new "Sync & Audio" bottom sheet reachable from the top bar.
- Implement **Audio Boost** slider (0% to 200%).
- Implement **Audio Delay** adjustment (-1.0s to +1.0s).
- Implement **Subtitle Sync** adjustment (delta offset).

#### [MODIFY] [PlayerViewModel.kt](file:///c:/games/MediaPlayer/app/src/main/java/com/nextgen/player/ui/PlayerViewModel.kt)
- Expose state for the new "Sync & Audio" controls.
- Add formatted strings for sync status (e.g., "+0.2s").

---

### Localization & Cleanup

#### [MODIFY] [strings.xml](file:///c:/games/MediaPlayer/app/src/main/res/values/strings.xml)
- Move all hardcoded strings (Settings, Volume, Brightness, Permission labels, etc.) from Composables to resources.

#### [MODIFY] [LibraryScreen.kt](file:///c:/games/MediaPlayer/feature-library/src/main/java/com/nextgen/player/library/ui/LibraryScreen.kt)
- Improve the "Empty Library" state with a more descriptive message and a "Scan Now" button.

---

## Verification Plan

### Manual Verification
1. **Background Playback**: Start a video -> Press Home -> Verify playback continues and a media notification appears in the status bar.
2. **Notification Controls**: Use the notification Play/Pause/Seek buttons -> Verify they control the app correctly.
3. **Sync Controls**: Open "Sync & Audio" sheet -> Adjust Subtitle Sync -> Verify subtitles shift appropriately.
4. **Localization**: Check that all UI elements use string resources (verified by lint/audit).
