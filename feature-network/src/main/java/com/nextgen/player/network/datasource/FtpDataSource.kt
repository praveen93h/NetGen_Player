package com.nextgen.player.network.datasource

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import com.nextgen.player.network.model.ServerConfig
import com.nextgen.player.network.model.ServerType
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import java.io.InputStream

@OptIn(UnstableApi::class)
class FtpDataSource(
    private val config: ServerConfig
) : BaseDataSource(/* isNetwork= */ true) {

    private var ftpClient: FTPClient? = null
    private var inputStream: InputStream? = null
    private var uri: Uri? = null
    private var bytesRemaining: Long = C.LENGTH_UNSET.toLong()

    override fun open(dataSpec: DataSpec): Long {
        uri = dataSpec.uri
        val filePath = dataSpec.uri.path ?: "/"

        val ftp = FTPClient().apply {
            connectTimeout = 15_000
            defaultTimeout = 15_000
        }
        ftp.connect(config.host, config.port)
        if (config.username.isNotEmpty()) {
            ftp.login(config.username, config.password)
        } else {
            ftp.login("anonymous", "")
        }
        ftp.enterLocalPassiveMode()
        ftp.setFileType(FTP.BINARY_FILE_TYPE)

        // Try to get file size for progress
        val size = ftp.getSize(filePath)

        if (dataSpec.position > 0) {
            ftp.setRestartOffset(dataSpec.position)
        }

        inputStream = ftp.retrieveFileStream(filePath)
            ?: throw Exception("Failed to open FTP file: ${ftp.replyString}")

        ftpClient = ftp

        bytesRemaining = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
            dataSpec.length
        } else if (size != null) {
            size.toLong() - dataSpec.position
        } else {
            C.LENGTH_UNSET.toLong()
        }

        transferStarted(dataSpec)
        return bytesRemaining
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT
        val toRead = if (bytesRemaining != C.LENGTH_UNSET.toLong()) {
            minOf(length.toLong(), bytesRemaining).toInt()
        } else length
        val bytesRead = inputStream?.read(buffer, offset, toRead) ?: C.RESULT_END_OF_INPUT
        if (bytesRead == -1) return C.RESULT_END_OF_INPUT
        if (bytesRemaining != C.LENGTH_UNSET.toLong()) bytesRemaining -= bytesRead
        bytesTransferred(bytesRead)
        return bytesRead
    }

    override fun getUri(): Uri? = uri

    override fun close() {
        try { inputStream?.close() } catch (_: Exception) { }
        try {
            ftpClient?.let {
                if (it.isConnected) {
                    it.completePendingCommand()
                    it.logout()
                    it.disconnect()
                }
            }
        } catch (_: Exception) { }
        inputStream = null
        ftpClient = null
        uri = null
        transferEnded()
    }

    private fun FTPClient.getSize(path: String): Long? {
        return try {
            sendCommand("SIZE", path)
            if (replyCode == 213) {
                replyString.trim().substringAfterLast(' ').toLongOrNull()
            } else {
                // Fallback: try MLST or listFiles to get size
                val files = listFiles(path)
                files.firstOrNull()?.size
            }
        } catch (_: Exception) {
            null
        }
    }
}
