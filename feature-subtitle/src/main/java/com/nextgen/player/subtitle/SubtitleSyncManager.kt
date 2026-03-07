package com.nextgen.player.subtitle

class SubtitleSyncManager {
    private var offsetMs: Long = 0L

    val currentOffsetMs: Long get() = offsetMs

    fun adjustOffset(deltaMs: Long) {
        offsetMs += deltaMs
    }

    fun setOffset(ms: Long) {
        offsetMs = ms
    }

    fun resetOffset() {
        offsetMs = 0L
    }

    fun incrementOffset(stepMs: Long = 50L) {
        offsetMs += stepMs
    }

    fun decrementOffset(stepMs: Long = 50L) {
        offsetMs -= stepMs
    }

    val formattedOffset: String
        get() {
            val sign = if (offsetMs >= 0) "+" else "-"
            val absMs = kotlin.math.abs(offsetMs)
            val seconds = absMs / 1000
            val millis = absMs % 1000
            return "${sign}${seconds}.${(millis / 100)}s"
        }
}
