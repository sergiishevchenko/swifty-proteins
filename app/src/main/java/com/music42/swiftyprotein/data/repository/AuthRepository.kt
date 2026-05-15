package com.music42.swiftyprotein.data.repository

import android.database.sqlite.SQLiteConstraintException
import at.favre.lib.crypto.bcrypt.BCrypt
import com.music42.swiftyprotein.data.local.UserDao
import com.music42.swiftyprotein.data.local.entity.User
import com.music42.swiftyprotein.data.security.SecureStorage
import com.music42.swiftyprotein.data.settings.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

sealed class AuthResult {
    data object Success : AuthResult()
    data class Error(val message: String) : AuthResult()
}

@Singleton
class AuthRepository @Inject constructor(
    private val userDao: UserDao,
    private val secureStorage: SecureStorage,
    private val settingsRepository: SettingsRepository
) {

    suspend fun register(username: String, password: String): AuthResult {
        if (username.isBlank() || password.isBlank()) {
            return AuthResult.Error("Username and password cannot be empty.")
        }
        if (password.length < 8) {
            return AuthResult.Error("Password must be at least 8 characters.")
        }
        val existing = userDao.findByUsername(username)
        if (existing != null) {
            return AuthResult.Error("Username already exists.")
        }
        return try {
            userDao.insert(
                User(
                    username = username,
                    passwordHash = hashPassword(password)
                )
            )
            secureStorage.setLastUsername(username)
            settingsRepository.setOnboardingCompleted(false)
            AuthResult.Success
        } catch (e: SQLiteConstraintException) {
            AuthResult.Error("Username already exists.")
        }
    }

    suspend fun login(username: String, password: String): AuthResult {
        if (username.isBlank() || password.isBlank()) {
            return AuthResult.Error("Username and password cannot be empty.")
        }
        val user = userDao.findByUsername(username)
            ?: return AuthResult.Error("Invalid credentials.")

        val stored = user.passwordHash
        val ok = if (isBcryptHash(stored)) {
            BCrypt.verifyer().verify(password.toCharArray(), stored).verified
        } else if (isLegacySha256Hex(stored)) {
            legacySha256Hex(password) == stored
        } else {
            false
        }

        if (!ok) return AuthResult.Error("Invalid credentials.")

        if (!isBcryptHash(stored)) {
            val upgraded = hashPassword(password)
            userDao.updatePasswordHash(user.id, upgraded)
        }

        secureStorage.setLastUsername(username)
        return AuthResult.Success
    }

    fun getLastUsername(): String? {
        return secureStorage.getLastUsername()
    }

    suspend fun userExists(username: String): Boolean {
        return userDao.findByUsername(username) != null
    }

    private fun hashPassword(password: String): String {
        return BCrypt.withDefaults().hashToString(12, password.toCharArray())
    }

    private fun isBcryptHash(value: String): Boolean {
        return value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$")
    }

    private fun isLegacySha256Hex(value: String): Boolean {
        return value.length == 64 && value.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
    }

    private fun legacySha256Hex(password: String): String {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(password.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
