package com.nextgen.player.data.local.repository

import com.nextgen.player.data.local.dao.FolderInfo
import com.nextgen.player.data.local.dao.MediaDao
import com.nextgen.player.data.local.entity.MediaEntity
import com.nextgen.player.data.local.scanner.MediaScanner
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

enum class SortOrder {
    NAME, DATE, SIZE, DURATION
}

@Singleton
class MediaRepository @Inject constructor(
    private val mediaDao: MediaDao,
    private val mediaScanner: MediaScanner
) {
    fun getAllMedia(sortOrder: SortOrder = SortOrder.NAME): Flow<List<MediaEntity>> {
        return when (sortOrder) {
            SortOrder.NAME -> mediaDao.getAllMedia()
            SortOrder.DATE -> mediaDao.getAllMediaByDate()
            SortOrder.SIZE -> mediaDao.getAllMediaBySize()
            SortOrder.DURATION -> mediaDao.getAllMediaByDuration()
        }
    }

    fun getRecentlyPlayed(limit: Int = 20): Flow<List<MediaEntity>> {
        return mediaDao.getRecentlyPlayed(limit)
    }

    fun getFavorites(): Flow<List<MediaEntity>> {
        return mediaDao.getFavorites()
    }

    fun getFolders(): Flow<List<FolderInfo>> {
        return mediaDao.getFolders()
    }

    fun getMediaInFolder(folderPath: String): Flow<List<MediaEntity>> {
        return mediaDao.getMediaInFolder(folderPath)
    }

    fun searchMedia(query: String): Flow<List<MediaEntity>> {
        return mediaDao.searchMedia(query)
    }

    suspend fun getMediaById(id: Long): MediaEntity? {
        return mediaDao.getMediaById(id)
    }

    suspend fun getMediaByPath(path: String): MediaEntity? {
        return mediaDao.getMediaByPath(path)
    }

    suspend fun updatePlaybackPosition(id: Long, position: Long) {
        mediaDao.updatePlaybackPosition(id, position)
    }

    suspend fun updatePositionOnly(id: Long, position: Long) {
        mediaDao.updatePositionOnly(id, position)
    }

    suspend fun toggleFavorite(id: Long, isFavorite: Boolean) {
        mediaDao.updateFavorite(id, isFavorite)
    }

    suspend fun scanAndSyncMedia() {
        val scannedMedia = mediaScanner.scanMedia()
        val existingMedia = mutableMapOf<Long, MediaEntity>()

        scannedMedia.forEach { scanned ->
            val existing = mediaDao.getMediaById(scanned.id)
            if (existing != null) {
                existingMedia[scanned.id] = scanned.copy(
                    lastPlayedAt = existing.lastPlayedAt,
                    lastPosition = existing.lastPosition,
                    playCount = existing.playCount,
                    isFavorite = existing.isFavorite
                )
            } else {
                existingMedia[scanned.id] = scanned
            }
        }

        mediaDao.insertAllMedia(existingMedia.values.toList())

        val validPaths = scannedMedia.map { it.path }
        mediaDao.deleteStaleMedia(validPaths)
    }
}
