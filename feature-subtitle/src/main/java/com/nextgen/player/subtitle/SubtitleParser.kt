package com.nextgen.player.subtitle

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.regex.Pattern

object SubtitleParser {

    fun parse(inputStream: InputStream, format: SubtitleFormat, fps: Double = 23.976): SubtitleTrack {
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        val content = reader.readText()
        reader.close()

        val cues = when (format) {
            SubtitleFormat.SRT -> parseSrt(content)
            SubtitleFormat.ASS, SubtitleFormat.SSA -> parseAss(content)
            SubtitleFormat.SUB -> parseSub(content, fps)
            SubtitleFormat.UNKNOWN -> parseSrt(content)
        }

        return SubtitleTrack(
            name = "External",
            language = null,
            cues = cues,
            format = format
        )
    }

    fun detectFormat(fileName: String): SubtitleFormat {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "srt" -> SubtitleFormat.SRT
            "ass" -> SubtitleFormat.ASS
            "ssa" -> SubtitleFormat.SSA
            "sub" -> SubtitleFormat.SUB
            else -> SubtitleFormat.UNKNOWN
        }
    }

    private val SRT_TIMESTAMP_PATTERN = Pattern.compile(
        "(\\d{2}):(\\d{2}):(\\d{2})[,.]?(\\d{0,3})\\s*-->\\s*(\\d{2}):(\\d{2}):(\\d{2})[,.]?(\\d{0,3})"
    )

    private fun parseSrt(content: String): List<SubtitleCue> {
        val cues = mutableListOf<SubtitleCue>()
        val blocks = content.trim().split(Regex("(?:\\r?\\n){2,}"))

        for (block in blocks) {
            val lines = block.trim().split(Regex("\\r?\\n"))
            if (lines.size < 2) continue

            var timestampIdx = -1
            for (i in lines.indices) {
                if (SRT_TIMESTAMP_PATTERN.matcher(lines[i].trim()).find()) {
                    timestampIdx = i
                    break
                }
            }
            if (timestampIdx == -1) continue

            val matcher = SRT_TIMESTAMP_PATTERN.matcher(lines[timestampIdx].trim())
            if (!matcher.find()) continue

            val startMs = timeToMs(
                matcher.group(1)!!.toInt(),
                matcher.group(2)!!.toInt(),
                matcher.group(3)!!.toInt(),
                matcher.group(4)?.padEnd(3, '0')?.toIntOrNull() ?: 0
            )

            val endMs = timeToMs(
                matcher.group(5)!!.toInt(),
                matcher.group(6)!!.toInt(),
                matcher.group(7)!!.toInt(),
                matcher.group(8)?.padEnd(3, '0')?.toIntOrNull() ?: 0
            )

            val text = lines.subList(timestampIdx + 1, lines.size)
                .joinToString("\n")
                .replace(Regex("<[^>]*>"), "")
                .trim()

            if (text.isNotEmpty()) {
                cues.add(SubtitleCue(startTimeMs = startMs, endTimeMs = endMs, text = text))
            }
        }

        return cues.sortedBy { it.startTimeMs }
    }

    private fun parseAss(content: String): List<SubtitleCue> {
        val cues = mutableListOf<SubtitleCue>()
        val lines = content.split(Regex("\\r?\\n"))

        var inEvents = false
        var formatFields = listOf<String>()

        for (line in lines) {
            val trimmed = line.trim()

            if (trimmed.equals("[Events]", ignoreCase = true)) {
                inEvents = true
                continue
            }
            if (trimmed.startsWith("[") && !trimmed.equals("[Events]", ignoreCase = true)) {
                inEvents = false
                continue
            }

            if (!inEvents) continue

            if (trimmed.startsWith("Format:", ignoreCase = true)) {
                formatFields = trimmed.substringAfter(":").split(",").map { it.trim().lowercase() }
                continue
            }

            if (trimmed.startsWith("Dialogue:", ignoreCase = true)) {
                val values = trimmed.substringAfter(":").trim()
                val parts = values.split(",", limit = formatFields.size.coerceAtLeast(10))

                val startIdx = formatFields.indexOf("start")
                val endIdx = formatFields.indexOf("end")
                val textIdx = formatFields.indexOf("text")

                if (startIdx >= 0 && endIdx >= 0 && textIdx >= 0 && parts.size > textIdx) {
                    val startMs = parseAssTimestamp(parts[startIdx].trim())
                    val endMs = parseAssTimestamp(parts[endIdx].trim())
                    val text = parts.subList(textIdx, parts.size).joinToString(",")
                        .replace(Regex("\\{[^}]*\\}"), "")
                        .replace("\\N", "\n")
                        .replace("\\n", "\n")
                        .trim()

                    if (text.isNotEmpty()) {
                        cues.add(SubtitleCue(startTimeMs = startMs, endTimeMs = endMs, text = text))
                    }
                }
            }
        }

        return cues.sortedBy { it.startTimeMs }
    }

    private val ASS_TIMESTAMP_PATTERN = Pattern.compile("(\\d+):(\\d{2}):(\\d{2})[.](\\d{2})")

    private fun parseAssTimestamp(time: String): Long {
        val matcher = ASS_TIMESTAMP_PATTERN.matcher(time)
        if (!matcher.find()) return 0L
        val hours = matcher.group(1)!!.toInt()
        val minutes = matcher.group(2)!!.toInt()
        val seconds = matcher.group(3)!!.toInt()
        val centiseconds = matcher.group(4)!!.toInt()
        return (hours * 3600000L) + (minutes * 60000L) + (seconds * 1000L) + (centiseconds * 10L)
    }

    private fun parseSub(content: String, defaultFps: Double = 23.976): List<SubtitleCue> {
        val cues = mutableListOf<SubtitleCue>()
        val lines = content.split(Regex("\\r?\\n"))

        var fps = defaultFps
        val fpsLine = lines.firstOrNull()?.trim() ?: ""
        val fpsMatcher = Pattern.compile("\\{\\d+\\}\\{\\d+\\}([\\d.]+)").matcher(fpsLine)
        if (fpsMatcher.find()) {
            fpsMatcher.group(1)?.toDoubleOrNull()?.let { detectedFps ->
                if (detectedFps in 10.0..120.0) fps = detectedFps
            }
        }

        for (line in lines) {
            val trimmed = line.trim()
            val matcher = Pattern.compile("\\{(\\d+)\\}\\{(\\d+)\\}(.+)").matcher(trimmed)
            if (matcher.find()) {
                val startFrame = matcher.group(1)!!.toInt()
                val endFrame = matcher.group(2)!!.toInt()
                val text = matcher.group(3)!!
                    .replace("|", "\n")
                    .replace(Regex("\\{[^}]*\\}"), "")
                    .trim()

                val startMs = (startFrame / fps * 1000).toLong()
                val endMs = (endFrame / fps * 1000).toLong()

                if (text.isNotEmpty()) {
                    cues.add(SubtitleCue(startTimeMs = startMs, endTimeMs = endMs, text = text))
                }
            }
        }

        return cues.sortedBy { it.startTimeMs }
    }

    private fun timeToMs(hours: Int, minutes: Int, seconds: Int, millis: Int): Long {
        return (hours * 3600000L) + (minutes * 60000L) + (seconds * 1000L) + millis
    }
}
