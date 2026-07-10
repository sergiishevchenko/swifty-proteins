package com.music42.swiftyprotein.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.music42.swiftyprotein.R
import com.music42.swiftyprotein.ui.proteinview.VisualizationMode

@Composable
internal fun SettingsVisualizationSection(
    selectedMode: VisualizationMode,
    onModeSelected: (VisualizationMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = stringResource(R.string.settings_default_vis_mode),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
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
                selected = selectedMode == mode,
                onClick = { onModeSelected(mode) },
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
}
