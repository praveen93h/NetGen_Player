package com.nextgen.player.ui

import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import android.app.Activity
import android.content.pm.ActivityInfo
import com.nextgen.player.player.AudioTrackInfo
import com.nextgen.player.player.PlayerEngine
import com.nextgen.player.player.PlayerState
import com.nextgen.player.player.SubtitleTrackInfo
import com.nextgen.player.player.VideoFilterState
import com.nextgen.player.player.gesture.GestureZoneType
import com.nextgen.player.player.audio.EqPreset
import com.nextgen.player.player.audio.EqualizerEngine
import com.nextgen.player.subtitle.OnlineSubtitle
import com.nextgen.player.subtitle.SubtitleDisplayConfig
import com.nextgen.player.subtitle.SubtitleOverlay
import com.nextgen.player.subtitle.SubtitleVerticalPosition
import com.nextgen.player.ui.components.GuidedDemoDialog
import com.nextgen.player.ui.components.GuidedDemoStep
import com.nextgen.player.ui.components.GuidedDemoSurface
import com.nextgen.player.ui.theme.Orange500
import com.nextgen.player.R
import kotlin.math.abs

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    mediaId: Long,
    mediaPath: String,
    onBackPressed: () -> Unit,
    onEnterPiP: (() -> Unit)? = null,
    onFloatingVideo: ((String, Long) -> Unit)? = null,
    isInPiPMode: Boolean = false,
    folderPath: String? = null,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playerState = uiState.playerState
    val player by viewModel.playerFlow.collectAsStateWithLifecycle()
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val screenWidthPx = with(LocalDensity.current) { screenWidthDp.dp.toPx() }
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val context = LocalContext.current
    val activity = context as? Activity
    var showPlayerDemo by remember { mutableStateOf(false) }

    LaunchedEffect(mediaId) {
        viewModel.initialize(mediaId, mediaPath, folderPath)
    }

    LaunchedEffect(isInPiPMode) {
        viewModel.setInPiPMode(isInPiPMode)
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.savePosition() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                }
            },
            update = { view ->
                if (view.player !== player) view.player = player
                val filterState = uiState.videoFilterState
                if (!filterState.isDefault) {
                    val cm = android.graphics.ColorMatrix(filterState.toColorMatrixArray())
                    val paint = android.graphics.Paint().apply {
                        colorFilter = android.graphics.ColorMatrixColorFilter(cm)
                    }
                    view.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, paint)
                } else {
                    view.setLayerType(android.view.View.LAYER_TYPE_NONE, null)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (uiState.subtitleCues.isNotEmpty()) {
            SubtitleOverlay(
                currentPositionMs = playerState.currentPosition,
                cues = uiState.subtitleCues,
                syncOffsetMs = uiState.subtitleSyncOffsetMs,
                config = uiState.subtitleDisplayConfig,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (uiState.secondarySubtitleCues.isNotEmpty()) {
            SubtitleOverlay(
                currentPositionMs = playerState.currentPosition,
                cues = uiState.secondarySubtitleCues,
                syncOffsetMs = uiState.subtitleSyncOffsetMs,
                config = uiState.subtitleDisplayConfig.copy(
                    fontSize = (uiState.subtitleDisplayConfig.fontSize - 2f).coerceAtLeast(12f),
                    showBackground = false,
                    bottomPadding = 104f
                ),
                position = SubtitleVerticalPosition.BOTTOM,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Night / Blue Light Filter overlay
        if (uiState.isNightFilterEnabled) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFFF8B00).copy(alpha = uiState.nightFilterIntensity))
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(uiState.isLocked) {
                    if (!uiState.isLocked) {
                        detectTapGestures(
                            onTap = { viewModel.toggleControls() },
                            onDoubleTap = { offset ->
                                val halfWidth = size.width / 2
                                if (offset.x < halfWidth) {
                                    viewModel.doubleTapSeekBackward()
                                } else {
                                    viewModel.doubleTapSeekForward()
                                }
                            }
                        )
                    } else {
                        detectTapGestures(
                            onTap = { viewModel.toggleControls() }
                        )
                    }
                }
                .pointerInput(uiState.isLocked) {
                    if (!uiState.isLocked) {
                        var dragType = GestureZoneType.NONE
                        var startX = 0f
                        var startY = 0f
                        var accumulatedX = 0f
                        var accumulatedY = 0f
                        var gestureDecided = false

                        detectDragGestures(
                            onDragStart = { offset ->
                                startX = offset.x
                                startY = offset.y
                                accumulatedX = 0f
                                accumulatedY = 0f
                                gestureDecided = false
                                dragType = GestureZoneType.NONE
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                accumulatedX += dragAmount.x
                                accumulatedY += dragAmount.y

                                if (!gestureDecided) {
                                    if (abs(accumulatedX) > 15f || abs(accumulatedY) > 15f) {
                                        gestureDecided = true
                                        dragType = if (abs(accumulatedX) > abs(accumulatedY)) {
                                            GestureZoneType.SEEK
                                        } else {
                                            val halfWidth = size.width / 2
                                            if (startX < halfWidth && uiState.swipeBrightnessEnabled)
                                                GestureZoneType.BRIGHTNESS
                                            else if (startX >= halfWidth && uiState.swipeVolumeEnabled)
                                                GestureZoneType.VOLUME
                                            else GestureZoneType.NONE
                                        }

                                        viewModel.onGestureStart(dragType)
                                        if (dragType == GestureZoneType.SEEK) {
                                            viewModel.startSeek()
                                        }
                                    }
                                }

                                if (gestureDecided) {
                                    when (dragType) {
                                        GestureZoneType.BRIGHTNESS -> {
                                            val delta = -dragAmount.y / size.height * 1.2f
                                            viewModel.adjustBrightness(delta)
                                        }
                                        GestureZoneType.VOLUME -> {
                                            val delta = -dragAmount.y / size.height * 1.2f
                                            viewModel.adjustVolume(delta)
                                        }
                                        GestureZoneType.SEEK -> {
                                            viewModel.updateSeek(dragAmount.x, size.width.toFloat())
                                        }
                                        else -> {}
                                    }
                                }
                            },
                            onDragEnd = {
                                if (gestureDecided) {
                                    when (dragType) {
                                        GestureZoneType.SEEK -> viewModel.endSeek()
                                        else -> {
                                            viewModel.onGestureEnd()
                                            viewModel.hideGestureIndicator()
                                        }
                                    }
                                }
                                dragType = GestureZoneType.NONE
                                gestureDecided = false
                            },
                            onDragCancel = {
                                if (gestureDecided) {
                                    viewModel.onGestureEnd()
                                    viewModel.hideGestureIndicator()
                                }
                                dragType = GestureZoneType.NONE
                                gestureDecided = false
                            }
                        )
                    }
                }
        )

        AnimatedVisibility(
            visible = uiState.showGestureIndicator
                    && uiState.gestureState.gestureType != GestureZoneType.BRIGHTNESS
                    && uiState.gestureState.gestureType != GestureZoneType.VOLUME,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = uiState.gestureIndicator,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        AnimatedVisibility(
            visible = uiState.showGestureIndicator && uiState.gestureState.gestureType == GestureZoneType.BRIGHTNESS,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 10.dp, vertical = 14.dp)
            ) {
                Icon(
                    if (uiState.brightnessLevel > 0.5f) Icons.Rounded.LightMode else Icons.Rounded.BrightnessLow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(140.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(fraction = uiState.brightnessLevel.coerceIn(0.01f, 1f))
                            .clip(RoundedCornerShape(2.dp))
                            .background(Orange500)
                            .align(Alignment.BottomCenter)
                    )
                }
                Text(
                    "${(uiState.brightnessLevel * 100).toInt()}%",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        AnimatedVisibility(
            visible = uiState.showGestureIndicator && uiState.gestureState.gestureType == GestureZoneType.VOLUME,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 10.dp, vertical = 14.dp)
            ) {
                Icon(
                    when {
                        uiState.volumeLevel > 0.5f -> Icons.AutoMirrored.Rounded.VolumeUp
                        uiState.volumeLevel > 0f -> Icons.AutoMirrored.Rounded.VolumeDown
                        else -> Icons.AutoMirrored.Rounded.VolumeOff
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(140.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(fraction = uiState.volumeLevel.coerceIn(0.01f, 1f))
                            .clip(RoundedCornerShape(2.dp))
                            .background(Orange500)
                            .align(Alignment.BottomCenter)
                    )
                }
                Text(
                    "${(uiState.volumeLevel * 100).toInt()}%",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        AnimatedVisibility(
            visible = uiState.doubleTapSide == DoubleTapSide.LEFT,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .fillMaxWidth(0.35f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.12f), Color.Transparent),
                            radius = 300f
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.FastRewind, null, tint = Color.White, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(4.dp))
                    Text("-${uiState.doubleTapSeekDuration}s", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }

        AnimatedVisibility(
            visible = uiState.doubleTapSide == DoubleTapSide.RIGHT,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .fillMaxWidth(0.35f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.12f), Color.Transparent),
                            radius = 300f
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.FastForward, null, tint = Color.White, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(4.dp))
                    Text("+${uiState.doubleTapSeekDuration}s", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }

        if (playerState.isLoading && !uiState.showControls) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.Center),
                color = Orange500,
                strokeWidth = 3.dp
            )
        }

        AnimatedVisibility(
            visible = uiState.showControls,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                PlayerTopBar(
                    title = uiState.mediaInfo?.title ?: stringResource(R.string.player_playing),
                    subtitle = uiState.mediaInfo?.resolution.orEmpty(),
                    subtitlesEnabled = playerState.currentSubtitleTrack >= 0 || uiState.primaryLocalSubtitleIndex >= 0,
                    isLocked = uiState.isLocked,
                    onBack = {
                        viewModel.savePosition()
                        onBackPressed()
                    },
                    onSubtitles = { viewModel.toggleSubtitleSelector() },
                    onAudio = { viewModel.toggleAudioTrackSelector() },
                    onSync = { viewModel.toggleSyncSheet() },
                    onGuide = { showPlayerDemo = true },
                    onLock = { viewModel.toggleLock() },
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                if (!uiState.isLocked) {
                    TransportControls(
                        isPlaying = playerState.isPlaying,
                        hasPrevious = viewModel.hasPrevious,
                        hasNext = viewModel.hasNext,
                        onPrevious = { viewModel.playPrevious() },
                        onRewind = { viewModel.seekBackward() },
                        onPlayPause = { viewModel.togglePlayPause() },
                        onForward = { viewModel.seekForward() },
                        onNext = { viewModel.playNext() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                if (!uiState.isLocked) {
                    PlayerBottomPanel(
                        state = uiState,
                        playerState = playerState,
                        hasQueue = uiState.queue.size > 1,
                        mediaPath = mediaPath,
                        onSeek = { viewModel.seekTo(it) },
                        onSpeed = { viewModel.toggleSpeedSelector() },
                        onLoop = { viewModel.toggleLoop() },
                        onAspect = { viewModel.cycleAspectRatio() },
                        onRotation = {
                            val mode = viewModel.cycleRotation()
                            activity?.requestedOrientation = when (mode) {
                                RotationMode.AUTO -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
                                RotationMode.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                RotationMode.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            }
                        },
                        onNight = { viewModel.toggleNightFilter() },
                        onPip = { onEnterPiP?.invoke() },
                        onFloating = { path, pos -> onFloatingVideo?.invoke(path, pos) },
                        onShuffle = { viewModel.toggleShuffle() },
                        onRepeat = { viewModel.cycleRepeatMode() },
                        onSkipSilence = { viewModel.toggleSkipSilence() },
                        onEqualizer = { viewModel.toggleEqualizerSheet() },
                        onFilters = { viewModel.toggleVideoFilterSheet() },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }

                if (uiState.isLocked) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(16.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.toggleLock() },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                Icons.Rounded.Lock,
                                stringResource(R.string.player_unlock),
                                tint = Orange500,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }

        if (uiState.showSpeedSelector) {
            SpeedSelectorDialog(
                currentSpeed = playerState.playbackSpeed,
                onSpeedSelected = {
                    viewModel.setSpeed(it)
                    viewModel.toggleSpeedSelector()
                },
                onDismiss = { viewModel.toggleSpeedSelector() }
            )
        }

        if (uiState.showAudioTrackSelector) {
            EnhancedAudioTrackDialog(
                trackInfoList = uiState.audioTrackInfoList,
                currentTrack = playerState.currentAudioTrack,
                onTrackSelected = {
                    viewModel.selectAudioTrack(it)
                    viewModel.toggleAudioTrackSelector()
                },
                onDismiss = { viewModel.toggleAudioTrackSelector() }
            )
        }

        if (uiState.showSubtitleSelector) {
            SubtitleSelectorDialog(
                embeddedTracks = uiState.subtitleTrackInfoList,
                localTracks = uiState.localSubtitleTracks,
                currentEmbeddedTrack = playerState.currentSubtitleTrack,
                currentPrimaryLocalTrack = uiState.primaryLocalSubtitleIndex,
                currentSecondaryLocalTrack = uiState.secondaryLocalSubtitleIndex,
                subtitleMessage = uiState.subtitleMessage,
                onEmbeddedTrackSelected = {
                    viewModel.selectSubtitleTrack(it)
                    viewModel.toggleSubtitleSelector()
                },
                onPrimaryLocalSelected = { viewModel.selectPrimaryLocalSubtitle(it) },
                onSecondaryLocalSelected = { viewModel.selectSecondaryLocalSubtitle(it) },
                onDisable = {
                    viewModel.disableSubtitles()
                    viewModel.toggleSubtitleSelector()
                },
                onSearchOnline = {
                    viewModel.toggleSubtitleSelector()
                    viewModel.toggleOnlineSubtitleDialog()
                    viewModel.searchOnlineSubtitles()
                },
                onStyle = {
                    viewModel.toggleSubtitleSelector()
                    viewModel.toggleSubtitleStyleSheet()
                },
                onDismiss = { viewModel.toggleSubtitleSelector() }
            )
        }

        if (uiState.showOnlineSubtitleDialog) {
            OnlineSubtitleDialog(
                results = uiState.onlineSubtitleResults,
                isSearching = uiState.isSearchingSubtitles,
                isDownloading = uiState.isDownloadingSubtitle,
                message = uiState.subtitleMessage,
                onRetry = { viewModel.searchOnlineSubtitles() },
                onDownload = { viewModel.downloadOnlineSubtitle(it) },
                onDismiss = { viewModel.toggleOnlineSubtitleDialog() }
            )
        }

        if (uiState.showSubtitleStyleSheet) {
            SubtitleStyleBottomSheet(
                config = uiState.subtitleDisplayConfig,
                onFontSizeChange = { viewModel.setSubtitleFontSize(it) },
                onBackgroundChange = { viewModel.setSubtitleBackground(it) },
                onColorChange = { viewModel.setSubtitleFontColor(it) },
                onOutlineChange = { viewModel.setSubtitleOutlineEnabled(it) },
                onShadowChange = { viewModel.setSubtitleShadowEnabled(it) },
                onDismiss = { viewModel.toggleSubtitleStyleSheet() }
            )
        }

        if (uiState.showSyncSheet) {
            SyncAudioBottomSheet(
                audioBoostLevel = uiState.audioBoostLevel,
                audioDelayMs = uiState.audioDelayMs,
                formattedAudioDelay = uiState.formattedAudioDelay,
                subtitleSyncOffsetMs = uiState.subtitleSyncOffsetMs,
                formattedSubtitleSync = uiState.formattedSubtitleSync,
                onAudioBoostChange = { viewModel.setAudioBoost(it) },
                onAudioDelayChange = { viewModel.setAudioDelay(it) },
                onSubtitleSyncAdjust = { viewModel.adjustSubtitleSync(it) },
                onDismiss = { viewModel.toggleSyncSheet() }
            )
        }

        if (uiState.showEqualizerSheet) {
            EqualizerBottomSheet(
                equalizerState = uiState.equalizerState,
                onEnabledChange = { viewModel.setEqualizerEnabled(it) },
                onBandLevelChange = { index, level -> viewModel.setEqBandLevel(index, level) },
                onPresetSelected = { viewModel.applyEqPreset(it) },
                onBassBoostChange = { viewModel.setBassBoost(it) },
                onVirtualizerChange = { viewModel.setVirtualizer(it) },
                onDismiss = { viewModel.toggleEqualizerSheet() }
            )
        }

        if (uiState.showVideoFilterSheet) {
            VideoFilterBottomSheet(
                filterState = uiState.videoFilterState,
                onBrightnessChange = { viewModel.setVideoBrightness(it) },
                onContrastChange = { viewModel.setVideoContrast(it) },
                onSaturationChange = { viewModel.setVideoSaturation(it) },
                onHueChange = { viewModel.setVideoHue(it) },
                onGammaChange = { viewModel.setVideoGamma(it) },
                onReset = { viewModel.resetVideoFilters() },
                onDismiss = { viewModel.toggleVideoFilterSheet() }
            )
        }

        if (showPlayerDemo) {
            GuidedDemoDialog(
                title = stringResource(R.string.player_demo_title),
                steps = listOf(
                    GuidedDemoStep(
                        title = stringResource(R.string.player_demo_transport_title),
                        description = stringResource(R.string.player_demo_transport_desc),
                        icon = Icons.Rounded.PlayCircle,
                        targetLabel = stringResource(R.string.player_play_pause)
                    ),
                    GuidedDemoStep(
                        title = stringResource(R.string.player_demo_top_tools_title),
                        description = stringResource(R.string.player_demo_top_tools_desc),
                        icon = Icons.Rounded.Subtitles,
                        targetLabel = stringResource(R.string.player_demo_top_tools_target)
                    ),
                    GuidedDemoStep(
                        title = stringResource(R.string.player_demo_gestures_title),
                        description = stringResource(R.string.player_demo_gestures_desc),
                        icon = Icons.Rounded.TouchApp,
                        targetLabel = stringResource(R.string.player_demo_gestures_target)
                    ),
                    GuidedDemoStep(
                        title = stringResource(R.string.player_demo_timeline_title),
                        description = stringResource(R.string.player_demo_timeline_desc),
                        icon = Icons.Rounded.Speed,
                        targetLabel = stringResource(R.string.player_demo_timeline_target)
                    ),
                    GuidedDemoStep(
                        title = stringResource(R.string.player_demo_view_tools_title),
                        description = stringResource(R.string.player_demo_view_tools_desc),
                        icon = Icons.Rounded.AspectRatio,
                        targetLabel = stringResource(R.string.player_demo_view_tools_target)
                    ),
                    GuidedDemoStep(
                        title = stringResource(R.string.player_demo_audio_video_title),
                        description = stringResource(R.string.player_demo_audio_video_desc),
                        icon = Icons.Rounded.Equalizer,
                        targetLabel = stringResource(R.string.player_demo_audio_video_target)
                    )
                ),
                surface = GuidedDemoSurface.PLAYER,
                doneText = stringResource(R.string.player_demo_done),
                previousText = stringResource(R.string.player_demo_previous),
                nextText = stringResource(R.string.player_demo_next),
                onDismiss = { showPlayerDemo = false }
            )
        }

        if (playerState.error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Rounded.ErrorOutline,
                        contentDescription = stringResource(R.string.player_error),
                        tint = Color(0xFFFF6B6B),
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = stringResource(R.string.player_playback_error),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = playerState.error ?: stringResource(R.string.player_unknown_error),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = onBackPressed,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.action_back))
                        }
                        Button(
                            onClick = { viewModel.initialize(mediaId, mediaPath) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Orange500
                            )
                        ) {
                            Icon(Icons.Rounded.Refresh, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.action_retry))
                        }
                    }
                }
            }
        }

        // Resume playback prompt banner
        AnimatedVisibility(
            visible = uiState.showResumePrompt && !uiState.isInPiPMode,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xDD1A1A1A))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Rounded.PlayCircle,
                    contentDescription = null,
                    tint = Orange500,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.player_resuming_from, formatTime(uiState.resumePosition)),
                    color = Color.White,
                    fontSize = 13.sp
                )
                TextButton(
                    onClick = { viewModel.startFromBeginning() },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(
                        stringResource(R.string.player_start_over),
                        color = Orange500,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(
                    onClick = { viewModel.dismissResumePrompt() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Rounded.Close, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                }
            }

            // Auto-dismiss after 6 seconds
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(6000)
                viewModel.dismissResumePrompt()
            }
        }
    }
}

