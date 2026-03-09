package com.music42.swiftyprotein.util

import androidx.compose.ui.graphics.Color

object CpkColors {

    private val colorMap = mapOf(
        "H"  to Color(0xFFEDEDED),
        "HE" to Color(0xFFD9FFFF),
        "LI" to Color(0xFFCC80FF),
        "BE" to Color(0xFFC2FF00),
        "B"  to Color(0xFFFFB5B5),
        "C"  to Color(0xFF1F8F1F),
        "N"  to Color(0xFF3050F8),
        "O"  to Color(0xFFFF0D0D),
        "F"  to Color(0xFF90E050),
        "NE" to Color(0xFFB3E3F5),
        "NA" to Color(0xFFAB5CF2),
        "MG" to Color(0xFF8AFF00),
        "AL" to Color(0xFFBFA6A6),
        "SI" to Color(0xFFF0C8A0),
        "P"  to Color(0xFFFF8000),
        "S"  to Color(0xFFFFFF30),
        "CL" to Color(0xFF1FF01F),
        "AR" to Color(0xFF80D1E3),
        "K"  to Color(0xFF8F40D4),
        "CA" to Color(0xFF3DFF00),
        "SC" to Color(0xFFE6E6E6),
        "TI" to Color(0xFFBFC2C7),
        "V"  to Color(0xFFA6A6AB),
        "CR" to Color(0xFF8A99C7),
        "MN" to Color(0xFF9C7AC7),
        "FE" to Color(0xFFE06633),
        "CO" to Color(0xFFF090A0),
        "NI" to Color(0xFF50D050),
        "CU" to Color(0xFFC88033),
        "ZN" to Color(0xFF7D80B0),
        "GA" to Color(0xFFC28F8F),
        "GE" to Color(0xFF668F8F),
        "AS" to Color(0xFFBD80E3),
        "SE" to Color(0xFFFFA100),
        "BR" to Color(0xFFA62929),
        "KR" to Color(0xFF5CB8D1),
        "RB" to Color(0xFF702EB0),
        "SR" to Color(0xFF00FF00),
        "Y"  to Color(0xFF94FFFF),
        "ZR" to Color(0xFF94E0E0),
        "MO" to Color(0xFF54B5B5),
        "AG" to Color(0xFFC0C0C0),
        "CD" to Color(0xFFFFD98F),
        "IN" to Color(0xFFA67573),
        "SN" to Color(0xFF668080),
        "SB" to Color(0xFF9E63B5),
        "TE" to Color(0xFFD47A00),
        "I"  to Color(0xFF940094),
        "XE" to Color(0xFF429EB0),
        "BA" to Color(0xFF00C900),
        "PT" to Color(0xFFD0D0E0),
        "AU" to Color(0xFFFFD123),
        "HG" to Color(0xFFB8B8D0),
        "PB" to Color(0xFF575961),
        "BI" to Color(0xFF9E4FB5),
    )

    private val DEFAULT = Color(0xFF1F8F1F)

    fun getColor(element: String): Color {
        val normalized = element.uppercase().trim().replace(Regex("[^A-Z]"), "")
        val key = when {
            normalized.length >= 2 && normalized.substring(0, 2) in colorMap.keys -> normalized.substring(0, 2)
            normalized.isNotEmpty() -> normalized.substring(0, 1)
            else -> ""
        }
        return colorMap[key] ?: DEFAULT
    }

    fun getColorFloats(element: String): FloatArray {
        val color = getColor(element)
        return floatArrayOf(color.red, color.green, color.blue, 1.0f)
    }
}
