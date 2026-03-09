package com.nextgen.player.network.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nextgen.player.network.R
import com.nextgen.player.network.model.NetworkFile
import com.nextgen.player.network.viewmodel.NetworkViewModel
import com.nextgen.player.ui.theme.Orange500

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerBrowserScreen(
    serverId: Long = -1,
    dlnaLocation: String? = null,
    dlnaName: String? = null,
    onBackClick: () -> Unit,
    onPlayFile: (url: String) -> Unit,
    viewModel: NetworkViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(serverId, dlnaLocation) {
        if (dlnaLocation != null) {
            viewModel.connectDlna(dlnaLocation)
        } else if (serverId > 0) {
            viewModel.connectServer(serverId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            uiState.currentServerName.ifEmpty { dlnaName ?: "Browser" },
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (uiState.currentPath.isNotEmpty()) {
                            Text(
                                uiState.currentPath,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (!viewModel.navigateUp()) onBackClick()
                    }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            when {
                uiState.isConnecting -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Orange500)
                            Spacer(Modifier.height(16.dp))
                            Text(stringResource(R.string.network_connecting))
                        }
                    }
                }
                uiState.connectionError != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                            Icon(Icons.Rounded.ErrorOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(16.dp))
                            Text(stringResource(R.string.network_connection_error), fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                uiState.connectionError ?: "",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                fontSize = 14.sp
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    if (dlnaLocation != null) viewModel.connectDlna(dlnaLocation)
                                    else viewModel.connectServer(serverId)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Orange500)
                            ) {
                                Text(stringResource(R.string.action_retry))
                            }
                        }
                    }
                }
                uiState.isLoadingFiles -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Orange500)
                    }
                }
                uiState.currentFiles.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.network_empty_folder), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(uiState.currentFiles, key = { it.path }) { file ->
                            FileListItem(
                                file = file,
                                onClick = {
                                    if (file.isDirectory) {
                                        viewModel.navigateToFolder(file.path, file.name)
                                    } else if (file.isMedia || dlnaLocation != null) {
                                        val url = viewModel.getPlaybackUrl(file)
                                        if (url != null) onPlayFile(url)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FileListItem(file: NetworkFile, onClick: () -> Unit) {
    val icon = when {
        file.isDirectory -> Icons.Rounded.Folder
        file.isVideo -> Icons.Rounded.VideoFile
        file.isMedia -> Icons.Rounded.AudioFile
        else -> Icons.Rounded.InsertDriveFile
    }
    val iconTint = when {
        file.isDirectory -> Orange500
        file.isVideo -> MaterialTheme.colorScheme.primary
        file.isMedia -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = iconTint, modifier = Modifier.size(32.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                file.name,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!file.isDirectory && file.size > 0) {
                Text(
                    file.formattedSize,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
        if (file.isDirectory) {
            Icon(
                Icons.Rounded.ChevronRight, null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}
