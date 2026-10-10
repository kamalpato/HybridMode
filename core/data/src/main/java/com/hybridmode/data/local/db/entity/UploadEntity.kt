package com.hybridmode.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hybridmode.domain.reporting.FieldReport
import com.hybridmode.domain.reporting.ReportCaptureMode
import com.hybridmode.domain.reporting.ReportSyncStatus

@Entity(tableName = "uploads")
data class UploadEntity(
    @PrimaryKey val id: String,
    val ownerUserId: String,
    val title: String,
    val description: String,
    val localFilePath: String,
    val mimeType: String,
    val schema: String,
    val syncStatus: String,
    val remoteUrl: String?,
    val errorMessage: String?,
    val createdAtEpochMs: Long,
    val syncedAtEpochMs: Long?,
) {
    fun toDomain(): FieldReport = FieldReport.rehydrate(
        id = id,
        ownerUserId = ownerUserId,
        title = title,
        description = description,
        localFilePath = localFilePath,
        mimeType = mimeType,
        captureMode = ReportCaptureMode.valueOf(schema),
        syncStatus = ReportSyncStatus.valueOf(syncStatus),
        remoteUrl = remoteUrl,
        errorMessage = errorMessage,
        createdAtEpochMs = createdAtEpochMs,
        syncedAtEpochMs = syncedAtEpochMs,
    )

    companion object {
        fun fromDomain(report: FieldReport): UploadEntity = UploadEntity(
            id = report.id,
            ownerUserId = report.ownerUserId,
            title = report.title.value,
            description = report.description.value,
            localFilePath = report.localImagePath.value,
            mimeType = report.mimeType.value,
            schema = report.captureMode.name,
            syncStatus = report.syncStatus.name,
            remoteUrl = report.remoteUrl,
            errorMessage = report.errorMessage,
            createdAtEpochMs = report.createdAtEpochMs,
            syncedAtEpochMs = report.syncedAtEpochMs,
        )
    }
}
