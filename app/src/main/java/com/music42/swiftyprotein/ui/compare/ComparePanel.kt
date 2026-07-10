package com.music42.swiftyprotein.ui.compare

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.music42.swiftyprotein.R
import com.music42.swiftyprotein.data.model.Ligand

@Composable
internal fun ComparePanel(
    title: String,
    ligand: Ligand,
    modifier: Modifier = Modifier
) {
    var zoomFactor by remember(ligand.id) { mutableFloatStateOf(1f) }
    var resetTick by remember(ligand.id) { mutableIntStateOf(0) }
    val background: Color = MaterialTheme.colorScheme.background

    Card(
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = title,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                style = MaterialTheme.typography.titleMedium
            )
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(background)
            ) {
                val density = LocalDensity.current
                val viewportWidthPx = with(density) { maxWidth.roundToPx() }
                val viewportHeightPx = with(density) { maxHeight.roundToPx() }
                CompareMoleculeViewer(
                    ligand = ligand,
                    zoomFactor = zoomFactor,
                    onZoomFactorChange = { zoomFactor = it },
                    resetTick = resetTick,
                    viewportWidthPx = viewportWidthPx,
                    viewportHeightPx = viewportHeightPx,
                    modifier = Modifier.fillMaxSize()
                )

                Popup(alignment = Alignment.BottomEnd) {
                    CompareZoomControls(
                        onZoomIn = { zoomFactor = (zoomFactor * 1.2f).coerceIn(0.3f, 5.0f) },
                        onZoomOut = { zoomFactor = (zoomFactor / 1.2f).coerceIn(0.3f, 5.0f) },
                        onReset = {
                            zoomFactor = 1f
                            resetTick++
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CompareZoomControls(
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier.padding(end = 8.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CompareZoomButton(onClick = onZoomIn) {
            Icon(
                Icons.Filled.Add,
                contentDescription = stringResource(R.string.cd_zoom_in),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        CompareZoomButton(onClick = onZoomOut) {
            Icon(
                Icons.Filled.Remove,
                contentDescription = stringResource(R.string.cd_zoom_out),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        CompareZoomButton(onClick = onReset) {
            Icon(
                Icons.Filled.Refresh,
                contentDescription = stringResource(R.string.cd_reset_view),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun CompareZoomButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.size(40.dp),
        shape = RoundedCornerShape(999.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        IconButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}
