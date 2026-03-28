package com.nextgen.player.library.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.nextgen.player.data.local.entity.MediaEntity
import com.nextgen.player.data.local.repository.MediaRepository
import com.nextgen.player.library.R
import com.nextgen.player.library.ui.components.MediaItemCard
import com.nextgen.player.ui.theme.Orange500
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FolderViewModel @Inject constructor(
    private val mediaRepository: MediaRepository
) : ViewModel() {
    private val _folderPath = MutableStateFlow("")

    val mediaList: StateFlow<List<MediaEntity>> = _folderPath
        .filter { it.isNotEmpty() }
        .flatMapLatest { path -> mediaRepository.getMediaInFolder(path) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun loadFolder(path: String) {
        _folderPath.value = path
    }

    private var _isGridView = MutableStateFlow(false)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    fun toggleViewMode() { _isGridView.value = !_isGridView.value }
}

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderScreen(
    folderPath: String,
    folderName: String,
    onMediaClick: (MediaEntity) -> Unit,
    onBackClick: () -> Unit,
    viewModel: FolderViewModel = hiltViewModel()
) {
    LaunchedEffect(folderPath) {
        viewModel.loadFolder(folderPath)
    }
    val mediaList by viewModel.mediaList.collectAsStateWithLifecycle()
    val isGridView by viewModel.isGridView.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(folderName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 17.sp)
                        Text(
                            stringResource(R.string.library_video_count, mediaList.size),
                            color = Color(0xFF777777),
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.library_back), tint = Color.White)
                    }
                },
                actions = {
                    // Play All button
                    if (mediaList.isNotEmpty()) {
                        IconButton(onClick = { onMediaClick(mediaList.first()) }) {
                            Icon(Icons.Rounded.PlayArrow, stringResource(R.string.library_play_all), tint = Orange500)
                        }
                    }
                    IconButton(onClick = { viewModel.toggleViewMode() }) {
                        Icon(
                            if (isGridView) Icons.AutoMirrored.Rounded.ViewList else Icons.Rounded.GridView,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D0D0D))
            )
        }
    ) { paddingValues ->
        if (mediaList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black).padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Rounded.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = Color(0xFF2A2A2A)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.library_folder_empty),
                        color = Color(0xFF555555),
                        fontSize = 16.sp
                    )
                }
            }
        } else if (isGridView) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.background(Color.Black).padding(paddingValues),
                contentPadding = PaddingValues(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(mediaList, key = { it.id }) { media ->
                    MediaItemCard(media = media, onClick = { onMediaClick(media) }, isGridView = true)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().background(Color.Black).padding(paddingValues)
            ) {
                items(mediaList, key = { it.id }) { media ->
                    MediaItemCard(media = media, onClick = { onMediaClick(media) }, isGridView = false)
                    HorizontalDivider(color = Color(0xFF181818), thickness = 0.5.dp, modifier = Modifier.padding(start = 124.dp))
                }
            }
        }
    }
}
