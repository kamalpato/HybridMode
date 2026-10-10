package com.hybridmode.domain.model

data class AuthSession(
    val user: User,
    val tokens: AuthTokens,
    val isOfflineMode: Boolean,
    val canOperateOffline: Boolean,
)
