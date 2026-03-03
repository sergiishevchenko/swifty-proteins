package com.music42.swiftyprotein.data.repository

import android.database.sqlite.SQLiteConstraintException
import com.music42.swiftyprotein.data.local.UserDao
import com.music42.swiftyprotein.data.local.entity.User
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

sealed class AuthResult {
    data object Success : AuthResult()
    data class Error(val message: String) : AuthResult()
}

@Singleton
class AuthRepository @Inject constructor(
    private val userDao: UserDao
) {

    suspend fun register(username: String, password: String): AuthResult {
        if (username.isBlank() || password.isBlank()) {
            return AuthResult.Error("Username and password cannot be empty.")
        }
        if (password.length < 4) {
            return AuthResult.Error("Password must be at least 4 characters.")
        }
        val existing = userDao.findByUsername(username)
        if (existing != null) {
            return AuthResult.Error("Username already exists.")
        }
        return try {
            userDao.insert(User(username = username, passwordHash = hashPassword(password)))
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
        return if (user.passwordHash == hashPassword(password)) {
            AuthResult.Success
        } else {
            AuthResult.Error("Invalid credentials.")
        }
    }

    suspend fun hasUsers(): Boolean {
        return userDao.getUserCount() > 0
    }

    suspend fun userExists(username: String): Boolean {
        return userDao.findByUsername(username) != null
    }

    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(password.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
