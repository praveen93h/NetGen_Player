package com.nextgen.player.library.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextgen.player.data.local.dao.FolderInfo
import com.nextgen.player.data.local.entity.MediaEntity
import com.nextgen.player.data.local.repository.MediaRepository
import com.nextgen.player.data.local.repository.SortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LibraryTab {
    ALL, FOLDERS, RECENT, FAVORITES
}

data class LibraryUiState(
    val mediaList: List<MediaEntity> = emptyList(),
    val folders: List<FolderInfo> = emptyList(),
    val recentlyPlayed: List<MediaEntity> = emptyList(),
    val favorites: List<MediaEntity> = emptyList(),
    val currentTab: LibraryTab = LibraryTab.ALL,
    val sortOrder: SortOrder = SortOrder.NAME,
    val isGridView: Boolean = true,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val isLoading: Boolean = false,
    val isScanning: Boolean = false,
    val mediaCount: Int = 0
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.NAME)
    private val _searchQuery = MutableStateFlow("")

    init {
        viewModelScope.launch {
            _sortOrder.flatMapLatest { sortOrder ->
                mediaRepository.getAllMedia(sortOrder)
            }.collect { mediaList ->
                _uiState.update { it.copy(mediaList = mediaList, mediaCount = mediaList.size) }
            }
        }

        viewModelScope.launch {
            mediaRepository.getFolders().collect { folders ->
                _uiState.update { it.copy(folders = folders) }
            }
        }

        viewModelScope.launch {
            mediaRepository.getRecentlyPlayed().collect { recent ->
                _uiState.update { it.copy(recentlyPlayed = recent) }
            }
        }

        viewModelScope.launch {
            mediaRepository.getFavorites().collect { favorites ->
                _uiState.update { it.copy(favorites = favorites) }
            }
        }

        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .filter { it.isNotBlank() }
                .flatMapLatest { query ->
                    mediaRepository.searchMedia(query)
                }
                .collect { results ->
                    _uiState.update { it.copy(mediaList = results) }
                }
        }

        scanMedia()
    }

    fun setTab(tab: LibraryTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
        _uiState.update { it.copy(sortOrder = order) }
    }

    fun toggleViewMode() {
        _uiState.update { it.copy(isGridView = !it.isGridView) }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }

        if (query.isBlank()) {
            viewModelScope.launch {
                _sortOrder.flatMapLatest { sortOrder ->
                    mediaRepository.getAllMedia(sortOrder)
                }.first().let { mediaList ->
                    _uiState.update { it.copy(mediaList = mediaList, mediaCount = mediaList.size) }
                }
            }
        }
    }

    fun toggleSearch() {
        _uiState.update {
            it.copy(
                isSearchActive = !it.isSearchActive,
                searchQuery = if (it.isSearchActive) "" else it.searchQuery
            )
        }
        if (!_uiState.value.isSearchActive) {
            setSearchQuery("")
        }
    }

    fun scanMedia() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true) }
            try {
                mediaRepository.scanAndSyncMedia()
            } catch (e: Exception) {
            } finally {
                _uiState.update { it.copy(isScanning = false) }
            }
        }
    }

    fun toggleFavorite(mediaId: Long, isFavorite: Boolean) {
        viewModelScope.launch {
            mediaRepository.toggleFavorite(mediaId, isFavorite)
        }
    }
}
