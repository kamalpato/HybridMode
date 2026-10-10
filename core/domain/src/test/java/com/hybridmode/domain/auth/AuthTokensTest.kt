package com.hybridmode.domain.auth

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AuthTokensTest {

    @Test
    fun maxTtlIsFiveMinutes() {
        assertThat(AuthTokens.MAX_ACCESS_TOKEN_TTL_MS).isEqualTo(5 * 60 * 1000L)
    }

    @Test
    fun isExpiredRespectsWallClock() {
        val tokens = AuthTokens(
            accessToken = "a",
            refreshToken = "r",
            issuedAtEpochMs = 1_000L,
            expiresAtEpochMs = 1_000L + AuthTokens.MAX_ACCESS_TOKEN_TTL_MS,
        )
        assertThat(tokens.isExpired(1_000L + AuthTokens.MAX_ACCESS_TOKEN_TTL_MS - 1)).isFalse()
        assertThat(tokens.isExpired(1_000L + AuthTokens.MAX_ACCESS_TOKEN_TTL_MS)).isTrue()
    }
}
