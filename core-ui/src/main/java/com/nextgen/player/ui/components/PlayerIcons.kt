package com.nextgen.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nextgen.player.ui.theme.PlayerOverlayBg

@Composable
fun PlayerIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    size: Dp = 48.dp,
    iconSize: Dp = 24.dp,
    enabled: Boolean = true
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(size),
        enabled = enabled
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else tint.copy(alpha = 0.38f),
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun PlayerCircleButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    backgroundColor: Color = PlayerOverlayBg,
    size: Dp = 56.dp,
    iconSize: Dp = 32.dp
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun BadgeOverlay(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.primary
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White
        )
    }
}

object PlayerIcons {
    val Play = Icons.Rounded.PlayArrow
    val Pause = Icons.Rounded.Pause
    val SkipNext = Icons.Rounded.SkipNext
    val SkipPrevious = Icons.Rounded.SkipPrevious
    val Forward10 = Icons.Rounded.Forward10
    val Replay10 = Icons.Rounded.Replay10
    val VolumeUp = Icons.Rounded.VolumeUp
    val VolumeDown = Icons.Rounded.VolumeDown
    val VolumeMute = Icons.Rounded.VolumeMute
    val VolumeOff = Icons.Rounded.VolumeOff
    val Fullscreen = Icons.Rounded.Fullscreen
    val FullscreenExit = Icons.Rounded.FullscreenExit
    val Settings = Icons.Rounded.Settings
    val Subtitles = Icons.Rounded.Subtitles
    val SubtitlesOff = Icons.Rounded.SubtitlesOff
    val Speed = Icons.Rounded.Speed
    val Lock = Icons.Rounded.Lock
    val LockOpen = Icons.Rounded.LockOpen
    val ArrowBack = Icons.Rounded.ArrowBack
    val MoreVert = Icons.Rounded.MoreVert
    val Brightness = Icons.Rounded.BrightnessHigh
    val BrightnessLow = Icons.Rounded.BrightnessLow
    val Repeat = Icons.Rounded.Repeat
    val RepeatOne = Icons.Rounded.RepeatOne
    val Shuffle = Icons.Rounded.Shuffle
    val PictureInPicture = Icons.Rounded.PictureInPictureAlt
    val FitScreen = Icons.Rounded.FitScreen
    val AspectRatio = Icons.Rounded.AspectRatio
    val AudioTrack = Icons.Rounded.Audiotrack
    val Folder = Icons.Rounded.Folder
    val VideoLibrary = Icons.Rounded.VideoLibrary
    val Search = Icons.Rounded.Search
    val Sort = Icons.Rounded.Sort
    val GridView = Icons.Rounded.GridView
    val ListView = Icons.Rounded.ViewList
    val Delete = Icons.Rounded.Delete
    val Share = Icons.Rounded.Share
    val Info = Icons.Rounded.Info
}
