package com.nextgen.player.library.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import com.nextgen.player.data.local.entity.MediaEntity
import com.nextgen.player.ui.theme.Orange500

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaItemCard(
    media: MediaEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isGridView: Boolean = true,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onFavoriteClick: ((Boolean) -> Unit)? = null
) {
    if (isGridView) GridMediaCard(media, onClick, modifier, isSelected, isSelectionMode, onLongClick, onFavoriteClick)
    else ListMediaCard(media, onClick, modifier, isSelected, isSelectionMode, onLongClick, onFavoriteClick)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GridMediaCard(
    media: MediaEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onFavoriteClick: ((Boolean) -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(Color(0xFF111111))
            .combinedClickable(
                onClick = onClick,
                onLongClick = { onLongClick?.invoke() }
            )
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(media.thumbnailPath ?: media.path)
                .decoderFactory(VideoFrameDecoder.Factory())
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isSelected) Orange500.copy(alpha = 0.3f) else Color.Transparent)
            )
            Icon(
                if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) Orange500 else Color.White.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .size(22.dp)
            )
        }

        if (media.is4K || media.isHD) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = if (isSelectionMode) 32.dp else 4.dp, top = 4.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (media.is4K) Orange500 else Color(0xFF1565C0))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (media.is4K) "4K" else "HD",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.Black.copy(alpha = 0.75f))
                .padding(horizontal = 5.dp, vertical = 2.dp)
        ) {
            Text(text = media.formattedDuration, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium)
        }

        if (media.progressPercent > 0.01f) {
            LinearProgressIndicator(
                progress = { media.progressPercent },
                modifier = Modifier.fillMaxWidth().height(2.dp).align(Alignment.BottomCenter),
                color = Orange500,
                trackColor = Color.White.copy(alpha = 0.15f)
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(start = 6.dp, end = 2.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = media.title,
                color = Color.White,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (onFavoriteClick != null && !isSelectionMode) {
                IconButton(
                    onClick = { onFavoriteClick(!media.isFavorite) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        if (media.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = null,
                        tint = if (media.isFavorite) Color(0xFFE91E63) else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ListMediaCard(
    media: MediaEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onFavoriteClick: ((Boolean) -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(if (isSelected) Orange500.copy(alpha = 0.15f) else Color.Black)
            .combinedClickable(
                onClick = onClick,
                onLongClick = { onLongClick?.invoke() }
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelectionMode) {
            Icon(
                if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) Orange500 else Color(0xFF555555),
                modifier = Modifier.size(24.dp).padding(end = 8.dp)
            )
        }
        Box(
            modifier = Modifier
                .size(width = 100.dp, height = 62.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF111111))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(media.thumbnailPath ?: media.path)
                    .decoderFactory(VideoFrameDecoder.Factory())
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(2.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.Black.copy(alpha = 0.8f))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(text = media.formattedDuration, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Medium)
            }
            if (media.progressPercent > 0.01f) {
                LinearProgressIndicator(
                    progress = { media.progressPercent },
                    modifier = Modifier.fillMaxWidth().height(2.dp).align(Alignment.BottomCenter),
                    color = Orange500,
                    trackColor = Color.Transparent
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = media.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = media.formattedSize, color = Color(0xFF777777), fontSize = 11.sp)
                if (media.resolution.isNotEmpty()) {
                    Text(text = "·", color = Color(0xFF444444), fontSize = 11.sp)
                    Text(text = media.resolution, color = Color(0xFF777777), fontSize = 11.sp)
                }
            }
        }
        if (onFavoriteClick != null && !isSelectionMode) {
            IconButton(
                onClick = { onFavoriteClick(!media.isFavorite) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    if (media.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = null,
                    tint = if (media.isFavorite) Color(0xFFE91E63) else Color(0xFF444444),
                    modifier = Modifier.size(18.dp)
                )
            }
        } else if (!isSelectionMode) {
            Icon(
                Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = Color(0xFF444444),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
