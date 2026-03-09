package com.nextgen.player.network.client

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.nextgen.player.network.model.NetworkFile
import com.nextgen.player.network.model.ServerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.Properties

class SftpClient {
    private var session: Session? = null
    private var channel: ChannelSftp? = null

    suspend fun connect(config: ServerConfig) = withContext(Dispatchers.IO) {
        disconnect()
        val jsch = JSch()
        val sess = jsch.getSession(config.username, config.host, config.port)
        sess.setPassword(config.password)
        val props = Properties()
        props["StrictHostKeyChecking"] = "no"
        sess.setConfig(props)
        sess.timeout = 15_000
        sess.connect()
        val ch = sess.openChannel("sftp") as ChannelSftp
        ch.connect(15_000)
        session = sess
        channel = ch
    }

    suspend fun listFiles(path: String): List<NetworkFile> = withContext(Dispatchers.IO) {
        val sftp = channel ?: throw IllegalStateException("Not connected")
        val entries = sftp.ls(path)
        entries.mapNotNull { entry ->
            val item = entry as ChannelSftp.LsEntry
            val name = item.filename
            if (name == "." || name == "..") return@mapNotNull null
            val fullPath = if (path.endsWith("/")) "$path$name" else "$path/$name"
            NetworkFile(
                name = name,
                path = fullPath,
                isDirectory = item.attrs.isDir,
                size = item.attrs.size,
                lastModified = item.attrs.mTime.toLong() * 1000
            )
        }.sortedWith(compareByDescending<NetworkFile> { it.isDirectory }.thenBy { it.name.lowercase() })
    }

    suspend fun openInputStream(filePath: String): InputStream = withContext(Dispatchers.IO) {
        val sftp = channel ?: throw IllegalStateException("Not connected")
        sftp.get(filePath)
    }

    fun disconnect() {
        try { channel?.disconnect() } catch (_: Exception) { }
        try { session?.disconnect() } catch (_: Exception) { }
        channel = null
        session = null
    }
}
