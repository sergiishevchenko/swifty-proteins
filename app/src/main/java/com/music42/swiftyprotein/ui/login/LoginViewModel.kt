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
    val biometricAvailable: Boolean = false,
    val lastUsername: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    application: Application,
    private val authRepository: AuthRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        val last = authRepository.getLastUsername()
        if (!last.isNullOrBlank()) {
            _uiState.update { it.copy(username = last, lastUsername = last) }
        }
        checkBiometricAvailability()
    }

    private fun checkBiometricAvailability() {
        val deviceSupports = BiometricHelper.canAuthenticate(getApplication())
        if (!deviceSupports) {
            _uiState.update { it.copy(biometricAvailable = false) }
            return
        }
        viewModelScope.launch {
            val last = authRepository.getLastUsername()?.trim().orEmpty()
            if (last.isBlank()) {
                _uiState.update { it.copy(biometricAvailable = false, lastUsername = null) }
                return@launch
            }
            val exists = authRepository.userExists(last)
            val typed = _uiState.value.username.trim()
            val enabledForTyped = exists && typed.isNotBlank() && typed == last
            _uiState.update { it.copy(biometricAvailable = enabledForTyped, lastUsername = last.takeIf { exists }) }
        }
    }

    fun onUsernameChange(value: String) {
        val next = value
        _uiState.update { it.copy(username = next, errorMessage = null) }
        checkBiometricAvailability()
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
        val last = authRepository.getLastUsername()?.trim().orEmpty()
        if (last.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Log in once with password first.") }
            checkBiometricAvailability()
            return
        }
        val typed = _uiState.value.username.trim()
        if (typed.isNotBlank() && typed != last) {
            _uiState.update {
                it.copy(errorMessage = "Biometric login is available only for the last signed-in user. Use password to switch user.")
            }
            checkBiometricAvailability()
            return
        }
        viewModelScope.launch {
            val exists = authRepository.userExists(last)
            if (exists) {
                _uiState.update {
                    it.copy(
                        username = last,
                        isAuthenticated = true,
                        errorMessage = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(errorMessage = "Biometric login is unavailable. Log in with password.")
                }
                checkBiometricAvailability()
            }
        }
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
