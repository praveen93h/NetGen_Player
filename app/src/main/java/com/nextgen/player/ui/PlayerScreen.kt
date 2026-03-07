package com.nextgen.player.ui

import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.nextgen.player.player.VideoFilterState
import com.nextgen.player.player.gesture.GestureZoneType
import com.nextgen.player.player.audio.EqPreset
import com.nextgen.player.player.audio.EqualizerEngine
import com.nextgen.player.subtitle.SubtitleOverlay
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
            factory = { context ->
                PlayerView(context).apply {
                    useController = false
                    setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                }
            },
            update = { view ->
                if (view.player !== player) view.player = player
                // Apply video color filter
                val filterState = uiState.videoFilterState
                if (!filterState.isDefault) {
                    val cm = android.graphics.ColorMatrix(filterState.toColorMatrixArray())
                    view.videoSurfaceView?.let { surface ->
                        if (surface is android.view.TextureView) {
                            // Cannot apply color filter to TextureView directly; handled via overlay
                        }
                    }
                    // Apply to overlay layer for SurfaceView-based rendering
                    view.overlayFrameLayout?.foreground = android.graphics.drawable.ColorDrawable().apply {
                        colorFilter = android.graphics.ColorMatrixColorFilter(cm)
                    }
                } else {
                    view.overlayFrameLayout?.foreground = null
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (uiState.subtitleCues.isNotEmpty()) {
            SubtitleOverlay(
                currentPositionMs = playerState.currentPosition,
                cues = uiState.subtitleCues,
                syncOffsetMs = uiState.subtitleSyncOffsetMs,
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
                                            if (startX < halfWidth) GestureZoneType.BRIGHTNESS
                                            else GestureZoneType.VOLUME
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
                    Text(stringResource(R.string.player_rewind), color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
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
                    Text(stringResource(R.string.player_forward), color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.7f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                        .align(Alignment.TopCenter)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            viewModel.savePosition()
                            onBackPressed()
                        }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back), tint = Color.White)
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = uiState.mediaInfo?.title ?: stringResource(R.string.player_playing),
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (uiState.mediaInfo?.resolution?.isNotEmpty() == true) {
                                Text(
                                    text = uiState.mediaInfo!!.resolution,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        IconButton(onClick = { viewModel.toggleSubtitleSelector() }) {
                            Icon(
                                if (playerState.currentSubtitleTrack >= 0)
                                    Icons.Rounded.Subtitles
                                else
                                    Icons.Rounded.SubtitlesOff,
                                stringResource(R.string.player_subtitles),
                                tint = Color.White
                            )
                        }

                        IconButton(onClick = { viewModel.toggleAudioTrackSelector() }) {
                            Icon(Icons.Rounded.Audiotrack, stringResource(R.string.player_audio), tint = Color.White)
                        }

                        IconButton(onClick = { viewModel.toggleSyncSheet() }) {
                            Icon(Icons.Rounded.Tune, stringResource(R.string.player_sync_audio), tint = Color.White)
                        }

                        IconButton(onClick = { viewModel.toggleLock() }) {
                            Icon(
                                if (uiState.isLocked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                                stringResource(R.string.player_lock),
                                tint = Color.White
                            )
                        }
                    }
                }

                if (!uiState.isLocked) {
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous video
                        if (viewModel.hasPrevious) {
                            IconButton(
                                onClick = { viewModel.playPrevious() },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.4f))
                            ) {
                                Icon(
                                    Icons.Rounded.SkipPrevious,
                                    stringResource(R.string.player_previous),
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.seekBackward() },
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.4f))
                        ) {
                            Icon(
                                Icons.Rounded.Replay10,
                                stringResource(R.string.player_rewind),
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.togglePlayPause() },
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(Orange500.copy(alpha = 0.9f))
                        ) {
                            Icon(
                                if (playerState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                stringResource(R.string.player_play_pause),
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.seekForward() },
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.4f))
                        ) {
                            Icon(
                                Icons.Rounded.Forward10,
                                stringResource(R.string.player_forward),
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // Next video
                        if (viewModel.hasNext) {
                            IconButton(
                                onClick = { viewModel.playNext() },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.4f))
                            ) {
                                Icon(
                                    Icons.Rounded.SkipNext,
                                    stringResource(R.string.player_next),
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                if (!uiState.isLocked) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.7f)
                                    )
                                )
                            )
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatTime(playerState.currentPosition),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Slider(
                                value = if (playerState.duration > 0)
                                    playerState.currentPosition.toFloat() / playerState.duration.toFloat()
                                else 0f,
                                onValueChange = { fraction ->
                                    viewModel.seekTo((fraction * playerState.duration).toLong())
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = Orange500,
                                    activeTrackColor = Orange500,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                )
                            )

                            Text(
                                text = formatTime(playerState.duration),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { viewModel.toggleSpeedSelector() }) {
                                Text(
                                    "${playerState.playbackSpeed}x",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Row {
                                IconButton(onClick = { viewModel.toggleLoop() }) {
                                    Icon(
                                        if (playerState.isLooping) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                                        stringResource(R.string.player_loop),
                                        tint = if (playerState.isLooping) Orange500 else Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(onClick = { viewModel.cycleAspectRatio() }) {
                                    Icon(
                                        Icons.Rounded.AspectRatio,
                                        stringResource(R.string.player_aspect_ratio),
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Rotation Lock
                                IconButton(onClick = {
                                    val mode = viewModel.cycleRotation()
                                    activity?.requestedOrientation = when (mode) {
                                        RotationMode.AUTO -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
                                        RotationMode.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                        RotationMode.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                    }
                                }) {
                                    Icon(
                                        when (uiState.rotationMode) {
                                            RotationMode.AUTO -> Icons.Rounded.ScreenRotation
                                            RotationMode.LANDSCAPE -> Icons.Rounded.ScreenLockLandscape
                                            RotationMode.PORTRAIT -> Icons.Rounded.ScreenLockPortrait
                                        },
                                        stringResource(R.string.player_rotation),
                                        tint = if (uiState.rotationMode != RotationMode.AUTO) Orange500 else Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Night / Blue Light Filter
                                IconButton(onClick = { viewModel.toggleNightFilter() }) {
                                    Icon(
                                        Icons.Rounded.NightsStay,
                                        stringResource(R.string.player_night_filter),
                                        tint = if (uiState.isNightFilterEnabled) Orange500 else Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(onClick = { onEnterPiP?.invoke() }) {
                                    Icon(
                                        Icons.Rounded.PictureInPictureAlt,
                                        stringResource(R.string.player_pip),
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // v1.3 Controls Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Queue info
                            if (uiState.queue.size > 1) {
                                Text(
                                    stringResource(R.string.player_queue, uiState.currentQueueIndex + 1, uiState.queue.size),
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            } else {
                                Spacer(modifier = Modifier.width(1.dp))
                            }

                            Row {
                                // Shuffle
                                if (uiState.queue.size > 1) {
                                    IconButton(onClick = { viewModel.toggleShuffle() }) {
                                        Icon(
                                            Icons.Rounded.Shuffle,
                                            stringResource(R.string.player_shuffle),
                                            tint = if (uiState.isShuffled) Orange500 else Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                // Repeat
                                IconButton(onClick = { viewModel.cycleRepeatMode() }) {
                                    Icon(
                                        when (uiState.repeatMode) {
                                            RepeatMode.OFF -> Icons.Rounded.Repeat
                                            RepeatMode.ALL -> Icons.Rounded.Repeat
                                            RepeatMode.ONE -> Icons.Rounded.RepeatOne
                                        },
                                        stringResource(
                                            when (uiState.repeatMode) {
                                                RepeatMode.OFF -> R.string.player_repeat_off
                                                RepeatMode.ALL -> R.string.player_repeat_all
                                                RepeatMode.ONE -> R.string.player_repeat_one
                                            }
                                        ),
                                        tint = if (uiState.repeatMode != RepeatMode.OFF) Orange500 else Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Skip Silence
                                IconButton(onClick = { viewModel.toggleSkipSilence() }) {
                                    Icon(
                                        Icons.Rounded.Speed,
                                        stringResource(R.string.player_skip_silence),
                                        tint = if (uiState.skipSilenceEnabled) Orange500 else Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Equalizer
                                IconButton(onClick = { viewModel.toggleEqualizerSheet() }) {
                                    Icon(
                                        Icons.Rounded.Equalizer,
                                        stringResource(R.string.player_equalizer),
                                        tint = if (uiState.equalizerState.isEnabled) Orange500 else Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Video Filters
                                IconButton(onClick = { viewModel.toggleVideoFilterSheet() }) {
                                    Icon(
                                        Icons.Rounded.Tune,
                                        stringResource(R.string.player_video_filters),
                                        tint = if (!uiState.videoFilterState.isDefault) Orange500 else Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
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
                trackCount = playerState.subtitleTrackCount,
                currentTrack = playerState.currentSubtitleTrack,
                onTrackSelected = {
                    viewModel.selectSubtitleTrack(it)
                    viewModel.toggleSubtitleSelector()
                },
                onDisable = {
                    viewModel.selectSubtitleTrack(-1)
                    viewModel.toggleSubtitleSelector()
                },
                onDismiss = { viewModel.toggleSubtitleSelector() }
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
    trackCount: Int,
    currentTrack: Int,
    onTrackSelected: (Int) -> Unit,
    onDisable: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.player_subtitle_track)) },
        text = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDisable() }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentTrack < 0,
                        onClick = { onDisable() }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.player_subtitles_off),
                        fontWeight = if (currentTrack < 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (currentTrack < 0)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }

                if (trackCount == 0) {
                    Text(
                        stringResource(R.string.player_no_subtitle_tracks),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 40.dp, top = 4.dp)
                    )
                } else {
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
                    valueRange = 0f..200f,
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
                    FilledTonalButton(onClick = { onAudioDelayChange((audioDelayMs - 100).coerceAtLeast(-1000)) }) {
                        Text(stringResource(R.string.player_minus_100ms))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(onClick = { onAudioDelayChange(0L) }) {
                        Text(stringResource(R.string.action_reset))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledTonalButton(onClick = { onAudioDelayChange((audioDelayMs + 100).coerceAtMost(1000)) }) {
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
                        "${equalizerState.bassBoostStrength / 10}%",
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
                        "${equalizerState.virtualizerStrength / 10}%",
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
