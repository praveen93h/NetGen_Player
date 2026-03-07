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
    val backgroundColor: Long? = null,
    val alignment: Int = 2
)

data class SubtitleTrack(
    val name: String,
    val language: String?,
    val cues: List<SubtitleCue>,
    val format: SubtitleFormat
)

enum class SubtitleFormat {
    SRT, ASS, SSA, SUB, UNKNOWN
}
