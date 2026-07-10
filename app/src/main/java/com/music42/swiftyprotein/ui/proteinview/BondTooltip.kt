package com.music42.swiftyprotein.ui.proteinview

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.music42.swiftyprotein.R
import com.music42.swiftyprotein.data.model.Bond
import com.music42.swiftyprotein.data.model.BondOrder
import com.music42.swiftyprotein.data.model.Ligand

@Composable
internal fun BondTooltip(
    bond: Bond,
    ligand: Ligand,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val a1 = ligand.atoms.firstOrNull { it.id == bond.atomId1 }
    val a2 = ligand.atoms.firstOrNull { it.id == bond.atomId2 }
    val length = if (a1 != null && a2 != null) {
        val dx = a1.x - a2.x
        val dy = a1.y - a2.y
        val dz = a1.z - a2.z
        kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
    } else null

    val order = when (bond.order) {
        BondOrder.SINGLE -> "Single"
        BondOrder.DOUBLE -> "Double"
        BondOrder.TRIPLE -> "Triple"
        BondOrder.AROMATIC -> "Aromatic"
    }
    val lengthText = length?.let { String.format("%.2f Å", it) } ?: "?"

    val bg = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.88f)
    val fg = MaterialTheme.colorScheme.inverseOnSurface
    Card(
        shape = RoundedCornerShape(999.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = modifier.clickable(onClick = onDismiss)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = "Bond info",
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$order  ·  ${bond.atomId1}–${bond.atomId2}  ·  $lengthText",
                style = MaterialTheme.typography.labelLarge,
                color = fg,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
