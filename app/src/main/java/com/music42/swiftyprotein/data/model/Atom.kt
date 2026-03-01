package com.music42.swiftyprotein.data.model

data class Atom(
    val id: String,
    val element: String,
    val x: Float,
    val y: Float,
    val z: Float
) {
    val elementName: String
        get() = ELEMENT_NAMES[element.uppercase()] ?: element

    companion object {
        private val ELEMENT_NAMES = mapOf(
            "H" to "Hydrogen", "HE" to "Helium", "LI" to "Lithium", "BE" to "Beryllium",
            "B" to "Boron", "C" to "Carbon", "N" to "Nitrogen", "O" to "Oxygen",
            "F" to "Fluorine", "NE" to "Neon", "NA" to "Sodium", "MG" to "Magnesium",
            "AL" to "Aluminum", "SI" to "Silicon", "P" to "Phosphorus", "S" to "Sulfur",
            "CL" to "Chlorine", "AR" to "Argon", "K" to "Potassium", "CA" to "Calcium",
            "TI" to "Titanium", "V" to "Vanadium", "CR" to "Chromium", "MN" to "Manganese",
            "FE" to "Iron", "CO" to "Cobalt", "NI" to "Nickel", "CU" to "Copper",
            "ZN" to "Zinc", "GA" to "Gallium", "GE" to "Germanium", "AS" to "Arsenic",
            "SE" to "Selenium", "BR" to "Bromine", "KR" to "Krypton", "RB" to "Rubidium",
            "SR" to "Strontium", "Y" to "Yttrium", "ZR" to "Zirconium", "MO" to "Molybdenum",
            "AG" to "Silver", "CD" to "Cadmium", "IN" to "Indium", "SN" to "Tin",
            "SB" to "Antimony", "TE" to "Tellurium", "I" to "Iodine", "XE" to "Xenon",
            "BA" to "Barium", "PT" to "Platinum", "AU" to "Gold", "HG" to "Mercury",
            "PB" to "Lead", "BI" to "Bismuth"
        )
    }
}
