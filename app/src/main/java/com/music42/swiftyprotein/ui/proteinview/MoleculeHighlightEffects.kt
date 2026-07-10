package com.music42.swiftyprotein.ui.proteinview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import com.music42.swiftyprotein.data.model.Atom
import com.music42.swiftyprotein.data.model.Bond
import com.music42.swiftyprotein.ui.proteinview.MoleculeSceneBuilder.BondMeshInfo
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.node.MeshNode

@Composable
internal fun MoleculeAtomHighlightEffect(
    atomNodeMap: Map<MeshNode, Atom>,
    materialLoader: MaterialLoader,
    selectedAtom: Atom?,
    measurementMode: Boolean,
    measurementAtomIds: List<String>,
    measurementBonds: List<Bond>
) {
    LaunchedEffect(selectedAtom?.id, measurementMode, measurementAtomIds, measurementBonds) {
        val measureAccent = Color(0xFFFF6A00)
        val measurementAtomIdSet: Set<String> = if (measurementMode) {
            if (measurementBonds.isNotEmpty()) {
                emptySet()
            } else {
                measurementAtomIds.takeLast(2).toSet()
            }
        } else {
            emptySet()
        }

        val selected = selectedAtom
        val selectedElement = selected?.element?.uppercase()?.trim()

        for ((node, atom) in atomNodeMap) {
            val base = com.music42.swiftyprotein.util.CpkColors.getColor(atom.element)
            val color = if (measurementMode) {
                if (measurementAtomIdSet.contains(atom.id)) {
                    val blend = 0.65f
                    Color(
                        base.red + (measureAccent.red - base.red) * blend,
                        base.green + (measureAccent.green - base.green) * blend,
                        base.blue + (measureAccent.blue - base.blue) * blend,
                        1f
                    )
                } else {
                    base
                }
            } else {
                val isSelected = selected != null && atom.id == selected.id
                val isSameElement = selectedElement != null &&
                    atom.element.uppercase().trim() == selectedElement
                when {
                    isSelected -> {
                        val blend = 0.45f
                        Color(
                            base.red + (1f - base.red) * blend,
                            base.green + (1f - base.green) * blend,
                            base.blue + (1f - base.blue) * blend,
                            1f
                        )
                    }
                    isSameElement -> {
                        val factor = 0.55f
                        Color(base.red * factor, base.green * factor, base.blue * factor, 1f)
                    }
                    else -> base
                }
            }

            runCatching {
                node.materialInstance = materialLoader.createColorInstance(
                    color = color,
                    metallic = 0.0f,
                    roughness = 0.6f,
                    reflectance = 0.3f
                )
            }
        }
    }
}

@Composable
internal fun MoleculeBondHighlightEffect(
    bondNodeMap: Map<MeshNode, BondMeshInfo>,
    materialLoader: MaterialLoader,
    selectedBond: Bond?,
    measurementMode: Boolean,
    measurementBonds: List<Bond>,
    mode: VisualizationMode,
    showHydrogens: Boolean
) {
    LaunchedEffect(selectedBond, measurementMode, measurementBonds, mode, showHydrogens) {
        val selected = selectedBond
        val measured = if (measurementMode) measurementBonds else emptyList()

        for ((node, info) in bondNodeMap) {
            val base = info.baseColor
            val isMeasured = measured.any { sameBond(it, info.bond) }
            val isSelected = selected != null && sameBond(selected, info.bond)
            val color = when {
                isMeasured -> Color(0.90f, 0.35f, 0.05f, 1f)
                isSelected -> Color(0.00f, 0.82f, 0.98f, 1f)
                else -> base
            }

            node.materialInstance = materialLoader.createColorInstance(
                color = color,
                metallic = 0.0f,
                roughness = 0.4f,
                reflectance = 0.5f
            )
        }
    }
}
