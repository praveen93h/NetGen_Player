package com.nextgen.player.player.audio

import android.content.Context
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import javax.inject.Singleton

@Singleton
class AudioEngine(
    private val context: Context
) {
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var audioBoostLevel: Int = 100
    private var audioDelayMs: Long = 0L

    val currentBoostLevel: Int get() = audioBoostLevel
    val currentDelayMs: Long get() = audioDelayMs

    fun initLoudnessEnhancer(audioSessionId: Int) {
        releaseLoudnessEnhancer()
        try {
            loudnessEnhancer = LoudnessEnhancer(audioSessionId)
            loudnessEnhancer?.enabled = true
            applyBoost()
        } catch (e: Exception) {
        }
    }

    fun setAudioBoost(percentage: Int) {
        audioBoostLevel = percentage.coerceIn(0, 300)
        applyBoost()
    }

    fun incrementBoost(step: Int = 10): Int {
        setAudioBoost(audioBoostLevel + step)
        return audioBoostLevel
    }

    fun decrementBoost(step: Int = 10): Int {
        setAudioBoost(audioBoostLevel - step)
        return audioBoostLevel
    }

    fun resetBoost() {
        setAudioBoost(100)
    }

    private fun applyBoost() {
        loudnessEnhancer?.let { enhancer ->
            if (audioBoostLevel <= 100) {
                enhancer.setTargetGain(0)
            } else {
                val gainMb = ((audioBoostLevel - 100) * 10).coerceAtMost(2000)
                enhancer.setTargetGain(gainMb)
            }
        }
    }

    fun setAudioDelay(delayMs: Long) {
        audioDelayMs = delayMs.coerceIn(-5000L, 5000L)
    }

    fun adjustAudioDelay(deltaMs: Long) {
        setAudioDelay(audioDelayMs + deltaMs)
    }

    fun resetAudioDelay() {
        audioDelayMs = 0L
    }

    val formattedDelay: String
        get() {
            val sign = if (audioDelayMs >= 0) "+" else "-"
            val absDelta = kotlin.math.abs(audioDelayMs)
            return "${sign}${absDelta}ms"
        }

    val formattedBoost: String
        get() = "${audioBoostLevel}%"

    @OptIn(UnstableApi::class)
    fun createExternalAudioMediaItem(audioUri: Uri): MediaItem.SubtitleConfiguration {
        return MediaItem.SubtitleConfiguration.Builder(audioUri)
            .setMimeType(MimeTypes.BASE_TYPE_AUDIO)
            .setLanguage("und")
            .setLabel("External Audio")
            .build()
    }

    fun releaseLoudnessEnhancer() {
        try {
            loudnessEnhancer?.release()
        } catch (_: Exception) { }
        loudnessEnhancer = null
    }

    fun release() {
        releaseLoudnessEnhancer()
        audioBoostLevel = 100
        audioDelayMs = 0L
    }
}
