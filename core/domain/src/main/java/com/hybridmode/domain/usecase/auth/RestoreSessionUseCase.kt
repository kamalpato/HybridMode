package com.hybridmode.domain.usecase.auth

import com.hybridmode.common.result.AppResult
import com.hybridmode.domain.model.AuthSession
import com.hybridmode.domain.repository.AuthRepository
import javax.inject.Inject

class RestoreSessionUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(): AppResult<AuthSession?> = authRepository.restoreSession()
}
