package com.music42.swiftyprotein.ui.login

import android.app.Application
import com.music42.swiftyprotein.data.repository.AuthRepository
import com.music42.swiftyprotein.util.BiometricHelper

internal data class BiometricAvailability(
    val available: Boolean,
    val lastUsername: String?
)

internal suspend fun resolveBiometricAvailability(
    application: Application,
    authRepository: AuthRepository,
    typedUsername: String
): BiometricAvailability {
    if (!BiometricHelper.canAuthenticate(application)) {
        return BiometricAvailability(available = false, lastUsername = null)
    }
    val last = authRepository.getLastUsername()?.trim().orEmpty()
    if (last.isBlank()) {
        return BiometricAvailability(available = false, lastUsername = null)
    }
    val exists = authRepository.userExists(last)
    val enabledForTyped = exists && typedUsername.isNotBlank() && typedUsername == last
    return BiometricAvailability(
        available = enabledForTyped,
        lastUsername = last.takeIf { exists }
    )
}
