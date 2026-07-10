package com.music42.swiftyprotein.ui.favorites

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.music42.swiftyprotein.R

@Composable
internal fun FavoritesListContent(
    favorites: List<String>,
    selectedForCompare: Set<String>,
    accentColor: Color,
    onToggleCompareSelection: (String) -> Unit,
    onRemoveFavorite: (String) -> Unit,
    onOpenLigand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (favorites.isEmpty()) {
            Text(
                text = stringResource(R.string.favorites_empty),
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                items(items = favorites, key = { it }) { ligandId ->
                    FavoriteLigandItem(
                        ligandId = ligandId,
                        isSelectedForCompare = selectedForCompare.contains(ligandId),
                        accentColor = accentColor,
                        onToggleCompareSelection = { onToggleCompareSelection(ligandId) },
                        onRemoveFavorite = { onRemoveFavorite(ligandId) },
                        onOpenLigand = { onOpenLigand(ligandId) }
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                }
            }
        }
    }
}
