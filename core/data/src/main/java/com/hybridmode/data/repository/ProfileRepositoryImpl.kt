package com.hybridmode.data.repository

import com.hybridmode.common.connectivity.ConnectivityStatus
import com.hybridmode.common.result.AppFailure
import com.hybridmode.common.result.AppResult
import com.hybridmode.common.time.Clock
import com.hybridmode.data.local.db.dao.UserDao
import com.hybridmode.data.local.db.entity.UserEntity
import com.hybridmode.data.local.secure.SecureTokenStore
import com.hybridmode.data.mapper.toDomain
import com.hybridmode.data.remote.api.AuthApi
import com.hybridmode.data.remote.network.NetworkErrorMapper
import com.hybridmode.domain.auth.User
import com.hybridmode.domain.connectivity.ConnectivityRepository
import com.hybridmode.domain.profile.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val userDao: UserDao,
    private val tokenStore: SecureTokenStore,
    private val connectivityRepository: ConnectivityRepository,
    private val clock: Clock,
    private val json: Json,
) : ProfileRepository {

    override fun observeCachedUser(): Flow<User?> =
        userDao.observeCurrentUser().map { it?.toDomain() }

    override suspend fun syncProfile(): AppResult<User> {
        val cached = userDao.getCurrentUser()?.toDomain()
        val online = connectivityRepository.currentStatus() == ConnectivityStatus.Online

        if (!online) {
            return if (cached != null) {
                AppResult.Success(cached)
            } else {
                AppResult.Error(AppFailure.Network("Profile unavailable offline"))
            }
        }

        val tokens = tokenStore.read()
            ?: return AppResult.Error(AppFailure.Unauthorized())

        if (tokens.isExpired(clock.nowMillis())) {
            return AppResult.Error(
                AppFailure.Unauthorized("Access token expired. Refresh required.")
            )
        }

        return runCatching {
            val remote = authApi.me("Bearer ${tokens.accessToken}").toDomain()
            userDao.upsert(UserEntity.fromDomain(remote, clock.nowMillis()))
            remote
        }.fold(
            onSuccess = { AppResult.Success(it) },
            onFailure = { AppResult.Error(NetworkErrorMapper.map(it, json)) },
        )
    }
}
