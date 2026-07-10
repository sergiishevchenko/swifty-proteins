package com.music42.swiftyprotein.ui.proteinview

import com.music42.swiftyprotein.data.model.Atom
import com.music42.swiftyprotein.data.model.Bond
import com.music42.swiftyprotein.data.model.BondOrder
import com.music42.swiftyprotein.data.model.Ligand
import io.github.sceneview.node.CameraNode
import io.github.sceneview.node.MeshNode
import kotlin.math.hypot

internal data class BondPickCandidate(
    val bond: Bond,
    val distancePx: Float,
    val thresholdPx: Float
)

internal fun worldPositionToScreenPx(
    worldPos: io.github.sceneview.math.Position,
    cameraNode: CameraNode,
    viewWidthPx: Int,
    viewHeightPx: Int
): Pair<Float, Float> {
    val view = cameraNode.worldToView(worldPos)
    return view.x * viewWidthPx.toFloat() to (1f - view.y) * viewHeightPx.toFloat()
}

private fun bondPickThresholdPx(screenSegmentLengthPx: Float, order: BondOrder): Float {
    val along = (screenSegmentLengthPx * 0.45f + 44f).coerceIn(60f, 120f)
    val orderExtra = when (order) {
        BondOrder.TRIPLE -> 16f
        BondOrder.DOUBLE, BondOrder.AROMATIC -> 12f
        else -> 0f
    }
    return along + orderExtra
}

internal fun pickBond(
    tapPxX: Float,
    tapPxY: Float,
    viewWidthPx: Int,
    viewHeightPx: Int,
    cameraNode: CameraNode,
    atomNodeMap: Map<MeshNode, Atom>,
    ligand: Ligand
): BondPickCandidate? {
    val nodeByAtomId = atomNodeMap.entries.associate { it.value.id to it.key }
    var best: BondPickCandidate? = null

    fun consider(bond: Bond, distancePx: Float, thresholdPx: Float) {
        if (distancePx > thresholdPx) return
        if (best == null || distancePx < best!!.distancePx) {
            best = BondPickCandidate(bond, distancePx, thresholdPx)
        }
    }

    for (bond in ligand.bonds) {
        val n1 = nodeByAtomId[bond.atomId1] ?: continue
        val n2 = nodeByAtomId[bond.atomId2] ?: continue
        val (x1, y1) = worldPositionToScreenPx(n1.worldPosition, cameraNode, viewWidthPx, viewHeightPx)
        val (x2, y2) = worldPositionToScreenPx(n2.worldPosition, cameraNode, viewWidthPx, viewHeightPx)
        val d = pointToSegmentDistanceForBondPick(tapPxX, tapPxY, x1, y1, x2, y2)
        val segLen = hypot(x2 - x1, y2 - y1)
        consider(bond, d, bondPickThresholdPx(segLen, bond.order))
    }

    return best
}

private fun pointToSegmentDistanceForBondPick(
    px: Float,
    py: Float,
    x1: Float,
    y1: Float,
    x2: Float,
    y2: Float
): Float {
    val vx = x2 - x1
    val vy = y2 - y1
    val vv = vx * vx + vy * vy
    if (vv <= 1e-12f) {
        return hypot(px - x1, py - y1)
    }
    val wx = px - x1
    val wy = py - y1
    val t = ((wx * vx + wy * vy) / vv).coerceIn(0f, 1f)
    val cx = x1 + t * vx
    val cy = y1 + t * vy
    val d = hypot(px - cx, py - cy)
    val segLen = hypot(vx, vy)
    val guard = minOf(32f, segLen * 0.24f)
    val distFromStart = hypot(cx - x1, cy - y1)
    val distFromEnd = hypot(cx - x2, cy - y2)
    val endpointPenalty = if (distFromStart < guard || distFromEnd < guard) guard * 1.4f else 0f
    return d + endpointPenalty
}
