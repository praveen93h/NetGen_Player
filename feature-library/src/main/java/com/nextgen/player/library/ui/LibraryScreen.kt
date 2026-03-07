package com.nextgen.player.library.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nextgen.player.data.local.dao.FolderInfo
import com.nextgen.player.data.local.entity.MediaEntity
import com.nextgen.player.data.local.repository.SortOrder
import com.nextgen.player.library.R
import com.nextgen.player.library.ui.components.MediaItemCard
import com.nextgen.player.library.viewmodel.LibraryTab
import com.nextgen.player.library.viewmodel.LibraryViewModel
import com.nextgen.player.ui.theme.Orange500

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onMediaClick: (MediaEntity) -> Unit,
    onFolderClick: (String) -> Unit,
    onSettingsClick: () -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showSortMenu by remember { mutableStateOf(false) }
    val selectedTabIndex = LibraryTab.entries.indexOf(uiState.currentTab)

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            if (uiState.isSearchActive) {
                SearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = { viewModel.setSearchQuery(it) },
                    onClose = { viewModel.toggleSearch() }
                )
            } else {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                stringResource(R.string.library_title),
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 18.sp
                            )
                            Text(
                                stringResource(R.string.library_video_count, uiState.mediaCount),
                                color = Color(0xFF777777),
                                fontSize = 12.sp
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.toggleSearch() }) {
                            Icon(Icons.Rounded.Search, stringResource(R.string.library_search), tint = Color.White)
                        }
                        IconButton(onClick = { viewModel.toggleViewMode() }) {
                            Icon(
                                if (uiState.isGridView) Icons.AutoMirrored.Rounded.ViewList else Icons.Rounded.GridView,
                                stringResource(R.string.library_toggle_view),
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.AutoMirrored.Rounded.Sort, stringResource(R.string.library_sort), tint = Color.White)
                        }
                        IconButton(onClick = { viewModel.scanMedia() }) {
                            Icon(Icons.Rounded.Refresh, stringResource(R.string.library_refresh), tint = Color.White)
                        }
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Rounded.Settings, stringResource(R.string.library_settings), tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            SortOrder.entries.forEach { order ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            when (order) {
                                                SortOrder.NAME -> stringResource(R.string.library_sort_name)
                                                SortOrder.DATE -> stringResource(R.string.library_sort_date)
                                                SortOrder.SIZE -> stringResource(R.string.library_sort_size)
                                                SortOrder.DURATION -> stringResource(R.string.library_sort_duration)
                                            }
                                        )
                                    },
                                    onClick = {
                                        viewModel.setSortOrder(order)
                                        showSortMenu = false
                                    },
                                    leadingIcon = {
                                        if (uiState.sortOrder == order) {
                                            Icon(Icons.Rounded.Check, null, tint = Orange500)
                                        }
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF0D0D0D)
                    )
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(paddingValues)
        ) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color(0xFF0D0D0D),
                contentColor = Orange500,
                indicator = { tabPositions ->
                    if (selectedTabIndex < tabPositions.size) {
                        Box(
                            modifier = Modifier
                                .tabIndicatorOffset(tabPositions[selectedTabIndex])
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(Orange500)
                        )
                    }
                },
                divider = {
                    HorizontalDivider(color = Color(0xFF1F1F1F), thickness = 1.dp)
                }
            ) {
                LibraryTab.entries.forEach { tab ->
                    Tab(
                        selected = uiState.currentTab == tab,
                        onClick = { viewModel.setTab(tab) },
                        selectedContentColor = Orange500,
                        unselectedContentColor = Color(0xFF666666),
                        text = {
                            Text(
                                when (tab) {
                                    LibraryTab.ALL -> stringResource(R.string.library_tab_all)
                                    LibraryTab.FOLDERS -> stringResource(R.string.library_tab_folders)
                                    LibraryTab.RECENT -> stringResource(R.string.library_tab_recent)
                                    LibraryTab.FAVORITES -> stringResource(R.string.library_tab_favorites)
                                },
                                fontWeight = if (uiState.currentTab == tab) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            if (uiState.isScanning) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = Orange500,
                    trackColor = Color(0xFF1F1F1F)
                )
            }

            when (uiState.currentTab) {
                LibraryTab.ALL -> MediaGrid(
                    mediaList = uiState.mediaList,
                    isGridView = uiState.isGridView,
                    onMediaClick = onMediaClick,
                    onScanClick = { viewModel.scanMedia() }
                )
                LibraryTab.FOLDERS -> FolderList(
                    folders = uiState.folders,
                    onFolderClick = onFolderClick
                )
                LibraryTab.RECENT -> MediaGrid(
                    mediaList = uiState.recentlyPlayed,
                    isGridView = uiState.isGridView,
                    onMediaClick = onMediaClick
                )
                LibraryTab.FAVORITES -> MediaGrid(
                    mediaList = uiState.favorites,
                    isGridView = uiState.isGridView,
                    onMediaClick = onMediaClick
                )
            }
        }
    }
}

