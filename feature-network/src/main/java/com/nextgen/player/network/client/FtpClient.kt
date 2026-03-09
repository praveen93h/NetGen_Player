package com.nextgen.player.network.client

import com.nextgen.player.network.model.NetworkFile
import com.nextgen.player.network.model.ServerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply
import java.io.InputStream

class FtpClient {
    private var client: FTPClient? = null

    suspend fun connect(config: ServerConfig) = withContext(Dispatchers.IO) {
        disconnect()
        val ftp = FTPClient().apply {
            connectTimeout = 15_000
            defaultTimeout = 15_000
            dataTimeout = java.time.Duration.ofMillis(30_000)
        }
        ftp.connect(config.host, config.port)
        val reply = ftp.replyCode
        if (!FTPReply.isPositiveCompletion(reply)) {
            ftp.disconnect()
            throw Exception("FTP server refused connection: $reply")
        }
        if (config.username.isNotEmpty()) {
            val loggedIn = ftp.login(config.username, config.password)
            if (!loggedIn) throw Exception("FTP login failed")
        } else {
            ftp.login("anonymous", "")
        }
        ftp.enterLocalPassiveMode()
        ftp.setFileType(FTP.BINARY_FILE_TYPE)
        client = ftp
    }

    suspend fun listFiles(path: String): List<NetworkFile> = withContext(Dispatchers.IO) {
        val ftp = client ?: throw IllegalStateException("Not connected")
        val files = ftp.listFiles(path)
        files.mapNotNull { file ->
            val name = file.name
            if (name == "." || name == "..") return@mapNotNull null
            val fullPath = if (path.endsWith("/")) "$path$name" else "$path/$name"
            NetworkFile(
                name = name,
                path = fullPath,
                isDirectory = file.isDirectory,
                size = file.size,
                lastModified = file.timestamp?.timeInMillis ?: 0
            )
        }.sortedWith(compareByDescending<NetworkFile> { it.isDirectory }.thenBy { it.name.lowercase() })
    }

    suspend fun openInputStream(filePath: String): InputStream = withContext(Dispatchers.IO) {
        val ftp = client ?: throw IllegalStateException("Not connected")
        ftp.retrieveFileStream(filePath)
            ?: throw Exception("Failed to open file: ${ftp.replyString}")
    }

    fun getStreamUri(config: ServerConfig, filePath: String): String {
        val credentials = if (config.username.isNotEmpty()) {
            "${config.username}:${config.password}@"
        } else ""
        return "ftp://$credentials${config.host}:${config.port}$filePath"
    }

    fun disconnect() {
        try {
            client?.let {
                if (it.isConnected) {
                    it.logout()
                    it.disconnect()
                }
            }
        } catch (_: Exception) { }
        client = null
    }
}
