package com.nextgen.player.network.client

import com.nextgen.player.network.model.DlnaDevice
import com.nextgen.player.network.model.NetworkFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket
import java.util.concurrent.TimeUnit

class DlnaClient {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun discoverDevices(timeoutMs: Int = 5000): List<DlnaDevice> = withContext(Dispatchers.IO) {
        val devices = mutableListOf<DlnaDevice>()
        val seen = mutableSetOf<String>()

        try {
            val ssdpAddress = InetAddress.getByName("239.255.255.250")
            val searchMessage = buildString {
                append("M-SEARCH * HTTP/1.1\r\n")
                append("HOST: 239.255.255.250:1900\r\n")
                append("MAN: \"ssdp:discover\"\r\n")
                append("MX: 3\r\n")
                append("ST: urn:schemas-upnp-org:device:MediaServer:1\r\n")
                append("\r\n")
            }

            val socket = MulticastSocket(0)
            socket.soTimeout = timeoutMs

            val sendData = searchMessage.toByteArray()
            val sendPacket = DatagramPacket(sendData, sendData.size, ssdpAddress, 1900)
            socket.send(sendPacket)

            val buf = ByteArray(4096)
            val endTime = System.currentTimeMillis() + timeoutMs

            while (System.currentTimeMillis() < endTime) {
                try {
                    val receivePacket = DatagramPacket(buf, buf.size)
                    socket.receive(receivePacket)
                    val response = String(receivePacket.data, 0, receivePacket.length)
                    val location = extractHeader(response, "LOCATION")
                    if (location != null && location !in seen) {
                        seen.add(location)
                        val device = fetchDeviceDescription(location)
                        if (device != null) devices.add(device)
                    }
                } catch (_: java.net.SocketTimeoutException) {
                    break
                }
            }
            socket.close()
        } catch (_: Exception) { }

        devices
    }

