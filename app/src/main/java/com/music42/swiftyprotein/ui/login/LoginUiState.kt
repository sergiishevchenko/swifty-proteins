package com.music42.swiftyprotein.ui.login

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isRegistering: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isAuthenticated: Boolean = false,
    val biometricAvailable: Boolean = false,
    val lastUsername: String? = null
)
