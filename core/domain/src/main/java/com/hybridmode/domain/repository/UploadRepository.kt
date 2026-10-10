package com.hybridmode.domain.repository

import com.hybridmode.common.result.AppResult
import com.hybridmode.domain.model.MediaUpload
import com.hybridmode.domain.model.NewMediaUpload
import com.hybridmode.domain.model.UploadSyncReport
import kotlinx.coroutines.flow.Flow

interface UploadRepository {
    fun observeUploads(): Flow<List<MediaUpload>>

    fun observeUpload(id: String): Flow<MediaUpload?>

    suspend fun submit(input: NewMediaUpload): AppResult<MediaUpload>

    suspend fun syncPending(): AppResult<UploadSyncReport>

    suspend fun retry(uploadId: String): AppResult<MediaUpload>

    suspend fun delete(uploadId: String): AppResult<Unit>
}
