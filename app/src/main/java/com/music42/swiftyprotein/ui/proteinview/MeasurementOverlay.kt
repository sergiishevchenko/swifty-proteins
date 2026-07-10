package com.music42.swiftyprotein.ui.proteinview

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.music42.swiftyprotein.R
import com.music42.swiftyprotein.data.model.Bond
import com.music42.swiftyprotein.data.model.Ligand

@Composable
internal fun MeasurementOverlay(
    ligand: Ligand,
    selectedAtomIds: List<String>,
    selectedBonds: List<Bond>,
    onClear: () -> Unit,
    onExitMeasurementMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val maxCardDp = kotlin.math.min(kotlin.math.max(screenWidthDp - 24, 120), 304)
    val atoms = selectedAtomIds.mapNotNull { id -> ligand.atoms.firstOrNull { it.id == id } }
    val details = when {
        selectedBonds.size >= 2 -> formatBondAngle(ligand, selectedBonds)
        atoms.size >= 2 -> formatAtomDistance(atoms)
        selectedBonds.size == 1 -> {
            val b = selectedBonds.last()
            "Bond 1/2: ${b.atomId1}–${b.atomId2}"
        }
        atoms.size == 1 -> "Atom 1/2: ${atoms[0].id}"
        else -> "2 atoms → distance, 2 bonds (shared atom) → angle"
    }
    val headerText = "Measure mode:"
    val detailText = details

    Card(
        modifier = modifier.widthIn(max = maxCardDp.dp),
        shape = RoundedCornerShape(999.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.88f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = "Close measurement mode",
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onExitMeasurementMode)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = headerText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFFF9800)
                )
                Text(
                    text = detailText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "Reset",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.inverseOnSurface,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable(onClick = onClear)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }
    }
}
