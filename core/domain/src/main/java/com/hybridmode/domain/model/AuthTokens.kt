package com.hybridmode.domain.model

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val issuedAtEpochMs: Long,
    val expiresAtEpochMs: Long,
) {
    companion object {
        const val MAX_ACCESS_TOKEN_TTL_MS: Long = 5 * 60 * 1000L
    }

    fun isExpired(nowEpochMs: Long): Boolean = nowEpochMs >= expiresAtEpochMs

    fun remainingTtlMs(nowEpochMs: Long): Long =
        (expiresAtEpochMs - nowEpochMs).coerceAtLeast(0L)
}
