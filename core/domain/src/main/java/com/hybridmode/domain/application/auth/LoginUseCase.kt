package com.hybridmode.domain.application.auth

import com.hybridmode.common.result.AppFailure
import com.hybridmode.common.result.AppResult
import com.hybridmode.domain.auth.AuthRepository
import com.hybridmode.domain.auth.AuthSession
import com.hybridmode.domain.auth.LoginCredentials
import com.hybridmode.domain.connectivity.ConnectivityRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val connectivityRepository: ConnectivityRepository,
) {
    suspend operator fun invoke(email: String, password: String): AppResult<AuthSession> {
        if (!connectivityRepository.isOnline()) {
            return AppResult.Error(
                AppFailure.Network("Sign-in requires an internet connection")
            )
        }
        val credentials = runCatching {
            LoginCredentials.of(email, password)
        }.getOrElse {
            return AppResult.Error(AppFailure.Validation(it.message ?: "Invalid credentials"))
        }
        return authRepository.login(credentials)
    }
}
