package com.music42.swiftyprotein.ui.favorites

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.music42.swiftyprotein.R
import com.music42.swiftyprotein.ui.common.FavoriteSnackbarEffect

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
    val accentColor = MaterialTheme.colorScheme.primary
    val snackbarHostState = remember { SnackbarHostState() }

    FavoriteSnackbarEffect(snackbarHostState, viewModel.favoriteSnackbar)

    fun toggleCompareSelection(ligandId: String) {
        selectedForCompare = if (selectedForCompare.contains(ligandId)) {
            selectedForCompare - ligandId
        } else if (selectedForCompare.size >= 2) {
            selectedForCompare
        } else {
            selectedForCompare + ligandId
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.favorites_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
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
                            contentDescription = stringResource(R.string.cd_compare),
                            tint = if (canCompare) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    if (!currentUsername.isNullOrBlank()) {
                        Text(
                            text = currentUsername,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                    }
                    IconButton(onClick = onLogout) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = stringResource(R.string.cd_logout)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    navigationIconContentColor = accentColor,
                    actionIconContentColor = accentColor,
                    titleContentColor = accentColor
                )
            )
        }
    ) { innerPadding ->
        FavoritesListContent(
            favorites = favorites,
            selectedForCompare = selectedForCompare,
            accentColor = accentColor,
            onToggleCompareSelection = ::toggleCompareSelection,
            onRemoveFavorite = viewModel::onToggleFavorite,
            onOpenLigand = onLigandSelected,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }
}
