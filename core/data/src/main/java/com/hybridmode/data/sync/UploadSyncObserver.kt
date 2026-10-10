package com.hybridmode.data.sync

import com.hybridmode.common.connectivity.ConnectivityStatus
import com.hybridmode.common.result.AppResult
import com.hybridmode.domain.connectivity.ConnectivityRepository
import com.hybridmode.domain.reporting.FieldReportRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UploadSyncObserver @Inject constructor(
    private val connectivityRepository: ConnectivityRepository,
    private val fieldReportRepository: FieldReportRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var started = false

    fun start() {
        if (started) return
        started = true
        scope.launch {
            connectivityRepository.status
                .distinctUntilChanged()
                .filter { it == ConnectivityStatus.Online }
                .collect {
                    when (val result = fieldReportRepository.syncPending()) {
                        is AppResult.Success -> Timber.i(
                            "Auto-sync: attempted=%s synced=%s failed=%s",
                            result.data.pendingAttempted,
                            result.data.synced,
                            result.data.failed,
                        )
                        is AppResult.Error -> Timber.w(
                            "Auto-sync skipped/failed: %s",
                            result.failure.message,
                        )
                    }
                }
        }
    }
}
