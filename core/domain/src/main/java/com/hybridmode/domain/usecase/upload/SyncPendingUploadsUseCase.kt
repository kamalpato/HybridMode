package com.hybridmode.domain.usecase.upload

import com.hybridmode.common.result.AppResult
import com.hybridmode.domain.model.UploadSyncReport
import com.hybridmode.domain.repository.UploadRepository
import javax.inject.Inject

class SyncPendingUploadsUseCase @Inject constructor(
    private val uploadRepository: UploadRepository,
) {
    suspend operator fun invoke(): AppResult<UploadSyncReport> = uploadRepository.syncPending()
}