@Composable
private fun PlayerTopBar(
    title: String,
    subtitle: String,
    subtitlesEnabled: Boolean,
    isLocked: Boolean,
    onBack: () -> Unit,
    onSubtitles: () -> Unit,
    onAudio: () -> Unit,
    onSync: () -> Unit,
    onGuide: () -> Unit,
    onLock: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Black.copy(alpha = 0.76f), Color.Transparent)
                )
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back), tint = Color.White)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.68f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        CompactToolButton(
            icon = if (subtitlesEnabled) Icons.Rounded.Subtitles else Icons.Rounded.SubtitlesOff,
            label = stringResource(R.string.player_subtitles),
            active = subtitlesEnabled,
            onClick = onSubtitles
        )
        CompactToolButton(Icons.Rounded.Audiotrack, stringResource(R.string.player_audio), onClick = onAudio)
        CompactToolButton(Icons.Rounded.Tune, stringResource(R.string.player_sync_audio), onClick = onSync)
        CompactToolButton(Icons.Rounded.TipsAndUpdates, stringResource(R.string.player_app_guide), onClick = onGuide)
        CompactToolButton(
            icon = if (isLocked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
            label = stringResource(R.string.player_lock),
            active = isLocked,
            onClick = onLock
        )
    }
}

@Composable
private fun TransportControls(
    isPlaying: Boolean,
    hasPrevious: Boolean,
    hasNext: Boolean,
    onPrevious: () -> Unit,
    onRewind: () -> Unit,
    onPlayPause: () -> Unit,
    onForward: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(40.dp))
            .background(Color.Black.copy(alpha = 0.28f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (hasPrevious) TransportButton(Icons.Rounded.SkipPrevious, stringResource(R.string.player_previous), onPrevious)
        TransportButton(Icons.Rounded.Replay10, stringResource(R.string.player_rewind), onRewind)
        IconButton(
            onClick = onPlayPause,
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(Orange500)
        ) {
            Icon(
                if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                stringResource(R.string.player_play_pause),
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }
        TransportButton(Icons.Rounded.Forward10, stringResource(R.string.player_forward), onForward)
        if (hasNext) TransportButton(Icons.Rounded.SkipNext, stringResource(R.string.player_next), onNext)
    }
}

@Composable
private fun PlayerBottomPanel(
    state: PlayerUiState,
    playerState: PlayerState,
    hasQueue: Boolean,
    mediaPath: String,
    onSeek: (Long) -> Unit,
    onSpeed: () -> Unit,
    onLoop: () -> Unit,
    onAspect: () -> Unit,
    onRotation: () -> Unit,
    onNight: () -> Unit,
    onPip: () -> Unit,
    onFloating: (String, Long) -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onSkipSilence: () -> Unit,
    onEqualizer: () -> Unit,
    onFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f))
                )
            )
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(formatTime(playerState.currentPosition), color = Color.White, fontSize = 12.sp)
            Slider(
                value = if (playerState.duration > 0) {
                    playerState.currentPosition.toFloat() / playerState.duration.toFloat()
                } else {
                    0f
                },
                onValueChange = { onSeek((it * playerState.duration).toLong()) },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Orange500,
                    activeTrackColor = Orange500,
                    inactiveTrackColor = Color.White.copy(alpha = 0.24f)
                )
            )
            Text(formatTime(playerState.duration), color = Color.White, fontSize = 12.sp)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onSpeed) {
                Icon(Icons.Rounded.Speed, null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("${playerState.playbackSpeed}x", color = Color.White)
            }
            CompactToolButton(
                icon = if (playerState.isLooping) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                label = stringResource(R.string.player_loop),
                active = playerState.isLooping,
                onClick = onLoop
            )
            CompactToolButton(Icons.Rounded.AspectRatio, stringResource(R.string.player_aspect_ratio), onClick = onAspect)
            CompactToolButton(
                icon = when (state.rotationMode) {
                    RotationMode.AUTO -> Icons.Rounded.ScreenRotation
                    RotationMode.LANDSCAPE -> Icons.Rounded.ScreenLockLandscape
                    RotationMode.PORTRAIT -> Icons.Rounded.ScreenLockPortrait
                },
                label = stringResource(R.string.player_rotation),
                active = state.rotationMode != RotationMode.AUTO,
                onClick = onRotation
            )
            CompactToolButton(
                Icons.Rounded.NightsStay,
                stringResource(R.string.player_night_filter),
                active = state.isNightFilterEnabled,
                onClick = onNight
            )
            CompactToolButton(Icons.Rounded.PictureInPictureAlt, stringResource(R.string.player_pip), onClick = onPip)
            CompactToolButton(Icons.Rounded.OpenInNew, "Floating Video", onClick = {
                onFloating(mediaPath, state.playerState.currentPosition)
            })
            if (hasQueue) {
                CompactToolButton(Icons.Rounded.Shuffle, stringResource(R.string.player_shuffle), state.isShuffled, onShuffle)
            }
            CompactToolButton(
                icon = if (state.repeatMode == RepeatMode.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                label = stringResource(
                    when (state.repeatMode) {
                        RepeatMode.OFF -> R.string.player_repeat_off
                        RepeatMode.ALL -> R.string.player_repeat_all
                        RepeatMode.ONE -> R.string.player_repeat_one
                    }
                ),
                active = state.repeatMode != RepeatMode.OFF,
                onClick = onRepeat
            )
            CompactToolButton(Icons.Rounded.GraphicEq, stringResource(R.string.player_skip_silence), state.skipSilenceEnabled, onSkipSilence)
            CompactToolButton(Icons.Rounded.Equalizer, stringResource(R.string.player_equalizer), state.equalizerState.isEnabled, onEqualizer)
            CompactToolButton(Icons.Rounded.Tune, stringResource(R.string.player_video_filters), !state.videoFilterState.isDefault, onFilters)
        }
    }
}

