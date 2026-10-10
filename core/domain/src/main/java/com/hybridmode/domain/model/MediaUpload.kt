package com.hybridmode.domain.model

enum class UploadSyncStatus {
    PendingOffline,
    Uploading,
    Synced,
    Failed,
}

enum class UploadSchema {
    Online,
    Offline,
}

data class MediaUpload(
    val id: String,
    val ownerUserId: String,
    val title: String,
    val description: String,
    val localFilePath: String,
    val mimeType: String,
    val schema: UploadSchema,
    val syncStatus: UploadSyncStatus,
    val remoteUrl: String?,
    val errorMessage: String?,
    val createdAtEpochMs: Long,
    val syncedAtEpochMs: Long?,
)

data class NewMediaUpload(
    val title: String,
    val description: String,
    val sourceUri: String,
    val mimeType: String,
)
