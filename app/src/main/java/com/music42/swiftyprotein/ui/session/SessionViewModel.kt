package com.music42.swiftyprotein.ui.session

import androidx.lifecycle.ViewModel
import com.music42.swiftyprotein.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _username = MutableStateFlow<String?>(authRepository.getLastUsername())
    val username: StateFlow<String?> = _username.asStateFlow()

    fun refresh() {
        _username.update { authRepository.getLastUsername() }
    }

    fun logout() {
        authRepository.logout()
        _username.update { null }
    }
}
