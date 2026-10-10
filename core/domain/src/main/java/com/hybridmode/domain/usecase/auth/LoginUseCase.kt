package com.hybridmode.domain.usecase.auth

import com.hybridmode.common.result.AppFailure
import com.hybridmode.common.result.AppResult
import com.hybridmode.domain.model.AuthSession
import com.hybridmode.domain.model.LoginCredentials
import com.hybridmode.domain.repository.AuthRepository
import com.hybridmode.domain.repository.ConnectivityRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val connectivityRepository: ConnectivityRepository,
) {
    suspend operator fun invoke(email: String, password: String): AppResult<AuthSession> {
        val normalizedEmail = email.trim().lowercase()
        if (normalizedEmail.isBlank() || !normalizedEmail.contains("@")) {
            return AppResult.Error(AppFailure.Validation("Enter a valid email address"))
        }
        if (password.length < 8) {
            return AppResult.Error(AppFailure.Validation("Password must be at least 8 characters"))
        }
        if (!connectivityRepository.isOnline()) {
            return AppResult.Error(
                AppFailure.Network("Sign-in requires an internet connection")
            )
        }
        return authRepository.login(LoginCredentials(normalizedEmail, password))
    }
}
