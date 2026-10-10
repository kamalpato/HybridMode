package com.hybridmode.domain.auth

import com.hybridmode.domain.shared.ValueObject

data class AuthSession(
    val user: User,
    val tokens: AuthTokens,
    val isOfflineMode: Boolean,
    val canOperateOffline: Boolean,
) : ValueObject {
    init {
        require(canOperateOffline || !isOfflineMode) {
            "Offline session requires offline capability"
        }
    }

    fun withOfflineMode(offline: Boolean): AuthSession = copy(isOfflineMode = offline)
}
