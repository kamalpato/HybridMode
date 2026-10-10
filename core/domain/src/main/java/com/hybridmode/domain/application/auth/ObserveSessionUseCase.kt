package com.hybridmode.domain.application.auth

import com.hybridmode.domain.auth.AuthSession
import com.hybridmode.domain.auth.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveSessionUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    operator fun invoke(): Flow<AuthSession?> = authRepository.sessionStream
}
