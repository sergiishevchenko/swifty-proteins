package com.music42.swiftyprotein.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.music42.swiftyprotein.data.repository.AuthRepository
import com.music42.swiftyprotein.data.repository.AuthResult
import com.music42.swiftyprotein.util.BiometricHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isRegistering: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isAuthenticated: Boolean = false,
    val biometricAvailable: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    application: Application,
    private val authRepository: AuthRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        checkBiometricAvailability()
    }

    private fun checkBiometricAvailability() {
        val deviceSupports = BiometricHelper.canAuthenticate(getApplication())
        if (!deviceSupports) {
            _uiState.update { it.copy(biometricAvailable = false) }
            return
        }
        viewModelScope.launch {
            val hasUsers = authRepository.hasUsers()
            _uiState.update { it.copy(biometricAvailable = hasUsers) }
        }
    }

    fun onUsernameChange(value: String) {
        _uiState.update { it.copy(username = value, errorMessage = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun toggleRegisterMode() {
        _uiState.update {
            it.copy(
                isRegistering = !it.isRegistering,
                errorMessage = null
            )
        }
    }

    fun onSubmit() {
        val state = _uiState.value
        if (state.isLoading) return

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = if (state.isRegistering) {
                authRepository.register(state.username, state.password)
            } else {
                authRepository.login(state.username, state.password)
            }

            when (result) {
                is AuthResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, isAuthenticated = true) }
                    if (state.isRegistering) {
                        checkBiometricAvailability()
                    }
                }
                is AuthResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
            }
        }
    }

    fun onBiometricSuccess() {
        _uiState.update { it.copy(isAuthenticated = true) }
    }

    fun onBiometricFailure(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun resetAuthState() {
        _uiState.update {
            it.copy(
                isAuthenticated = false,
                password = "",
                errorMessage = null
            )
        }
    }
}
