package com.hybridmode.domain.repository

import com.hybridmode.common.connectivity.ConnectivityStatus
import kotlinx.coroutines.flow.Flow

interface ConnectivityRepository {
    val status: Flow<ConnectivityStatus>

    fun currentStatus(): ConnectivityStatus

    fun isOnline(): Boolean = currentStatus() == ConnectivityStatus.Online
}
