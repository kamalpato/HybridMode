package com.hybridmode.domain.usecase.profile

import com.hybridmode.common.result.AppResult
import com.hybridmode.domain.model.User
import com.hybridmode.domain.repository.ProfileRepository
import javax.inject.Inject

class SyncProfileUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
) {
    suspend operator fun invoke(): AppResult<User> = profileRepository.syncProfile()
}
