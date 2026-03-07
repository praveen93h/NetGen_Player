package com.nextgen.player.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextgen.player.data.SettingsRepository
import com.nextgen.player.data.local.entity.MediaEntity
import com.nextgen.player.data.local.repository.MediaRepository
import com.nextgen.player.player.AudioTrackInfo
import com.nextgen.player.player.PlayerEngine
import com.nextgen.player.player.PlayerState
import com.nextgen.player.player.VideoFilterState
import com.nextgen.player.player.audio.AudioEngine
import com.nextgen.player.player.audio.EqualizerEngine
import com.nextgen.player.player.audio.EqualizerState
import com.nextgen.player.player.gesture.GestureController
import com.nextgen.player.player.gesture.GestureState
import com.nextgen.player.player.gesture.GestureZoneType
import com.nextgen.player.player.gesture.VideoScaleType
import com.nextgen.player.subtitle.SubtitleCue
import com.nextgen.player.subtitle.SubtitleSyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

enum class DoubleTapSide { NONE, LEFT, RIGHT }
enum class RotationMode { AUTO, LANDSCAPE, PORTRAIT }
enum class RepeatMode { OFF, ALL, ONE }

data class PlayerUiState(
    val mediaInfo: MediaEntity? = null,
    val playerState: PlayerState = PlayerState(),
    val showControls: Boolean = true,
    val isLocked: Boolean = false,
    val subtitleCues: List<SubtitleCue> = emptyList(),
    val subtitleSyncOffsetMs: Long = 0L,
    val audioBoostLevel: Int = 100,
    val audioDelayMs: Long = 0L,
    val showSpeedSelector: Boolean = false,
    val showAudioTrackSelector: Boolean = false,
    val showSubtitleSelector: Boolean = false,
    val showSyncSheet: Boolean = false,
    val brightnessLevel: Float = 0.5f,
    val volumeLevel: Float = 0.5f,
    val gestureIndicator: String = "",
    val showGestureIndicator: Boolean = false,
    val gestureState: GestureState = GestureState(),
    val scaleType: VideoScaleType = VideoScaleType.FIT,
    val doubleTapSide: DoubleTapSide = DoubleTapSide.NONE,
    // v1.2 features
    val showResumePrompt: Boolean = false,
    val resumePosition: Long = 0L,
    val isNightFilterEnabled: Boolean = false,
    val nightFilterIntensity: Float = 0.3f,
    val rotationMode: RotationMode = RotationMode.AUTO,
    val isInPiPMode: Boolean = false,
    val queue: List<MediaEntity> = emptyList(),
    val currentQueueIndex: Int = -1,
    val audioTrackInfoList: List<AudioTrackInfo> = emptyList(),
    val autoPlayNext: Boolean = true,
    // v1.3 features
    val equalizerState: EqualizerState = EqualizerState(),
    val showEqualizerSheet: Boolean = false,
    val videoFilterState: VideoFilterState = VideoFilterState(),
    val showVideoFilterSheet: Boolean = false,
    val isShuffled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val skipSilenceEnabled: Boolean = false,
    val originalQueue: List<MediaEntity> = emptyList()
) {
    val formattedAudioDelay: String
        get() {
            val seconds = audioDelayMs / 1000.0
            return if (audioDelayMs >= 0) "+%.1fs".format(seconds) else "%.1fs".format(seconds)
        }

    val formattedSubtitleSync: String
        get() {
            val seconds = subtitleSyncOffsetMs / 1000.0
            return if (subtitleSyncOffsetMs >= 0) "+%.1fs".format(seconds) else "%.1fs".format(seconds)
        }
}

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerEngine: PlayerEngine,
    private val audioEngine: AudioEngine,
    private val equalizerEngine: EqualizerEngine,
    private val gestureController: GestureController,
    private val mediaRepository: MediaRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val subtitleSyncManager = SubtitleSyncManager()
    private var controlsHideJob: Job? = null
    private var positionUpdateJob: Job? = null

    val playerEngineInstance: PlayerEngine get() = playerEngine
    val playerFlow get() = playerEngine.playerFlow

    init {
        playerEngine.initialize()
        playerEngine.setOnPlaybackCompleted {
            viewModelScope.launch {
                val state = _uiState.value
                when (state.repeatMode) {
                    RepeatMode.ONE -> {
                        playerEngine.seekTo(0L)
                        playerEngine.play()
                    }
                    RepeatMode.ALL -> {
                        if (state.currentQueueIndex < state.queue.size - 1) {
                            playNext()
                        } else if (state.queue.isNotEmpty()) {
                            // Wrap around to first
                            val firstMedia = state.queue[0]
                            _uiState.update { it.copy(currentQueueIndex = 0, mediaInfo = firstMedia, showResumePrompt = false) }
                            playerEngine.prepare(Uri.parse(firstMedia.path), 0L)
                            playerEngine.play()
                        }
                    }
                    RepeatMode.OFF -> {
                        if (state.autoPlayNext && state.currentQueueIndex < state.queue.size - 1) {
                            playNext()
                        }
                    }
                }
            }
        }
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.update {
                    it.copy(
                        autoPlayNext = settings.autoPlayNext,
                        nightFilterIntensity = settings.nightFilterIntensity
                    )
                }
            }
        }
        viewModelScope.launch {
            equalizerEngine.state.collect { eqState ->
                _uiState.update { it.copy(equalizerState = eqState) }
            }
        }
    }

    fun initialize(mediaId: Long, mediaPath: String, folderPath: String? = null) {
        playerEngine.initialize()

        viewModelScope.launch {
            val mediaInfo = mediaRepository.getMediaById(mediaId)
            _uiState.update { it.copy(mediaInfo = mediaInfo) }

            // Load queue from folder if available
            if (!folderPath.isNullOrEmpty()) {
                mediaRepository.getMediaInFolder(folderPath).first().let { folderMedia ->
                    val idx = folderMedia.indexOfFirst { it.id == mediaId }.coerceAtLeast(0)
                    _uiState.update { it.copy(queue = folderMedia, currentQueueIndex = idx) }
                }
            } else {
                if (mediaInfo != null) {
                    _uiState.update { it.copy(queue = listOf(mediaInfo), currentQueueIndex = 0) }
                }
            }

            val startPosition = mediaInfo?.lastPosition ?: 0L

            // Show resume prompt if position is significant (>5 seconds)
            if (startPosition > 5000L) {
                _uiState.update { it.copy(showResumePrompt = true, resumePosition = startPosition) }
            }

            playerEngine.prepare(Uri.parse(mediaPath), startPosition)
            playerEngine.play()

            // Fetch audio track info after a short delay for tracks to load
            delay(500)
            _uiState.update { it.copy(audioTrackInfoList = playerEngine.getAudioTrackInfoList()) }

            playerEngine.player?.let { player ->
                audioEngine.initLoudnessEnhancer(player.audioSessionId)
                equalizerEngine.initialize(player.audioSessionId)
            }
        }

        viewModelScope.launch {
            playerEngine.state.collect { state ->
                _uiState.update { it.copy(playerState = state) }
            }
        }

        positionUpdateJob = viewModelScope.launch {
            var tickCount = 0
            while (isActive) {
                delay(200)
                tickCount++
                val pos = playerEngine.getCurrentPosition()
                val dur = playerEngine.getDuration()
                _uiState.update {
                    it.copy(
                        playerState = it.playerState.copy(
                            currentPosition = pos,
                            duration = dur
                        )
                    )
                }
                playerEngine.checkABRepeat()

                if (tickCount % 75 == 0) {
                    savePosition()
                }
            }
        }

        scheduleHideControls()
    }

    fun initializeFromUri(uri: Uri) {
        playerEngine.initialize()

        viewModelScope.launch {
            playerEngine.prepare(uri, 0L)
            playerEngine.play()

            playerEngine.player?.let { player ->
                audioEngine.initLoudnessEnhancer(player.audioSessionId)
                equalizerEngine.initialize(player.audioSessionId)
            }
        }

        viewModelScope.launch {
            playerEngine.state.collect { state ->
                _uiState.update { it.copy(playerState = state) }
            }
        }

        positionUpdateJob = viewModelScope.launch {
            var tickCount = 0
            while (isActive) {
                delay(200)
                tickCount++
                val pos = playerEngine.getCurrentPosition()
                val dur = playerEngine.getDuration()
                _uiState.update {
                    it.copy(
                        playerState = it.playerState.copy(
                            currentPosition = pos,
                            duration = dur
                        )
                    )
                }
                playerEngine.checkABRepeat()
                if (tickCount % 75 == 0) {
                    savePosition()
                }
            }
        }

        scheduleHideControls()
    }

    fun togglePlayPause() {
        playerEngine.togglePlayPause()
        scheduleHideControls()
    }

    fun seekTo(positionMs: Long) {
        playerEngine.seekTo(positionMs)
        scheduleHideControls()
    }

    fun seekForward() {
        playerEngine.seekForward()
        scheduleHideControls()
    }

    fun seekBackward() {
        playerEngine.seekBackward()
        scheduleHideControls()
    }

    fun doubleTapSeekForward() {
        playerEngine.seekForward()
        _uiState.update { it.copy(doubleTapSide = DoubleTapSide.RIGHT) }
        viewModelScope.launch {
            delay(600)
            _uiState.update { it.copy(doubleTapSide = DoubleTapSide.NONE) }
        }
    }

    fun doubleTapSeekBackward() {
        playerEngine.seekBackward()
        _uiState.update { it.copy(doubleTapSide = DoubleTapSide.LEFT) }
        viewModelScope.launch {
            delay(600)
            _uiState.update { it.copy(doubleTapSide = DoubleTapSide.NONE) }
        }
    }

    fun setSpeed(speed: Float) {
        playerEngine.setSpeed(speed)
    }

    fun cycleSpeed(): Float {
        return playerEngine.cycleSpeed()
    }

    fun toggleLoop() {
        playerEngine.toggleLoop()
    }

    fun setABRepeatStart() {
        playerEngine.setABRepeatStart()
    }

    fun setABRepeatEnd() {
        playerEngine.setABRepeatEnd()
    }

    fun clearABRepeat() {
        playerEngine.clearABRepeat()
    }

    fun selectAudioTrack(index: Int) {
        playerEngine.selectAudioTrack(index)
    }

    fun selectSubtitleTrack(index: Int) {
        playerEngine.selectSubtitleTrack(index)
    }

    fun setAudioBoost(level: Int) {
        audioEngine.setAudioBoost(level)
        _uiState.update { it.copy(audioBoostLevel = level) }
    }

    fun setAudioDelay(delayMs: Long) {
        audioEngine.setAudioDelay(delayMs)
        _uiState.update { it.copy(audioDelayMs = delayMs) }
    }

    fun adjustSubtitleSync(deltaMs: Long) {
        subtitleSyncManager.adjustOffset(deltaMs)
        _uiState.update { it.copy(subtitleSyncOffsetMs = subtitleSyncManager.currentOffsetMs) }
    }

    fun onGestureStart(zone: GestureZoneType) {
        gestureController.onGestureStart(zone)
        syncGestureState()
    }

    fun onGestureEnd() {
        gestureController.onGestureEnd()
        syncGestureState()
    }

    fun adjustBrightness(delta: Float) {
        gestureController.adjustBrightness(delta)
        syncGestureState()
        _uiState.update {
            it.copy(
                gestureIndicator = gestureController.state.indicatorText,
                showGestureIndicator = true,
                brightnessLevel = gestureController.state.brightnessLevel
            )
        }
    }

    fun adjustVolume(delta: Float) {
        gestureController.adjustVolume(delta)
        syncGestureState()
        _uiState.update {
            it.copy(
                gestureIndicator = gestureController.state.indicatorText,
                showGestureIndicator = true,
                volumeLevel = gestureController.state.volumeLevel
            )
        }
    }

    fun startSeek() {
        gestureController.startSeek(playerEngine.getCurrentPosition())
        syncGestureState()
    }

    fun updateSeek(deltaPx: Float, screenWidth: Float) {
        val currentPos = playerEngine.getCurrentPosition()
        val duration = playerEngine.getDuration()
        gestureController.updateSeek(deltaPx, currentPos, duration, screenWidth)
        syncGestureState()
        _uiState.update {
            it.copy(
                gestureIndicator = gestureController.state.indicatorText,
                showGestureIndicator = true
            )
        }
    }

    fun endSeek() {
        val seekPosition = gestureController.endSeek()
        playerEngine.seekTo(seekPosition)
        syncGestureState()
        _uiState.update { it.copy(showGestureIndicator = false) }
    }

    fun cycleAspectRatio(): VideoScaleType {
        val newScale = gestureController.cycleScale()
        _uiState.update {
            it.copy(
                scaleType = newScale,
                gestureIndicator = newScale.displayName,
                showGestureIndicator = true
            )
        }
        viewModelScope.launch {
            delay(1500)
            _uiState.update { it.copy(showGestureIndicator = false) }
        }
        return newScale
    }

    private fun syncGestureState() {
        _uiState.update { it.copy(gestureState = gestureController.state) }
    }

    fun toggleControls() {
        val show = !_uiState.value.showControls
        _uiState.update { it.copy(showControls = show) }
        if (show) scheduleHideControls()
    }

    fun showControls() {
        _uiState.update { it.copy(showControls = true) }
        scheduleHideControls()
    }

    fun toggleLock() {
        _uiState.update { it.copy(isLocked = !it.isLocked) }
    }

    fun showGestureIndicator(text: String) {
        _uiState.update { it.copy(gestureIndicator = text, showGestureIndicator = true) }
    }

    fun hideGestureIndicator() {
        _uiState.update { it.copy(showGestureIndicator = false) }
    }

    fun toggleSpeedSelector() {
        _uiState.update { it.copy(showSpeedSelector = !it.showSpeedSelector) }
    }

    fun toggleAudioTrackSelector() {
        _uiState.update { it.copy(showAudioTrackSelector = !it.showAudioTrackSelector) }
    }

    fun toggleSubtitleSelector() {
        _uiState.update { it.copy(showSubtitleSelector = !it.showSubtitleSelector) }
    }

    fun toggleSyncSheet() {
        _uiState.update { it.copy(showSyncSheet = !it.showSyncSheet) }
    }

    private fun scheduleHideControls() {
        controlsHideJob?.cancel()
        controlsHideJob = viewModelScope.launch {
            delay(4000)
            _uiState.update { it.copy(showControls = false) }
        }
    }

    fun savePosition() {
        viewModelScope.launch {
            val mediaInfo = _uiState.value.mediaInfo ?: return@launch
            val position = playerEngine.getCurrentPosition()
            mediaRepository.updatePlaybackPosition(mediaInfo.id, position)
        }
    }

    // === Resume Prompt ===
    fun dismissResumePrompt() {
        _uiState.update { it.copy(showResumePrompt = false) }
    }

    fun startFromBeginning() {
        playerEngine.seekTo(0L)
        _uiState.update { it.copy(showResumePrompt = false) }
    }

    // === Queue / Auto-Next ===
    fun playNext() {
        val state = _uiState.value
        val nextIndex = state.currentQueueIndex + 1
        if (nextIndex < state.queue.size) {
            savePosition()
            val nextMedia = state.queue[nextIndex]
            _uiState.update { it.copy(currentQueueIndex = nextIndex, mediaInfo = nextMedia, showResumePrompt = false) }
            playerEngine.prepare(Uri.parse(nextMedia.path), 0L)
            playerEngine.play()
            viewModelScope.launch {
                delay(500)
                _uiState.update { it.copy(audioTrackInfoList = playerEngine.getAudioTrackInfoList()) }
                playerEngine.player?.let { player ->
                    audioEngine.initLoudnessEnhancer(player.audioSessionId)
                    equalizerEngine.initialize(player.audioSessionId)
                }
            }
            scheduleHideControls()
        }
    }

    fun playPrevious() {
        val state = _uiState.value
        val prevIndex = state.currentQueueIndex - 1
        if (prevIndex >= 0) {
            savePosition()
            val prevMedia = state.queue[prevIndex]
            _uiState.update { it.copy(currentQueueIndex = prevIndex, mediaInfo = prevMedia, showResumePrompt = false) }
            playerEngine.prepare(Uri.parse(prevMedia.path), 0L)
            playerEngine.play()
            viewModelScope.launch {
                delay(500)
                _uiState.update { it.copy(audioTrackInfoList = playerEngine.getAudioTrackInfoList()) }
                playerEngine.player?.let { player ->
                    audioEngine.initLoudnessEnhancer(player.audioSessionId)
                    equalizerEngine.initialize(player.audioSessionId)
                }
            }
            scheduleHideControls()
        }
    }

    val hasNext: Boolean get() {
        val s = _uiState.value
        return s.currentQueueIndex < s.queue.size - 1
    }

    val hasPrevious: Boolean get() {
        return _uiState.value.currentQueueIndex > 0
    }

    // === Night Filter ===
    fun toggleNightFilter() {
        _uiState.update { it.copy(isNightFilterEnabled = !it.isNightFilterEnabled) }
    }

    fun setNightFilterIntensity(intensity: Float) {
        _uiState.update { it.copy(nightFilterIntensity = intensity.coerceIn(0.1f, 0.7f)) }
        viewModelScope.launch {
            settingsRepository.updateNightFilterIntensity(intensity.coerceIn(0.1f, 0.7f))
        }
    }

    // === Rotation ===
    fun cycleRotation(): RotationMode {
        val next = when (_uiState.value.rotationMode) {
            RotationMode.AUTO -> RotationMode.LANDSCAPE
            RotationMode.LANDSCAPE -> RotationMode.PORTRAIT
            RotationMode.PORTRAIT -> RotationMode.AUTO
        }
        _uiState.update {
            it.copy(
                rotationMode = next,
                gestureIndicator = when (next) {
                    RotationMode.AUTO -> "Auto Rotate"
                    RotationMode.LANDSCAPE -> "Landscape"
                    RotationMode.PORTRAIT -> "Portrait"
                },
                showGestureIndicator = true
            )
        }
        viewModelScope.launch {
            delay(1500)
            _uiState.update { it.copy(showGestureIndicator = false) }
        }
        return next
    }

    // === PiP ===
    fun setInPiPMode(inPiP: Boolean) {
        _uiState.update { it.copy(isInPiPMode = inPiP, showControls = !inPiP) }
    }

    // === Equalizer ===
    fun toggleEqualizerSheet() {
        _uiState.update { it.copy(showEqualizerSheet = !it.showEqualizerSheet) }
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        equalizerEngine.setEnabled(enabled)
    }

    fun setEqBandLevel(bandIndex: Int, normalized: Float) {
        equalizerEngine.setBandLevelNormalized(bandIndex, normalized)
    }

    fun applyEqPreset(presetName: String) {
        equalizerEngine.applyPresetByName(presetName)
    }

    fun setBassBoost(strength: Int) {
        equalizerEngine.setBassBoostStrength(strength)
    }

    fun setVirtualizer(strength: Int) {
        equalizerEngine.setVirtualizerStrength(strength)
    }

    // === Video Filters ===
    fun toggleVideoFilterSheet() {
        _uiState.update { it.copy(showVideoFilterSheet = !it.showVideoFilterSheet) }
    }

    fun setVideoBrightness(value: Float) {
        _uiState.update { it.copy(videoFilterState = it.videoFilterState.copy(brightness = value.coerceIn(-1f, 1f))) }
    }

    fun setVideoContrast(value: Float) {
        _uiState.update { it.copy(videoFilterState = it.videoFilterState.copy(contrast = value.coerceIn(0f, 2f))) }
    }

    fun setVideoSaturation(value: Float) {
        _uiState.update { it.copy(videoFilterState = it.videoFilterState.copy(saturation = value.coerceIn(0f, 2f))) }
    }

    fun setVideoHue(value: Float) {
        _uiState.update { it.copy(videoFilterState = it.videoFilterState.copy(hue = value.coerceIn(-180f, 180f))) }
    }

    fun setVideoGamma(value: Float) {
        _uiState.update { it.copy(videoFilterState = it.videoFilterState.copy(gamma = value.coerceIn(0.5f, 2f))) }
    }

    fun resetVideoFilters() {
        _uiState.update { it.copy(videoFilterState = VideoFilterState()) }
    }

    // === Skip Silence ===
    fun toggleSkipSilence() {
        val newValue = playerEngine.toggleSkipSilence()
        _uiState.update { it.copy(skipSilenceEnabled = newValue) }
    }

    // === Shuffle & Repeat ===
    fun toggleShuffle() {
        val state = _uiState.value
        if (state.isShuffled) {
            // Unshuffle: restore original order, find current media
            val currentMedia = state.queue.getOrNull(state.currentQueueIndex)
            val originalIdx = state.originalQueue.indexOfFirst { it.id == currentMedia?.id }.coerceAtLeast(0)
            _uiState.update { it.copy(queue = state.originalQueue, currentQueueIndex = originalIdx, isShuffled = false) }
        } else {
            // Shuffle: save original, shuffle keeping current at front
            val currentMedia = state.queue.getOrNull(state.currentQueueIndex)
            val shuffled = state.queue.toMutableList()
            if (currentMedia != null) {
                shuffled.remove(currentMedia)
                shuffled.shuffle()
                shuffled.add(0, currentMedia)
            } else {
                shuffled.shuffle()
            }
            _uiState.update { it.copy(originalQueue = state.queue, queue = shuffled, currentQueueIndex = 0, isShuffled = true) }
        }
    }

    fun cycleRepeatMode(): RepeatMode {
        val next = when (_uiState.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _uiState.update { it.copy(repeatMode = next) }
        return next
    }

    override fun onCleared() {
        super.onCleared()
        savePosition()
        equalizerEngine.release()
        audioEngine.release()
        playerEngine.release()
        positionUpdateJob?.cancel()
        controlsHideJob?.cancel()
    }
}
