package com.hybridmode.domain.auth

import com.hybridmode.domain.shared.ValueObject

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val issuedAtEpochMs: Long,
    val expiresAtEpochMs: Long,
) : ValueObject {
    init {
        require(accessToken.isNotBlank()) { "Access token required" }
        require(expiresAtEpochMs >= issuedAtEpochMs) { "Invalid token window" }
    }

    companion object {
        const val MAX_ACCESS_TOKEN_TTL_MS: Long = 5 * 60 * 1000L
    }

    fun isExpired(nowEpochMs: Long): Boolean = nowEpochMs >= expiresAtEpochMs

    fun remainingTtlMs(nowEpochMs: Long): Long =
        (expiresAtEpochMs - nowEpochMs).coerceAtLeast(0L)
}
