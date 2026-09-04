package com.nextgen.player.subtitle

data class SubtitleCue(
    val startTimeMs: Long,
    val endTimeMs: Long,
    val text: String,
    val style: SubtitleStyle = SubtitleStyle()
)

data class SubtitleStyle(
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val fontName: String? = null,
    val fontSize: Float? = null,
    val primaryColor: Long? = null,
    val outlineColor: Long? = null,
    val shadowColor: Long? = null,
    val backgroundColor: Long? = null,
    val alignment: Int = 2,
    val outlineWidth: Float = 0f,
    val shadowEnabled: Boolean = false
)

data class SubtitleTrack(
    val name: String,
    val language: String?,
    val cues: List<SubtitleCue>,
    val format: SubtitleFormat,
    val source: SubtitleSource = SubtitleSource.EXTERNAL,
    val filePath: String? = null
)

enum class SubtitleFormat {
    SRT, ASS, SSA, SUB, VTT, PGS, UNKNOWN
}

enum class SubtitleSource {
    EMBEDDED, EXTERNAL, ONLINE
}

enum class SubtitleVerticalPosition {
    TOP, BOTTOM, CUSTOM
}

data class OnlineSubtitle(
    val id: String,
    val fileId: Int,
    val language: String,
    val languageName: String,
    val releaseName: String,
    val fileName: String,
    val downloadCount: Int,
    val ratings: Float,
    val fps: Float?,
    val hearingImpaired: Boolean,
    val fromTrusted: Boolean
) {
    val displayName: String
        get() = buildString {
            append(if (releaseName.isNotBlank()) releaseName else fileName.ifBlank { "Subtitle $fileId" })
            if (languageName.isNotBlank()) append(" - ").append(languageName)
        }
}

data class SubtitleSearchRequest(
    val videoPath: String,
    val fileName: String,
    val language: String,
    val movieHash: String? = null
)
