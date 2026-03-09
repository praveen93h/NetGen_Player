package com.nextgen.player.network.model

enum class ServerType(val label: String, val defaultPort: Int) {
    SMB("SMB", 445),
    FTP("FTP", 21),
    SFTP("SFTP", 22),
    WEBDAV("WebDAV", 80);

    companion object {
        fun fromString(value: String): ServerType =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: SMB
    }
}

data class ServerConfig(
    val id: Long = 0,
    val name: String,
    val type: ServerType,
    val host: String,
    val port: Int = type.defaultPort,
    val path: String = "/",
    val username: String = "",
    val password: String = "",
    val domain: String = ""
)

data class NetworkFile(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long = 0L,
    val lastModified: Long = 0L,
    val mimeType: String? = null
) {
    val isVideo: Boolean
        get() {
            val ext = name.substringAfterLast('.', "").lowercase()
            return ext in VIDEO_EXTENSIONS
        }

    val isMedia: Boolean
        get() {
            val ext = name.substringAfterLast('.', "").lowercase()
            return ext in VIDEO_EXTENSIONS || ext in AUDIO_EXTENSIONS || ext in PLAYLIST_EXTENSIONS
        }

    val formattedSize: String
        get() {
            val kb = size / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format("%.1f GB", gb)
                mb >= 1.0 -> String.format("%.1f MB", mb)
                kb >= 1.0 -> String.format("%.0f KB", kb)
                else -> "$size B"
            }
        }

    companion object {
        val VIDEO_EXTENSIONS = setOf(
            "mp4", "mkv", "avi", "mov", "flv", "webm", "ts", "m4v",
            "wmv", "mpg", "mpeg", "3gp", "ogv", "divx", "vob"
        )
        val AUDIO_EXTENSIONS = setOf(
            "mp3", "aac", "flac", "ogg", "wav", "m4a", "opus", "wma"
        )
        val PLAYLIST_EXTENSIONS = setOf("m3u", "m3u8", "pls")
    }
}

data class DlnaDevice(
    val friendlyName: String,
    val location: String,
    val udn: String,
    val iconUrl: String? = null
)

data class StreamInfo(
    val url: String,
    val title: String = "",
    val isHls: Boolean = false,
    val isDash: Boolean = false
) {
    companion object {
        fun detect(url: String, title: String = ""): StreamInfo {
            val lower = url.lowercase()
            return StreamInfo(
                url = url,
                title = title.ifEmpty { url.substringAfterLast('/').substringBefore('?') },
                isHls = lower.contains(".m3u8") || lower.contains("format=m3u8"),
                isDash = lower.contains(".mpd")
            )
        }
    }
}
