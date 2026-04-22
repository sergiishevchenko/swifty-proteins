package com.music42.swiftyprotein.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.music42.swiftyprotein.data.settings.ThemeMode
import com.music42.swiftyprotein.ui.navigation.SwiftyProteinNavHost
import com.music42.swiftyprotein.ui.settings.SettingsViewModel
import com.music42.swiftyprotein.ui.theme.SwiftyProteinTheme

@Composable
fun AppRoot(
    shouldShowLogin: Boolean,
    onLoginShown: () -> Unit,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by settingsViewModel.settings.collectAsState()
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (settings.themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    SwiftyProteinTheme(darkTheme = darkTheme) {
        SwiftyProteinNavHost(
            shouldShowLogin = shouldShowLogin,
            onLoginShown = onLoginShown
        )
    }
}
