package com.music42.swiftyprotein.util

import androidx.compose.ui.graphics.Color

object CpkColors {

    private val colorMap = mapOf(
        "H"  to Color(0xFFFFFFFF),  // White
        "C"  to Color(0xFF555555),  // Dark gray
        "N"  to Color(0xFF3050F8),  // Blue
        "O"  to Color(0xFFFF0D0D),  // Red
        "F"  to Color(0xFF90E050),  // Green
        "CL" to Color(0xFF1FF01F),  // Green
        "BR" to Color(0xFFA62929),  // Dark red
        "I"  to Color(0xFF940094),  // Dark violet
        "HE" to Color(0xFFD9FFFF),  // Cyan
        "NE" to Color(0xFFB3E3F5),  // Cyan-ish
        "AR" to Color(0xFF80D1E3),  // Cyan-ish
        "KR" to Color(0xFF5CB8D1),  // Cyan-ish
        "XE" to Color(0xFF429EB0),  // Cyan-ish
        "P"  to Color(0xFFFF8000),  // Orange
        "S"  to Color(0xFFFFFF30),  // Yellow
        "B"  to Color(0xFFFFB5B5),  // Salmon
        "LI" to Color(0xFFCC80FF),  // Violet
        "NA" to Color(0xFFAB5CF2),  // Violet
        "K"  to Color(0xFF8F40D4),  // Violet
        "BE" to Color(0xFFC2FF00),  // Yellow-green
        "MG" to Color(0xFF8AFF00),  // Green
        "CA" to Color(0xFF3DFF00),  // Green
        "TI" to Color(0xFFBFC2C7),  // Gray
        "CR" to Color(0xFF8A99C7),  // Steel blue
        "MN" to Color(0xFF9C7AC7),  // Purple
        "FE" to Color(0xFFE06633),  // Orange
        "CO" to Color(0xFFF090A0),  // Pink
        "NI" to Color(0xFF50D050),  // Green
        "CU" to Color(0xFFC88033),  // Copper
        "ZN" to Color(0xFF7D80B0),  // Slate
        "SE" to Color(0xFFFFA100),  // Orange
        "AG" to Color(0xFFC0C0C0),  // Silver
        "AU" to Color(0xFFFFD123),  // Gold
        "PT" to Color(0xFFD0D0E0),  // Light gray
    )

    private val DEFAULT = Color(0xFFFF1493) // Deep pink

    fun getColor(element: String): Color {
        return colorMap[element.uppercase().trim()] ?: DEFAULT
    }

    fun getColorFloats(element: String): FloatArray {
        val color = getColor(element)
        return floatArrayOf(color.red, color.green, color.blue, 1.0f)
    }
}
