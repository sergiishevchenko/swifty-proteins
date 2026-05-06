package com.music42.swiftyprotein.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.music42.swiftyprotein.data.settings.ThemeMode
import com.music42.swiftyprotein.ui.proteinview.VisualizationMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    currentUsername: String?,
    onLogout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val accentGreen = Color(0xFF4CAF50)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!currentUsername.isNullOrBlank()) {
                        Text(
                            text = currentUsername,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = accentGreen,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    navigationIconContentColor = accentGreen,
                    actionIconContentColor = accentGreen,
                    titleContentColor = accentGreen
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Theme",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = settings.themeMode == mode,
                        onClick = { viewModel.setThemeMode(mode) },
                        label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Default visualization mode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    VisualizationMode.BALL_AND_STICK,
                    VisualizationMode.SPACE_FILL,
                    VisualizationMode.STICKS_ONLY,
                    VisualizationMode.WIREFRAME
                ).forEach { mode ->
                    FilterChip(
                        selected = settings.defaultVisualizationMode == mode,
                        onClick = { viewModel.setDefaultVisualizationMode(mode) },
                        label = {
                            Text(
                                when (mode) {
                                    VisualizationMode.BALL_AND_STICK -> "Balls"
                                    VisualizationMode.SPACE_FILL -> "Fill"
                                    VisualizationMode.STICKS_ONLY -> "Sticks"
                                    VisualizationMode.WIREFRAME -> "Wire"
                                }
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Show hydrogens by default",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(
                    text = "Display hydrogen atoms when opening a ligand",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = settings.showHydrogensByDefault,
                    onCheckedChange = { viewModel.setShowHydrogensByDefault(it) }
                )
            }
        }
    }
}
