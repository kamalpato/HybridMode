package com.hybridmode.data.local.secure

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.hybridmode.domain.auth.AuthTokens
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureTokenStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        FILE_NAME,
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun save(tokens: AuthTokens) {
        prefs.edit()
            .putString(KEY_ACCESS, tokens.accessToken)
            .putString(KEY_REFRESH, tokens.refreshToken)
            .putLong(KEY_ISSUED_AT, tokens.issuedAtEpochMs)
            .putLong(KEY_EXPIRES_AT, tokens.expiresAtEpochMs)
            .apply()
    }

    fun read(): AuthTokens? {
        val access = prefs.getString(KEY_ACCESS, null) ?: return null
        val refresh = prefs.getString(KEY_REFRESH, null) ?: return null
        val issuedAt = prefs.getLong(KEY_ISSUED_AT, -1L)
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, -1L)
        if (issuedAt < 0 || expiresAt < 0) return null
        return AuthTokens(
            accessToken = access,
            refreshToken = refresh,
            issuedAtEpochMs = issuedAt,
            expiresAtEpochMs = expiresAt,
        )
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val FILE_NAME = "hybrid_secure_tokens"
        const val KEY_ACCESS = "access_token"
        const val KEY_REFRESH = "refresh_token"
        const val KEY_ISSUED_AT = "issued_at"
        const val KEY_EXPIRES_AT = "expires_at"
    }
}
