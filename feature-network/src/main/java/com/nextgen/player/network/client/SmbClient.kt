package com.nextgen.player.network.client

import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2CreateOptions
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.SmbConfig
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import com.nextgen.player.network.model.NetworkFile
import com.nextgen.player.network.model.ServerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.EnumSet
import java.util.concurrent.TimeUnit

class SmbClient {
    private var client: SMBClient? = null
    private var connection: Connection? = null
    private var session: Session? = null
    private var currentShare: DiskShare? = null
    private var currentShareName: String? = null

    suspend fun connect(config: ServerConfig) = withContext(Dispatchers.IO) {
        disconnect()
        val smbConfig = SmbConfig.builder()
            .withTimeout(15, TimeUnit.SECONDS)
            .withSoTimeout(15, TimeUnit.SECONDS)
            .build()
        client = SMBClient(smbConfig)
        connection = client!!.connect(config.host, config.port)
        val authContext = if (config.username.isNotEmpty()) {
            AuthenticationContext(config.username, config.password.toCharArray(), config.domain)
        } else {
            AuthenticationContext.anonymous()
        }
        session = connection!!.authenticate(authContext)
    }

    suspend fun listShares(): List<NetworkFile> = withContext(Dispatchers.IO) {
        val shares = mutableListOf<NetworkFile>()
        // List available shares
        session?.connectShare("IPC\$")?.use { ipcShare ->
            // Use transport to enumerate shares
        }
        // Fallback: try common share names
        val commonShares = listOf("Public", "Media", "Videos", "shared", "data")
        for (shareName in commonShares) {
            try {
                val share = session?.connectShare(shareName) as? DiskShare
                if (share != null) {
                    shares.add(NetworkFile(
                        name = shareName,
                        path = shareName,
                        isDirectory = true
                    ))
                    share.close()
                }
            } catch (_: Exception) { }
        }
        // If no common shares found, return a hint
        if (shares.isEmpty()) {
            // Try to list all by connecting
            try {
                val diskShares = session?.connectShare("") as? DiskShare
                diskShares?.close()
            } catch (_: Exception) { }
        }
        shares
    }

    suspend fun listFiles(shareName: String, path: String): List<NetworkFile> = withContext(Dispatchers.IO) {
        ensureShare(shareName)
        val share = currentShare ?: throw IllegalStateException("Not connected to share")
        val listing = share.list(path)
        listing.mapNotNull { item ->
            val name = item.fileName
            if (name == "." || name == "..") return@mapNotNull null
            val isDir = item.fileAttributes and FileAttributes.FILE_ATTRIBUTE_DIRECTORY.value != 0L
            val fullPath = if (path.isEmpty() || path == "\\") name else "$path\\$name"
            NetworkFile(
                name = name,
                path = fullPath,
                isDirectory = isDir,
                size = item.endOfFile,
                lastModified = item.lastWriteTime?.toEpochMillis() ?: 0
            )
        }.sortedWith(compareByDescending<NetworkFile> { it.isDirectory }.thenBy { it.name.lowercase() })
    }

    suspend fun openInputStream(shareName: String, filePath: String): InputStream = withContext(Dispatchers.IO) {
        ensureShare(shareName)
        val share = currentShare ?: throw IllegalStateException("Not connected to share")
        val file = share.openFile(
            filePath,
            EnumSet.of(AccessMask.GENERIC_READ),
            null,
            EnumSet.of(SMB2ShareAccess.FILE_SHARE_READ),
            SMB2CreateDisposition.FILE_OPEN,
            EnumSet.of(SMB2CreateOptions.FILE_NON_DIRECTORY_FILE)
        )
        file.inputStream
    }

    fun getStreamUri(config: ServerConfig, shareName: String, filePath: String): String {
        val user = if (config.username.isNotEmpty()) "${config.username}:${config.password}@" else ""
        return "smb://$user${config.host}/$shareName/$filePath"
    }

    private fun ensureShare(shareName: String) {
        if (currentShareName != shareName || currentShare == null) {
            currentShare?.close()
            currentShare = session?.connectShare(shareName) as? DiskShare
            currentShareName = shareName
        }
    }

    fun disconnect() {
        try { currentShare?.close() } catch (_: Exception) { }
        try { session?.close() } catch (_: Exception) { }
        try { connection?.close() } catch (_: Exception) { }
        try { client?.close() } catch (_: Exception) { }
        currentShare = null
        currentShareName = null
        session = null
        connection = null
        client = null
    }
}
