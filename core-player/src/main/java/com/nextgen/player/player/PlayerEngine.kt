package com.nextgen.player.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.MediaSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Singleton

data class PlayerState(
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val bufferedPosition: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val isLooping: Boolean = false,
    val abRepeatStart: Long = -1L,
    val abRepeatEnd: Long = -1L,
    val isAbRepeatEnabled: Boolean = false,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val audioTrackCount: Int = 0,
    val subtitleTrackCount: Int = 0,
    val currentAudioTrack: Int = 0,
    val currentSubtitleTrack: Int = -1,
    val isCompleted: Boolean = false
)

data class AudioTrackInfo(
    val index: Int,
    val language: String?,
    val label: String?,
    val codec: String?,
    val channelCount: Int,
    val bitrate: Int,
    val sampleRate: Int
) {
    val displayName: String
        get() {
            val parts = mutableListOf<String>()
            if (!label.isNullOrBlank()) parts.add(label)
            else if (!language.isNullOrBlank()) parts.add(java.util.Locale(language).displayLanguage)
            else parts.add("Track ${index + 1}")
            
            val details = mutableListOf<String>()
            if (!codec.isNullOrBlank()) {
                val short = codec.substringAfterLast("/").substringBefore("-").uppercase()
                details.add(short)
            }
            when (channelCount) {
                1 -> details.add("Mono")
                2 -> details.add("Stereo")
                6 -> details.add("5.1")
                8 -> details.add("7.1")
                in 3..5 -> details.add("${channelCount}ch")
            }
            if (details.isNotEmpty()) parts.add(details.joinToString(" · "))
            return parts.joinToString(" — ")
        }
}

