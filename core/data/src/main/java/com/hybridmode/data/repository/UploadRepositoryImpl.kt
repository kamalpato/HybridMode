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
import com.hybridmode.data.mapper.absoluteUrl
import com.hybridmode.data.mapper.buildStrapiFileInfoJson
import com.hybridmode.data.remote.api.UploadApi
import com.hybridmode.data.remote.network.NetworkErrorMapper
import com.hybridmode.domain.auth.AuthRepository
import com.hybridmode.domain.connectivity.ConnectivityRepository
import com.hybridmode.domain.reporting.FieldReport
import com.hybridmode.domain.reporting.FieldReportRepository
import com.hybridmode.domain.reporting.FieldReportSyncReport
import com.hybridmode.domain.reporting.NewFieldReport
import com.hybridmode.domain.reporting.ReportSyncDomainService
import com.hybridmode.domain.reporting.ReportSyncStatus
import com.hybridmode.domain.shared.DomainEventPublisher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
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
    private val authRepository: AuthRepository,
    private val localImageStore: LocalImageStore,
    private val connectivityRepository: ConnectivityRepository,
    private val clock: Clock,
    private val json: Json,
    private val eventPublisher: DomainEventPublisher,
    @ApplicationScope appScope: CoroutineScope,
) : FieldReportRepository {

    private val mutex = Mutex()

    private val reports: Flow<List<FieldReport>> = userDao.observeCurrentUser()
        .flatMapLatest { user ->
            if (user == null) {
                flowOf(emptyList())
            } else {
                uploadDao.observeByOwner(user.id).map { list -> list.map { it.toDomain() } }
            }
        }
        .shareIn(
            scope = appScope,
            // Eager + zero replay expiry so logout cannot flash another user's list.
            started = SharingStarted.Eagerly,
            replay = 1,
        )

    override fun observeReports(): Flow<List<FieldReport>> = reports

    override fun observeReport(id: String): Flow<FieldReport?> =
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

    override suspend fun submit(input: NewFieldReport): AppResult<FieldReport> = mutex.withLock {
        val ownerUserId = userDao.getCurrentUser()?.id
            ?: return AppResult.Error(
                AppFailure.Unauthorized("Please sign in to submit a report")
            )

        val now = clock.nowMillis()
        val id = UUID.randomUUID().toString()
        val online = connectivityRepository.currentStatus() == ConnectivityStatus.Online

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

        var report = if (online) {
            FieldReport.startOnlineUpload(
                id = id,
                ownerUserId = ownerUserId,
                title = input.title,
                description = input.description,
                localFilePath = localFile.absolutePath,
                mimeType = input.mimeType,
                createdAtEpochMs = now,
            )
        } else {
            FieldReport.queueOffline(
                id = id,
                ownerUserId = ownerUserId,
                title = input.title,
                description = input.description,
                localFilePath = localFile.absolutePath,
                mimeType = input.mimeType,
                createdAtEpochMs = now,
            )
        }
        uploadDao.upsert(UploadEntity.fromDomain(report))
        eventPublisher.publish(report.pullDomainEvents())

        if (!online) {
            Timber.i("Offline schema: queued report id=%s owner=%s", id, ownerUserId)
            return AppResult.Success(report)
        }

        return when (val result = uploadToRemote(report)) {
            is AppResult.Success -> AppResult.Success(result.data)
            is AppResult.Error -> {
                // Already persisted as Failed inside uploadToRemote; treat as saved locally.
                AppResult.Success(
                    uploadDao.getById(report.id)?.toDomain() ?: report.markFailed(result.failure.message)
                )
            }
        }
    }

    override suspend fun syncPending(): AppResult<FieldReportSyncReport> = mutex.withLock {
        if (connectivityRepository.currentStatus() != ConnectivityStatus.Online) {
            return AppResult.Error(AppFailure.Network("Cannot sync while offline"))
        }
        val ownerUserId = userDao.getCurrentUser()?.id
            ?: return AppResult.Error(AppFailure.Unauthorized("Please sign in to sync"))
        val pending = uploadDao.getPendingOrFailed(ownerUserId)
        var syncedCount = 0
        var failedCount = 0
        for (entity in pending) {
            val report = entity.toDomain()
            if (!ReportSyncDomainService.canAttemptRemoteSync(online = true, report = report)) {
                continue
            }
            when (uploadToRemote(report)) {
                is AppResult.Success -> syncedCount++
                is AppResult.Error -> failedCount++
            }
        }
        AppResult.Success(
            FieldReportSyncReport(
                pendingAttempted = pending.size,
                synced = syncedCount,
                failed = failedCount,
                alreadySyncedTotal = uploadDao.countSynced(ownerUserId),
            )
        )
    }

    override suspend fun retry(reportId: String): AppResult<FieldReport> = mutex.withLock {
        if (connectivityRepository.currentStatus() != ConnectivityStatus.Online) {
            return AppResult.Error(AppFailure.Network("Retry requires an internet connection"))
        }
        val ownerUserId = userDao.getCurrentUser()?.id
            ?: return AppResult.Error(AppFailure.Unauthorized("Please sign in"))
        val existing = uploadDao.getById(reportId)?.toDomain()
            ?.takeIf { it.isOwnedBy(ownerUserId) }
            ?: return AppResult.Error(AppFailure.NotFound("Report not found"))
        if (existing.syncStatus == ReportSyncStatus.Synced) {
            return AppResult.Success(existing)
        }
        val uploading = existing.markUploading()
            ?: return AppResult.Error(AppFailure.Validation("This report cannot be uploaded now"))
        uploadDao.upsert(UploadEntity.fromDomain(uploading))
        eventPublisher.publish(uploading.pullDomainEvents())
        uploadToRemote(uploading)
    }

    override suspend fun delete(reportId: String): AppResult<Unit> = mutex.withLock {
        val ownerUserId = userDao.getCurrentUser()?.id
            ?: return AppResult.Error(AppFailure.Unauthorized("Please sign in"))
        val existing = uploadDao.getById(reportId)
            ?.takeIf { it.ownerUserId == ownerUserId }
            ?: return AppResult.Error(AppFailure.NotFound("Report not found"))
        localImageStore.deleteIfExists(existing.localFilePath)
        uploadDao.deleteById(reportId)
        AppResult.Success(Unit)
    }

    private suspend fun uploadToRemote(report: FieldReport): AppResult<FieldReport> {
        val tokens = when (val tokenResult = authRepository.ensureValidAccessToken()) {
            is AppResult.Success -> tokenResult.data
            is AppResult.Error -> {
                val failed = report.markFailed(tokenResult.failure.message)
                uploadDao.upsert(UploadEntity.fromDomain(failed))
                eventPublisher.publish(failed.pullDomainEvents())
                return AppResult.Error(tokenResult.failure)
            }
        }

        val file = File(report.localImagePath.value)
        if (!file.exists()) {
            val failed = report.markFailed("Local image file missing")
            uploadDao.upsert(UploadEntity.fromDomain(failed))
            eventPublisher.publish(failed.pullDomainEvents())
            return AppResult.Error(AppFailure.NotFound("Local image file missing"))
        }

        return runCatching {
            val mediaType = report.mimeType.value.toMediaTypeOrNull()
            val fileBody = file.asRequestBody(mediaType)
            val part = MultipartBody.Part.createFormData("files", file.name, fileBody)
            val fileInfo = buildStrapiFileInfoJson(
                json,
                report.title.value,
                report.description.value,
            ).toRequestBody(TEXT)

            val response = uploadApi.upload(
                bearer = "Bearer ${tokens.accessToken}",
                files = part,
                fileInfo = fileInfo,
            )
            val uploaded = response.firstOrNull()
                ?: error("Strapi upload returned an empty list")

            val synced = report.markSynced(uploaded.absoluteUrl(), clock.nowMillis())
            uploadDao.upsert(UploadEntity.fromDomain(synced))
            eventPublisher.publish(synced.pullDomainEvents())
            synced
        }.fold(
            onSuccess = { AppResult.Success(it) },
            onFailure = { error ->
                val failure = NetworkErrorMapper.map(error, json)
                val failed = report.markFailed(failure.message)
                uploadDao.upsert(UploadEntity.fromDomain(failed))
                eventPublisher.publish(failed.pullDomainEvents())
                AppResult.Error(failure)
            },
        )
    }

    private companion object {
        val TEXT = "text/plain".toMediaType()
    }
}
