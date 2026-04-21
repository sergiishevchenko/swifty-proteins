package com.music42.swiftyprotein.data.model

data class Ligand(
    val id: String,
    val name: String,
    val formula: String,
    val atoms: List<Atom>,
    val bonds: List<Bond>
)
