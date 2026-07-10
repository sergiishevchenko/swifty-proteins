package com.music42.swiftyprotein.ui.proteinlist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.music42.swiftyprotein.R
import com.music42.swiftyprotein.data.repository.LigandRepository

@Composable
internal fun ProteinListContent(
    isLoading: Boolean,
    filteredLigands: List<String>,
    searchQuery: String,
    cachedInfo: Map<String, LigandRepository.LigandCacheInfo>,
    favoriteIds: Set<String>,
    onEnsureCachedInfo: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onLigandClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                items(
                    items = filteredLigands,
                    key = { it }
                ) { ligandId ->
                    LaunchedEffect(ligandId) {
                        onEnsureCachedInfo(ligandId)
                    }
                    LigandItem(
                        ligandId = ligandId,
                        subtitle = formatLigandSubtitle(cachedInfo[ligandId]),
                        isFavorite = favoriteIds.contains(ligandId),
                        onToggleFavorite = { onToggleFavorite(ligandId) },
                        onClick = { onLigandClick(ligandId) }
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                }
            }
        }

        if (filteredLigands.isEmpty() && searchQuery.isNotEmpty() && !isLoading) {
            Text(
                text = stringResource(R.string.search_no_results, searchQuery),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
