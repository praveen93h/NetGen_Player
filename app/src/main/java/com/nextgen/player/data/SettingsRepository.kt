package com.nextgen.player.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class AppSettings(
    val defaultSpeed: Float = 1.0f,
    val resumePlayback: Boolean = true,
    val hardwareDecoding: Boolean = true,
    val subtitleFontSize: Float = 18f,
    val subtitleBackground: Boolean = true,
    val darkTheme: Boolean = true,
    val autoPiP: Boolean = true,
    val autoPlayNext: Boolean = true,
    val nightFilterIntensity: Float = 0.3f
)

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val DEFAULT_SPEED = floatPreferencesKey("default_speed")
        val RESUME_PLAYBACK = booleanPreferencesKey("resume_playback")
        val HARDWARE_DECODING = booleanPreferencesKey("hardware_decoding")
        val SUBTITLE_FONT_SIZE = floatPreferencesKey("subtitle_font_size")
        val SUBTITLE_BACKGROUND = booleanPreferencesKey("subtitle_background")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val AUTO_PIP = booleanPreferencesKey("auto_pip")
        val AUTO_PLAY_NEXT = booleanPreferencesKey("auto_play_next")
        val NIGHT_FILTER_INTENSITY = floatPreferencesKey("night_filter_intensity")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            defaultSpeed = prefs[Keys.DEFAULT_SPEED] ?: 1.0f,
            resumePlayback = prefs[Keys.RESUME_PLAYBACK] ?: true,
            hardwareDecoding = prefs[Keys.HARDWARE_DECODING] ?: true,
            subtitleFontSize = prefs[Keys.SUBTITLE_FONT_SIZE] ?: 18f,
            subtitleBackground = prefs[Keys.SUBTITLE_BACKGROUND] ?: true,
            darkTheme = prefs[Keys.DARK_THEME] ?: true,
            autoPiP = prefs[Keys.AUTO_PIP] ?: true,
            autoPlayNext = prefs[Keys.AUTO_PLAY_NEXT] ?: true,
            nightFilterIntensity = prefs[Keys.NIGHT_FILTER_INTENSITY] ?: 0.3f
        )
    }

    suspend fun updateDefaultSpeed(speed: Float) {
        context.dataStore.edit { it[Keys.DEFAULT_SPEED] = speed }
    }

    suspend fun updateResumePlayback(enabled: Boolean) {
        context.dataStore.edit { it[Keys.RESUME_PLAYBACK] = enabled }
    }

    suspend fun updateHardwareDecoding(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HARDWARE_DECODING] = enabled }
    }

    suspend fun updateSubtitleFontSize(size: Float) {
        context.dataStore.edit { it[Keys.SUBTITLE_FONT_SIZE] = size }
    }

    suspend fun updateSubtitleBackground(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SUBTITLE_BACKGROUND] = enabled }
    }

    suspend fun updateDarkTheme(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DARK_THEME] = enabled }
    }

    suspend fun updateAutoPiP(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_PIP] = enabled }
    }

    suspend fun updateAutoPlayNext(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_PLAY_NEXT] = enabled }
    }

    suspend fun updateNightFilterIntensity(intensity: Float) {
        context.dataStore.edit { it[Keys.NIGHT_FILTER_INTENSITY] = intensity }
    }
}
