package com.nextgen.player.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextgen.player.data.AppSettings
import com.nextgen.player.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    fun setDefaultSpeed(speed: Float) {
        viewModelScope.launch { settingsRepository.updateDefaultSpeed(speed) }
    }

    fun setResumePlayback(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateResumePlayback(enabled) }
    }

    fun setHardwareDecoding(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateHardwareDecoding(enabled) }
    }

    fun setSubtitleFontSize(size: Float) {
        viewModelScope.launch { settingsRepository.updateSubtitleFontSize(size) }
    }

    fun setSubtitleBackground(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateSubtitleBackground(enabled) }
    }

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateDarkTheme(enabled) }
    }

    fun setAutoPiP(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateAutoPiP(enabled) }
    }

    fun setAutoPlayNext(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateAutoPlayNext(enabled) }
    }

    fun setNightFilterIntensity(intensity: Float) {
        viewModelScope.launch { settingsRepository.updateNightFilterIntensity(intensity) }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch { settingsRepository.updateThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateDynamicColor(enabled) }
    }

    fun setAccentColorHex(hex: String) {
        viewModelScope.launch { settingsRepository.updateAccentColorHex(hex) }
    }

    fun setDoubleTapSeekDuration(seconds: Int) {
        viewModelScope.launch { settingsRepository.updateDoubleTapSeekDuration(seconds) }
    }

    fun setSeekSensitivity(sensitivity: Float) {
        viewModelScope.launch { settingsRepository.updateSeekSensitivity(sensitivity) }
    }

    fun setSwipeBrightnessEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateSwipeBrightnessEnabled(enabled) }
    }

    fun setSwipeVolumeEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateSwipeVolumeEnabled(enabled) }
    }
}