@Composable
private fun MediaGrid(
    mediaList: List<MediaEntity>,
    isGridView: Boolean,
    onMediaClick: (MediaEntity) -> Unit,
    onScanClick: (() -> Unit)? = null
) {
    if (mediaList.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Rounded.VideoLibrary,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = Color(0xFF2A2A2A)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    stringResource(R.string.library_no_videos),
                    color = Color(0xFF555555),
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    stringResource(R.string.library_no_videos_desc),
                    color = Color(0xFF3A3A3A),
                    fontSize = 13.sp
                )
                if (onScanClick != null) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onScanClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Orange500)
                    ) {
                        Icon(Icons.Rounded.Refresh, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.library_scan_now), color = Color.White)
                    }
                }
            }
        }
        return
    }
    if (isGridView) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxSize().background(Color.Black)
        ) {
            items(mediaList, key = { it.id }) { media ->
                MediaItemCard(media = media, onClick = { onMediaClick(media) }, isGridView = true)
            }
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            items(mediaList, key = { it.id }) { media ->
                MediaItemCard(media = media, onClick = { onMediaClick(media) }, isGridView = false)
                HorizontalDivider(color = Color(0xFF181818), thickness = 0.5.dp, modifier = Modifier.padding(start = 124.dp))
            }
        }
    }
}

@Composable
private fun FolderList(
    folders: List<FolderInfo>,
    onFolderClick: (String) -> Unit
) {
    if (folders.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text(stringResource(R.string.library_no_folders), color = Color(0xFF555555), fontSize = 16.sp)
        }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        items(folders) { folder ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .clickable { onFolderClick(folder.folderPath) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1A1A1A)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Folder,
                        contentDescription = null,
                        tint = Orange500,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = folder.folderName,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = folder.folderPath,
                        color = Color(0xFF555555),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${folder.videoCount}",
                        color = Orange500,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (folder.videoCount == 1) "Video" else "Videos",
                        color = Color(0xFF555555),
                        fontSize = 10.sp
                    )
                }
            }
            HorizontalDivider(
                color = Color(0xFF191919),
                thickness = 0.5.dp,
                modifier = Modifier.padding(start = 82.dp)
            )
        }
    }
}

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit
) {
    TopAppBar(
        title = {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text(stringResource(R.string.library_search_hint), color = Color(0xFF555555)) },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF0D0D0D),
                    unfocusedContainerColor = Color(0xFF0D0D0D),
                    focusedIndicatorColor = Orange500,
                    unfocusedIndicatorColor = Color(0xFF333333),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Orange500
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.library_close_search), tint = Color.White)
            }
        },
        actions = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Rounded.Clear, stringResource(R.string.library_clear_search), tint = Color.White)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D0D0D))
    )
}
