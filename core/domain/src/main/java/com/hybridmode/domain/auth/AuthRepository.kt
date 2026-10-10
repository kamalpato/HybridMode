package com.hybridmode.domain.auth

import com.hybridmode.common.result.AppResult
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val sessionStream: Flow<AuthSession?>

    suspend fun login(credentials: LoginCredentials): AppResult<AuthSession>

    suspend fun register(credentials: RegisterCredentials): AppResult<AuthSession>

    suspend fun restoreSession(): AppResult<AuthSession?>

    suspend fun refreshSession(): AppResult<AuthSession>

    /** Single mutex-gated token refresh used by uploads/sync. */
    suspend fun ensureValidAccessToken(): AppResult<AuthTokens>

    suspend fun logout(): AppResult<Unit>

    suspend fun currentUser(): User?
}
