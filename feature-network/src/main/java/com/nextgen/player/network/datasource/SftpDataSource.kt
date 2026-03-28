package com.nextgen.player.network.datasource

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.nextgen.player.network.model.ServerConfig
import java.io.InputStream
import java.util.Properties

@OptIn(UnstableApi::class)
class SftpDataSource(
    private val config: ServerConfig
) : BaseDataSource(/* isNetwork= */ true) {

    private var session: Session? = null
    private var channel: ChannelSftp? = null
    private var inputStream: InputStream? = null
    private var uri: Uri? = null
    private var bytesRemaining: Long = C.LENGTH_UNSET.toLong()

    override fun open(dataSpec: DataSpec): Long {
        uri = dataSpec.uri
        val filePath = dataSpec.uri.path ?: "/"

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

        val fileSize = try { ch.lstat(filePath).size } catch (_: Exception) { -1L }

        if (dataSpec.position > 0) {
            inputStream = ch.get(filePath, null, dataSpec.position)
        } else {
            inputStream = ch.get(filePath)
        }

        bytesRemaining = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
            dataSpec.length
        } else if (fileSize > 0) {
            fileSize - dataSpec.position
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
        try { channel?.disconnect() } catch (_: Exception) { }
        try { session?.disconnect() } catch (_: Exception) { }
        inputStream = null
        channel = null
        session = null
        uri = null
        transferEnded()
    }
}