@Composable
private fun CompactToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean = false,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(if (active) Orange500.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.08f))
    ) {
        Icon(
            icon,
            label,
            tint = if (active) Orange500 else Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun TransportButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(50.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.10f))
    ) {
        Icon(icon, label, tint = Color.White, modifier = Modifier.size(26.dp))
    }
}

@Composable
private fun SpeedSelectorDialog(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.player_playback_speed)) },
        text = {
            Column {
                PlayerEngine.PLAYBACK_SPEEDS.forEach { speed ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSpeedSelected(speed) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentSpeed == speed,
                            onClick = { onSpeedSelected(speed) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "${speed}x",
                            fontWeight = if (currentSpeed == speed) FontWeight.Bold else FontWeight.Normal,
                            color = if (currentSpeed == speed)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        }
    )
}

@Composable
private fun TrackSelectorDialog(
    title: String,
    trackCount: Int,
    currentTrack: Int,
    onTrackSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            if (trackCount == 0) {
                Text(stringResource(R.string.player_no_tracks), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Column {
                    for (i in 0 until trackCount) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onTrackSelected(i) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentTrack == i,
                                onClick = { onTrackSelected(i) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.player_track_number, i + 1),
                                fontWeight = if (currentTrack == i) FontWeight.Bold else FontWeight.Normal,
                                color = if (currentTrack == i)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        }
    )
}

@Composable
private fun EnhancedAudioTrackDialog(
    trackInfoList: List<AudioTrackInfo>,
    currentTrack: Int,
    onTrackSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.player_audio_track)) },
        text = {
            if (trackInfoList.isEmpty()) {
                Text(stringResource(R.string.player_no_tracks), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Column {
                    trackInfoList.forEach { info ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onTrackSelected(info.index) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentTrack == info.index,
                                onClick = { onTrackSelected(info.index) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    info.displayName,
                                    fontWeight = if (currentTrack == info.index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (currentTrack == info.index)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp
                                )
                                if (info.bitrate > 0) {
                                    Text(
                                        "${info.bitrate / 1000} kbps",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        }
    )
}

@Composable
private fun SubtitleSelectorDialog(
    embeddedTracks: List<SubtitleTrackInfo>,
    localTracks: List<PlayerSubtitleTrack>,
    currentEmbeddedTrack: Int,
    currentPrimaryLocalTrack: Int,
    currentSecondaryLocalTrack: Int,
    subtitleMessage: String?,
    onEmbeddedTrackSelected: (Int) -> Unit,
    onPrimaryLocalSelected: (Int) -> Unit,
    onSecondaryLocalSelected: (Int) -> Unit,
    onDisable: () -> Unit,
    onSearchOnline: () -> Unit,
    onStyle: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        modifier = Modifier.widthIn(min = 360.dp, max = 560.dp),
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.player_subtitle_track)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = onSearchOnline,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.CloudDownload, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.player_search_subtitles))
                    }
                    OutlinedButton(
                        onClick = onStyle,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.FormatColorText, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.player_subtitle_style))
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDisable() }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentEmbeddedTrack < 0 && currentPrimaryLocalTrack < 0,
                        onClick = { onDisable() }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.player_subtitles_off),
                        fontWeight = if (currentEmbeddedTrack < 0 && currentPrimaryLocalTrack < 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (currentEmbeddedTrack < 0 && currentPrimaryLocalTrack < 0)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    stringResource(R.string.player_embedded_subtitles),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                if (embeddedTracks.isEmpty()) {
                    Text(
                        stringResource(R.string.player_no_subtitle_tracks),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                } else {
                    embeddedTracks.forEach { track ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onEmbeddedTrackSelected(track.index) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentEmbeddedTrack == track.index,
                                onClick = { onEmbeddedTrackSelected(track.index) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                track.displayName,
                                fontWeight = if (currentEmbeddedTrack == track.index) FontWeight.Bold else FontWeight.Normal,
                                color = if (currentEmbeddedTrack == track.index)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Text(
                    stringResource(R.string.player_sidecar_subtitles),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                if (localTracks.isEmpty()) {
                    Text(
                        stringResource(R.string.player_no_sidecar_subtitles),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                } else {
                    localTracks.forEach { track ->
                        SubtitleLocalTrackRow(
                            track = track,
                            label = stringResource(R.string.player_primary_subtitle),
                            selected = currentPrimaryLocalTrack == track.index,
                            onClick = { onPrimaryLocalSelected(track.index) }
                        )
                        SubtitleLocalTrackRow(
                            track = track,
                            label = stringResource(R.string.player_secondary_subtitle),
                            selected = currentSecondaryLocalTrack == track.index,
                            onClick = { onSecondaryLocalSelected(track.index) }
                        )
                    }
                    if (currentSecondaryLocalTrack >= 0) {
                        TextButton(onClick = { onSecondaryLocalSelected(-1) }) {
                            Text("${stringResource(R.string.player_secondary_subtitle)} ${stringResource(R.string.player_subtitles_off)}")
                        }
                    }
                }

                subtitleMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        }
    )
}

@Composable
private fun SubtitleLocalTrackRow(
    track: PlayerSubtitleTrack,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                "$label: ${track.name}",
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp
            )
            Text(
                track.source.name.lowercase().replaceFirstChar { it.uppercase() },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun OnlineSubtitleDialog(
    results: List<OnlineSubtitle>,
    isSearching: Boolean,
    isDownloading: Boolean,
    message: String?,
    onRetry: () -> Unit,
    onDownload: (OnlineSubtitle) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        modifier = Modifier.widthIn(min = 360.dp, max = 560.dp),
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.player_online_subtitles)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    isSearching -> {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(stringResource(R.string.player_searching_subtitles))
                    }
                    isDownloading -> {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(stringResource(R.string.player_downloading_subtitle))
                    }
                    results.isEmpty() -> {
                        Text(
                            message ?: stringResource(R.string.player_no_online_subtitles),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    else -> {
                        results.take(8).forEach { subtitle ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onDownload(subtitle) }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Subtitles, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        subtitle.displayName,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        "${subtitle.downloadCount} downloads - ${subtitle.language}",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
                message?.takeIf { results.isNotEmpty() }?.let {
                    Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onRetry, enabled = !isSearching && !isDownloading) {
                Text(stringResource(R.string.action_retry))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        }
    )
}

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubtitleStyleBottomSheet(
    config: SubtitleDisplayConfig,
    onFontSizeChange: (Float) -> Unit,
    onBackgroundChange: (Boolean) -> Unit,
    onColorChange: (String) -> Unit,
    onOutlineChange: (Boolean) -> Unit,
    onShadowChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                stringResource(R.string.player_subtitle_style),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            FilterSliderRow(
                label = stringResource(R.string.settings_font_size),
                value = config.fontSize,
                valueRange = 12f..36f,
                valueText = "${config.fontSize.toInt()}sp",
                onValueChange = onFontSizeChange
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.settings_subtitle_bg))
                Switch(checked = config.showBackground, onCheckedChange = onBackgroundChange)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.settings_subtitle_outline))
                Switch(checked = config.outlineWidth > 0f, onCheckedChange = onOutlineChange)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.settings_subtitle_shadow))
                Switch(checked = config.shadowEnabled, onCheckedChange = onShadowChange)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "FFFFFFFF" to Color.White,
                    "FFFFFF00" to Color.Yellow,
                    "FF00E5FF" to Color.Cyan,
                    "FFFFAB40" to Color(0xFFFFAB40),
                    "FF69F0AE" to Color(0xFF69F0AE)
                ).forEach { (hex, color) ->
                    IconButton(
                        onClick = { onColorChange(hex) },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    ) {
                        if (config.fontColor == color) {
                            Icon(Icons.Rounded.Check, null, tint = Color.Black)
                        }
                    }
                }
            }
        }
    }
}

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SyncAudioBottomSheet(
    audioBoostLevel: Int,
    audioDelayMs: Long,
    formattedAudioDelay: String,
    subtitleSyncOffsetMs: Long,
    formattedSubtitleSync: String,
    onAudioBoostChange: (Int) -> Unit,
    onAudioDelayChange: (Long) -> Unit,
    onSubtitleSyncAdjust: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                stringResource(R.string.player_sync_audio),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        stringResource(R.string.player_audio_boost),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "$audioBoostLevel%",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Orange500
                    )
                }
                Slider(
                    value = audioBoostLevel.toFloat(),
                    onValueChange = { onAudioBoostChange(it.toInt()) },
                    valueRange = 0f..300f,
                    colors = SliderDefaults.colors(
                        thumbColor = Orange500,
                        activeTrackColor = Orange500,
                        inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    )
                )
            }

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        stringResource(R.string.player_audio_delay),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        formattedAudioDelay,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Orange500
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(onClick = { onAudioDelayChange((audioDelayMs - 100).coerceAtLeast(-5000)) }) {
                        Text(stringResource(R.string.player_minus_100ms))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(onClick = { onAudioDelayChange(0L) }) {
                        Text(stringResource(R.string.action_reset))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledTonalButton(onClick = { onAudioDelayChange((audioDelayMs + 100).coerceAtMost(5000)) }) {
                        Text(stringResource(R.string.player_plus_100ms))
                    }
                }
            }

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        stringResource(R.string.player_subtitle_sync),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        formattedSubtitleSync,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Orange500
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(onClick = { onSubtitleSyncAdjust(-50L) }) {
                        Text(stringResource(R.string.player_minus_50ms))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(onClick = { onSubtitleSyncAdjust(-subtitleSyncOffsetMs) }) {
                        Text(stringResource(R.string.action_reset))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledTonalButton(onClick = { onSubtitleSyncAdjust(50L) }) {
                        Text(stringResource(R.string.player_plus_50ms))
                    }
                }
            }
        }
    }
}

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EqualizerBottomSheet(
    equalizerState: com.nextgen.player.player.audio.EqualizerState,
    onEnabledChange: (Boolean) -> Unit,
    onBandLevelChange: (Int, Float) -> Unit,
    onPresetSelected: (String) -> Unit,
    onBassBoostChange: (Int) -> Unit,
    onVirtualizerChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.player_equalizer),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Switch(
                    checked = equalizerState.isEnabled,
                    onCheckedChange = onEnabledChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Orange500,
                        checkedTrackColor = Orange500.copy(alpha = 0.3f)
                    )
                )
            }

            // Presets
            Column {
                Text(
                    stringResource(R.string.player_eq_preset),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EqualizerEngine.PRESETS.forEach { preset ->
                        FilterChip(
                            selected = equalizerState.currentPresetName == preset.name,
                            onClick = { onPresetSelected(preset.name) },
                            label = { Text(preset.name, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Orange500.copy(alpha = 0.2f),
                                selectedLabelColor = Orange500
                            )
                        )
                    }
                }
            }

            // EQ Bands
            if (equalizerState.bands.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    equalizerState.bands.forEach { band ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Vertical slider (rotated)
                            Box(
                                modifier = Modifier
                                    .height(120.dp)
                                    .width(36.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Slider(
                                    value = band.normalizedLevel,
                                    onValueChange = { onBandLevelChange(band.index, it) },
                                    modifier = Modifier
                                        .graphicsLayer {
                                            rotationZ = -90f
                                        }
                                        .width(120.dp),
                                    colors = SliderDefaults.colors(
                                        thumbColor = Orange500,
                                        activeTrackColor = Orange500,
                                        inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                    ),
                                    enabled = equalizerState.isEnabled
                                )
                            }
                            Text(
                                band.displayFreq,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Bass Boost
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        stringResource(R.string.player_eq_bass_boost),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "${(equalizerState.bassBoostStrength / 10f).toInt()}%",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Orange500
                    )
                }
                Slider(
                    value = equalizerState.bassBoostStrength.toFloat(),
                    onValueChange = { onBassBoostChange(it.toInt()) },
                    valueRange = 0f..1000f,
                    enabled = equalizerState.isEnabled,
                    colors = SliderDefaults.colors(
                        thumbColor = Orange500,
                        activeTrackColor = Orange500,
                        inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    )
                )
            }

            // Virtualizer
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        stringResource(R.string.player_eq_virtualizer),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "${(equalizerState.virtualizerStrength / 10f).toInt()}%",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Orange500
                    )
                }
                Slider(
                    value = equalizerState.virtualizerStrength.toFloat(),
                    onValueChange = { onVirtualizerChange(it.toInt()) },
                    valueRange = 0f..1000f,
                    enabled = equalizerState.isEnabled,
                    colors = SliderDefaults.colors(
                        thumbColor = Orange500,
                        activeTrackColor = Orange500,
                        inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    )
                )
            }
        }
    }
}

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoFilterBottomSheet(
    filterState: VideoFilterState,
    onBrightnessChange: (Float) -> Unit,
    onContrastChange: (Float) -> Unit,
    onSaturationChange: (Float) -> Unit,
    onHueChange: (Float) -> Unit,
    onGammaChange: (Float) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.player_video_filters),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = onReset) {
                    Text(stringResource(R.string.player_filter_reset), color = Orange500)
                }
            }

            FilterSliderRow(
                label = stringResource(R.string.player_filter_brightness),
                value = filterState.brightness,
                valueRange = -1f..1f,
                valueText = "%+.0f%%".format(filterState.brightness * 100),
                onValueChange = onBrightnessChange
            )

            FilterSliderRow(
                label = stringResource(R.string.player_filter_contrast),
                value = filterState.contrast,
                valueRange = 0f..2f,
                valueText = "%.0f%%".format(filterState.contrast * 100),
                onValueChange = onContrastChange
            )

            FilterSliderRow(
                label = stringResource(R.string.player_filter_saturation),
                value = filterState.saturation,
                valueRange = 0f..2f,
                valueText = "%.0f%%".format(filterState.saturation * 100),
                onValueChange = onSaturationChange
            )

            FilterSliderRow(
                label = stringResource(R.string.player_filter_hue),
                value = filterState.hue,
                valueRange = -180f..180f,
                valueText = "%+.0f°".format(filterState.hue),
                onValueChange = onHueChange
            )

            FilterSliderRow(
                label = stringResource(R.string.player_filter_gamma),
                value = filterState.gamma,
                valueRange = 0.5f..2f,
                valueText = "%.2f".format(filterState.gamma),
                onValueChange = onGammaChange
            )
        }
    }
}

@Composable
private fun FilterSliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueText: String,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                valueText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = Orange500
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = Orange500,
                activeTrackColor = Orange500,
                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            )
        )
    }
}

private fun formatTime(ms: Long): String {
    if (ms < 0) return "0:00"
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
