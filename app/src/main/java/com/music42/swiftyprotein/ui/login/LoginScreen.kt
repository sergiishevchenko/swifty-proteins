package com.music42.swiftyprotein.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.music42.swiftyprotein.ui.scaffoldSymmetricContentPadding

@Composable
fun LoginScreen(
    onLoginSuccess: (showOnboarding: Boolean) -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val layoutDirection = LocalLayoutDirection.current

    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) {
            val showOnboarding = viewModel.consumeShowOnboardingAfterRegister()
            onLoginSuccess(showOnboarding)
            viewModel.resetAuthState()
        }
    }

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scaffoldSymmetricContentPadding(innerPadding, layoutDirection)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
                    .widthIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LoginFormCard(
                    username = uiState.username,
                    password = uiState.password,
                    isRegistering = uiState.isRegistering,
                    isLoading = uiState.isLoading,
                    biometricAvailable = uiState.biometricAvailable,
                    onUsernameChange = viewModel::onUsernameChange,
                    onPasswordChange = viewModel::onPasswordChange,
                    onSubmit = viewModel::onSubmit,
                    onToggleRegisterMode = viewModel::toggleRegisterMode,
                    onBiometricSuccess = viewModel::onBiometricSuccess,
                    onBiometricFailure = viewModel::onBiometricFailure
                )
            }
        }
    }

    uiState.errorMessage?.let { message ->
        LoginErrorDialog(
            message = message,
            onDismiss = viewModel::dismissError
        )
    }
}
