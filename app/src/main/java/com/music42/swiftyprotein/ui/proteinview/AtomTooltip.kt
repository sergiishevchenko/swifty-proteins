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
import com.music42.swiftyprotein.data.model.Atom

@Composable
internal fun AtomTooltip(
    atom: Atom,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                contentDescription = "Atom info",
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${atom.element}  ·  ${atom.elementName}  ·  ${atom.id}",
                style = MaterialTheme.typography.labelLarge,
                color = fg,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
