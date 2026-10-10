package com.hybridmode.domain.usecase.auth

import com.hybridmode.common.result.AppFailure
import com.hybridmode.common.result.AppResult
import com.hybridmode.domain.model.AuthSession
import com.hybridmode.domain.model.RegisterCredentials
import com.hybridmode.domain.repository.AuthRepository
import com.hybridmode.domain.repository.ConnectivityRepository
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
        val normalizedEmail = email.trim().lowercase()
        val name = displayName.trim()
        if (name.length < 2) {
            return AppResult.Error(AppFailure.Validation("Display name is too short"))
        }
        if (normalizedEmail.isBlank() || !normalizedEmail.contains("@")) {
            return AppResult.Error(AppFailure.Validation("Enter a valid email address"))
        }
        if (password.length < 8) {
            return AppResult.Error(AppFailure.Validation("Password must be at least 8 characters"))
        }
        if (!connectivityRepository.isOnline()) {
            return AppResult.Error(
                AppFailure.Network("Registration requires an internet connection")
            )
        }
        return authRepository.register(
            RegisterCredentials(
                email = normalizedEmail,
                password = password,
                displayName = name,
            )
        )
    }
}
