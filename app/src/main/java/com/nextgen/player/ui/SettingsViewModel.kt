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
}
