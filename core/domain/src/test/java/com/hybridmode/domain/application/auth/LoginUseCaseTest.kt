package com.hybridmode.domain.application.auth

import com.google.common.truth.Truth.assertThat
import com.hybridmode.common.connectivity.ConnectivityStatus
import com.hybridmode.common.result.AppFailure
import com.hybridmode.common.result.AppResult
import com.hybridmode.domain.auth.AuthRepository
import com.hybridmode.domain.auth.AuthSession
import com.hybridmode.domain.auth.AuthTokens
import com.hybridmode.domain.auth.LoginCredentials
import com.hybridmode.domain.auth.RegisterCredentials
import com.hybridmode.domain.auth.User
import com.hybridmode.domain.connectivity.ConnectivityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class LoginUseCaseTest {

    @Test
    fun rejectsInvalidEmail() = runTest {
        val useCase = LoginUseCase(FakeAuthRepository(), FakeConnectivity(true))
        val result = useCase("bad", "password123")
        assertThat(result).isInstanceOf(AppResult.Error::class.java)
        assertThat((result as AppResult.Error).failure).isInstanceOf(AppFailure.Validation::class.java)
    }

    @Test
    fun rejectsOfflineLogin() = runTest {
        val useCase = LoginUseCase(FakeAuthRepository(), FakeConnectivity(false))
        val result = useCase("demo@hybridmode.app", "password123")
        assertThat((result as AppResult.Error).failure).isInstanceOf(AppFailure.Network::class.java)
    }

    @Test
    fun logsInWhenOnline() = runTest {
        val repo = FakeAuthRepository()
        val useCase = LoginUseCase(repo, FakeConnectivity(true))
        val result = useCase("demo@hybridmode.app", "password123")
        assertThat(result.isSuccess).isTrue()
    }

    private class FakeConnectivity(private val online: Boolean) : ConnectivityRepository {
        override val status: Flow<ConnectivityStatus> =
            flowOf(if (online) ConnectivityStatus.Online else ConnectivityStatus.Offline)

        override fun currentStatus(): ConnectivityStatus =
            if (online) ConnectivityStatus.Online else ConnectivityStatus.Offline
    }

    private class FakeAuthRepository : AuthRepository {
        private val session = MutableStateFlow<AuthSession?>(null)
        override val sessionStream: Flow<AuthSession?> = session

        override suspend fun login(credentials: LoginCredentials): AppResult<AuthSession> {
            val auth = AuthSession(
                user = User.rehydrate("1", credentials.email.value, "Demo", 0L),
                tokens = AuthTokens("a", "r", 0L, AuthTokens.MAX_ACCESS_TOKEN_TTL_MS),
                isOfflineMode = false,
                canOperateOffline = true,
            )
            session.value = auth
            return AppResult.Success(auth)
        }

        override suspend fun register(credentials: RegisterCredentials) =
            AppResult.Error(AppFailure.Unknown())

        override suspend fun restoreSession() = AppResult.Success(session.value)
        override suspend fun refreshSession() = AppResult.Error(AppFailure.Unknown())
        override suspend fun ensureValidAccessToken() = AppResult.Error(AppFailure.Unknown())
        override suspend fun logout() = AppResult.Success(Unit)
        override suspend fun currentUser() = session.value?.user
    }
}
