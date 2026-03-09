package com.nextgen.player.data.local.dao

import androidx.room.*
import com.nextgen.player.data.local.entity.ServerBookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerBookmarkDao {

    @Query("SELECT * FROM server_bookmarks ORDER BY lastAccessed DESC")
    fun getAll(): Flow<List<ServerBookmarkEntity>>

    @Query("SELECT * FROM server_bookmarks WHERE id = :id")
    suspend fun getById(id: Long): ServerBookmarkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: ServerBookmarkEntity): Long

    @Update
    suspend fun update(bookmark: ServerBookmarkEntity)

    @Delete
    suspend fun delete(bookmark: ServerBookmarkEntity)

    @Query("UPDATE server_bookmarks SET lastAccessed = :timestamp WHERE id = :id")
    suspend fun updateLastAccessed(id: Long, timestamp: Long)
}
