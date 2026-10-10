package com.hybridmode.domain.usecase.upload

import com.hybridmode.common.result.AppFailure
import com.hybridmode.common.result.AppResult
import com.hybridmode.domain.model.MediaUpload
import com.hybridmode.domain.model.NewMediaUpload
import com.hybridmode.domain.repository.UploadRepository
import javax.inject.Inject

class SubmitUploadUseCase @Inject constructor(
    private val uploadRepository: UploadRepository,
) {
    suspend operator fun invoke(
        title: String,
        description: String,
        sourceUri: String,
        mimeType: String,
    ): AppResult<MediaUpload> {
        val cleanTitle = title.trim()
        if (cleanTitle.length < 2) {
            return AppResult.Error(AppFailure.Validation("Title must be at least 2 characters"))
        }
        if (sourceUri.isBlank()) {
            return AppResult.Error(AppFailure.Validation("Please select an image"))
        }
        return uploadRepository.submit(
            NewMediaUpload(
                title = cleanTitle,
                description = description.trim(),
                sourceUri = sourceUri,
                mimeType = mimeType.ifBlank { "image/jpeg" },
            )
        )
    }
}