    suspend fun browseContentDirectory(
        deviceLocation: String,
        objectId: String = "0"
    ): List<NetworkFile> = withContext(Dispatchers.IO) {
        try {
            val controlUrl = getContentDirectoryControlUrl(deviceLocation) ?: return@withContext emptyList()
            val soapBody = buildBrowseRequest(objectId)
            val request = Request.Builder()
                .url(controlUrl)
                .post(okhttp3.RequestBody.create(
                    "text/xml; charset=utf-8".toMediaType(),
                    soapBody
                ))
                .header("SOAPAction", "\"urn:schemas-upnp-org:service:ContentDirectory:1#Browse\"")
                .header("Content-Type", "text/xml; charset=utf-8")
                .build()
            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""
            response.close()
            parseDidlLite(body)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun fetchDeviceDescription(location: String): DlnaDevice? {
        return try {
            val request = Request.Builder().url(location).build()
            val response = httpClient.newCall(request).execute()
            val xml = response.body?.string() ?: ""
            response.close()
            parseDeviceDescription(xml, location)
        } catch (_: Exception) { null }
    }

    private fun parseDeviceDescription(xml: String, location: String): DlnaDevice? {
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))
            var friendlyName = ""
            var udn = ""
            var iconUrl: String? = null

            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "friendlyName" -> friendlyName = parser.nextText().trim()
                        "UDN" -> udn = parser.nextText().trim()
                    }
                }
                parser.next()
            }
            return if (friendlyName.isNotEmpty()) {
                DlnaDevice(friendlyName, location, udn, iconUrl)
            } else null
        } catch (_: Exception) { return null }
    }

    private fun getContentDirectoryControlUrl(deviceLocation: String): String? {
        return try {
            val request = Request.Builder().url(deviceLocation).build()
            val response = httpClient.newCall(request).execute()
            val xml = response.body?.string() ?: ""
            response.close()

            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            var inContentDir = false
            var controlUrl: String? = null
            val baseUrl = deviceLocation.substringBeforeLast('/')

            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG) {
                    if (parser.name == "serviceType") {
                        val text = parser.nextText()
                        inContentDir = text.contains("ContentDirectory")
                    }
                    if (parser.name == "controlURL" && inContentDir) {
                        val path = parser.nextText().trim()
                        controlUrl = if (path.startsWith("http")) path else "$baseUrl$path"
                        inContentDir = false
                    }
                }
                parser.next()
            }
            controlUrl
        } catch (_: Exception) { null }
    }

    private fun buildBrowseRequest(objectId: String): String = """<?xml version="1.0" encoding="utf-8"?>
<s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
  <s:Body>
    <u:Browse xmlns:u="urn:schemas-upnp-org:service:ContentDirectory:1">
      <ObjectID>$objectId</ObjectID>
      <BrowseFlag>BrowseDirectChildren</BrowseFlag>
      <Filter>*</Filter>
      <StartingIndex>0</StartingIndex>
      <RequestedCount>200</RequestedCount>
      <SortCriteria></SortCriteria>
    </u:Browse>
  </s:Body>
</s:Envelope>"""

    private fun parseDidlLite(soapResponse: String): List<NetworkFile> {
        val files = mutableListOf<NetworkFile>()
        // Extract DIDL-Lite content from SOAP response
        val didlStart = soapResponse.indexOf("&lt;DIDL-Lite")
        val didlEnd = soapResponse.indexOf("&lt;/DIDL-Lite&gt;")
        if (didlStart < 0) {
            // Try unescaped
            val rawStart = soapResponse.indexOf("<DIDL-Lite")
            if (rawStart >= 0) {
                val rawEnd = soapResponse.indexOf("</DIDL-Lite>")
                if (rawEnd >= 0) {
                    val didl = soapResponse.substring(rawStart, rawEnd + "</DIDL-Lite>".length)
                    return parseDidlContent(didl)
                }
            }
            // Try Result tag
            val resultStart = soapResponse.indexOf("<Result>")
            val resultEnd = soapResponse.indexOf("</Result>")
            if (resultStart >= 0 && resultEnd >= 0) {
                val escaped = soapResponse.substring(resultStart + 8, resultEnd)
                val unescaped = escaped.replace("&lt;", "<").replace("&gt;", ">")
                    .replace("&amp;", "&").replace("&quot;", "\"")
                return parseDidlContent(unescaped)
            }
            return files
        }

        val escaped = soapResponse.substring(didlStart, didlEnd + "&lt;/DIDL-Lite&gt;".length)
        val unescaped = escaped.replace("&lt;", "<").replace("&gt;", ">")
            .replace("&amp;", "&").replace("&quot;", "\"")
        return parseDidlContent(unescaped)
    }

    private fun parseDidlContent(didl: String): List<NetworkFile> {
        val files = mutableListOf<NetworkFile>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(StringReader(didl))

            var isContainer = false
            var title = ""
            var resUrl = ""
            var size = 0L
            var id = ""
            var inItem = false

            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                when (parser.eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "container" -> {
                                inItem = true; isContainer = true
                                id = parser.getAttributeValue(null, "id") ?: ""
                            }
                            "item" -> {
                                inItem = true; isContainer = false
                                id = parser.getAttributeValue(null, "id") ?: ""
                            }
                            "title" -> if (inItem) title = parser.nextText().trim()
                            "res" -> if (inItem) {
                                val sizeStr = parser.getAttributeValue(null, "size")
                                size = sizeStr?.toLongOrNull() ?: 0
                                resUrl = parser.nextText().trim()
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if ((parser.name == "container" || parser.name == "item") && inItem) {
                            files.add(NetworkFile(
                                name = title,
                                path = if (isContainer) id else resUrl,
                                isDirectory = isContainer,
                                size = size
                            ))
                            title = ""; resUrl = ""; size = 0; id = ""; inItem = false
                        }
                    }
                }
                parser.next()
            }
        } catch (_: Exception) { }
        return files.sortedWith(compareByDescending<NetworkFile> { it.isDirectory }.thenBy { it.name.lowercase() })
    }

    private fun extractHeader(response: String, headerName: String): String? {
        val lines = response.lines()
        for (line in lines) {
            if (line.startsWith("$headerName:", ignoreCase = true)) {
                return line.substringAfter(":").trim()
            }
        }
        return null
    }
}
