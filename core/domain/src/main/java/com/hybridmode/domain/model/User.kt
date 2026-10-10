package com.hybridmode.domain.model

data class User(
    val id: String,
    val email: String,
    val displayName: String,
    val createdAtEpochMs: Long,
)
