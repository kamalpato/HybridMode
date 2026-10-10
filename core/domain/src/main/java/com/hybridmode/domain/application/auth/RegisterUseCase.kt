package com.hybridmode.domain.application.auth

import com.hybridmode.common.result.AppFailure
import com.hybridmode.common.result.AppResult
import com.hybridmode.domain.auth.AuthRepository
import com.hybridmode.domain.auth.AuthSession
import com.hybridmode.domain.auth.RegisterCredentials
import com.hybridmode.domain.connectivity.ConnectivityRepository
import javax.inject.Inject

class RegisterUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val connectivityRepository: ConnectivityRepository,
) {
    suspend operator fun invoke(
        email: String,
        password: String,
        displayName: String,
    ): AppResult<AuthSession> {
        if (!connectivityRepository.isOnline()) {
            return AppResult.Error(
                AppFailure.Network("Registration requires an internet connection")
            )
        }
        val credentials = runCatching {
            RegisterCredentials.of(email, password, displayName)
        }.getOrElse {
            return AppResult.Error(AppFailure.Validation(it.message ?: "Invalid registration"))
        }
        return authRepository.register(credentials)
    }
}
