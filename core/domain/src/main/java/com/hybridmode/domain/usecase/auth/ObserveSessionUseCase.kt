package com.hybridmode.domain.usecase.auth

import com.hybridmode.domain.model.AuthSession
import com.hybridmode.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveSessionUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    operator fun invoke(): Flow<AuthSession?> = authRepository.sessionStream
}
