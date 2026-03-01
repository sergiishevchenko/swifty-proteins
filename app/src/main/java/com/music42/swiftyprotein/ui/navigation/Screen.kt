package com.music42.swiftyprotein.ui.navigation

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object ProteinList : Screen("protein_list")
    data object ProteinView : Screen("protein_view/{ligandId}") {
        fun createRoute(ligandId: String) = "protein_view/$ligandId"
    }
}
