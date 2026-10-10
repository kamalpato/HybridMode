package com.hybridmode.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hybridmode.domain.model.MediaUpload
import com.hybridmode.domain.model.UploadSchema
import com.hybridmode.domain.model.UploadSyncStatus

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
    fun toDomain(): MediaUpload = MediaUpload(
        id = id,
        ownerUserId = ownerUserId,
        title = title,
        description = description,
        localFilePath = localFilePath,
        mimeType = mimeType,
        schema = UploadSchema.valueOf(schema),
        syncStatus = UploadSyncStatus.valueOf(syncStatus),
        remoteUrl = remoteUrl,
        errorMessage = errorMessage,
        createdAtEpochMs = createdAtEpochMs,
        syncedAtEpochMs = syncedAtEpochMs,
    )

    companion object {
        fun fromDomain(upload: MediaUpload): UploadEntity = UploadEntity(
            id = upload.id,
            ownerUserId = upload.ownerUserId,
            title = upload.title,
            description = upload.description,
            localFilePath = upload.localFilePath,
            mimeType = upload.mimeType,
            schema = upload.schema.name,
            syncStatus = upload.syncStatus.name,
            remoteUrl = upload.remoteUrl,
            errorMessage = upload.errorMessage,
            createdAtEpochMs = upload.createdAtEpochMs,
            syncedAtEpochMs = upload.syncedAtEpochMs,
        )
    }
}
