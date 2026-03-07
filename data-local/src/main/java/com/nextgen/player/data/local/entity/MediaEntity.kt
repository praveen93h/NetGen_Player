package com.nextgen.player.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_items")
data class MediaEntity(
    @PrimaryKey
    val id: Long,
    val path: String,
    val title: String,
    val displayName: String,
    val mimeType: String,
    val duration: Long = 0L,
    val size: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val dateAdded: Long = 0L,
    val dateModified: Long = 0L,
    val folderPath: String = "",
    val folderName: String = "",
    val thumbnailPath: String? = null,
    val lastPlayedAt: Long = 0L,
    val lastPosition: Long = 0L,
    val playCount: Int = 0,
    val isFavorite: Boolean = false
) {
    val resolution: String
        get() = if (width > 0 && height > 0) "${width}x${height}" else ""

    val is4K: Boolean
        get() = width >= 3840 || height >= 2160

    val isHD: Boolean
        get() = width >= 1280 || height >= 720

    val formattedDuration: String
        get() {
            val totalSeconds = duration / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%d:%02d", minutes, seconds)
            }
        }

    val formattedSize: String
        get() {
            val kb = size / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format("%.1f GB", gb)
                mb >= 1.0 -> String.format("%.1f MB", mb)
                else -> String.format("%.0f KB", kb)
            }
        }

    val progressPercent: Float
        get() = if (duration > 0) (lastPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
}
