package com.hybridmode.domain.application.reporting

import com.hybridmode.common.result.AppResult
import com.hybridmode.domain.reporting.FieldReportRepository
import javax.inject.Inject

class DeleteUploadUseCase @Inject constructor(
    private val fieldReportRepository: FieldReportRepository,
) {
    suspend operator fun invoke(uploadId: String): AppResult<Unit> =
        fieldReportRepository.delete(uploadId)
}
