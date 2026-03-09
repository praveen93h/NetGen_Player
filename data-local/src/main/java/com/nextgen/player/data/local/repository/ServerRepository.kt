package com.nextgen.player.data.local.repository

import com.nextgen.player.data.local.dao.ServerBookmarkDao
import com.nextgen.player.data.local.entity.ServerBookmarkEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerRepository @Inject constructor(
    private val serverBookmarkDao: ServerBookmarkDao
) {
    val bookmarks: Flow<List<ServerBookmarkEntity>> = serverBookmarkDao.getAll()

    suspend fun getById(id: Long): ServerBookmarkEntity? = serverBookmarkDao.getById(id)

    suspend fun addBookmark(bookmark: ServerBookmarkEntity): Long =
        serverBookmarkDao.insert(bookmark)

    suspend fun updateBookmark(bookmark: ServerBookmarkEntity) =
        serverBookmarkDao.update(bookmark)

    suspend fun deleteBookmark(bookmark: ServerBookmarkEntity) =
        serverBookmarkDao.delete(bookmark)

    suspend fun touchBookmark(id: Long) =
        serverBookmarkDao.updateLastAccessed(id, System.currentTimeMillis())
}
