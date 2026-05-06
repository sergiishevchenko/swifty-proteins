package com.music42.swiftyprotein.ui.favorites

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onBack: () -> Unit,
    onLigandSelected: (String) -> Unit,
    onCompareSelected: (String, String) -> Unit,
    currentUsername: String?,
    onLogout: () -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val favorites by viewModel.favoriteLigandIds.collectAsState()
    var selectedForCompare by remember { mutableStateOf(setOf<String>()) }
    val accentGreen = Color(0xFF4CAF50)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Favorites") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val canCompare = selectedForCompare.size == 2
                    IconButton(
                        onClick = {
                            val list = selectedForCompare.toList()
                            if (list.size == 2) onCompareSelected(list[0], list[1])
                        },
                        enabled = canCompare
                    ) {
                        Icon(
                            Icons.Default.CompareArrows,
                            contentDescription = "Compare",
                            tint = if (canCompare) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
            if (favorites.isEmpty()) {
                Text(
                    text = "No favorites yet.",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 12.dp,
                        vertical = 8.dp
                    )
                ) {
                    items(items = favorites, key = { it }) { ligandId ->
                        val selected = selectedForCompare.contains(ligandId)
                        val accentGreen = Color(0xFF4CAF50)
                        val containerColor by animateColorAsState(
                            targetValue = if (selected)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surface,
                            animationSpec = tween(durationMillis = 200),
                            label = "fav_compare_color"
                        )
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                                .clickable {
                                    selectedForCompare = if (selected) {
                                        selectedForCompare - ligandId
                                    } else {
                                        if (selectedForCompare.size >= 2) selectedForCompare else selectedForCompare + ligandId
                                    }
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = containerColor
                            ),
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
                                        color = accentGreen
                                    )
                                    if (selected) {
                                        Text(
                                            text = "Selected for compare",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                                IconButton(onClick = { onLigandSelected(ligandId) }) {
                                    Icon(
                                        Icons.Default.OpenInNew,
                                        contentDescription = "Open",
                                        tint = accentGreen
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.size(8.dp))
                    }
                }
            }
        }
    }
}
