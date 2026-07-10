package com.music42.swiftyprotein.data.parser

import com.music42.swiftyprotein.data.model.Bond
import com.music42.swiftyprotein.data.model.BondOrder

internal fun parseCifBonds(lines: List<String>): List<Bond> {
    val bonds = mutableListOf<Bond>()
    val loopRange = findCifLoopSection(lines, "_chem_comp_bond.") ?: return bonds

    val headers = loopRange.first
    val dataLines = loopRange.second

    val idxAtom1 = headers.indexOf("_chem_comp_bond.atom_id_1")
    val idxAtom2 = headers.indexOf("_chem_comp_bond.atom_id_2")
    val idxOrder = headers.indexOf("_chem_comp_bond.value_order")

    if (idxAtom1 < 0 || idxAtom2 < 0) return bonds

    for (line in dataLines) {
        val tokens = tokenizeCifLine(line)
        if (tokens.size <= maxOf(idxAtom1, idxAtom2, if (idxOrder >= 0) idxOrder else 0)) continue

        bonds.add(
            Bond(
                atomId1 = tokens[idxAtom1],
                atomId2 = tokens[idxAtom2],
                order = if (idxOrder >= 0 && idxOrder < tokens.size)
                    BondOrder.fromCif(tokens[idxOrder])
                else BondOrder.SINGLE
            )
        )
    }
    return bonds
}
