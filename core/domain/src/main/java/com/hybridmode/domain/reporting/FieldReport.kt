package com.hybridmode.domain.reporting

import com.hybridmode.domain.reporting.value.LocalImagePath
import com.hybridmode.domain.reporting.value.MimeType
import com.hybridmode.domain.reporting.value.ReportDescription
import com.hybridmode.domain.reporting.value.ReportId
import com.hybridmode.domain.reporting.value.ReportTitle
import com.hybridmode.domain.shared.AggregateRoot

/**
 * Reporting bounded context — FieldReport aggregate root.
 */
class FieldReport private constructor(
    val reportId: ReportId,
    val ownerUserId: String,
    val title: ReportTitle,
    val description: ReportDescription,
    val localImagePath: LocalImagePath,
    val mimeType: MimeType,
    val captureMode: ReportCaptureMode,
    val syncStatus: ReportSyncStatus,
    val remoteUrl: String?,
    val errorMessage: String?,
    val createdAtEpochMs: Long,
    val syncedAtEpochMs: Long?,
) : AggregateRoot() {

    val id: String get() = reportId.value

    companion object {
        fun queueOffline(
            id: String,
            ownerUserId: String,
            title: String,
            description: String,
            localFilePath: String,
            mimeType: String,
            createdAtEpochMs: Long,
        ): FieldReport {
            val report = FieldReport(
                reportId = ReportId.create(id),
                ownerUserId = ownerUserId,
                title = ReportTitle.create(title),
                description = ReportDescription.create(description),
                localImagePath = LocalImagePath.create(localFilePath),
                mimeType = MimeType.create(mimeType),
                captureMode = ReportCaptureMode.Offline,
                syncStatus = ReportSyncStatus.PendingOffline,
                remoteUrl = null,
                errorMessage = null,
                createdAtEpochMs = createdAtEpochMs,
                syncedAtEpochMs = null,
            )
            report.record(
                ReportQueuedOffline(report.id, ownerUserId, createdAtEpochMs),
            )
            return report
        }

        fun startOnlineUpload(
            id: String,
            ownerUserId: String,
            title: String,
            description: String,
            localFilePath: String,
            mimeType: String,
            createdAtEpochMs: Long,
        ): FieldReport {
            val report = FieldReport(
                reportId = ReportId.create(id),
                ownerUserId = ownerUserId,
                title = ReportTitle.create(title),
                description = ReportDescription.create(description),
                localImagePath = LocalImagePath.create(localFilePath),
                mimeType = MimeType.create(mimeType),
                captureMode = ReportCaptureMode.Online,
                syncStatus = ReportSyncStatus.Uploading,
                remoteUrl = null,
                errorMessage = null,
                createdAtEpochMs = createdAtEpochMs,
                syncedAtEpochMs = null,
            )
            report.record(ReportUploadStarted(report.id, createdAtEpochMs))
            return report
        }

        fun rehydrate(
            id: String,
            ownerUserId: String,
            title: String,
            description: String,
            localFilePath: String,
            mimeType: String,
            captureMode: ReportCaptureMode,
            syncStatus: ReportSyncStatus,
            remoteUrl: String?,
            errorMessage: String?,
            createdAtEpochMs: Long,
            syncedAtEpochMs: Long?,
        ): FieldReport = FieldReport(
            reportId = ReportId.rehydrate(id),
            ownerUserId = ownerUserId,
            title = ReportTitle.rehydrate(title),
            description = ReportDescription.rehydrate(description),
            localImagePath = LocalImagePath.rehydrate(localFilePath),
            mimeType = MimeType.rehydrate(mimeType),
            captureMode = captureMode,
            syncStatus = syncStatus,
            remoteUrl = remoteUrl,
            errorMessage = errorMessage,
            createdAtEpochMs = createdAtEpochMs,
            syncedAtEpochMs = syncedAtEpochMs,
        )
    }

    fun markUploading(): FieldReport? {
        if (syncStatus == ReportSyncStatus.Uploading) return this
        if (!canRetry()) return null
        val next = copyState(
            captureMode = ReportCaptureMode.Online,
            syncStatus = ReportSyncStatus.Uploading,
            errorMessage = null,
        )
        next.record(ReportUploadStarted(id, System.currentTimeMillis()))
        return next
    }

    fun markSynced(remoteUrl: String, syncedAtEpochMs: Long): FieldReport {
        require(remoteUrl.isNotBlank()) { "Remote URL required" }
        val next = copyState(
            captureMode = ReportCaptureMode.Online,
            syncStatus = ReportSyncStatus.Synced,
            remoteUrl = remoteUrl,
            errorMessage = null,
            syncedAtEpochMs = syncedAtEpochMs,
        )
        next.record(ReportSynced(id, remoteUrl, syncedAtEpochMs))
        return next
    }

    fun markFailed(message: String?): FieldReport {
        val next = copyState(
            syncStatus = ReportSyncStatus.Failed,
            errorMessage = message,
        )
        next.record(ReportSyncFailed(id, message, System.currentTimeMillis()))
        return next
    }

    fun isOwnedBy(userId: String): Boolean = ownerUserId == userId

    fun canRetry(): Boolean =
        syncStatus == ReportSyncStatus.Failed ||
            syncStatus == ReportSyncStatus.PendingOffline ||
            syncStatus == ReportSyncStatus.Uploading

    private fun copyState(
        captureMode: ReportCaptureMode = this.captureMode,
        syncStatus: ReportSyncStatus = this.syncStatus,
        remoteUrl: String? = this.remoteUrl,
        errorMessage: String? = this.errorMessage,
        syncedAtEpochMs: Long? = this.syncedAtEpochMs,
    ): FieldReport = FieldReport(
        reportId = reportId,
        ownerUserId = ownerUserId,
        title = title,
        description = description,
        localImagePath = localImagePath,
        mimeType = mimeType,
        captureMode = captureMode,
        syncStatus = syncStatus,
        remoteUrl = remoteUrl,
        errorMessage = errorMessage,
        createdAtEpochMs = createdAtEpochMs,
        syncedAtEpochMs = syncedAtEpochMs,
    )

    override fun equals(other: Any?): Boolean =
        other is FieldReport && other.reportId == reportId

    override fun hashCode(): Int = reportId.hashCode()
}

data class NewFieldReport(
    val title: String,
    val description: String,
    val sourceUri: String,
    val mimeType: String,
)
