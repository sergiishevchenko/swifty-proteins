package com.music42.swiftyprotein.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.music42.swiftyprotein.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onShowOnboarding: () -> Unit,
    currentUsername: String?,
    onLogout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val cifCacheSizeBytes by viewModel.cifCacheSizeBytes.collectAsState()
    val isClearingCifCache by viewModel.isClearingCifCache.collectAsState()
    val accentColor = MaterialTheme.colorScheme.primary

    LaunchedEffect(Unit) {
        viewModel.refreshCifCacheSize()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    if (!currentUsername.isNullOrBlank()) {
                        Text(
                            text = currentUsername,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                    }
                    IconButton(onClick = onLogout) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = stringResource(R.string.cd_logout)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    navigationIconContentColor = accentColor,
                    actionIconContentColor = accentColor,
                    titleContentColor = accentColor
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            SettingsThemeSection(
                selectedMode = settings.themeMode,
                onModeSelected = viewModel::setThemeMode
            )

            Spacer(modifier = Modifier.height(18.dp))

            SettingsVisualizationSection(
                selectedMode = settings.defaultVisualizationMode,
                onModeSelected = viewModel::setDefaultVisualizationMode
            )

            Spacer(modifier = Modifier.height(18.dp))

            SettingsHydrogensSection(
                showHydrogensByDefault = settings.showHydrogensByDefault,
                onShowHydrogensChange = viewModel::setShowHydrogensByDefault
            )

            Spacer(modifier = Modifier.height(18.dp))

            SettingsOnboardingSection(
                onReplayOnboarding = { viewModel.replayOnboarding(onShowOnboarding) }
            )

            Spacer(modifier = Modifier.height(18.dp))

            SettingsCacheSection(
                cacheSizeBytes = cifCacheSizeBytes,
                isClearing = isClearingCifCache,
                onClearCache = viewModel::clearCifCache
            )
        }
    }
}
