package com.music42.swiftyprotein.ui.proteinview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music42.swiftyprotein.data.model.Atom
import com.music42.swiftyprotein.data.model.Bond
import com.music42.swiftyprotein.data.model.Ligand
import com.music42.swiftyprotein.data.repository.LigandRepository
import com.music42.swiftyprotein.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class VisualizationMode {
    BALL_AND_STICK,
    SPACE_FILL,
    STICKS_ONLY,
    WIREFRAME
}

data class ProteinViewUiState(
    val ligandId: String = "",
    val ligand: Ligand? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val selectedAtom: Atom? = null,
    val selectedBond: Bond? = null,
    val visualizationMode: VisualizationMode = VisualizationMode.BALL_AND_STICK,
    val showAtomLabels: Boolean = false,
    val showHydrogens: Boolean = false,
    val measurementMode: Boolean = false,
    val measurementAtomIds: List<String> = emptyList(),
    val measurementBonds: List<Bond> = emptyList(),
    val loadingStage: String = "Loading",
    val loadingProgress: Float = 0f,
    val largeMoleculeWarning: Boolean = false
)

@HiltViewModel
class ProteinViewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val ligandRepository: LigandRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProteinViewUiState())
    val uiState: StateFlow<ProteinViewUiState> = _uiState.asStateFlow()

    companion object {
        const val LARGE_MOLECULE_THRESHOLD = 200
    }

    init {
        val ligandId: String = savedStateHandle["ligandId"] ?: ""
        _uiState.update { it.copy(ligandId = ligandId) }
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            _uiState.update {
                it.copy(
                    visualizationMode = settings.defaultVisualizationMode,
                    showHydrogens = settings.showHydrogensByDefault
                )
            }
        }
        fetchLigand(ligandId)
    }

    private fun fetchLigand(ligandId: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    loadingStage = "Starting",
                    loadingProgress = 0f
                )
            }
            val startMs = System.currentTimeMillis()
            val result = ligandRepository.fetchLigand(ligandId) { stage, progress ->
                _uiState.update { it.copy(loadingStage = stage, loadingProgress = progress) }
            }
            val elapsedMs = System.currentTimeMillis() - startMs
            val minVisibleMs = 350L
            if (elapsedMs < minVisibleMs) {
                delay(minVisibleMs - elapsedMs)
            }
            result.fold(
                onSuccess = { ligand ->
                    val isLarge = ligand.atoms.size > LARGE_MOLECULE_THRESHOLD
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            ligand = ligand,
                            loadingProgress = 1f,
                            largeMoleculeWarning = isLarge,
                            showHydrogens = if (isLarge) false else state.showHydrogens,
                            visualizationMode = if (isLarge && state.visualizationMode == VisualizationMode.SPACE_FILL)
                                VisualizationMode.WIREFRAME else state.visualizationMode
                        )
                    }
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
        _uiState.update { it.copy(selectedAtom = atom, selectedBond = null) }
    }

    fun dismissAtomInfo() {
        _uiState.update { it.copy(selectedAtom = null) }
    }

    fun onBondSelected(bond: Bond?) {
        _uiState.update { it.copy(selectedBond = bond, selectedAtom = null) }
    }

    fun dismissBondInfo() {
        _uiState.update { it.copy(selectedBond = null) }
    }

    fun setVisualizationMode(mode: VisualizationMode) {
        _uiState.update {
            it.copy(
                visualizationMode = mode,
                showAtomLabels = if (mode != VisualizationMode.BALL_AND_STICK) false else it.showAtomLabels,
                measurementMode = if (mode != VisualizationMode.BALL_AND_STICK) false else it.measurementMode
            )
        }
    }

    fun setShowAtomLabels(show: Boolean) {
        _uiState.update { it.copy(showAtomLabels = show) }
    }

    fun setShowHydrogens(show: Boolean) {
        _uiState.update { it.copy(showHydrogens = show) }
    }

    fun setMeasurementMode(enabled: Boolean) {
        _uiState.update {
            it.copy(
                measurementMode = enabled,
                measurementAtomIds = if (enabled) it.measurementAtomIds else emptyList(),
                measurementBonds = if (enabled) it.measurementBonds else emptyList(),
                selectedAtom = if (enabled) null else it.selectedAtom,
                selectedBond = if (enabled) null else it.selectedBond
            )
        }
    }

    fun onAtomTappedForMeasurement(atom: Atom) {
        val state = _uiState.value
        if (!state.measurementMode) return

        val next = state.measurementAtomIds.toMutableList()
        if (next.isNotEmpty() && next.last() == atom.id) return
        next.add(atom.id)
        while (next.size > 2) next.removeAt(0)
        _uiState.update {
            it.copy(
                measurementAtomIds = next,
                measurementBonds = emptyList()
            )
        }
    }

    fun onBondTappedForMeasurement(bond: Bond) {
        val state = _uiState.value
        if (!state.measurementMode) return

        val next = state.measurementBonds.toMutableList()
        if (next.isNotEmpty()) {
            val last = next.last()
            val same =
                (last.atomId1 == bond.atomId1 && last.atomId2 == bond.atomId2) ||
                    (last.atomId1 == bond.atomId2 && last.atomId2 == bond.atomId1)
            if (same) return
        }
        next.add(bond)
        while (next.size > 2) next.removeAt(0)
        _uiState.update {
            it.copy(
                measurementBonds = next,
                measurementAtomIds = emptyList()
            )
        }
    }

    fun clearMeasurement() {
        _uiState.update { it.copy(measurementAtomIds = emptyList(), measurementBonds = emptyList()) }
    }

    fun dismissLargeMoleculeWarning() {
        _uiState.update { it.copy(largeMoleculeWarning = false) }
    }
}
