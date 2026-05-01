package com.music42.swiftyprotein.ui.proteinlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music42.swiftyprotein.data.repository.FavoritesRepository
import com.music42.swiftyprotein.data.repository.LigandRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProteinListUiState(
    val allLigands: List<String> = emptyList(),
    val filteredLigands: List<String> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val loadingLigandId: String? = null,
    val errorMessage: String? = null,
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

    init {
        loadLigands()
        observeFavorites()
        loadCachedInfo()
    }

    private fun loadLigands() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val ids = ligandRepository.getLigandIds()
            _uiState.update {
                it.copy(
                    allLigands = ids,
                    filteredLigands = ids,
                    isLoading = false
                )
            }
        }
    }

    private fun loadCachedInfo() {
        viewModelScope.launch {
            val ids = ligandRepository.getLigandIds()
            val info = mutableMapOf<String, LigandRepository.LigandCacheInfo>()
            for (id in ids) {
                ligandRepository.getCachedInfo(id)?.let { info[id] = it }
            }
            _uiState.update { it.copy(cachedInfo = info) }
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
        val state = _uiState.value
        if (state.loadingLigandId != null) return

        _uiState.update {
            it.copy(
                loadingLigandId = ligandId,
                errorMessage = null,
                navigateToLigand = null
            )
        }

        
        viewModelScope.launch {
            val result = ligandRepository.fetchLigand(ligandId)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(loadingLigandId = null, navigateToLigand = ligandId) }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            loadingLigandId = null,
                            errorMessage = e.localizedMessage ?: "Unknown error"
                        )
                    }
                }
            )
        }
    }

    fun onNavigated() {
        _uiState.update { it.copy(navigateToLigand = null) }
    }

    fun onToggleFavorite(ligandId: String) {
        viewModelScope.launch {
            favoritesRepository.toggleFavorite(ligandId)
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
