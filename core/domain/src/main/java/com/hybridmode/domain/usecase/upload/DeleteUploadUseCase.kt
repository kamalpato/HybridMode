package com.hybridmode.domain.usecase.upload

import com.hybridmode.common.result.AppResult
import com.hybridmode.domain.repository.UploadRepository
import javax.inject.Inject

class DeleteUploadUseCase @Inject constructor(
    private val uploadRepository: UploadRepository,
) {
    suspend operator fun invoke(uploadId: String): AppResult<Unit> =
        uploadRepository.delete(uploadId)
}
