package com.hybridmode.domain.model

data class LoginCredentials(
    val email: String,
    val password: String,
)

data class RegisterCredentials(
    val email: String,
    val password: String,
    val displayName: String,
)
