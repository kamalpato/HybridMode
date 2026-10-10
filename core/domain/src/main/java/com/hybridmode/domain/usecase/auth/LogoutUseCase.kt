package com.hybridmode.domain.usecase.auth

import com.hybridmode.common.result.AppResult
import com.hybridmode.domain.repository.AuthRepository
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(): AppResult<Unit> = authRepository.logout()
}
