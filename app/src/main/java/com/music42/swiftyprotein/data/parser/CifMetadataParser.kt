package com.music42.swiftyprotein.data.parser

internal fun parseCifName(lines: List<String>): String {
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

internal fun parseCifFormula(lines: List<String>): String {
    for (i in lines.indices) {
        val line = lines[i].trim()
        if (line.startsWith("_chem_comp.formula")) {
            val inlineValue = line.removePrefix("_chem_comp.formula").trim()
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
