package com.music42.swiftyprotein.ui.proteinview

import com.music42.swiftyprotein.data.model.Atom
import com.music42.swiftyprotein.data.model.Bond
import com.music42.swiftyprotein.data.model.Ligand
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.SceneView
import io.github.sceneview.node.CameraNode
import io.github.sceneview.node.MeshNode

internal data class MoleculeTapResult(
    val bondHit: Boolean,
    val bondPick: BondPickCandidate?,
    val atomWins: Boolean,
    val closestAtom: Atom?
)

internal fun resolveMoleculeTap(
    tapX: Float,
    tapY: Float,
    sceneView: SceneView,
    cameraNode: CameraNode,
    atomNodeMap: Map<MeshNode, Atom>,
    ligand: Ligand
): MoleculeTapResult {
    val tapNormX = tapX / sceneView.width.toFloat()
    val tapNormY = 1f - tapY / sceneView.height.toFloat()
    val bondPick = pickBond(
        tapPxX = tapX,
        tapPxY = tapY,
        viewWidthPx = sceneView.width,
        viewHeightPx = sceneView.height,
        cameraNode = cameraNode,
        atomNodeMap = atomNodeMap,
        ligand = ligand
    )

    var closestAtom: Atom? = null
    var closestDist = Float.MAX_VALUE
    for ((meshNode, atom) in atomNodeMap) {
        val viewPos = cameraNode.worldToView(meshNode.worldPosition)
        val dx = tapNormX - viewPos.x
        val dy = tapNormY - viewPos.y
        val distance = kotlin.math.sqrt(dx * dx + dy * dy)
        if (distance < closestDist) {
            closestDist = distance
            closestAtom = atom
        }
    }

    val atomDistPx = closestDist * sceneView.width.toFloat()
    val atomHit = closestAtom != null && closestDist < ATOM_PICK_RADIUS_NORM
    val bondDistPx = bondPick?.distancePx ?: Float.MAX_VALUE
    val bondScreenHit = bondPick != null && bondDistPx <= bondPick.thresholdPx
    val bondHit = bondScreenHit && (!atomHit || bondDistPx + 18f < atomDistPx)
    val atomWins = atomHit && (!bondScreenHit || atomDistPx + 18f <= bondDistPx)

    return MoleculeTapResult(
        bondHit = bondHit,
        bondPick = bondPick,
        atomWins = atomWins,
        closestAtom = closestAtom
    )
}

internal fun focusTargetForDoubleTap(
    ligand: Ligand,
    atom: Atom
): Float3 {
    val centerAtoms = ligandCenterAtoms(ligand)
    val cx = centerAtoms.map { it.x }.average().toFloat()
    val cy = centerAtoms.map { it.y }.average().toFloat()
    val cz = centerAtoms.map { it.z }.average().toFloat()
    return Float3(atom.x - cx, atom.y - cy, atom.z - cz)
}

internal fun handleMoleculeTapResult(
    result: MoleculeTapResult,
    measurementMode: Boolean,
    ligand: Ligand,
    eventTime: Long,
    lastTap: LongArray,
    lastTapAtomId: Array<String?>,
    onMeasurementBondTapped: (Bond) -> Unit,
    onMeasurementAtomTapped: (Atom) -> Unit,
    onBondSelected: (Bond) -> Unit,
    onAtomSelected: (Atom) -> Unit,
    onDismissAtom: () -> Unit,
    onDismissBond: () -> Unit,
    onFocusTarget: (Float3) -> Unit
) {
    if (measurementMode) {
        when {
            result.bondHit -> onMeasurementBondTapped(result.bondPick!!.bond)
            result.atomWins -> onMeasurementAtomTapped(result.closestAtom!!)
        }
        return
    }

    when {
        result.bondHit -> onBondSelected(result.bondPick!!.bond)
        result.atomWins -> {
            val atom = result.closestAtom!!
            onAtomSelected(atom)
            val prev = lastTap[0]
            val prevId = lastTapAtomId[0]
            if (prevId == atom.id && eventTime - prev < 550L) {
                onFocusTarget(focusTargetForDoubleTap(ligand, atom))
            }
            lastTap[0] = eventTime
            lastTapAtomId[0] = atom.id
        }
        else -> {
            onDismissAtom()
            onDismissBond()
        }
    }
}
