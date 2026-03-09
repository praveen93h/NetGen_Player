package com.nextgen.player.network.datasource

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2CreateOptions
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.SmbConfig
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.share.DiskShare
import com.hierynomus.smbj.share.File
import java.io.InputStream
import java.util.EnumSet
import java.util.concurrent.TimeUnit

@OptIn(UnstableApi::class)
class SmbDataSource(
    private val host: String,
    private val shareName: String,
    private val username: String = "",
    private val password: String = "",
    private val domain: String = "",
    private val port: Int = 445
) : BaseDataSource(/* isNetwork= */ true) {

    private var client: SMBClient? = null
    private var share: DiskShare? = null
    private var file: File? = null
    private var inputStream: InputStream? = null
    private var uri: Uri? = null
    private var bytesRemaining: Long = 0

    override fun open(dataSpec: DataSpec): Long {
        uri = dataSpec.uri
        val filePath = dataSpec.uri.path?.removePrefix("/") ?: ""

        val config = SmbConfig.builder()
            .withTimeout(15, TimeUnit.SECONDS)
            .withSoTimeout(15, TimeUnit.SECONDS)
            .build()
        client = SMBClient(config)
        val connection = client!!.connect(host, port)
        val auth = if (username.isNotEmpty()) {
            AuthenticationContext(username, password.toCharArray(), domain)
        } else {
            AuthenticationContext.anonymous()
        }
        val session = connection.authenticate(auth)
        share = session.connectShare(shareName) as DiskShare

        file = share!!.openFile(
            filePath,
            EnumSet.of(AccessMask.GENERIC_READ),
            null,
            EnumSet.of(SMB2ShareAccess.FILE_SHARE_READ),
            SMB2CreateDisposition.FILE_OPEN,
            EnumSet.of(SMB2CreateOptions.FILE_NON_DIRECTORY_FILE)
        )

        val fileLength = file!!.fileInformation.standardInformation.endOfFile
        val position = dataSpec.position
        inputStream = file!!.inputStream

        if (position > 0) {
            inputStream!!.skip(position)
        }

        bytesRemaining = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
            dataSpec.length
        } else {
            fileLength - position
        }

        transferStarted(dataSpec)
        return bytesRemaining
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT
        val toRead = minOf(length.toLong(), bytesRemaining).toInt()
        val bytesRead = inputStream?.read(buffer, offset, toRead) ?: C.RESULT_END_OF_INPUT
        if (bytesRead == -1) return C.RESULT_END_OF_INPUT
        bytesRemaining -= bytesRead
        bytesTransferred(bytesRead)
        return bytesRead
    }

    override fun getUri(): Uri? = uri

    override fun close() {
        try { inputStream?.close() } catch (_: Exception) { }
        try { file?.close() } catch (_: Exception) { }
        try { share?.close() } catch (_: Exception) { }
        try { client?.close() } catch (_: Exception) { }
        inputStream = null
        file = null
        share = null
        client = null
        uri = null
        transferEnded()
    }
}
