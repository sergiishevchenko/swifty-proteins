package com.music42.swiftyprotein.ui.proteinview

import com.music42.swiftyprotein.data.model.Atom
import com.music42.swiftyprotein.data.model.Ligand
import io.github.sceneview.math.Position

internal fun ligandCenterAtoms(ligand: Ligand): List<Atom> {
    return ligand.atoms.filterNot {
        val element = it.element.uppercase().trim()
        element == "H" || element == "D"
    }.ifEmpty { ligand.atoms }
}

internal fun computeBoundingRadius(
    ligand: Ligand,
    mode: VisualizationMode,
    centerAtoms: List<Atom>,
    cx: Float,
    cy: Float,
    cz: Float
): Float {
    return (centerAtoms.maxOfOrNull { atom ->
        val dx = atom.x - cx
        val dy = atom.y - cy
        val dz = atom.z - cz
        val dist = kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
        val visualRadius = if (mode == VisualizationMode.SPACE_FILL) {
            MoleculeSceneBuilder.BALL_RADIUS * MoleculeSceneBuilder.SPACE_FILL_BASE_SCALE *
                (com.music42.swiftyprotein.util.VdwRadii.radiusAngstrom(atom.element) / MoleculeSceneBuilder.SPACE_FILL_REF_VDW)
        } else {
            MoleculeSceneBuilder.BALL_RADIUS
        }
        dist + visualRadius
    } ?: 5f).coerceAtLeast(1f)
}

internal fun computeCameraPosition(
    lastCameraVector: FloatArray,
    distance: Float,
    defaultCamX: Float,
    defaultCamY: Float,
    defaultCamZ: Float,
    defaultCamLen: Float
): Position {
    val x = lastCameraVector[0]
    val y = lastCameraVector[1]
    val z = lastCameraVector[2]
    val len = kotlin.math.sqrt(x * x + y * y + z * z)
    return if (len > 0.0001f) {
        Position(x / len * distance, y / len * distance, z / len * distance)
    } else {
        Position(
            defaultCamX / defaultCamLen * distance,
            defaultCamY / defaultCamLen * distance,
            defaultCamZ / defaultCamLen * distance
        )
    }
}

internal fun resetDefaultCameraVector(
    lastCameraVector: FloatArray,
    defaultCamX: Float,
    defaultCamY: Float,
    defaultCamZ: Float,
    defaultCamLen: Float,
    baseCameraDistance: Float
) {
    lastCameraVector[0] = defaultCamX / defaultCamLen * baseCameraDistance
    lastCameraVector[1] = defaultCamY / defaultCamLen * baseCameraDistance
    lastCameraVector[2] = defaultCamZ / defaultCamLen * baseCameraDistance
}
