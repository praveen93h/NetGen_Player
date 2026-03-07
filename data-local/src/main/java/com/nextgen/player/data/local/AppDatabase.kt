package com.nextgen.player.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nextgen.player.data.local.dao.MediaDao
import com.nextgen.player.data.local.entity.MediaEntity

@Database(
    entities = [MediaEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao

    companion object {
        const val DATABASE_NAME = "nextgen_player_db"
    }
}
