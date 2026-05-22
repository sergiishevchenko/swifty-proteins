package com.music42.swiftyprotein.ui.proteinlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music42.swiftyprotein.data.repository.FavoriteToggleAction
import com.music42.swiftyprotein.data.repository.FavoritesRepository
import com.music42.swiftyprotein.data.repository.LigandRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

data class ProteinListUiState(
    val allLigands: List<String> = emptyList(),
    val filteredLigands: List<String> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val loadErrorMessage: String? = null,
    val navigateToLigand: String? = null,
    val favoriteIds: Set<String> = emptySet(),
    val cachedInfo: Map<String, LigandRepository.LigandCacheInfo> = emptyMap()
)

@HiltViewModel
class ProteinListViewModel @Inject constructor(
    private val ligandRepository: LigandRepository,
    private val favoritesRepository: FavoritesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProteinListUiState())
    val uiState: StateFlow<ProteinListUiState> = _uiState.asStateFlow()

    private val _favoriteSnackbar = MutableSharedFlow<FavoriteToggleAction>(extraBufferCapacity = 1)
    val favoriteSnackbar: SharedFlow<FavoriteToggleAction> = _favoriteSnackbar.asSharedFlow()
    private val cacheInfoInFlight = mutableSetOf<String>()
    private val cacheInfoMutex = Mutex()

    init {
        loadLigands()
        observeFavorites()
        observeCacheCleared()
    }

    private fun observeCacheCleared() {
        viewModelScope.launch {
            ligandRepository.cacheCleared.collect {
                cacheInfoMutex.withLock { cacheInfoInFlight.clear() }
                _uiState.update { it.copy(cachedInfo = emptyMap()) }
            }
        }
    }

    private fun loadLigands() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadErrorMessage = null) }
            try {
                val ids = ligandRepository.getLigandIds()
                _uiState.update {
                    it.copy(
                        allLigands = ids,
                        filteredLigands = ids,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadErrorMessage = e.localizedMessage ?: "Failed to load ligand list"
                    )
                }
            }
        }
    }

    fun retryLoadLigands() {
        loadLigands()
    }

    fun dismissLoadError() {
        _uiState.update { it.copy(loadErrorMessage = null) }
    }

    fun ensureCachedInfo(ligandId: String) {
        if (_uiState.value.cachedInfo.containsKey(ligandId)) return
        viewModelScope.launch {
            val shouldLoad = cacheInfoMutex.withLock {
                if (ligandId in _uiState.value.cachedInfo || ligandId in cacheInfoInFlight) {
                    false
                } else {
                    cacheInfoInFlight.add(ligandId)
                    true
                }
            }
            if (!shouldLoad) return@launch
            try {
                val info = ligandRepository.getCachedInfo(ligandId) ?: return@launch
                _uiState.update { state ->
                    state.copy(cachedInfo = state.cachedInfo + (ligandId to info))
                }
            } finally {
                cacheInfoMutex.withLock { cacheInfoInFlight.remove(ligandId) }
            }
        }
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            favoritesRepository.observeFavoriteIds().collect { ids ->
                _uiState.update { it.copy(favoriteIds = ids.toSet()) }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { state ->
            val q = query.trim()
            val filtered = if (q.isBlank()) {
                state.allLigands
            } else {
                state.allLigands
                    .asSequence()
                    .mapNotNull { id ->
                        val idx = id.indexOf(q, ignoreCase = true)
                        if (idx >= 0) id to idx else null
                    }
                    .sortedWith(
                        compareBy<Pair<String, Int>> { it.second }
                            .thenBy { it.first }
                    )
                    .map { it.first }
                    .toList()
            }
            state.copy(searchQuery = query, filteredLigands = filtered)
        }
    }

    fun onLigandClick(ligandId: String) {
        _uiState.update { it.copy(navigateToLigand = ligandId) }
    }

    fun onNavigated() {
        _uiState.update { it.copy(navigateToLigand = null) }
    }

    fun onToggleFavorite(ligandId: String) {
        viewModelScope.launch {
            val action = favoritesRepository.toggleFavorite(ligandId)
            _favoriteSnackbar.emit(action)
        }
    }

}
