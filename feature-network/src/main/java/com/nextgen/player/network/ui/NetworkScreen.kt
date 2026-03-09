package com.nextgen.player.network.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nextgen.player.data.local.entity.ServerBookmarkEntity
import com.nextgen.player.network.R
import com.nextgen.player.network.model.DlnaDevice
import com.nextgen.player.network.model.ServerType
import com.nextgen.player.network.viewmodel.NetworkViewModel
import com.nextgen.player.ui.theme.Orange500

enum class NetworkTab(val labelRes: Int) {
    SERVERS(R.string.network_tab_servers),
    STREAM(R.string.network_tab_stream),
    DLNA(R.string.network_tab_dlna)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkScreen(
    onBackClick: () -> Unit,
    onBrowseServer: (serverId: Long) -> Unit,
    onBrowseDlna: (location: String, name: String) -> Unit,
    onPlayUrl: (url: String) -> Unit,
    viewModel: NetworkViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showUrlDialog by remember { mutableStateOf(false) }
    var editingServer by remember { mutableStateOf<ServerBookmarkEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.network_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            when (selectedTab) {
                0 -> FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = Orange500
                ) {
                    Icon(Icons.Rounded.Add, stringResource(R.string.network_add_server))
                }
                1 -> FloatingActionButton(
                    onClick = { showUrlDialog = true },
                    containerColor = Orange500
                ) {
                    Icon(Icons.Rounded.Link, stringResource(R.string.network_open_url))
                }
                2 -> FloatingActionButton(
                    onClick = { viewModel.refreshDlna() },
                    containerColor = Orange500
                ) {
                    Icon(Icons.Rounded.Refresh, stringResource(R.string.network_refresh))
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = Orange500,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Orange500
                        )
                    }
                }
            ) {
                NetworkTab.entries.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                stringResource(tab.labelRes),
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        selectedContentColor = Orange500,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            when (selectedTab) {
                0 -> ServersTab(
                    servers = uiState.bookmarks,
                    onServerClick = { server -> onBrowseServer(server.id) },
                    onEditServer = { server -> editingServer = server; showAddDialog = true },
                    onDeleteServer = { viewModel.deleteBookmark(it) }
                )
                1 -> StreamTab(
                    recentUrls = uiState.recentUrls,
                    onOpenUrl = { showUrlDialog = true },
                    onPlayRecent = onPlayUrl
                )
                2 -> DlnaTab(
                    devices = uiState.dlnaDevices,
                    isScanning = uiState.isDlnaScanning,
                    onDeviceClick = { device -> onBrowseDlna(device.location, device.friendlyName) },
                    onRefresh = { viewModel.refreshDlna() }
                )
            }
        }
    }

    if (showAddDialog) {
        AddServerDialog(
            existingServer = editingServer,
            onDismiss = { showAddDialog = false; editingServer = null },
            onSave = { bookmark ->
                if (editingServer != null) {
                    viewModel.updateBookmark(bookmark)
                } else {
                    viewModel.addBookmark(bookmark)
                }
                showAddDialog = false
                editingServer = null
            }
        )
    }

    if (showUrlDialog) {
        UrlStreamDialog(
            onDismiss = { showUrlDialog = false },
            onPlay = { url ->
                viewModel.addRecentUrl(url)
                onPlayUrl(url)
                showUrlDialog = false
            }
        )
    }
}

@Composable
private fun ServersTab(
    servers: List<ServerBookmarkEntity>,
    onServerClick: (ServerBookmarkEntity) -> Unit,
    onEditServer: (ServerBookmarkEntity) -> Unit,
    onDeleteServer: (ServerBookmarkEntity) -> Unit
) {
    if (servers.isEmpty()) {
        EmptyState(
            icon = Icons.Rounded.Dns,
            message = stringResource(R.string.network_no_servers),
            hint = stringResource(R.string.network_no_servers_hint)
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(servers, key = { it.id }) { server ->
                ServerCard(
                    server = server,
                    onClick = { onServerClick(server) },
                    onEdit = { onEditServer(server) },
                    onDelete = { onDeleteServer(server) }
                )
            }
        }
    }
}

@Composable
private fun ServerCard(
    server: ServerBookmarkEntity,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val serverType = ServerType.fromString(server.type)
    val icon = when (serverType) {
        ServerType.SMB -> Icons.Rounded.FolderShared
        ServerType.FTP -> Icons.Rounded.CloudDownload
        ServerType.SFTP -> Icons.Rounded.Security
        ServerType.WEBDAV -> Icons.Rounded.Cloud
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = Orange500, modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(server.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${server.type} · ${server.host}:${server.port}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Rounded.MoreVert, "Options")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_edit)) },
                        onClick = { showMenu = false; onEdit() },
                        leadingIcon = { Icon(Icons.Rounded.Edit, null) }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_delete)) },
                        onClick = { showMenu = false; onDelete() },
                        leadingIcon = { Icon(Icons.Rounded.Delete, null) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StreamTab(
    recentUrls: List<String>,
    onOpenUrl: () -> Unit,
    onPlayRecent: (String) -> Unit
) {
    if (recentUrls.isEmpty()) {
        EmptyState(
            icon = Icons.Rounded.Link,
            message = stringResource(R.string.network_no_urls),
            hint = stringResource(R.string.network_no_urls_hint)
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    stringResource(R.string.network_recent_urls),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Orange500,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            items(recentUrls) { url ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onPlayRecent(url) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.PlayCircle, null, tint = Orange500)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            url,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DlnaTab(
    devices: List<DlnaDevice>,
    isScanning: Boolean,
    onDeviceClick: (DlnaDevice) -> Unit,
    onRefresh: () -> Unit
) {
    if (isScanning) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Orange500)
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.network_scanning), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        }
    } else if (devices.isEmpty()) {
        EmptyState(
            icon = Icons.Rounded.Devices,
            message = stringResource(R.string.network_no_dlna),
            hint = stringResource(R.string.network_no_dlna_hint)
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(devices, key = { it.udn }) { device ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onDeviceClick(device) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Tv, null, tint = Orange500, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(device.friendlyName, fontWeight = FontWeight.Bold)
                            Text(
                                device.location.substringAfter("://").substringBefore("/"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String,
    hint: String
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                icon, null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
            Spacer(Modifier.height(16.dp))
            Text(message, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(8.dp))
            Text(hint, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 14.sp)
        }
    }
}
