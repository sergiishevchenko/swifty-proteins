package com.music42.swiftyprotein.ui.common

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import com.music42.swiftyprotein.R
import com.music42.swiftyprotein.data.repository.FavoriteToggleAction
import kotlinx.coroutines.flow.Flow

@Composable
fun FavoriteSnackbarEffect(
    snackbarHostState: SnackbarHostState,
    events: Flow<FavoriteToggleAction>
) {
    val addedMessage = stringResource(R.string.favorite_added)
    val removedMessage = stringResource(R.string.favorite_removed)
    LaunchedEffect(events, addedMessage, removedMessage) {
        events.collect { action ->
            val message = when (action) {
                FavoriteToggleAction.Added -> addedMessage
                FavoriteToggleAction.Removed -> removedMessage
            }
            snackbarHostState.showSnackbar(message)
        }
    }
}
