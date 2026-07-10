package com.music42.swiftyprotein.ui.favorites

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.music42.swiftyprotein.R

@Composable
internal fun FavoriteLigandItem(
    ligandId: String,
    isSelectedForCompare: Boolean,
    accentColor: Color,
    onToggleCompareSelection: () -> Unit,
    onRemoveFavorite: () -> Unit,
    onOpenLigand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (isSelectedForCompare) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(durationMillis = 200),
        label = "fav_compare_color"
    )
    val starScale by animateFloatAsState(
        targetValue = 1.12f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "favorite_scale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onToggleCompareSelection),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ligandId,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor
                )
                if (isSelectedForCompare) {
                    Text(
                        text = stringResource(R.string.favorites_selected_for_compare),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
            IconButton(onClick = onRemoveFavorite) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = stringResource(R.string.cd_remove_from_favorites),
                    tint = accentColor,
                    modifier = Modifier.graphicsLayer {
                        scaleX = starScale
                        scaleY = starScale
                    }
                )
            }
            IconButton(onClick = onOpenLigand) {
                Icon(
                    Icons.Default.OpenInNew,
                    contentDescription = stringResource(R.string.cd_open_ligand),
                    tint = accentColor
                )
            }
        }
    }
}
