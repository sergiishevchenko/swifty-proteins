package com.music42.swiftyprotein.data.parser

import com.music42.swiftyprotein.data.model.Atom
import com.music42.swiftyprotein.data.model.Bond
import com.music42.swiftyprotein.data.model.BondOrder
import com.music42.swiftyprotein.data.model.Ligand

object CifParser {

    fun parse(ligandId: String, cifText: String): Ligand {
        val lines = cifText.lines()
        val name = parseName(lines)
        val atoms = parseAtoms(lines)
        val bonds = parseBonds(lines)
        return Ligand(
            id = ligandId,
            name = name,
            atoms = atoms,
            bonds = bonds
        )
    }

    private fun parseName(lines: List<String>): String {
        for (i in lines.indices) {
            val line = lines[i].trim()
            if (line.startsWith("_chem_comp.name")) {
                val inlineValue = line.removePrefix("_chem_comp.name").trim()
                if (inlineValue.isNotEmpty()) {
                    return inlineValue.trim('"', '\'', ' ')
                }
                if (i + 1 < lines.size) {
                    val nextLine = lines[i + 1].trim()
                    return nextLine.trim('"', '\'', ';', ' ')
                }
            }
        }
        return ""
    }

    private fun parseAtoms(lines: List<String>): List<Atom> {
        val atoms = mutableListOf<Atom>()
        val loopRange = findLoopSection(lines, "_chem_comp_atom.") ?: return atoms

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
                    element = tokens[idxElement],
                    x = x, y = y, z = z
                )
            )
        }
        return atoms
    }

    private fun parseBonds(lines: List<String>): List<Bond> {
        val bonds = mutableListOf<Bond>()
        val loopRange = findLoopSection(lines, "_chem_comp_bond.") ?: return bonds

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

    private fun findLoopSection(
        lines: List<String>,
        prefix: String
    ): Pair<List<String>, List<String>>? {
        var i = 0
        while (i < lines.size) {
            if (lines[i].trim() == "loop_") {
                val headers = mutableListOf<String>()
                var j = i + 1
                while (j < lines.size && lines[j].trim().startsWith("_")) {
                    headers.add(lines[j].trim())
                    j++
                }
                if (headers.any { it.startsWith(prefix) }) {
                    val dataLines = mutableListOf<String>()
                    while (j < lines.size) {
                        val trimmed = lines[j].trim()
                        if (trimmed.isEmpty() || trimmed == "#" || trimmed == "loop_" ||
                            trimmed.startsWith("_") || trimmed.startsWith("data_")
                        ) break
                        dataLines.add(trimmed)
                        j++
                    }
                    return headers to dataLines
                }
            }
            i++
        }
        return null
    }

    private fun tokenizeCifLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        var i = 0
        while (i < line.length) {
            when {
                line[i].isWhitespace() -> i++
                line[i] == '\'' || line[i] == '"' -> {
                    val quote = line[i]
                    val start = i + 1
                    i = start
                    while (i < line.length && !(line[i] == quote && (i + 1 >= line.length || line[i + 1].isWhitespace()))) {
                        i++
                    }
                    tokens.add(line.substring(start, i))
                    if (i < line.length) i++
                }
                else -> {
                    val start = i
                    while (i < line.length && !line[i].isWhitespace()) i++
                    tokens.add(line.substring(start, i))
                }
            }
        }
        return tokens
    }
}
