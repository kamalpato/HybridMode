package com.hybridmode.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hybridmode.common.connectivity.ConnectivityStatus
import com.hybridmode.domain.auth.AuthSession
import com.hybridmode.domain.auth.User
import com.hybridmode.domain.application.auth.LogoutUseCase
import com.hybridmode.domain.application.auth.ObserveSessionUseCase
import com.hybridmode.domain.application.connectivity.ObserveConnectivityUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val session: AuthSession? = null,
    val connectivity: ConnectivityStatus = ConnectivityStatus.Offline,
    val profile: User? = null,
    val message: String? = null,
    val isBusy: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeSessionUseCase: ObserveSessionUseCase,
    observeConnectivityUseCase: ObserveConnectivityUseCase,
    private val logoutUseCase: LogoutUseCase,
) : ViewModel() {

    private val ephemeral = MutableStateFlow(HomeEphemeral())

    val uiState: StateFlow<HomeUiState> = combine(
        observeSessionUseCase(),
        observeConnectivityUseCase(),
        ephemeral,
    ) { session, connectivity, epi ->
        HomeUiState(
            session = session,
            connectivity = connectivity,
            profile = epi.profile ?: session?.user,
            message = epi.message,
            isBusy = epi.isBusy,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    fun logout() {
        viewModelScope.launch {
            ephemeral.update { it.copy(isBusy = true) }
            logoutUseCase()
            ephemeral.update { it.copy(isBusy = false) }
        }
    }

    fun clearMessage() {
        ephemeral.update { it.copy(message = null) }
    }

    private data class HomeEphemeral(
        val profile: User? = null,
        val message: String? = null,
        val isBusy: Boolean = false,
    )
}
