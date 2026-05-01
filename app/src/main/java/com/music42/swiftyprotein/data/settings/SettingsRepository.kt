package com.music42.swiftyprotein.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.music42.swiftyprotein.ui.proteinview.VisualizationMode
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "swifty_protein_settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DEFAULT_VIS_MODE = stringPreferencesKey("default_visualization_mode")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val SHOW_HYDROGENS = booleanPreferencesKey("show_hydrogens_by_default")
    }

    val settings: Flow<AppSettings> =
        context.dataStore.data.map { prefs ->
            AppSettings(
                themeMode = prefs.readEnum(Keys.THEME_MODE, ThemeMode.SYSTEM),
                defaultVisualizationMode = prefs.readEnum(Keys.DEFAULT_VIS_MODE, VisualizationMode.BALL_AND_STICK),
                onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
                showHydrogensByDefault = prefs[Keys.SHOW_HYDROGENS] ?: false
            )
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setDefaultVisualizationMode(mode: VisualizationMode) {
        context.dataStore.edit { it[Keys.DEFAULT_VIS_MODE] = mode.name }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setShowHydrogensByDefault(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_HYDROGENS] = show }
    }

    private inline fun <reified T : Enum<T>> Preferences.readEnum(
        key: Preferences.Key<String>,
        default: T
    ): T {
        val raw = this[key] ?: return default
        return runCatching { enumValueOf<T>(raw) }.getOrDefault(default)
    }
}

