package com.hybridmode.domain.repository

import com.hybridmode.common.result.AppResult
import com.hybridmode.domain.model.AuthSession
import com.hybridmode.domain.model.LoginCredentials
import com.hybridmode.domain.model.RegisterCredentials
import com.hybridmode.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val sessionStream: Flow<AuthSession?>

    suspend fun login(credentials: LoginCredentials): AppResult<AuthSession>

    suspend fun register(credentials: RegisterCredentials): AppResult<AuthSession>

    suspend fun restoreSession(): AppResult<AuthSession?>

    suspend fun refreshSession(): AppResult<AuthSession>

    suspend fun logout(): AppResult<Unit>

    suspend fun currentUser(): User?
}
