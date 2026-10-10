package com.hybridmode.domain.application.reporting

import com.hybridmode.domain.reporting.FieldReport
import com.hybridmode.domain.reporting.FieldReportRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveUploadUseCase @Inject constructor(
    private val fieldReportRepository: FieldReportRepository,
) {
    operator fun invoke(id: String): Flow<FieldReport?> = fieldReportRepository.observeReport(id)
}
