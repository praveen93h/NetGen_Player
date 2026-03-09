package com.nextgen.player.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nextgen.player.data.local.dao.MediaDao
import com.nextgen.player.data.local.dao.ServerBookmarkDao
import com.nextgen.player.data.local.entity.MediaEntity
import com.nextgen.player.data.local.entity.ServerBookmarkEntity

@Database(
    entities = [MediaEntity::class, ServerBookmarkEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao
    abstract fun serverBookmarkDao(): ServerBookmarkDao

    companion object {
        const val DATABASE_NAME = "nextgen_player_db"
    }
}
