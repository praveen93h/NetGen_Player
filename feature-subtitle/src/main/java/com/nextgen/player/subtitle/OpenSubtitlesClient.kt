package com.nextgen.player.subtitle

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream
import java.util.zip.ZipInputStream

class OpenSubtitlesClient(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val API_BASE = "https://api.opensubtitles.com/api/v1"
        private val JSON = "application/json".toMediaType()
        private const val USER_AGENT = "NextGenMediaPlayer v1.6.0"
    }

    suspend fun search(
        apiKey: String,
        request: SubtitleSearchRequest,
        limit: Int = 20
    ): Result<List<OnlineSubtitle>> = runCatching {
        require(apiKey.isNotBlank()) { "OpenSubtitles API key is required" }

        val params = mutableListOf(
            "languages" to request.language.lowercase(Locale.US),
            "query" to request.fileName,
            "order_by" to "download_count",
            "order_direction" to "desc"
        )
        request.movieHash?.takeIf { it.isNotBlank() }?.let { params.add("moviehash" to it) }

        val query = params.joinToString("&") { (key, value) ->
            "${key}=${URLEncoder.encode(value, "UTF-8")}"
        }
        val url = "$API_BASE/subtitles?$query"
        val response = httpClient.newCall(
            Request.Builder()
                .url(url)
                .header("Api-Key", apiKey)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .build()
        ).execute()

        val root = response.readJsonOrThrow("search")
        val data = root.optJSONArray("data") ?: return@runCatching emptyList()
        buildList {
            for (i in 0 until data.length().coerceAtMost(limit)) {
                val item = data.optJSONObject(i) ?: continue
                val attributes = item.optJSONObject("attributes") ?: continue
                val files = attributes.optJSONArray("files") ?: continue
                val firstFile = files.optJSONObject(0) ?: continue
                val fileId = firstFile.optInt("file_id", -1)
                if (fileId <= 0) continue

                add(
                    OnlineSubtitle(
                        id = item.optString("id", fileId.toString()),
                        fileId = fileId,
                        language = attributes.optString("language", request.language),
                        languageName = attributes.optString("language_name", ""),
                        releaseName = attributes.optString("release", ""),
                        fileName = firstFile.optString("file_name", ""),
                        downloadCount = attributes.optInt("download_count", 0),
                        ratings = attributes.optDouble("ratings", 0.0).toFloat(),
                        fps = attributes.optDouble("fps", -1.0).takeIf { it > 0 }?.toFloat(),
                        hearingImpaired = attributes.optBoolean("hearing_impaired", false),
                        fromTrusted = attributes.optBoolean("from_trusted", false)
                    )
                )
            }
        }
    }

    suspend fun download(apiKey: String, fileId: Int): Result<ByteArray> = runCatching {
        require(apiKey.isNotBlank()) { "OpenSubtitles API key is required" }
        val body = JSONObject()
            .put("file_id", fileId)
            .put("sub_format", "srt")
            .toString()
            .toRequestBody(JSON)

        val response = httpClient.newCall(
            Request.Builder()
                .url("$API_BASE/download")
                .post(body)
                .header("Api-Key", apiKey)
                .header("User-Agent", USER_AGENT)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .build()
        ).execute()

        val link = response.readJsonOrThrow("download").optString("link")
        require(link.isNotBlank()) { "OpenSubtitles did not return a download link" }

        val fileResponse = httpClient.newCall(
            Request.Builder()
                .url(link)
                .header("Api-Key", apiKey)
                .header("User-Agent", USER_AGENT)
                .build()
        ).execute()
        if (!fileResponse.isSuccessful) {
            error("Subtitle file download failed (${fileResponse.code}): ${fileResponse.safeBodyPreview()}")
        }
        unpackSubtitleBytes(fileResponse.body?.bytes() ?: ByteArray(0))
    }

    private fun unpackSubtitleBytes(bytes: ByteArray): ByteArray {
        if (bytes.size < 2) return bytes
        return when {
            bytes[0] == 0x1f.toByte() && bytes[1] == 0x8b.toByte() -> {
                GZIPInputStream(ByteArrayInputStream(bytes)).readAllBytesCompat()
            }
            bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte() -> {
                ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory) return zip.readAllBytesCompat()
                        entry = zip.nextEntry
                    }
                }
                bytes
            }
            else -> bytes
        }
    }

    private fun Response.readJsonOrThrow(action: String): JSONObject {
        use { response ->
            val bodyText = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                error("OpenSubtitles $action failed (${response.code}): ${bodyText.toReadableApiMessage()}")
            }
            return try {
                JSONObject(bodyText)
            } catch (_: JSONException) {
                val contentType = response.header("Content-Type").orEmpty()
                error(
                    "OpenSubtitles returned ${contentType.ifBlank { "a non-JSON response" }}. " +
                        bodyText.toReadableApiMessage()
                )
            }
        }
    }

    private fun Response.safeBodyPreview(): String {
        return runCatching { body?.string().orEmpty().toReadableApiMessage() }
            .getOrDefault("No error details returned")
    }

    private fun String.toReadableApiMessage(): String {
        val text = trim()
        if (text.isBlank()) return "No error details returned"
        if (text.startsWith("<", ignoreCase = true) || text.contains("<html", ignoreCase = true)) {
            return "The API returned an HTML page instead of JSON. Check that the API key is active for API Consumers and try again later."
        }
        return text.take(300)
    }
}

object SubtitleFileMatcher {
    private const val HASH_CHUNK_SIZE = 64 * 1024

    fun buildSearchRequest(videoPath: String, language: String): SubtitleSearchRequest {
        val file = File(videoPath)
        return SubtitleSearchRequest(
            videoPath = videoPath,
            fileName = file.name.ifBlank { videoPath.substringAfterLast('/') },
            language = language,
            movieHash = computeOpenSubtitlesHash(file)
        )
    }

    fun computeOpenSubtitlesHash(file: File): String? {
        if (!file.isFile || file.length() < HASH_CHUNK_SIZE * 2L) return null
        return runCatching {
            file.inputStream().use { input ->
                var hash = file.length()
                hash += input.readChunkLongSum(HASH_CHUNK_SIZE)
                file.inputStream().use { tailInput ->
                    tailInput.skip((file.length() - HASH_CHUNK_SIZE).coerceAtLeast(0L))
                    hash += tailInput.readChunkLongSum(HASH_CHUNK_SIZE)
                }
                java.lang.Long.toHexString(hash).padStart(16, '0').takeLast(16)
            }
        }.getOrNull()
    }

    fun targetSubtitleFile(videoPath: String, language: String): File {
        val video = File(videoPath)
        val baseName = video.name.substringBeforeLast('.', video.name)
        val parent = video.parentFile ?: File(".")
        return File(parent, "$baseName.$language.srt")
    }

    private fun InputStream.readChunkLongSum(bytesToRead: Int): Long {
        val buffer = ByteArray(8)
        var total = 0L
        var readBytes = 0
        while (readBytes < bytesToRead) {
            val count = read(buffer)
            if (count < 8) break
            var value = 0L
            for (i in 0 until 8) {
                value = value or ((buffer[i].toLong() and 0xffL) shl (8 * i))
            }
            total += value
            readBytes += count
        }
        return total
    }
}

private fun InputStream.readAllBytesCompat(): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    while (true) {
        val read = read(buffer)
        if (read <= 0) break
        output.write(buffer, 0, read)
    }
    return output.toByteArray()
}
