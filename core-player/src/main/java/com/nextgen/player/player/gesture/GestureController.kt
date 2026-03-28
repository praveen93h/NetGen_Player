package com.nextgen.player.player.gesture

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import android.view.WindowManager
import kotlin.math.abs

enum class GestureZoneType {
    BRIGHTNESS,
    VOLUME,
    SEEK,
    NONE
}

enum class VideoScaleType {
    FIT,
    FILL,
    RATIO_16_9,
    RATIO_4_3,
    CROP;

    fun next(): VideoScaleType {
        val values = entries.toTypedArray()
        return values[(ordinal + 1) % values.size]
    }

    val displayName: String
        get() = when (this) {
            FIT -> "Fit"
            FILL -> "Fill"
            RATIO_16_9 -> "16:9"
            RATIO_4_3 -> "4:3"
            CROP -> "Crop"
        }
}

data class GestureState(
    val isActive: Boolean = false,
    val gestureType: GestureZoneType = GestureZoneType.NONE,
    val brightnessLevel: Float = 0.5f,
    val volumeLevel: Float = 0.5f,
    val seekDelta: Long = 0L,
    val seekPreviewPosition: Long = 0L,
    val isLocked: Boolean = false,
    val scaleType: VideoScaleType = VideoScaleType.FIT,
    val showIndicator: Boolean = false,
    val indicatorText: String = ""
)

class GestureController(
    private val context: Context
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    private var initialStateSynced = false
    private var activityRef: Activity? = null

    private var _state = GestureState()
    val state: GestureState get() = _state

    fun attachActivity(activity: Activity) {
        activityRef = activity
    }

    fun detachActivity() {
        activityRef = null
    }

    private fun syncInitialState() {
        if (initialStateSynced) return
        initialStateSynced = true
        _state = _state.copy(
            brightnessLevel = getBrightness(),
            volumeLevel = getVolume()
        )
    }

    fun getBrightness(): Float {
        val activity = activityRef ?: return 0.5f
        val layoutParams = activity.window.attributes
        return if (layoutParams.screenBrightness >= 0) {
            layoutParams.screenBrightness
        } else {
            try {
                Settings.System.getInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS
                ) / 255f
            } catch (e: Exception) {
                0.5f
            }
        }
    }

    fun setBrightness(level: Float) {
        val activity = activityRef ?: return
        val clamped = level.coerceIn(0.01f, 1f)
        val layoutParams = activity.window.attributes
        layoutParams.screenBrightness = clamped
        activity.window.attributes = layoutParams
        _state = _state.copy(
            brightnessLevel = clamped,
            showIndicator = true,
            indicatorText = "Brightness: ${(clamped * 100).toInt()}%",
            gestureType = GestureZoneType.BRIGHTNESS
        )
    }

    fun adjustBrightness(delta: Float) {
        val current = getBrightness()
        setBrightness(current + delta)
    }

    fun getVolume(): Float {
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        return currentVolume.toFloat() / maxVolume
    }

    fun setVolume(level: Float) {
        val clamped = level.coerceIn(0f, 1f)
        val volumeInt = (clamped * maxVolume).toInt()
        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            volumeInt,
            0
        )
        _state = _state.copy(
            volumeLevel = clamped,
            showIndicator = true,
            indicatorText = "Volume: ${(clamped * 100).toInt()}%",
            gestureType = GestureZoneType.VOLUME
        )
    }

    fun adjustVolume(delta: Float) {
        val current = getVolume()
        setVolume(current + delta)
    }

    fun startSeek(currentPosition: Long) {
        _state = _state.copy(
            isActive = true,
            gestureType = GestureZoneType.SEEK,
            seekDelta = 0L,
            seekPreviewPosition = currentPosition
        )
    }

    fun updateSeek(deltaPx: Float, currentPosition: Long, duration: Long, screenWidth: Float) {
        val seekSensitivity = duration / screenWidth * 0.5f
        val seekDelta = (deltaPx * seekSensitivity).toLong()
        val newPosition = (currentPosition + seekDelta).coerceIn(0L, duration)

        val sign = if (seekDelta >= 0) "+" else "-"
        val absDelta = abs(seekDelta)
        val deltaSeconds = absDelta / 1000
        val deltaMinutes = deltaSeconds / 60
        val deltaRemainder = deltaSeconds % 60

        val timeText = if (deltaMinutes > 0) {
            "$sign${deltaMinutes}:${String.format("%02d", deltaRemainder)}"
        } else {
            "$sign${deltaRemainder}s"
        }

        _state = _state.copy(
            seekDelta = seekDelta,
            seekPreviewPosition = newPosition,
            showIndicator = true,
            indicatorText = "${formatTime(newPosition)} ($timeText)"
        )
    }

    fun endSeek(): Long {
        val position = _state.seekPreviewPosition
        _state = _state.copy(
            isActive = false,
            gestureType = GestureZoneType.NONE,
            showIndicator = false
        )
        return position
    }

    fun toggleLock() {
        _state = _state.copy(isLocked = !_state.isLocked)
    }

    fun isLocked(): Boolean = _state.isLocked

    fun cycleScale(): VideoScaleType {
        val newScale = _state.scaleType.next()
        _state = _state.copy(
            scaleType = newScale,
            showIndicator = true,
            indicatorText = newScale.displayName
        )
        return newScale
    }

    fun hideIndicator() {
        _state = _state.copy(showIndicator = false)
    }

    fun onGestureStart(zone: GestureZoneType) {
        syncInitialState()
        _state = _state.copy(isActive = true, gestureType = zone)
    }

    fun onGestureEnd() {
        _state = _state.copy(isActive = false, gestureType = GestureZoneType.NONE, showIndicator = false)
    }

    private fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }
}
