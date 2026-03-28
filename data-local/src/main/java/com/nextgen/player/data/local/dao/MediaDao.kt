package com.nextgen.player.data.local.dao

import androidx.room.*
import com.nextgen.player.data.local.entity.MediaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {

    @Query("SELECT * FROM media_items ORDER BY title ASC")
    fun getAllMedia(): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_items ORDER BY dateAdded DESC")
    fun getAllMediaByDate(): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_items ORDER BY size DESC")
    fun getAllMediaBySize(): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_items ORDER BY duration DESC")
    fun getAllMediaByDuration(): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_items WHERE lastPlayedAt > 0 ORDER BY lastPlayedAt DESC LIMIT :limit")
    fun getRecentlyPlayed(limit: Int = 20): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_items WHERE lastPosition > 0 AND duration > 0 AND CAST(lastPosition AS REAL) / duration < 0.95 AND CAST(lastPosition AS REAL) / duration > 0.01 ORDER BY lastPlayedAt DESC LIMIT :limit")
    fun getContinueWatching(limit: Int = 10): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_items WHERE isFavorite = 1 ORDER BY title ASC")
    fun getFavorites(): Flow<List<MediaEntity>>

    @Query("SELECT folderPath, folderName, COUNT(*) AS videoCount FROM media_items GROUP BY folderPath, folderName ORDER BY folderName ASC")
    fun getFolders(): Flow<List<FolderInfo>>

    @Query("SELECT * FROM media_items WHERE folderPath = :folderPath ORDER BY title ASC")
    fun getMediaInFolder(folderPath: String): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_items WHERE title LIKE '%' || :query || '%' OR displayName LIKE '%' || :query || '%'")
    fun searchMedia(query: String): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_items WHERE id = :id")
    suspend fun getMediaById(id: Long): MediaEntity?

    @Query("SELECT * FROM media_items WHERE id IN (:ids)")
    suspend fun getMediaByIds(ids: List<Long>): List<MediaEntity>

    @Query("SELECT * FROM media_items WHERE path = :path")
    suspend fun getMediaByPath(path: String): MediaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(media: MediaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllMedia(mediaList: List<MediaEntity>)

    @Update
    suspend fun updateMedia(media: MediaEntity)

    @Query("UPDATE media_items SET lastPosition = :position, lastPlayedAt = :timestamp, playCount = playCount + 1 WHERE id = :id")
    suspend fun updatePlaybackPosition(id: Long, position: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE media_items SET lastPosition = :position WHERE id = :id")
    suspend fun updatePositionOnly(id: Long, position: Long)

    @Query("UPDATE media_items SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Delete
    suspend fun deleteMedia(media: MediaEntity)

    @Query("DELETE FROM media_items WHERE id IN (:ids)")
    suspend fun deleteMediaByIds(ids: List<Long>)

    @Query("UPDATE media_items SET lastPlayedAt = 0, lastPosition = 0, playCount = 0 WHERE lastPlayedAt > 0")
    suspend fun clearWatchHistory()

    @Query("DELETE FROM media_items WHERE path NOT IN (:validPaths)")
    suspend fun deleteStaleMedia(validPaths: List<String>)

    @Query("SELECT COUNT(*) FROM media_items")
    suspend fun getMediaCount(): Int
}

data class FolderInfo(
    val folderPath: String,
    val folderName: String,
    val videoCount: Int = 0
)