@OptIn(UnstableApi::class)
@Singleton
class PlayerEngine(
    private val context: Context
) {
    private var _player: ExoPlayer? = null
    val player: ExoPlayer? get() = _player

    private val _playerFlow = MutableStateFlow<ExoPlayer?>(null)
    val playerFlow: StateFlow<ExoPlayer?> = _playerFlow.asStateFlow()

    private var _mediaSession: MediaSession? = null
    val mediaSession: MediaSession? get() = _mediaSession

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private var trackSelector: DefaultTrackSelector? = null
    private var resumePosition: Long = 0L
    private var onPlaybackCompleted: (() -> Unit)? = null

    private val _skipSilence = MutableStateFlow(false)
    val skipSilence: StateFlow<Boolean> = _skipSilence.asStateFlow()

    companion object {
        val PLAYBACK_SPEEDS = floatArrayOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f)
    }

    fun initialize() {
        if (_player != null) return

        trackSelector = DefaultTrackSelector(context)

        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)

        _player = ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            .setTrackSelector(trackSelector!!)
            .setHandleAudioBecomingNoisy(true)
            .build()
            .apply {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build()
                setAudioAttributes(audioAttributes, true)

                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        updateState()
                        if (playbackState == Player.STATE_ENDED) {
                            _state.value = _state.value.copy(isCompleted = true)
                            onPlaybackCompleted?.invoke()
                        }
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        updateState()
                    }

                    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
                        _state.value = _state.value.copy(playbackSpeed = playbackParameters.speed)
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        _state.value = _state.value.copy(error = error.message)
                    }

                    override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                        _state.value = _state.value.copy(
                            videoWidth = videoSize.width,
                            videoHeight = videoSize.height
                        )
                    }

                    override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                        var audioCount = 0
                        var subtitleCount = 0
                        for (group in tracks.groups) {
                            when (group.type) {
                                C.TRACK_TYPE_AUDIO -> audioCount += group.length
                                C.TRACK_TYPE_TEXT -> subtitleCount += group.length
                            }
                        }
                        _state.value = _state.value.copy(
                            audioTrackCount = audioCount,
                            subtitleTrackCount = subtitleCount
                        )
                    }
                })
            }

        _playerFlow.value = _player
        _mediaSession = MediaSession.Builder(context, _player!!).build()
    }

    fun prepare(uri: Uri, startPosition: Long = 0L) {
        resumePosition = startPosition
        _state.value = _state.value.copy(isCompleted = false)
        val mediaItem = MediaItem.fromUri(uri)
        _player?.apply {
            setMediaItem(mediaItem)
            prepare()
            if (startPosition > 0) {
                seekTo(startPosition)
            }
        }
    }

    fun prepare(path: String, startPosition: Long = 0L) {
        prepare(Uri.parse(path), startPosition)
    }

    fun play() {
        _player?.play()
    }

    fun pause() {
        _player?.pause()
    }

    fun togglePlayPause() {
        _player?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    fun seekTo(positionMs: Long) {
        _player?.seekTo(positionMs.coerceAtLeast(0L))
    }

    fun seekForward(deltaMs: Long = 10000L) {
        _player?.let { seekTo(it.currentPosition + deltaMs) }
    }

    fun seekBackward(deltaMs: Long = 10000L) {
        _player?.let { seekTo(it.currentPosition - deltaMs) }
    }

    fun setSpeed(speed: Float) {
        _player?.setPlaybackSpeed(speed.coerceIn(0.25f, 3.0f))
    }

    fun cycleSpeed(): Float {
        val currentSpeed = _state.value.playbackSpeed
        val currentIdx = PLAYBACK_SPEEDS.indexOfFirst { it >= currentSpeed }
        val nextIdx = if (currentIdx < 0 || currentIdx >= PLAYBACK_SPEEDS.size - 1) {
            PLAYBACK_SPEEDS.indexOfFirst { it == 1.0f }
        } else {
            currentIdx + 1
        }
        val newSpeed = PLAYBACK_SPEEDS[nextIdx]
        setSpeed(newSpeed)
        return newSpeed
    }

    fun toggleLoop() {
        _player?.let {
            val newLooping = !_state.value.isLooping
            it.repeatMode = if (newLooping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            _state.value = _state.value.copy(isLooping = newLooping)
        }
    }

    fun setABRepeatStart() {
        _player?.let {
            _state.value = _state.value.copy(abRepeatStart = it.currentPosition)
        }
    }

    fun setABRepeatEnd() {
        _player?.let {
            _state.value = _state.value.copy(
                abRepeatEnd = it.currentPosition,
                isAbRepeatEnabled = true
            )
        }
    }

    fun clearABRepeat() {
        _state.value = _state.value.copy(
            abRepeatStart = -1L,
            abRepeatEnd = -1L,
            isAbRepeatEnabled = false
        )
    }

    fun checkABRepeat() {
        val state = _state.value
        if (state.isAbRepeatEnabled && state.abRepeatEnd > state.abRepeatStart) {
            _player?.let {
                if (it.currentPosition >= state.abRepeatEnd - 250) {
                    it.seekTo(state.abRepeatStart)
                }
            }
        }
    }

    fun selectAudioTrack(trackIndex: Int) {
        _player?.let { player ->
            val tracks = player.currentTracks
            var audioGroupIdx = 0
            for (group in tracks.groups) {
                if (group.type == C.TRACK_TYPE_AUDIO) {
                    if (audioGroupIdx == trackIndex && group.length > 0) {
                        trackSelector?.setParameters(
                            trackSelector!!.buildUponParameters()
                                .setOverrideForType(
                                    TrackSelectionOverride(group.mediaTrackGroup, 0)
                                )
                        )
                        _state.value = _state.value.copy(currentAudioTrack = trackIndex)
                        break
                    }
                    audioGroupIdx++
                }
            }
        }
    }

    fun selectSubtitleTrack(trackIndex: Int) {
        _player?.let { player ->
            if (trackIndex < 0) {
            trackSelector?.setParameters(
                    trackSelector!!.buildUponParameters()
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                )
                _state.value = _state.value.copy(currentSubtitleTrack = -1)
                return
            }

            trackSelector?.setParameters(
                trackSelector!!.buildUponParameters()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            )

            val tracks = player.currentTracks
            var subtitleGroupIdx = 0
            for (group in tracks.groups) {
                if (group.type == C.TRACK_TYPE_TEXT) {
                    if (subtitleGroupIdx == trackIndex && group.length > 0) {
                        trackSelector?.setParameters(
                            trackSelector!!.buildUponParameters()
                                .setOverrideForType(
                                    TrackSelectionOverride(group.mediaTrackGroup, 0)
                                )
                        )
                        _state.value = _state.value.copy(currentSubtitleTrack = trackIndex)
                        break
                    }
                    subtitleGroupIdx++
                }
            }
        }
    }

    fun getCurrentPosition(): Long = _player?.currentPosition ?: 0L

    fun getDuration(): Long = _player?.duration ?: 0L

    fun setOnPlaybackCompleted(callback: (() -> Unit)?) {
        onPlaybackCompleted = callback
    }

    fun getAudioTrackInfoList(): List<AudioTrackInfo> {
        val result = mutableListOf<AudioTrackInfo>()
        _player?.let { player ->
            var audioIndex = 0
            for (group in player.currentTracks.groups) {
                if (group.type == C.TRACK_TYPE_AUDIO) {
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        result.add(
                            AudioTrackInfo(
                                index = audioIndex,
                                language = format.language,
                                label = format.label,
                                codec = format.sampleMimeType,
                                channelCount = format.channelCount,
                                bitrate = format.bitrate,
                                sampleRate = format.sampleRate
                            )
                        )
                        audioIndex++
                    }
                }
            }
        }
        return result
    }

    fun getAudioSessionId(): Int = _player?.audioSessionId ?: 0

    fun setSkipSilence(enabled: Boolean) {
        _player?.skipSilenceEnabled = enabled
        _skipSilence.value = enabled
    }

    fun toggleSkipSilence(): Boolean {
        val newValue = !_skipSilence.value
        setSkipSilence(newValue)
        return newValue
    }

    private fun updateState() {
        _player?.let { p ->
            _state.value = _state.value.copy(
                isPlaying = p.isPlaying,
                currentPosition = p.currentPosition,
                duration = p.duration.coerceAtLeast(0L),
                bufferedPosition = p.bufferedPosition,
                isLoading = p.playbackState == Player.STATE_BUFFERING
            )
        }
    }

    fun release() {
        _mediaSession?.release()
        _mediaSession = null
        _player?.release()
        _player = null
        _playerFlow.value = null
        trackSelector = null
        _state.value = PlayerState()
    }
}
