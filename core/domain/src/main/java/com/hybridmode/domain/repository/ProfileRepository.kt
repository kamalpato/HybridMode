package com.hybridmode.domain.repository

import com.hybridmode.common.result.AppResult
import com.hybridmode.domain.model.User
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun observeCachedUser(): Flow<User?>

    suspend fun syncProfile(): AppResult<User>
}
