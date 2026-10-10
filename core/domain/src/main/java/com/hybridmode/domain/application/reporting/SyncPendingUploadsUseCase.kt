package com.hybridmode.domain.application.reporting

import com.hybridmode.common.result.AppResult
import com.hybridmode.domain.reporting.FieldReportRepository
import com.hybridmode.domain.reporting.FieldReportSyncReport
import javax.inject.Inject

class SyncPendingUploadsUseCase @Inject constructor(
    private val fieldReportRepository: FieldReportRepository,
) {
    suspend operator fun invoke(): AppResult<FieldReportSyncReport> =
        fieldReportRepository.syncPending()
}
