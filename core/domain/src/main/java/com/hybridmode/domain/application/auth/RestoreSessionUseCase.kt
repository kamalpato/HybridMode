package com.hybridmode.domain.application.auth

import com.hybridmode.common.result.AppResult
import com.hybridmode.domain.auth.AuthSession
import com.hybridmode.domain.auth.AuthRepository
import javax.inject.Inject

class RestoreSessionUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(): AppResult<AuthSession?> = authRepository.restoreSession()
}
