package com.music42.swiftyprotein.ui.proteinview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun ProteinViewModeBanners(
    measurementMode: Boolean,
    showAtomLabels: Boolean,
    showHydrogens: Boolean,
    showBallsModeHint: Boolean,
    onExitMeasurement: () -> Unit,
    onExitLabels: () -> Unit,
    onExitHydrogens: () -> Unit,
    onDismissBallsHint: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 12.dp)
    ) {
        AnimatedVisibility(
            visible = measurementMode,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            ModeBanner(
                text = "MEASURE MODE",
                onClick = onExitMeasurement
            )
        }
        AnimatedVisibility(
            visible = showAtomLabels,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            ModeBanner(
                text = "LABELS MODE",
                onClick = onExitLabels,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        AnimatedVisibility(
            visible = showHydrogens,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            ModeBanner(
                text = "HYDROGENS VISIBLE",
                onClick = onExitHydrogens,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        AnimatedVisibility(
            visible = showBallsModeHint,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            ModeBanner(
                text = "Switch to Balls mode",
                onClick = onDismissBallsHint,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
