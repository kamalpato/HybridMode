package com.hybridmode.domain.profile

import com.hybridmode.common.result.AppResult
import com.hybridmode.domain.auth.User
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun observeCachedUser(): Flow<User?>

    suspend fun syncProfile(): AppResult<User>
}
