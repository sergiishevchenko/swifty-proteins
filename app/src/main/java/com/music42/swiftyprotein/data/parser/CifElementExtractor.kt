package com.music42.swiftyprotein.data.parser

private val TWO_LETTER_ELEMENTS = setOf(
    "HE", "LI", "BE", "NE", "NA", "MG", "AL", "SI", "CL", "AR", "CA", "SC", "TI",
    "CR", "MN", "FE", "CO", "NI", "CU", "ZN", "GA", "GE", "AS", "SE", "BR", "KR",
    "RB", "SR", "ZR", "MO", "AG", "CD", "IN", "SN", "SB", "TE", "XE", "BA", "PT",
    "AU", "HG", "PB", "BI"
)

internal fun extractElementSymbol(rawTypeSymbol: String, atomId: String): String {
    val fromType = rawTypeSymbol
        .trim()
        .uppercase()
        .replace(Regex("[^A-Z]"), "")
    if (fromType.isNotEmpty()) {
        return when {
            fromType.length >= 2 && fromType.substring(0, 2) in TWO_LETTER_ELEMENTS -> fromType.substring(0, 2)
            else -> fromType.substring(0, 1)
        }
    }

    val fromAtomId = atomId
        .trim()
        .uppercase()
        .replace(Regex("[^A-Z]"), "")
    if (fromAtomId.isNotEmpty()) {
        return when {
            fromAtomId.length >= 2 && fromAtomId.substring(0, 2) in TWO_LETTER_ELEMENTS -> fromAtomId.substring(0, 2)
            else -> fromAtomId.substring(0, 1)
        }
    }

    return "C"
}
