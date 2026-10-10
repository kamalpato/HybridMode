package com.hybridmode.domain.usecase.upload

import com.hybridmode.domain.model.MediaUpload
import com.hybridmode.domain.repository.UploadRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveUploadUseCase @Inject constructor(
    private val uploadRepository: UploadRepository,
) {
    operator fun invoke(id: String): Flow<MediaUpload?> = uploadRepository.observeUpload(id)
}
