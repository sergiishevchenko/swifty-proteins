package com.music42.swiftyprotein.ui.proteinview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.music42.swiftyprotein.R

@Composable
internal fun ProteinViewActionButtons(
    isLandscape: Boolean,
    bottomBarHeight: Dp,
    isRecording: Boolean,
    measurementMode: Boolean,
    showAtomLabels: Boolean,
    showHydrogens: Boolean,
    visualizationMode: VisualizationMode,
    onStartRecording: () -> Unit,
    onToggleMeasurement: () -> Unit,
    onToggleLabels: () -> Unit,
    onToggleHydrogens: () -> Unit,
    onShare: () -> Unit,
    onBallsModeRequired: () -> Unit
) {
    val recordButton = @Composable {
        CircleActionButton(
            containerColor = if (isRecording)
                MaterialTheme.colorScheme.errorContainer
            else
                MaterialTheme.colorScheme.surface,
            onClick = { if (!isRecording) onStartRecording() }
        ) {
            Icon(
                Icons.Default.Videocam,
                contentDescription = "Record video",
                tint = if (isRecording)
                    MaterialTheme.colorScheme.onErrorContainer
                else
                    MaterialTheme.colorScheme.primary
            )
        }
    }
    val measureButton = @Composable {
        CircleActionButton(
            containerColor = if (measurementMode)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface,
            onClick = {
                if (visualizationMode != VisualizationMode.BALL_AND_STICK) {
                    onBallsModeRequired()
                } else {
                    onToggleMeasurement()
                }
            }
        ) {
            Icon(
                Icons.Default.Straighten,
                contentDescription = "Toggle measurement mode",
                tint = if (measurementMode)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.primary
            )
        }
    }
    val labelsButton = @Composable {
        CircleActionButton(
            containerColor = if (showAtomLabels)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface,
            onClick = {
                if (visualizationMode != VisualizationMode.BALL_AND_STICK) {
                    onBallsModeRequired()
                } else {
                    onToggleLabels()
                }
            }
        ) {
            Icon(
                Icons.Default.Label,
                contentDescription = "Toggle atom labels",
                tint = if (showAtomLabels)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.primary
            )
        }
    }
    val hydrogensButton = @Composable {
        CircleActionButton(
            containerColor = if (showHydrogens)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface,
            onClick = onToggleHydrogens
        ) {
            Text(
                text = "H",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (showHydrogens)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.primary
            )
        }
    }
    val shareButton = @Composable {
        CircleActionButton(
            containerColor = MaterialTheme.colorScheme.surface,
            onClick = onShare
        ) {
            Icon(
                Icons.Default.Share,
                contentDescription = "Share",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }

    if (isLandscape) {
        Column(
            modifier = Modifier.padding(end = 8.dp, top = 8.dp),
            horizontalAlignment = Alignment.End
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                recordButton()
                measureButton()
                labelsButton()
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                hydrogensButton()
                shareButton()
            }
        }
    } else {
        Column(
            modifier = Modifier
                .padding(end = 8.dp, top = 8.dp, bottom = bottomBarHeight + 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            recordButton()
            Spacer(modifier = Modifier.height(8.dp))
            measureButton()
            Spacer(modifier = Modifier.height(8.dp))
            labelsButton()
            Spacer(modifier = Modifier.height(8.dp))
            hydrogensButton()
            Spacer(modifier = Modifier.height(8.dp))
            shareButton()
        }
    }
}

@Composable
internal fun ProteinViewZoomControls(
    isLandscape: Boolean,
    bottomBarHeight: Dp,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onReset: () -> Unit
) {
    val zoomInButton = @Composable {
        PrimaryCircleButton(onClick = onZoomIn) {
            Icon(
                Icons.Default.Add,
                contentDescription = stringResource(R.string.cd_zoom_in),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
    val zoomOutButton = @Composable {
        PrimaryCircleButton(onClick = onZoomOut) {
            Icon(
                Icons.Default.Remove,
                contentDescription = stringResource(R.string.cd_zoom_out),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
    val resetButton = @Composable {
        PrimaryCircleButton(onClick = onReset) {
            Icon(
                Icons.Filled.Refresh,
                contentDescription = stringResource(R.string.cd_reset_view),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }

    if (isLandscape) {
        Row(
            modifier = Modifier.padding(start = 8.dp, top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            zoomInButton()
            zoomOutButton()
            resetButton()
        }
    } else {
        Column(
            modifier = Modifier
                .padding(start = 8.dp, top = 8.dp, bottom = bottomBarHeight + 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            zoomInButton()
            Spacer(modifier = Modifier.height(8.dp))
            zoomOutButton()
            Spacer(modifier = Modifier.height(8.dp))
            resetButton()
        }
    }
}

@Composable
internal fun ProteinViewVisualizationBar(
    selectedMode: VisualizationMode,
    onModeSelected: (VisualizationMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VisualizationMode.entries.forEach { mode ->
                FilterChip(
                    modifier = Modifier.weight(1f),
                    selected = selectedMode == mode,
                    onClick = { onModeSelected(mode) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    label = {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = when (mode) {
                                VisualizationMode.BALL_AND_STICK -> "Balls"
                                VisualizationMode.SPACE_FILL -> "Fill"
                                VisualizationMode.STICKS_ONLY -> "Sticks"
                                VisualizationMode.WIREFRAME -> "Wire"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun CircleActionButton(
    containerColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.size(42.dp),
        shape = CircleShape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        IconButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}

@Composable
private fun PrimaryCircleButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.size(42.dp),
        shape = CircleShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        IconButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}
