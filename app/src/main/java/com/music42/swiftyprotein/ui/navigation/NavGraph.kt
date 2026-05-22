package com.music42.swiftyprotein.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.hilt.navigation.compose.hiltViewModel
import com.music42.swiftyprotein.ui.compare.CompareScreen
import com.music42.swiftyprotein.ui.favorites.FavoritesScreen
import com.music42.swiftyprotein.ui.onboarding.OnboardingScreen
import com.music42.swiftyprotein.ui.login.LoginScreen
import com.music42.swiftyprotein.ui.proteinlist.ProteinListScreen
import com.music42.swiftyprotein.ui.proteinview.ProteinViewScreen
import com.music42.swiftyprotein.ui.settings.SettingsScreen
import com.music42.swiftyprotein.ui.settings.SettingsViewModel
import com.music42.swiftyprotein.ui.session.SessionViewModel

@Composable
fun SwiftyProteinNavHost(
    shouldShowLogin: Boolean,
    onLoginShown: () -> Unit,
    navController: NavHostController = rememberNavController()
) {
    val sessionViewModel: SessionViewModel = hiltViewModel()
    val username by sessionViewModel.username.collectAsState()
    var showLogoutConfirm by remember { mutableStateOf(false) }

    val onLogoutRequest: () -> Unit = { showLogoutConfirm = true }
    val onLogoutConfirmed: () -> Unit = {
        showLogoutConfirm = false
        sessionViewModel.logout()
        navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
    }

    LogoutConfirmDialog(
        visible = showLogoutConfirm,
        onDismiss = { showLogoutConfirm = false },
        onConfirm = onLogoutConfirmed,
    )

    LaunchedEffect(shouldShowLogin) {
        if (shouldShowLogin) {
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
            onLoginShown()
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val settings by settingsViewModel.settings.collectAsState()
            LoginScreen(
                onLoginSuccess = { forceOnboarding ->
                    sessionViewModel.refresh()
                    val next = if (forceOnboarding || !settings.onboardingCompleted) {
                        Screen.Onboarding.route
                    } else {
                        Screen.ProteinList.route
                    }
                    navController.navigate(next) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onDone = {
                    navController.navigate(Screen.ProteinList.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.ProteinList.route) {
            ProteinListScreen(
                onLigandSelected = { ligandId ->
                    navController.navigate(Screen.ProteinView.createRoute(ligandId))
                },
                onOpenFavorites = {
                    navController.navigate(Screen.Favorites.route)
                },
                onOpenSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                currentUsername = username,
                onLogout = onLogoutRequest
            )
        }

        composable(Screen.Favorites.route) {
            FavoritesScreen(
                onBack = { navController.popBackStack() },
                onLigandSelected = { ligandId ->
                    navController.navigate(Screen.ProteinView.createRoute(ligandId))
                },
                onCompareSelected = { a, b ->
                    navController.navigate(Screen.Compare.createRoute(a, b))
                },
                currentUsername = username,
                onLogout = onLogoutRequest
            )
        }

        composable(
            route = Screen.Compare.route,
            arguments = listOf(
                navArgument("ligandA") { type = NavType.StringType },
                navArgument("ligandB") { type = NavType.StringType }
            )
        ) {
            CompareScreen(
                onBack = { navController.popBackStack() },
                currentUsername = username,
                onLogout = onLogoutRequest
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onShowOnboarding = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Settings.route) { inclusive = true }
                    }
                },
                currentUsername = username,
                onLogout = onLogoutRequest
            )
        }

        composable(
            route = Screen.ProteinView.route,
            arguments = listOf(navArgument("ligandId") { type = NavType.StringType })
        ) { backStackEntry ->
            val ligandId = backStackEntry.arguments?.getString("ligandId") ?: return@composable
            ProteinViewScreen(
                ligandId = ligandId,
                onBack = { navController.popBackStack() },
                currentUsername = username,
                onLogout = onLogoutRequest
            )
        }
    }
}
