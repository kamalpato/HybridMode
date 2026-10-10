package com.hybridmode.domain.usecase.connectivity

import com.hybridmode.common.connectivity.ConnectivityStatus
import com.hybridmode.domain.repository.ConnectivityRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveConnectivityUseCase @Inject constructor(
    private val connectivityRepository: ConnectivityRepository,
) {
    operator fun invoke(): Flow<ConnectivityStatus> = connectivityRepository.status
}
