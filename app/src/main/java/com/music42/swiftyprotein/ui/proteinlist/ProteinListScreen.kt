package com.music42.swiftyprotein.ui.proteinlist

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProteinListScreen(
    onLigandSelected: (String) -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenSettings: () -> Unit,
    currentUsername: String?,
    onLogout: () -> Unit,
    viewModel: ProteinListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val accentGreen = Color(0xFF4CAF50)

    LaunchedEffect(uiState.navigateToLigand) {
        uiState.navigateToLigand?.let { ligandId ->
            onLigandSelected(ligandId)
            viewModel.onNavigated()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ligands") },
                actions = {
                    IconButton(onClick = onOpenFavorites) {
                        Icon(Icons.Default.Star, contentDescription = "Favorites")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                    if (!currentUsername.isNullOrBlank()) {
                        Text(
                            text = currentUsername,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = accentGreen,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    navigationIconContentColor = accentGreen,
                    actionIconContentColor = accentGreen,
                    titleContentColor = accentGreen
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search ligand...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 12.dp,
                            vertical = 8.dp
                        )
                    ) {
                        items(
                            items = uiState.filteredLigands,
                            key = { it }
                        ) { ligandId ->
                            val info = uiState.cachedInfo[ligandId]
                            LigandItem(
                                ligandId = ligandId,
                                subtitle = info?.let {
                                    buildString {
                                        if (it.formula.isNotBlank()) append(it.formula)
                                        if (it.atomCount > 0) {
                                            if (isNotEmpty()) append(" · ")
                                            append("${it.atomCount} atoms")
                                        }
                                    }.ifEmpty { null }
                                },
                                isLoading = uiState.loadingLigandId == ligandId,
                                isFavorite = uiState.favoriteIds.contains(ligandId),
                                onToggleFavorite = { viewModel.onToggleFavorite(ligandId) },
                                onClick = { viewModel.onLigandClick(ligandId) }
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                        }
                    }
                }
            }

            if (uiState.filteredLigands.isEmpty() && uiState.searchQuery.isNotEmpty() && !uiState.isLoading) {
                Text(
                    text = "No ligands found for \"${uiState.searchQuery}\"",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (uiState.loadingLigandId != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(modifier = Modifier.size(48.dp))
                        Text(
                            text = "Loading ${uiState.loadingLigandId ?: ""}...",
                            modifier = Modifier.padding(top = 12.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }

    if (uiState.errorMessage != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissError,
            title = { Text("Error") },
            text = { Text(uiState.errorMessage!!) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissError) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun LigandItem(
    ligandId: String,
    subtitle: String?,
    isLoading: Boolean,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit
) {
    val containerColor by animateColorAsState(
        targetValue = if (isLoading) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        animationSpec = tween(durationMillis = 220),
        label = "ligand_container"
    )
    val starScale by animateFloatAsState(
        targetValue = if (isFavorite) 1.12f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "favorite_scale"
    )
    val accentGreen = Color(0xFF4CAF50)
    val starTint by animateColorAsState(
        targetValue = if (isFavorite) accentGreen else accentGreen.copy(alpha = 0.65f),
        animationSpec = tween(durationMillis = 220),
        label = "favorite_tint"
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            .clickable(enabled = !isLoading, onClick = onClick)
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ligandId,
                    fontWeight = FontWeight.SemiBold,
                    color = accentGreen
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp
                )
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = if (isFavorite) "Unfavorite" else "Favorite",
                    tint = starTint,
                    modifier = Modifier.graphicsLayer {
                        scaleX = starScale
                        scaleY = starScale
                    }
                )
            }
        }
    }
}
