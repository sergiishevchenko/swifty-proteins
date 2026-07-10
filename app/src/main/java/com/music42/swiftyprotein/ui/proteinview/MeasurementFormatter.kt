package com.music42.swiftyprotein.ui.proteinview

import com.music42.swiftyprotein.data.model.Atom
import com.music42.swiftyprotein.data.model.Bond
import com.music42.swiftyprotein.data.model.Ligand
import dev.romainguy.kotlin.math.Float3

internal fun angleDegrees(vertex: Atom, arm1: Atom, arm2: Atom): Float {
    val v1 = Float3(arm1.x - vertex.x, arm1.y - vertex.y, arm1.z - vertex.z)
    val v2 = Float3(arm2.x - vertex.x, arm2.y - vertex.y, arm2.z - vertex.z)
    val dot = v1.x * v2.x + v1.y * v2.y + v1.z * v2.z
    val len1 = kotlin.math.sqrt(v1.x * v1.x + v1.y * v1.y + v1.z * v1.z)
    val len2 = kotlin.math.sqrt(v2.x * v2.x + v2.y * v2.y + v2.z * v2.z)
    if (len1 <= 1e-6f || len2 <= 1e-6f) return 0f
    val cos = (dot / (len1 * len2)).coerceIn(-1f, 1f)
    return kotlin.math.acos(cos) * 180f / kotlin.math.PI.toFloat()
}

internal fun formatAtomDistance(atoms: List<Atom>): String {
    val a = atoms[atoms.size - 2]
    val b = atoms[atoms.size - 1]
    val dx = a.x - b.x
    val dy = a.y - b.y
    val dz = a.z - b.z
    val d = kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
    return "Distance ${a.id}–${b.id}: ${String.format("%.2f Å", d)}"
}

internal fun formatBondAngle(ligand: Ligand, selectedBonds: List<Bond>): String {
    val b1 = selectedBonds[selectedBonds.size - 2]
    val b2 = selectedBonds[selectedBonds.size - 1]
    val common = sequenceOf(b1.atomId1, b1.atomId2).firstOrNull { it == b2.atomId1 || it == b2.atomId2 }
        ?: return "Angle: pick 2 bonds sharing one atom"
    val vertex = ligand.atoms.firstOrNull { it.id == common } ?: return "Angle: unavailable"
    val other1Id = if (b1.atomId1 == common) b1.atomId2 else b1.atomId1
    val other2Id = if (b2.atomId1 == common) b2.atomId2 else b2.atomId1
    val p1 = ligand.atoms.firstOrNull { it.id == other1Id }
    val p2 = ligand.atoms.firstOrNull { it.id == other2Id }
    if (p1 == null || p2 == null) return "Angle: unavailable"
    val angle = angleDegrees(vertex, p1, p2)
    return "Angle at ${vertex.id}: ${String.format("%.1f°", angle)}"
}
