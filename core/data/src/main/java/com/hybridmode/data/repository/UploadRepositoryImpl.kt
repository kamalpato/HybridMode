package com.hybridmode.data.repository

import com.hybridmode.common.connectivity.ConnectivityStatus
import com.hybridmode.common.result.AppFailure
import com.hybridmode.common.result.AppResult
import com.hybridmode.common.time.Clock
import com.hybridmode.data.di.ApplicationScope
import com.hybridmode.data.local.db.dao.UploadDao
import com.hybridmode.data.local.db.dao.UserDao
import com.hybridmode.data.local.db.entity.UploadEntity
import com.hybridmode.data.local.file.LocalImageStore
import com.hybridmode.data.local.secure.SecureTokenStore
import com.hybridmode.data.mapper.absoluteUrl
import com.hybridmode.data.mapper.buildStrapiFileInfoJson
import com.hybridmode.data.mapper.toTokens
import com.hybridmode.data.remote.api.AuthApi
import com.hybridmode.data.remote.api.UploadApi
import com.hybridmode.data.remote.dto.StrapiRefreshRequestDto
import com.hybridmode.data.remote.network.NetworkErrorMapper
import com.hybridmode.domain.model.AuthTokens
import com.hybridmode.domain.model.MediaUpload
import com.hybridmode.domain.model.NewMediaUpload
import com.hybridmode.domain.model.UploadSchema
import com.hybridmode.domain.model.UploadSyncReport
import com.hybridmode.domain.model.UploadSyncStatus
import com.hybridmode.domain.repository.ConnectivityRepository
import com.hybridmode.domain.repository.UploadRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@Singleton
class UploadRepositoryImpl @Inject constructor(
    private val uploadDao: UploadDao,
    private val userDao: UserDao,
    private val uploadApi: UploadApi,
    private val authApi: AuthApi,
    private val localImageStore: LocalImageStore,
    private val tokenStore: SecureTokenStore,
    private val connectivityRepository: ConnectivityRepository,
    private val clock: Clock,
    private val json: Json,
    @ApplicationScope appScope: CoroutineScope,
) : UploadRepository {

    private val mutex = Mutex()

    private val uploads: Flow<List<MediaUpload>> = userDao.observeCurrentUser()
        .flatMapLatest { user ->
            if (user == null) {
                flowOf(emptyList())
            } else {
                uploadDao.observeByOwner(user.id).map { list -> list.map { it.toDomain() } }
            }
        }
        .distinctUntilChanged()
        .shareIn(
            scope = appScope,
            started = SharingStarted.WhileSubscribed(5_000),
            replay = 1,
        )

    override fun observeUploads(): Flow<List<MediaUpload>> = uploads

    override fun observeUpload(id: String): Flow<MediaUpload?> =
        userDao.observeCurrentUser()
            .flatMapLatest { user ->
                if (user == null) {
                    flowOf(null)
                } else {
                    uploadDao.observeById(id).map { entity ->
                        entity
                            ?.takeIf { it.ownerUserId == user.id }
                            ?.toDomain()
                    }
                }
            }
            .distinctUntilChanged()

    override suspend fun submit(input: NewMediaUpload): AppResult<MediaUpload> = mutex.withLock {
        val ownerUserId = userDao.getCurrentUser()?.id
            ?: return AppResult.Error(
                AppFailure.Unauthorized("Please sign in to submit a report")
            )

        val now = clock.nowMillis()
        val id = UUID.randomUUID().toString()
        val online = connectivityRepository.currentStatus() == ConnectivityStatus.Online
        val schema = if (online) UploadSchema.Online else UploadSchema.Offline

        val localFile = runCatching {
            localImageStore.persistFromUri(input.sourceUri, input.mimeType)
        }.getOrElse { error ->
            return AppResult.Error(
                AppFailure.Validation(
                    message = error.message ?: "Could not save image",
                    cause = error,
                )
            )
        }

        var upload = MediaUpload(
            id = id,
            ownerUserId = ownerUserId,
            title = input.title,
            description = input.description,
            localFilePath = localFile.absolutePath,
            mimeType = input.mimeType,
            schema = schema,
            syncStatus = if (online) UploadSyncStatus.Uploading else UploadSyncStatus.PendingOffline,
            remoteUrl = null,
            errorMessage = null,
            createdAtEpochMs = now,
            syncedAtEpochMs = null,
        )
        uploadDao.upsert(UploadEntity.fromDomain(upload))

        if (!online) {
            Timber.i("Offline schema: queued upload id=%s owner=%s", id, ownerUserId)
            return AppResult.Success(upload)
        }

        return when (val result = uploadToRemote(upload)) {
            is AppResult.Success -> AppResult.Success(result.data)
            is AppResult.Error -> {
                upload = upload.copy(
                    syncStatus = UploadSyncStatus.Failed,
                    errorMessage = result.failure.message,
                )
                uploadDao.upsert(UploadEntity.fromDomain(upload))
                AppResult.Error(result.failure)
            }
        }
    }

    override suspend fun syncPending(): AppResult<UploadSyncReport> = mutex.withLock {
        if (connectivityRepository.currentStatus() != ConnectivityStatus.Online) {
            return AppResult.Error(AppFailure.Network("Cannot sync while offline"))
        }
        val ownerUserId = userDao.getCurrentUser()?.id
            ?: return AppResult.Error(AppFailure.Unauthorized("Please sign in to sync"))
        val pending = uploadDao.getPendingOrFailed(ownerUserId)
        var syncedCount = 0
        var failedCount = 0
        for (entity in pending) {
            when (uploadToRemote(entity.toDomain())) {
                is AppResult.Success -> syncedCount++
                is AppResult.Error -> failedCount++
            }
        }
        AppResult.Success(
            UploadSyncReport(
                pendingAttempted = pending.size,
                synced = syncedCount,
                failed = failedCount,
                alreadySyncedTotal = uploadDao.countSynced(ownerUserId),
            )
        )
    }

    override suspend fun retry(uploadId: String): AppResult<MediaUpload> = mutex.withLock {
        if (connectivityRepository.currentStatus() != ConnectivityStatus.Online) {
            return AppResult.Error(AppFailure.Network("Retry requires an internet connection"))
        }
        val ownerUserId = userDao.getCurrentUser()?.id
            ?: return AppResult.Error(AppFailure.Unauthorized("Please sign in"))
        val existing = uploadDao.getById(uploadId)?.toDomain()
            ?.takeIf { it.ownerUserId == ownerUserId }
            ?: return AppResult.Error(AppFailure.NotFound("Upload not found"))
        val uploading = existing.copy(
            syncStatus = UploadSyncStatus.Uploading,
            errorMessage = null,
            schema = UploadSchema.Online,
        )
        uploadDao.upsert(UploadEntity.fromDomain(uploading))
        uploadToRemote(uploading)
    }

    override suspend fun delete(uploadId: String): AppResult<Unit> = mutex.withLock {
        val ownerUserId = userDao.getCurrentUser()?.id
            ?: return AppResult.Error(AppFailure.Unauthorized("Please sign in"))
        val existing = uploadDao.getById(uploadId)
            ?.takeIf { it.ownerUserId == ownerUserId }
            ?: return AppResult.Error(AppFailure.NotFound("Upload not found"))
        localImageStore.deleteIfExists(existing.localFilePath)
        uploadDao.deleteById(uploadId)
        AppResult.Success(Unit)
    }

    private suspend fun uploadToRemote(upload: MediaUpload): AppResult<MediaUpload> {
        val tokens = ensureFreshAccessToken()
            ?: return AppResult.Error(
                AppFailure.Unauthorized(
                    "Your session expired. Please sign in again."
                )
            )

        val file = File(upload.localFilePath)
        if (!file.exists()) {
            return AppResult.Error(AppFailure.NotFound("Local image file missing"))
        }

        return runCatching {
            val mediaType = upload.mimeType.toMediaTypeOrNull()
            val fileBody = file.asRequestBody(mediaType)
            val part = MultipartBody.Part.createFormData("files", file.name, fileBody)
            val fileInfo = buildStrapiFileInfoJson(json, upload.title, upload.description)
                .toRequestBody(TEXT)

            val response = uploadApi.upload(
                bearer = "Bearer ${tokens.accessToken}",
                files = part,
                fileInfo = fileInfo,
            )
            val uploaded = response.firstOrNull()
                ?: error("Strapi upload returned an empty list")

            val synced = upload.copy(
                syncStatus = UploadSyncStatus.Synced,
                schema = UploadSchema.Online,
                remoteUrl = uploaded.absoluteUrl(),
                errorMessage = null,
                syncedAtEpochMs = clock.nowMillis(),
            )
            uploadDao.upsert(UploadEntity.fromDomain(synced))
            synced
        }.fold(
            onSuccess = { AppResult.Success(it) },
            onFailure = { error ->
                val failure = NetworkErrorMapper.map(error, json)
                val failed = upload.copy(
                    syncStatus = UploadSyncStatus.Failed,
                    errorMessage = failure.message,
                )
                uploadDao.upsert(UploadEntity.fromDomain(failed))
                AppResult.Error(failure)
            },
        )
    }

    private suspend fun ensureFreshAccessToken(): AuthTokens? {
        val current = tokenStore.read() ?: return null
        if (!current.isExpired(clock.nowMillis())) return current

        val hasRefresh = current.refreshToken.isNotBlank() &&
            current.refreshToken != current.accessToken
        if (!hasRefresh) return null

        return runCatching {
            val response = authApi.refresh(StrapiRefreshRequestDto(current.refreshToken))
            val renewed = response.toTokens(clock, previousRefresh = current.refreshToken)
            tokenStore.save(renewed)
            Timber.i("Refreshed access token before upload")
            renewed
        }.getOrElse {
            Timber.w(it, "Token refresh before upload failed")
            null
        }
    }

    private companion object {
        val TEXT = "text/plain".toMediaType()
    }
}
