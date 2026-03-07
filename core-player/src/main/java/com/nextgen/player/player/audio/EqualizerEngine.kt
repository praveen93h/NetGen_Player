package com.nextgen.player.player.audio

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Singleton

data class EqBand(
    val index: Int,
    val centerFreq: Int,       // Hz
    val minLevel: Int,         // millibels
    val maxLevel: Int,         // millibels
    val currentLevel: Int      // millibels
) {
    val displayFreq: String
        get() = if (centerFreq >= 1000) "${centerFreq / 1000}kHz" else "${centerFreq}Hz"

    val normalizedLevel: Float
        get() = if (maxLevel != minLevel)
            (currentLevel - minLevel).toFloat() / (maxLevel - minLevel).toFloat()
        else 0.5f
}

data class EqPreset(
    val name: String,
    val bandLevels: List<Int>  // millibels per band
)

data class EqualizerState(
    val isEnabled: Boolean = false,
    val bands: List<EqBand> = emptyList(),
    val currentPresetName: String = "Flat",
    val bassBoostStrength: Int = 0,       // 0-1000
    val virtualizerStrength: Int = 0      // 0-1000
)

@Singleton
class EqualizerEngine {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    private val _state = MutableStateFlow(EqualizerState())
    val state: StateFlow<EqualizerState> = _state.asStateFlow()

    private var audioSessionId: Int = 0

    companion object {
        val PRESETS = listOf(
            EqPreset("Flat", listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)),
            EqPreset("Bass Boost", listOf(600, 500, 400, 200, 0, 0, 0, 0, 0, 0)),
            EqPreset("Vocal", listOf(-200, -100, 0, 200, 400, 400, 300, 100, 0, -200)),
            EqPreset("Cinema", listOf(400, 300, 200, 100, 0, 0, 100, 200, 300, 400)),
            EqPreset("Treble Boost", listOf(0, 0, 0, 0, 0, 200, 300, 400, 500, 600)),
            EqPreset("Rock", listOf(500, 300, 0, -100, -200, 0, 200, 400, 500, 500)),
            EqPreset("Pop", listOf(-100, 200, 400, 400, 200, -100, -200, -100, 200, 300)),
            EqPreset("Jazz", listOf(300, 200, 0, 200, -200, -200, 0, 200, 300, 400)),
            EqPreset("Classical", listOf(400, 300, 200, 100, -100, -100, 0, 200, 300, 400)),
            EqPreset("Custom", listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0))
        )
    }

    fun initialize(sessionId: Int) {
        if (sessionId == audioSessionId && equalizer != null) return
        release()
        audioSessionId = sessionId

        try {
            equalizer = Equalizer(0, sessionId).apply {
                enabled = _state.value.isEnabled
            }
            bassBoost = BassBoost(0, sessionId).apply {
                enabled = _state.value.isEnabled
                if (strengthSupported) {
                    setStrength(_state.value.bassBoostStrength.toShort())
                }
            }
            virtualizer = Virtualizer(0, sessionId).apply {
                enabled = _state.value.isEnabled
                if (strengthSupported) {
                    setStrength(_state.value.virtualizerStrength.toShort())
                }
            }
            readBandsFromEqualizer()
        } catch (_: Exception) {
            // Audio effects not supported on this device
        }
    }

    private fun readBandsFromEqualizer() {
        val eq = equalizer ?: return
        val numBands = eq.numberOfBands.toInt()
        val minLevel = eq.bandLevelRange[0].toInt()
        val maxLevel = eq.bandLevelRange[1].toInt()

        val bands = (0 until numBands).map { i ->
            EqBand(
                index = i,
                centerFreq = eq.getCenterFreq(i.toShort()) / 1000, // milliHz to Hz
                minLevel = minLevel,
                maxLevel = maxLevel,
                currentLevel = eq.getBandLevel(i.toShort()).toInt()
            )
        }
        _state.value = _state.value.copy(bands = bands)
    }

    fun setEnabled(enabled: Boolean) {
        equalizer?.enabled = enabled
        bassBoost?.enabled = enabled
        virtualizer?.enabled = enabled
        _state.value = _state.value.copy(isEnabled = enabled)
    }

    fun toggleEnabled() {
        setEnabled(!_state.value.isEnabled)
    }

    fun setBandLevel(bandIndex: Int, level: Int) {
        equalizer?.let { eq ->
            val clamped = level.coerceIn(
                eq.bandLevelRange[0].toInt(),
                eq.bandLevelRange[1].toInt()
            )
            eq.setBandLevel(bandIndex.toShort(), clamped.toShort())
            val bands = _state.value.bands.toMutableList()
            if (bandIndex in bands.indices) {
                bands[bandIndex] = bands[bandIndex].copy(currentLevel = clamped)
            }
            _state.value = _state.value.copy(bands = bands, currentPresetName = "Custom")
        }
    }

    fun setBandLevelNormalized(bandIndex: Int, normalized: Float) {
        val bands = _state.value.bands
        if (bandIndex !in bands.indices) return
        val band = bands[bandIndex]
        val level = (band.minLevel + (normalized * (band.maxLevel - band.minLevel))).toInt()
        setBandLevel(bandIndex, level)
    }

    fun applyPreset(preset: EqPreset) {
        val eq = equalizer ?: return
        val numBands = eq.numberOfBands.toInt()
        val minLevel = eq.bandLevelRange[0].toInt()
        val maxLevel = eq.bandLevelRange[1].toInt()

        val updatedBands = mutableListOf<EqBand>()
        for (i in 0 until numBands) {
            val presetLevel = if (i < preset.bandLevels.size) preset.bandLevels[i] else 0
            val clamped = presetLevel.coerceIn(minLevel, maxLevel)
            eq.setBandLevel(i.toShort(), clamped.toShort())
            updatedBands.add(
                _state.value.bands.getOrElse(i) {
                    EqBand(i, eq.getCenterFreq(i.toShort()) / 1000, minLevel, maxLevel, clamped)
                }.copy(currentLevel = clamped)
            )
        }
        _state.value = _state.value.copy(bands = updatedBands, currentPresetName = preset.name)
    }

    fun applyPresetByName(name: String) {
        PRESETS.find { it.name == name }?.let { applyPreset(it) }
    }

    fun setBassBoostStrength(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        try {
            bassBoost?.setStrength(clamped.toShort())
        } catch (_: Exception) { }
        _state.value = _state.value.copy(bassBoostStrength = clamped)
    }

    fun setVirtualizerStrength(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        try {
            virtualizer?.setStrength(clamped.toShort())
        } catch (_: Exception) { }
        _state.value = _state.value.copy(virtualizerStrength = clamped)
    }

    fun getCurrentBandLevels(): List<Int> {
        return _state.value.bands.map { it.currentLevel }
    }

    fun release() {
        try { equalizer?.release() } catch (_: Exception) { }
        try { bassBoost?.release() } catch (_: Exception) { }
        try { virtualizer?.release() } catch (_: Exception) { }
        equalizer = null
        bassBoost = null
        virtualizer = null
    }
}
