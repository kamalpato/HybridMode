package com.hybridmode.domain.application.auth

import com.hybridmode.common.result.AppResult
import com.hybridmode.domain.auth.AuthSession
import com.hybridmode.domain.auth.AuthRepository
import com.hybridmode.domain.connectivity.ConnectivityRepository
import javax.inject.Inject

class RefreshSessionUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val connectivityRepository: ConnectivityRepository,
) {
    suspend operator fun invoke(): AppResult<AuthSession> {
        if (!connectivityRepository.isOnline()) {
            return authRepository.restoreSession().let { result ->
                when (result) {
                    is AppResult.Success -> {
                        val session = result.data
                        if (session != null) {
                            AppResult.Success(session)
                        } else {
                            AppResult.Error(
                                com.hybridmode.common.result.AppFailure.Unauthorized()
                            )
                        }
                    }
                    is AppResult.Error -> result
                }
            }
        }
        return authRepository.refreshSession()
    }
}
