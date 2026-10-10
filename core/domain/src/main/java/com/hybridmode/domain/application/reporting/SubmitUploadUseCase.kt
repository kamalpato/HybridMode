package com.hybridmode.domain.application.reporting

import com.hybridmode.common.result.AppFailure
import com.hybridmode.common.result.AppResult
import com.hybridmode.domain.reporting.FieldReport
import com.hybridmode.domain.reporting.FieldReportRepository
import com.hybridmode.domain.reporting.NewFieldReport
import com.hybridmode.domain.reporting.value.ReportTitle
import com.hybridmode.domain.shared.DomainEventPublisher
import javax.inject.Inject

class SubmitUploadUseCase @Inject constructor(
    private val fieldReportRepository: FieldReportRepository,
    private val eventPublisher: DomainEventPublisher,
) {
    suspend operator fun invoke(
        title: String,
        description: String,
        sourceUri: String,
        mimeType: String,
    ): AppResult<FieldReport> {
        runCatching { ReportTitle.create(title) }.getOrElse {
            return AppResult.Error(AppFailure.Validation(it.message ?: "Invalid title"))
        }
        if (sourceUri.isBlank()) {
            return AppResult.Error(AppFailure.Validation("Please select an image"))
        }
        val result = fieldReportRepository.submit(
            NewFieldReport(
                title = title.trim(),
                description = description.trim(),
                sourceUri = sourceUri,
                mimeType = mimeType.ifBlank { "image/jpeg" },
            )
        )
        if (result is AppResult.Success) {
            eventPublisher.publish(result.data.pullDomainEvents())
        }
        return result
    }
}
