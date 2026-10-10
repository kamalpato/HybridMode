package com.hybridmode.app.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hybridmode.common.connectivity.ConnectivityStatus
import com.hybridmode.domain.auth.AuthSession
import com.hybridmode.domain.application.auth.ObserveSessionUseCase
import com.hybridmode.domain.application.auth.RestoreSessionUseCase
import com.hybridmode.domain.application.connectivity.ObserveConnectivityUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SessionUiState(
    val isBootstrapping: Boolean = true,
    val session: AuthSession? = null,
    val connectivity: ConnectivityStatus = ConnectivityStatus.Offline,
)

@HiltViewModel
class SessionViewModel @Inject constructor(
    observeSessionUseCase: ObserveSessionUseCase,
    observeConnectivityUseCase: ObserveConnectivityUseCase,
    private val restoreSessionUseCase: RestoreSessionUseCase,
) : ViewModel() {

    private val bootstrapping = MutableStateFlow(true)

    val uiState: StateFlow<SessionUiState> = combine(
        bootstrapping,
        observeSessionUseCase(),
        observeConnectivityUseCase(),
    ) { loading, session, connectivity ->
        SessionUiState(
            isBootstrapping = loading,
            session = session,
            connectivity = connectivity,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SessionUiState(),
    )

    init {
        viewModelScope.launch {
            restoreSessionUseCase()
            bootstrapping.value = false
        }
    }
}
