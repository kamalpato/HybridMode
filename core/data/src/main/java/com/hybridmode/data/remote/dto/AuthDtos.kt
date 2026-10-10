package com.hybridmode.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StrapiLoginRequestDto(
    val identifier: String,
    val password: String,
)

@Serializable
data class StrapiRegisterRequestDto(
    val username: String,
    val email: String,
    val password: String,
)

@Serializable
data class StrapiRefreshRequestDto(
    val refreshToken: String,
)

@Serializable
data class StrapiLogoutRequestDto(
    val refreshToken: String? = null,
    val scope: String? = null,
)

@Serializable
data class StrapiLogoutResponseDto(
    val ok: Boolean? = null,
)

@Serializable
data class StrapiAuthResponseDto(
    val jwt: String,
    val user: StrapiUserDto? = null,
    val refreshToken: String? = null,
)

@Serializable
data class StrapiUserDto(
    val id: Long,
    val username: String? = null,
    val email: String,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("updatedAt") val updatedAt: String? = null,
    val confirmed: Boolean? = null,
    val blocked: Boolean? = null,
    val provider: String? = null,
)

@Serializable
data class StrapiErrorEnvelope(
    val error: StrapiErrorBody? = null,
    val message: String? = null,
)

@Serializable
data class StrapiErrorBody(
    val status: Int? = null,
    val name: String? = null,
    val message: String? = null,
)
