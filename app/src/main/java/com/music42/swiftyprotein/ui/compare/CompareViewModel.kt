package com.music42.swiftyprotein.ui.compare

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music42.swiftyprotein.data.model.Ligand
import com.music42.swiftyprotein.data.repository.LigandRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CompareUiState(
    val ligandAId: String = "",
    val ligandBId: String = "",
    val ligandA: Ligand? = null,
    val ligandB: Ligand? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class CompareViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val ligandRepository: LigandRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CompareUiState())
    val uiState: StateFlow<CompareUiState> = _uiState.asStateFlow()

    init {
        val a = savedStateHandle["ligandA"] ?: ""
        val b = savedStateHandle["ligandB"] ?: ""
        _uiState.update { it.copy(ligandAId = a, ligandBId = b) }
        fetch(a, b)
    }

    private fun fetch(a: String, b: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val da = async { ligandRepository.fetchLigand(a) }
            val db = async { ligandRepository.fetchLigand(b) }
            val ra = da.await()
            val rb = db.await()
            if (ra.isFailure) {
                _uiState.update { it.copy(isLoading = false, errorMessage = ra.exceptionOrNull()?.localizedMessage) }
                return@launch
            }
            if (rb.isFailure) {
                _uiState.update { it.copy(isLoading = false, errorMessage = rb.exceptionOrNull()?.localizedMessage) }
                return@launch
            }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    ligandA = ra.getOrNull(),
                    ligandB = rb.getOrNull()
                )
            }
        }
    }
}
