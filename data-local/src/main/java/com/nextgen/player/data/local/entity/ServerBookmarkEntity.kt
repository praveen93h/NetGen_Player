package com.nextgen.player.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "server_bookmarks")
data class ServerBookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String, // SMB, FTP, SFTP, WEBDAV
    val host: String,
    val port: Int,
    val path: String = "/",
    val username: String = "",
    val password: String = "",
    val domain: String = "", // SMB domain/workgroup
    val lastAccessed: Long = 0L
)
