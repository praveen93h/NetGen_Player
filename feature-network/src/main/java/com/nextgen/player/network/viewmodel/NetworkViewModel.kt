package com.nextgen.player.network.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextgen.player.data.local.entity.ServerBookmarkEntity
import com.nextgen.player.data.local.repository.ServerRepository
import com.nextgen.player.network.client.*
import com.nextgen.player.network.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class NetworkUiState(
    val bookmarks: List<ServerBookmarkEntity> = emptyList(),
    val recentUrls: List<String> = emptyList(),
    val dlnaDevices: List<DlnaDevice> = emptyList(),
    val isDlnaScanning: Boolean = false,
    val currentFiles: List<NetworkFile> = emptyList(),
    val currentPath: String = "",
    val currentServerName: String = "",
    val isConnecting: Boolean = false,
    val connectionError: String? = null,
    val isLoadingFiles: Boolean = false
)

@HiltViewModel
class NetworkViewModel @Inject constructor(
    private val serverRepository: ServerRepository,
    private val dlnaClient: DlnaClient,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(NetworkUiState())
    val uiState: StateFlow<NetworkUiState> = _uiState.asStateFlow()

    private var currentClient: Any? = null
    private var currentConfig: ServerConfig? = null
    private var currentShareName: String? = null
    private val pathStack = mutableListOf<String>()
    private var isDlnaBrowsing = false
    private var dlnaLocation: String? = null

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("network_prefs", Context.MODE_PRIVATE)
    }

    init {
        viewModelScope.launch {
            serverRepository.bookmarks.collect { bookmarks ->
                _uiState.update { it.copy(bookmarks = bookmarks) }
            }
        }
        loadRecentUrls()
    }

    fun addBookmark(bookmark: ServerBookmarkEntity) {
        viewModelScope.launch { serverRepository.addBookmark(bookmark) }
    }

    fun updateBookmark(bookmark: ServerBookmarkEntity) {
        viewModelScope.launch { serverRepository.updateBookmark(bookmark) }
    }

    fun deleteBookmark(bookmark: ServerBookmarkEntity) {
        viewModelScope.launch { serverRepository.deleteBookmark(bookmark) }
    }

    fun addRecentUrl(url: String) {
        val current = _uiState.value.recentUrls.toMutableList()
        current.remove(url)
        current.add(0, url)
        val limited = current.take(20)
        _uiState.update { it.copy(recentUrls = limited) }
        saveRecentUrls(limited)
    }

    fun connectServer(serverId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isConnecting = true, connectionError = null, currentFiles = emptyList()) }
            try {
                val bookmark = serverRepository.getById(serverId)
                    ?: throw Exception("Server not found")

                serverRepository.touchBookmark(serverId)

                val config = ServerConfig(
                    id = bookmark.id,
                    name = bookmark.name,
                    type = ServerType.fromString(bookmark.type),
                    host = bookmark.host,
                    port = bookmark.port,
                    path = bookmark.path,
                    username = bookmark.username,
                    password = bookmark.password,
                    domain = bookmark.domain
                )
                currentConfig = config
                isDlnaBrowsing = false
                pathStack.clear()

                when (config.type) {
                    ServerType.SMB -> connectSmb(config)
                    ServerType.FTP -> connectFtp(config)
                    ServerType.SFTP -> connectSftp(config)
                    ServerType.WEBDAV -> connectWebDav(config)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isConnecting = false, connectionError = e.message ?: "Connection failed")
                }
            }
        }
    }

    private suspend fun connectSmb(config: ServerConfig) {
        val client = SmbClient()
        client.connect(config)
        currentClient = client
        _uiState.update { it.copy(currentServerName = config.name, isConnecting = false) }

        // List shares or files in path
        if (config.path != "/" && config.path.isNotEmpty()) {
            val parts = config.path.trimStart('/').split("/", limit = 2)
            currentShareName = parts[0]
            val subPath = if (parts.size > 1) parts[1] else ""
            pathStack.add(subPath)
            loadSmbFiles(client, parts[0], subPath)
        } else {
            val shares = client.listShares()
            _uiState.update { it.copy(currentFiles = shares, currentPath = "/") }
        }
    }

    private suspend fun connectFtp(config: ServerConfig) {
        val client = FtpClient()
        client.connect(config)
        currentClient = client
        _uiState.update { it.copy(currentServerName = config.name, isConnecting = false) }
        loadFtpFiles(client, config.path)
    }

    private suspend fun connectSftp(config: ServerConfig) {
        val client = SftpClient()
        client.connect(config)
        currentClient = client
        _uiState.update { it.copy(currentServerName = config.name, isConnecting = false) }
        loadSftpFiles(client, config.path)
    }

    private suspend fun connectWebDav(config: ServerConfig) {
        val client = WebDavClient()
        client.connect(config)
        currentClient = client
        _uiState.update { it.copy(currentServerName = config.name, isConnecting = false) }
        loadWebDavFiles(client, config.path)
    }

    fun connectDlna(location: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isConnecting = true, connectionError = null, currentFiles = emptyList()) }
            isDlnaBrowsing = true
            dlnaLocation = location
            pathStack.clear()
            pathStack.add("0")
            try {
                val files = withContext(Dispatchers.IO) {
                    dlnaClient.browseContentDirectory(location, "0")
                }
                _uiState.update {
                    it.copy(isConnecting = false, currentFiles = files, currentPath = "/")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isConnecting = false, connectionError = e.message ?: "DLNA browse failed")
                }
            }
        }
    }

    fun navigateToFolder(path: String, name: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingFiles = true) }
            try {
                if (isDlnaBrowsing) {
                    pathStack.add(path)
                    val files = withContext(Dispatchers.IO) {
                        dlnaClient.browseContentDirectory(dlnaLocation!!, path)
                    }
                    _uiState.update {
                        it.copy(currentFiles = files, currentPath = name, isLoadingFiles = false)
                    }
                } else {
                    val config = currentConfig ?: return@launch
                    when (config.type) {
                        ServerType.SMB -> {
                            val client = currentClient as? SmbClient ?: return@launch
                            // If at share level, the folder name IS the share
                            if (currentShareName == null) {
                                currentShareName = path
                                pathStack.add("")
                                loadSmbFiles(client, path, "")
                            } else {
                                pathStack.add(path)
                                loadSmbFiles(client, currentShareName!!, path)
                            }
                        }
                        ServerType.FTP -> {
                            pathStack.add(path)
                            loadFtpFiles(currentClient as FtpClient, path)
                        }
                        ServerType.SFTP -> {
                            pathStack.add(path)
                            loadSftpFiles(currentClient as SftpClient, path)
                        }
                        ServerType.WEBDAV -> {
                            pathStack.add(path)
                            loadWebDavFiles(currentClient as WebDavClient, path)
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoadingFiles = false, connectionError = e.message)
                }
            }
        }
    }

    fun navigateUp(): Boolean {
        if (pathStack.size <= 1) return false
        pathStack.removeLast()
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingFiles = true) }
            try {
                if (isDlnaBrowsing) {
                    val objectId = pathStack.lastOrNull() ?: "0"
                    val files = withContext(Dispatchers.IO) {
                        dlnaClient.browseContentDirectory(dlnaLocation!!, objectId)
                    }
                    _uiState.update {
                        it.copy(currentFiles = files, currentPath = if (pathStack.size <= 1) "/" else "...", isLoadingFiles = false)
                    }
                } else {
                    val config = currentConfig ?: return@launch
                    when (config.type) {
                        ServerType.SMB -> {
                            val client = currentClient as? SmbClient ?: return@launch
                            if (pathStack.isEmpty()) {
                                // Back to share listing
                                currentShareName = null
                                val shares = client.listShares()
                                _uiState.update { it.copy(currentFiles = shares, currentPath = "/", isLoadingFiles = false) }
                            } else {
                                val path = pathStack.last()
                                loadSmbFiles(client, currentShareName!!, path)
                            }
                        }
                        ServerType.FTP -> {
                            val path = pathStack.lastOrNull() ?: "/"
                            loadFtpFiles(currentClient as FtpClient, path)
                        }
                        ServerType.SFTP -> {
                            val path = pathStack.lastOrNull() ?: "/"
                            loadSftpFiles(currentClient as SftpClient, path)
                        }
                        ServerType.WEBDAV -> {
                            val path = pathStack.lastOrNull() ?: "/"
                            loadWebDavFiles(currentClient as WebDavClient, path)
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingFiles = false, connectionError = e.message) }
            }
        }
        return true
    }

    fun getPlaybackUrl(file: NetworkFile): String? {
        if (isDlnaBrowsing) {
            // DLNA file path is the streaming URL
            return file.path
        }
        val config = currentConfig ?: return null
        return when (config.type) {
            ServerType.SMB -> {
                val shareName = currentShareName ?: return null
                (currentClient as? SmbClient)?.getStreamUri(config, shareName, file.path)
            }
            ServerType.FTP -> {
                (currentClient as? FtpClient)?.getStreamUri(config, file.path)
            }
            ServerType.SFTP -> {
                "sftp://${config.host}:${config.port}${file.path}?user=${java.net.URLEncoder.encode(config.username, "UTF-8")}&pass=${java.net.URLEncoder.encode(config.password, "UTF-8")}"
            }
            ServerType.WEBDAV -> {
                (currentClient as? WebDavClient)?.getStreamUrl(file.path)
            }
        }
    }

    fun refreshDlna() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDlnaScanning = true, dlnaDevices = emptyList()) }
            try {
                val devices = withContext(Dispatchers.IO) {
                    dlnaClient.discoverDevices(5000)
                }
                _uiState.update { it.copy(dlnaDevices = devices, isDlnaScanning = false) }
            } catch (_: Exception) {
                _uiState.update { it.copy(isDlnaScanning = false) }
            }
        }
    }

    private suspend fun loadSmbFiles(client: SmbClient, shareName: String, path: String) {
        val files = client.listFiles(shareName, path)
        _uiState.update {
            it.copy(currentFiles = files, currentPath = "$shareName/$path".trimEnd('/'), isLoadingFiles = false)
        }
    }

    private suspend fun loadFtpFiles(client: FtpClient, path: String) {
        val files = client.listFiles(path)
        _uiState.update { it.copy(currentFiles = files, currentPath = path, isLoadingFiles = false) }
    }

    private suspend fun loadSftpFiles(client: SftpClient, path: String) {
        val files = client.listFiles(path)
        _uiState.update { it.copy(currentFiles = files, currentPath = path, isLoadingFiles = false) }
    }

    private suspend fun loadWebDavFiles(client: WebDavClient, path: String) {
        val files = client.listFiles(path)
        _uiState.update { it.copy(currentFiles = files, currentPath = path, isLoadingFiles = false) }
    }

    private fun loadRecentUrls() {
        val urls = prefs.getString("recent_urls", null)
            ?.split("\n")
            ?.filter { it.isNotBlank() }
            ?: emptyList()
        _uiState.update { it.copy(recentUrls = urls) }
    }

    private fun saveRecentUrls(urls: List<String>) {
        prefs.edit().putString("recent_urls", urls.joinToString("\n")).apply()
    }

    fun disconnect() {
        disconnectAll()
        _uiState.update { it.copy(currentFiles = emptyList(), currentPath = "", currentServerName = "", connectionError = null) }
    }

    override fun onCleared() {
        super.onCleared()
        disconnectAll()
    }

    private fun disconnectAll() {
        when (val client = currentClient) {
            is SmbClient -> client.disconnect()
            is FtpClient -> client.disconnect()
            is SftpClient -> client.disconnect()
            is WebDavClient -> client.disconnect()
        }
        currentClient = null
        currentConfig = null
        currentShareName = null
        pathStack.clear()
    }
}
