package com.music42.swiftyprotein.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music42.swiftyprotein.data.settings.AppSettings
import com.music42.swiftyprotein.data.settings.SettingsRepository
import com.music42.swiftyprotein.data.settings.ThemeMode
import com.music42.swiftyprotein.ui.proteinview.VisualizationMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    val settings: StateFlow<AppSettings> =
        settingsRepository.settings.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            AppSettings()
        )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setDefaultVisualizationMode(mode: VisualizationMode) {
        viewModelScope.launch { settingsRepository.setDefaultVisualizationMode(mode) }
    }

    fun setOnboardingCompleted(completed: Boolean) {
        viewModelScope.launch { settingsRepository.setOnboardingCompleted(completed) }
    }

    fun setShowHydrogensByDefault(show: Boolean) {
        viewModelScope.launch { settingsRepository.setShowHydrogensByDefault(show) }
    }
}
