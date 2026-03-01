package com.music42.swiftyprotein.data.model

data class Bond(
    val atomId1: String,
    val atomId2: String,
    val order: BondOrder
)

enum class BondOrder {
    SINGLE, DOUBLE, TRIPLE, AROMATIC;

    companion object {
        fun fromCif(value: String): BondOrder = when (value.uppercase()) {
            "SING" -> SINGLE
            "DOUB" -> DOUBLE
            "TRIP" -> TRIPLE
            "AROM" -> AROMATIC
            else -> SINGLE
        }
    }
}
