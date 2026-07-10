package com.music42.swiftyprotein.ui.proteinview

import com.music42.swiftyprotein.data.model.Atom
import com.music42.swiftyprotein.data.model.Bond
import com.music42.swiftyprotein.data.model.Ligand

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
