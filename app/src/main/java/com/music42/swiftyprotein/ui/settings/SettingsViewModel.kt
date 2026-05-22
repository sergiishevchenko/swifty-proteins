package com.music42.swiftyprotein.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music42.swiftyprotein.data.repository.LigandRepository
import com.music42.swiftyprotein.data.settings.AppSettings
import com.music42.swiftyprotein.data.settings.SettingsRepository
import com.music42.swiftyprotein.data.settings.ThemeMode
import com.music42.swiftyprotein.ui.proteinview.VisualizationMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val ligandRepository: LigandRepository
) : ViewModel() {
    val settings: StateFlow<AppSettings> =
        settingsRepository.settings.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            AppSettings()
        )

    private val _cifCacheSizeBytes = MutableStateFlow(0L)
    val cifCacheSizeBytes: StateFlow<Long> = _cifCacheSizeBytes.asStateFlow()

    private val _isClearingCifCache = MutableStateFlow(false)
    val isClearingCifCache: StateFlow<Boolean> = _isClearingCifCache.asStateFlow()

    init {
        refreshCifCacheSize()
    }

    fun refreshCifCacheSize() {
        viewModelScope.launch {
            _cifCacheSizeBytes.value = ligandRepository.getCifCacheSizeBytes()
        }
    }

    fun clearCifCache() {
        if (_isClearingCifCache.value) return
        viewModelScope.launch {
            _isClearingCifCache.update { true }
            try {
                ligandRepository.clearCifCache()
                _cifCacheSizeBytes.value = ligandRepository.getCifCacheSizeBytes()
            } finally {
                _isClearingCifCache.update { false }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setDefaultVisualizationMode(mode: VisualizationMode) {
        viewModelScope.launch { settingsRepository.setDefaultVisualizationMode(mode) }
    }

    fun setOnboardingCompleted(completed: Boolean) {
        viewModelScope.launch { settingsRepository.setOnboardingCompleted(completed) }
    }

    fun replayOnboarding(onReady: () -> Unit) {
        viewModelScope.launch {
            settingsRepository.setOnboardingCompleted(false)
            onReady()
        }
    }

    fun setShowHydrogensByDefault(show: Boolean) {
        viewModelScope.launch { settingsRepository.setShowHydrogensByDefault(show) }
    }
}
