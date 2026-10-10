package com.hybridmode.data.repository

import com.hybridmode.common.connectivity.ConnectivityStatus
import com.hybridmode.common.result.AppFailure
import com.hybridmode.common.result.AppResult
import com.hybridmode.common.time.Clock
import com.hybridmode.data.local.db.dao.UserDao
import com.hybridmode.data.local.db.entity.UserEntity
import com.hybridmode.data.local.secure.SecureTokenStore
import com.hybridmode.data.mapper.renewLocalWindow
import com.hybridmode.data.mapper.toDomain
import com.hybridmode.data.mapper.toTokens
import com.hybridmode.data.mapper.toUserOrNull
import com.hybridmode.data.remote.api.AuthApi
import com.hybridmode.data.remote.dto.StrapiLoginRequestDto
import com.hybridmode.data.remote.dto.StrapiRefreshRequestDto
import com.hybridmode.data.remote.dto.StrapiRegisterRequestDto
import com.hybridmode.data.remote.network.NetworkErrorMapper
import com.hybridmode.domain.model.AuthSession
import com.hybridmode.domain.model.AuthTokens
import com.hybridmode.domain.model.LoginCredentials
import com.hybridmode.domain.model.RegisterCredentials
import com.hybridmode.domain.model.User
import com.hybridmode.domain.repository.AuthRepository
import com.hybridmode.domain.repository.ConnectivityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val userDao: UserDao,
    private val tokenStore: SecureTokenStore,
    private val connectivityRepository: ConnectivityRepository,
    private val clock: Clock,
    private val json: Json,
) : AuthRepository {

    private val sessionState = MutableStateFlow<AuthSession?>(null)
    private val mutex = Mutex()

    override val sessionStream: Flow<AuthSession?> = sessionState.asStateFlow()

    override suspend fun login(credentials: LoginCredentials): AppResult<AuthSession> =
        mutex.withLock {
            runCatching {
                val response = authApi.login(
                    StrapiLoginRequestDto(
                        identifier = credentials.email,
                        password = credentials.password,
                    )
                )
                val user = response.toUserOrNull()
                    ?: error("Strapi login response missing user")
                persistSession(user, response.toTokens(clock), offline = false)
            }.fold(
                onSuccess = { AppResult.Success(it) },
                onFailure = { AppResult.Error(NetworkErrorMapper.map(it, json)) },
            )
        }

    override suspend fun register(credentials: RegisterCredentials): AppResult<AuthSession> =
        mutex.withLock {
            runCatching {
                val username = credentials.displayName
                    .trim()
                    .lowercase()
                    .replace(Regex("[^a-z0-9._-]"), "_")
                    .ifBlank { credentials.email.substringBefore("@") }
                val response = authApi.register(
                    StrapiRegisterRequestDto(
                        username = username,
                        email = credentials.email,
                        password = credentials.password,
                    )
                )
                val user = response.toUserOrNull()
                    ?: error("Strapi register response missing user")
                persistSession(user, response.toTokens(clock), offline = false)
            }.fold(
                onSuccess = { AppResult.Success(it) },
                onFailure = { AppResult.Error(NetworkErrorMapper.map(it, json)) },
            )
        }

    override suspend fun restoreSession(): AppResult<AuthSession?> = mutex.withLock {
        val tokens = tokenStore.read()
        val cachedUser = userDao.getCurrentUser()?.toDomain()
        if (tokens == null || cachedUser == null) {
            clearLocal()
            return AppResult.Success(null)
        }

        val online = connectivityRepository.currentStatus() == ConnectivityStatus.Online
        return if (online) {
            if (tokens.isExpired(clock.nowMillis())) {
                refreshLocked(tokens, cachedUser)
            } else {
                AppResult.Success(
                    persistSession(cachedUser, tokens, offline = false, writeTokens = false)
                )
            }
        } else {
            Timber.i("Restoring offline session for user=%s", cachedUser.id)
            AppResult.Success(
                persistSession(
                    user = cachedUser,
                    tokens = tokens,
                    offline = true,
                    writeTokens = false,
                )
            )
        }
    }

    override suspend fun refreshSession(): AppResult<AuthSession> = mutex.withLock {
        val tokens = tokenStore.read()
            ?: return AppResult.Error(AppFailure.Unauthorized())
        val cachedUser = userDao.getCurrentUser()?.toDomain()
        refreshLocked(tokens, cachedUser)
    }

    override suspend fun logout(): AppResult<Unit> = mutex.withLock {
        clearLocal()
        AppResult.Success(Unit)
    }

    override suspend fun currentUser(): User? = sessionState.value?.user

    private suspend fun refreshLocked(
        tokens: AuthTokens,
        cachedUser: User?,
    ): AppResult<AuthSession> {
        val hasDistinctRefresh = tokens.refreshToken.isNotBlank() &&
            tokens.refreshToken != tokens.accessToken

        return runCatching {
            if (hasDistinctRefresh) {
                val response = authApi.refresh(StrapiRefreshRequestDto(tokens.refreshToken))
                val user = response.toUserOrNull()
                    ?: cachedUser
                    ?: authApi.me("Bearer ${response.jwt}").toDomain()
                persistSession(
                    user = user,
                    tokens = response.toTokens(clock, previousRefresh = tokens.refreshToken),
                    offline = false,
                )
            } else {
                val remoteUser = authApi.me("Bearer ${tokens.accessToken}").toDomain()
                persistSession(remoteUser, tokens.renewLocalWindow(clock), offline = false)
            }
        }.fold(
            onSuccess = { AppResult.Success(it) },
            onFailure = { error ->
                val failure = NetworkErrorMapper.map(error, json)
                if (failure is AppFailure.Unauthorized) {
                    clearLocal()
                }
                AppResult.Error(failure)
            },
        )
    }

    private suspend fun persistSession(
        user: User,
        tokens: AuthTokens,
        offline: Boolean,
        writeTokens: Boolean = true,
    ): AuthSession {
        if (writeTokens) {
            tokenStore.save(tokens)
        }
        userDao.upsert(UserEntity.fromDomain(user, clock.nowMillis()))
        val session = AuthSession(
            user = user,
            tokens = tokens,
            isOfflineMode = offline,
            canOperateOffline = true,
        )
        sessionState.value = session
        return session
    }

    private suspend fun clearLocal() {
        tokenStore.clear()
        userDao.clear()
        sessionState.value = null
    }
}
