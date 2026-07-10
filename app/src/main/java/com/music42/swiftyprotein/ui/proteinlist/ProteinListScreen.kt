package com.music42.swiftyprotein.ui.proteinlist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.music42.swiftyprotein.R
import com.music42.swiftyprotein.ui.common.FavoriteSnackbarEffect

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
    val accentColor = MaterialTheme.colorScheme.primary
    val snackbarHostState = remember { SnackbarHostState() }

    FavoriteSnackbarEffect(snackbarHostState, viewModel.favoriteSnackbar)

    LaunchedEffect(uiState.navigateToLigand) {
        uiState.navigateToLigand?.let { ligandId ->
            onLigandSelected(ligandId)
            viewModel.onNavigated()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ligands_title)) },
                actions = {
                    IconButton(onClick = onOpenFavorites) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = stringResource(R.string.cd_favorites)
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.cd_settings)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ProteinListSearchField(
                query = uiState.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange
            )
            ProteinListContent(
                isLoading = uiState.isLoading,
                filteredLigands = uiState.filteredLigands,
                searchQuery = uiState.searchQuery,
                cachedInfo = uiState.cachedInfo,
                favoriteIds = uiState.favoriteIds,
                onEnsureCachedInfo = viewModel::ensureCachedInfo,
                onToggleFavorite = viewModel::onToggleFavorite,
                onLigandClick = viewModel::onLigandClick,
                modifier = Modifier.weight(1f)
            )
        }
    }

    uiState.loadErrorMessage?.let { message ->
        ProteinListLoadErrorDialog(
            message = message,
            onRetry = viewModel::retryLoadLigands,
            onDismiss = viewModel::dismissLoadError
        )
    }
}
