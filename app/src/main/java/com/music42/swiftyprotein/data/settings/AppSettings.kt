package com.music42.swiftyprotein.data.settings

import com.music42.swiftyprotein.ui.proteinview.VisualizationMode

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val defaultVisualizationMode: VisualizationMode = VisualizationMode.BALL_AND_STICK,
    val onboardingCompleted: Boolean = false,
    val showHydrogensByDefault: Boolean = false
)
