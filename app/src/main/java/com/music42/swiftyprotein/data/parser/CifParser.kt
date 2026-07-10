package com.music42.swiftyprotein.data.parser

import com.music42.swiftyprotein.data.model.Ligand

object CifParser {

    fun parse(ligandId: String, cifText: String): Ligand {
        val lines = cifText.lines()
        val name = parseCifName(lines)
        val formula = parseCifFormula(lines)
        val atoms = parseCifAtoms(lines)
        val bonds = parseCifBonds(lines)
        return Ligand(
            id = ligandId,
            name = name,
            formula = formula,
            atoms = atoms,
            bonds = bonds
        )
    }
}
