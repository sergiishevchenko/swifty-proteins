package com.music42.swiftyprotein.ui.proteinview

import com.music42.swiftyprotein.data.model.Atom
import com.music42.swiftyprotein.data.model.Bond

internal fun appendMeasurementAtom(
    currentAtomIds: List<String>,
    atom: Atom
): List<String> {
    if (currentAtomIds.isNotEmpty() && currentAtomIds.last() == atom.id) {
        return currentAtomIds
    }
    val next = currentAtomIds.toMutableList()
    next.add(atom.id)
    while (next.size > 2) next.removeAt(0)
    return next
}

internal fun appendMeasurementBond(
    currentBonds: List<Bond>,
    bond: Bond
): List<Bond> {
    if (currentBonds.isNotEmpty()) {
        val last = currentBonds.last()
        val same = (last.atomId1 == bond.atomId1 && last.atomId2 == bond.atomId2) ||
            (last.atomId1 == bond.atomId2 && last.atomId2 == bond.atomId1)
        if (same) return currentBonds
    }
    val next = currentBonds.toMutableList()
    next.add(bond)
    while (next.size > 2) next.removeAt(0)
    return next
}
