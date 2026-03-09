package com.nextgen.player.network.parser

import com.nextgen.player.network.model.StreamInfo

object M3uParser {

    fun parse(content: String, baseUrl: String = ""): List<StreamInfo> {
        val lines = content.lines().map { it.trim() }
        val items = mutableListOf<StreamInfo>()
        var currentTitle = ""

        for (i in lines.indices) {
            val line = lines[i]
            when {
                line.isEmpty() || line.startsWith("#EXTM3U") -> continue
                line.startsWith("#EXTINF:") -> {
                    // Format: #EXTINF:duration,title
                    currentTitle = line.substringAfter(",", "").trim()
                }
                line.startsWith("#") -> continue // Skip other comments
                else -> {
                    val url = resolveUrl(line, baseUrl)
                    if (url.isNotEmpty()) {
                        val title = currentTitle.ifEmpty {
                            url.substringAfterLast('/').substringBefore('?')
                        }
                        items.add(StreamInfo.detect(url, title))
                    }
                    currentTitle = ""
                }
            }
        }
        return items
    }

    fun isM3uContent(content: String): Boolean {
        val trimmed = content.trim()
        return trimmed.startsWith("#EXTM3U") || trimmed.startsWith("#EXTINF")
    }

    fun isM3uUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.endsWith(".m3u") || lower.endsWith(".m3u8") ||
                lower.endsWith(".pls") || lower.contains("format=m3u")
    }

    private fun resolveUrl(url: String, baseUrl: String): String {
        if (url.startsWith("http://") || url.startsWith("https://") ||
            url.startsWith("rtsp://") || url.startsWith("rtp://")) {
            return url
        }
        if (baseUrl.isEmpty()) return url
        val base = baseUrl.substringBeforeLast('/')
        return "$base/$url"
    }
}
