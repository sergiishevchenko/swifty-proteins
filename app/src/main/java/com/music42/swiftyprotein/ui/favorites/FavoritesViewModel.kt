package com.music42.swiftyprotein.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music42.swiftyprotein.data.repository.FavoritesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    favoritesRepository: FavoritesRepository
) : ViewModel() {
    val favoriteLigandIds: StateFlow<List<String>> =
        favoritesRepository.observeFavorites()
            .map { list -> list.map { it.ligandId } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
