package com.hybridmode.data.sync

import com.hybridmode.common.connectivity.ConnectivityStatus
import com.hybridmode.common.result.AppResult
import com.hybridmode.domain.repository.ConnectivityRepository
import com.hybridmode.domain.repository.UploadRepository
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
    private val uploadRepository: UploadRepository,
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
                    when (val result = uploadRepository.syncPending()) {
                        is AppResult.Success -> {
                            if (result.data.synced > 0) {
                                Timber.i(
                                    "Auto-synced %d pending upload(s)",
                                    result.data.synced,
                                )
                            }
                        }
                        is AppResult.Error -> {
                            Timber.w("Auto-sync skipped: %s", result.failure.message)
                        }
                    }
                }
        }
    }
}
