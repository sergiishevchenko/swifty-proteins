package com.music42.swiftyprotein.ui.proteinview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music42.swiftyprotein.data.model.Atom
import com.music42.swiftyprotein.data.model.Ligand
import com.music42.swiftyprotein.data.repository.LigandRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class VisualizationMode {
    BALL_AND_STICK,
    SPACE_FILL,
    STICKS_ONLY
}

data class ProteinViewUiState(
    val ligandId: String = "",
    val ligand: Ligand? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val selectedAtom: Atom? = null,
    val visualizationMode: VisualizationMode = VisualizationMode.BALL_AND_STICK
)

@HiltViewModel
class ProteinViewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val ligandRepository: LigandRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProteinViewUiState())
    val uiState: StateFlow<ProteinViewUiState> = _uiState.asStateFlow()

    init {
        val ligandId: String = savedStateHandle["ligandId"] ?: ""
        _uiState.update { it.copy(ligandId = ligandId) }
        fetchLigand(ligandId)
    }

    private fun fetchLigand(ligandId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = ligandRepository.fetchLigand(ligandId)
            result.fold(
                onSuccess = { ligand ->
                    _uiState.update { it.copy(isLoading = false, ligand = ligand) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Unknown error"
                        )
                    }
                }
            )
        }
    }

    fun onAtomSelected(atom: Atom?) {
        _uiState.update { it.copy(selectedAtom = atom) }
    }

    fun dismissAtomInfo() {
        _uiState.update { it.copy(selectedAtom = null) }
    }

    fun setVisualizationMode(mode: VisualizationMode) {
        _uiState.update { it.copy(visualizationMode = mode) }
    }
}
