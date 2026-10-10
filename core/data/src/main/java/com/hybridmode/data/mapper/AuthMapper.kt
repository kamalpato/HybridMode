package com.hybridmode.data.mapper

import com.hybridmode.common.time.Clock
import com.hybridmode.data.remote.dto.StrapiAuthResponseDto
import com.hybridmode.data.remote.dto.StrapiUserDto
import com.hybridmode.domain.model.AuthTokens
import com.hybridmode.domain.model.User
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

fun StrapiUserDto.toDomain(): User = User(
    id = id.toString(),
    email = email,
    displayName = username?.takeIf { it.isNotBlank() } ?: email.substringBefore("@"),
    createdAtEpochMs = parseStrapiDate(createdAt),
)

fun StrapiAuthResponseDto.toTokens(clock: Clock, previousRefresh: String? = null): AuthTokens {
    val issuedAt = clock.nowMillis()
    return AuthTokens(
        accessToken = jwt,
        refreshToken = refreshToken?.takeIf { it.isNotBlank() }
            ?: previousRefresh
            ?: jwt,
        issuedAtEpochMs = issuedAt,
        expiresAtEpochMs = issuedAt + AuthTokens.MAX_ACCESS_TOKEN_TTL_MS,
    )
}

fun StrapiAuthResponseDto.toUserOrNull(): User? = user?.toDomain()

fun AuthTokens.renewLocalWindow(clock: Clock): AuthTokens {
    val now = clock.nowMillis()
    return copy(
        issuedAtEpochMs = now,
        expiresAtEpochMs = now + AuthTokens.MAX_ACCESS_TOKEN_TTL_MS,
    )
}

internal fun parseStrapiDate(value: String?): Long {
    if (value.isNullOrBlank()) return 0L
    return try {
        Instant.parse(value).toEpochMilli()
    } catch (_: DateTimeParseException) {
        try {
            OffsetDateTime.parse(value).toInstant().toEpochMilli()
        } catch (_: Exception) {
            0L
        }
    }
}
