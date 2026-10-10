package com.hybridmode.domain.application.reporting

import com.hybridmode.common.result.AppResult
import com.hybridmode.domain.reporting.FieldReport
import com.hybridmode.domain.reporting.FieldReportRepository
import com.hybridmode.domain.shared.DomainEventPublisher
import javax.inject.Inject

class RetryUploadUseCase @Inject constructor(
    private val fieldReportRepository: FieldReportRepository,
    private val eventPublisher: DomainEventPublisher,
) {
    suspend operator fun invoke(uploadId: String): AppResult<FieldReport> {
        val result = fieldReportRepository.retry(uploadId)
        if (result is AppResult.Success) {
            eventPublisher.publish(result.data.pullDomainEvents())
        }
        return result
    }
}
