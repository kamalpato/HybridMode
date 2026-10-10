package com.hybridmode.domain.application.profile

import com.hybridmode.common.result.AppResult
import com.hybridmode.domain.auth.User
import com.hybridmode.domain.profile.ProfileRepository
import javax.inject.Inject

class SyncProfileUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
) {
    suspend operator fun invoke(): AppResult<User> = profileRepository.syncProfile()
}
