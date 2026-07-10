package com.music42.swiftyprotein.data.parser

import com.music42.swiftyprotein.data.model.Atom

internal fun parseCifAtoms(lines: List<String>): List<Atom> {
    val atoms = mutableListOf<Atom>()
    val loopRange = findCifLoopSection(lines, "_chem_comp_atom.") ?: return atoms

    val headers = loopRange.first
    val dataLines = loopRange.second

    val idxAtomId = headers.indexOf("_chem_comp_atom.atom_id")
    val idxElement = headers.indexOf("_chem_comp_atom.type_symbol")
    val idxX = headers.indexOf("_chem_comp_atom.pdbx_model_Cartn_x_ideal")
    val idxY = headers.indexOf("_chem_comp_atom.pdbx_model_Cartn_y_ideal")
    val idxZ = headers.indexOf("_chem_comp_atom.pdbx_model_Cartn_z_ideal")

    val idxXFall = if (idxX >= 0) idxX else headers.indexOf("_chem_comp_atom.model_Cartn_x")
    val idxYFall = if (idxY >= 0) idxY else headers.indexOf("_chem_comp_atom.model_Cartn_y")
    val idxZFall = if (idxZ >= 0) idxZ else headers.indexOf("_chem_comp_atom.model_Cartn_z")

    if (idxAtomId < 0 || idxElement < 0 || idxXFall < 0 || idxYFall < 0 || idxZFall < 0) {
        return atoms
    }

    for (line in dataLines) {
        val tokens = tokenizeCifLine(line)
        if (tokens.size <= maxOf(idxAtomId, idxElement, idxXFall, idxYFall, idxZFall)) continue

        val x = tokens[idxXFall].toFloatOrNull() ?: continue
        val y = tokens[idxYFall].toFloatOrNull() ?: continue
        val z = tokens[idxZFall].toFloatOrNull() ?: continue

        atoms.add(
            Atom(
                id = tokens[idxAtomId],
                element = extractElementSymbol(
                    rawTypeSymbol = tokens[idxElement],
                    atomId = tokens[idxAtomId]
                ),
                x = x, y = y, z = z
            )
        )
    }
    return atoms
}
