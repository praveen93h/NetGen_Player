package com.nextgen.player.network.client

import com.nextgen.player.network.model.NetworkFile
import com.nextgen.player.network.model.ServerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.io.StringReader
import java.util.concurrent.TimeUnit

class WebDavClient {
    private var client: OkHttpClient? = null
    private var config: ServerConfig? = null

    suspend fun connect(config: ServerConfig) = withContext(Dispatchers.IO) {
        this@WebDavClient.config = config
        val builder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
        if (config.username.isNotEmpty()) {
            builder.authenticator { _, response ->
                response.request.newBuilder()
                    .header("Authorization", Credentials.basic(config.username, config.password))
                    .build()
            }
        }
        client = builder.build()
        // Test connection
        val url = buildUrl(config, config.path)
        val request = Request.Builder().url(url).method("PROPFIND", propfindBody())
            .header("Depth", "0")
            .header("Content-Type", "application/xml")
            .build()
        val response = client!!.newCall(request).execute()
        if (!response.isSuccessful && response.code != 207) {
            throw Exception("WebDAV connection failed: ${response.code}")
        }
        response.close()
    }

    suspend fun listFiles(path: String): List<NetworkFile> = withContext(Dispatchers.IO) {
        val cfg = config ?: throw IllegalStateException("Not connected")
        val http = client ?: throw IllegalStateException("Not connected")
        val url = buildUrl(cfg, path)
        val request = Request.Builder().url(url)
            .method("PROPFIND", propfindBody())
            .header("Depth", "1")
            .header("Content-Type", "application/xml")
            .build()
        val response = http.newCall(request).execute()
        val body = response.body?.string() ?: ""
        response.close()
        parseMultiStatus(body, path)
    }

    fun getStreamUrl(path: String): String {
        val cfg = config ?: throw IllegalStateException("Not connected")
        return buildUrl(cfg, path)
    }

    fun disconnect() {
        client = null
        config = null
    }

    private fun buildUrl(config: ServerConfig, path: String): String {
        val scheme = if (config.port == 443) "https" else "http"
        val portSuffix = if (config.port == 80 || config.port == 443) "" else ":${config.port}"
        val cleanPath = if (path.startsWith("/")) path else "/$path"
        return "$scheme://${config.host}$portSuffix$cleanPath"
    }

    private fun propfindBody() =
        """<?xml version="1.0" encoding="utf-8"?>
<D:propfind xmlns:D="DAV:">
  <D:prop>
    <D:displayname/>
    <D:getcontentlength/>
    <D:getlastmodified/>
    <D:resourcetype/>
    <D:getcontenttype/>
  </D:prop>
</D:propfind>""".toRequestBody()

    private fun parseMultiStatus(xml: String, currentPath: String): List<NetworkFile> {
        val files = mutableListOf<NetworkFile>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))
            var href = ""
            var displayName = ""
            var contentLength = 0L
            var isCollection = false
            var contentType: String? = null
            var inResponse = false

            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                when (parser.eventType) {
                    XmlPullParser.START_TAG -> {
                        val tag = parser.name?.substringAfterLast(':') ?: ""
                        when (tag) {
                            "response" -> {
                                inResponse = true; href = ""; displayName = ""
                                contentLength = 0; isCollection = false; contentType = null
                            }
                            "href" -> if (inResponse) href = parser.nextText().trim()
                            "displayname" -> if (inResponse) displayName = parser.nextText().trim()
                            "getcontentlength" -> if (inResponse) {
                                contentLength = parser.nextText().trim().toLongOrNull() ?: 0
                            }
                            "getcontenttype" -> if (inResponse) contentType = parser.nextText().trim()
                            "collection" -> if (inResponse) isCollection = true
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        val tag = parser.name?.substringAfterLast(':') ?: ""
                        if (tag == "response" && inResponse) {
                            inResponse = false
                            val decodedHref = java.net.URLDecoder.decode(href, "UTF-8")
                            val name = displayName.ifEmpty { decodedHref.trimEnd('/').substringAfterLast('/') }
                            // Skip the current directory entry itself
                            val normalizedCurrent = currentPath.trimEnd('/')
                            val normalizedHref = decodedHref.trimEnd('/')
                            if (name.isNotEmpty() && !normalizedHref.endsWith(normalizedCurrent)) {
                                files.add(NetworkFile(
                                    name = name,
                                    path = decodedHref,
                                    isDirectory = isCollection,
                                    size = contentLength,
                                    mimeType = contentType
                                ))
                            }
                        }
                    }
                }
                parser.next()
            }
        } catch (_: Exception) { }
        return files.sortedWith(compareByDescending<NetworkFile> { it.isDirectory }.thenBy { it.name.lowercase() })
    }
}
