package com.music42.swiftyprotein.data.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureStorage(
    private val context: Context
) {
    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun setLastUsername(username: String) {
        prefs.edit().putString(KEY_LAST_USERNAME, username).apply()
    }

    fun getLastUsername(): String? {
        return prefs.getString(KEY_LAST_USERNAME, null)
    }

    companion object {
        private const val KEY_LAST_USERNAME = "last_username"
    }
}
