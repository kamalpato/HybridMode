package com.hybridmode.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hybridmode.common.result.AppResult
import com.hybridmode.domain.usecase.auth.LoginUseCase
import com.hybridmode.domain.usecase.auth.RegisterUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthFormState(
    val email: String = "",
    val password: String = "",
    val displayName: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
) : ViewModel() {

    private val _loginState = MutableStateFlow(
        AuthFormState(
            email = "demo@hybridmode.app",
            password = "password123",
        )
    )
    val loginState: StateFlow<AuthFormState> = _loginState.asStateFlow()

    private val _registerState = MutableStateFlow(AuthFormState())
    val registerState: StateFlow<AuthFormState> = _registerState.asStateFlow()

    fun onLoginEmailChange(value: String) {
        _loginState.update { it.copy(email = value, errorMessage = null) }
    }

    fun onLoginPasswordChange(value: String) {
        _loginState.update { it.copy(password = value, errorMessage = null) }
    }

    fun onRegisterEmailChange(value: String) {
        _registerState.update { it.copy(email = value, errorMessage = null) }
    }

    fun onRegisterPasswordChange(value: String) {
        _registerState.update { it.copy(password = value, errorMessage = null) }
    }

    fun onRegisterDisplayNameChange(value: String) {
        _registerState.update { it.copy(displayName = value, errorMessage = null) }
    }

    fun login() {
        val state = _loginState.value
        viewModelScope.launch {
            _loginState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = loginUseCase(state.email, state.password)) {
                is AppResult.Success -> _loginState.update {
                    it.copy(isLoading = false, isSuccess = true)
                }
                is AppResult.Error -> _loginState.update {
                    it.copy(isLoading = false, errorMessage = result.failure.message)
                }
            }
        }
    }

    fun register() {
        val state = _registerState.value
        viewModelScope.launch {
            _registerState.update { it.copy(isLoading = true, errorMessage = null) }
            when (
                val result = registerUseCase(
                    email = state.email,
                    password = state.password,
                    displayName = state.displayName,
                )
            ) {
                is AppResult.Success -> _registerState.update {
                    it.copy(isLoading = false, isSuccess = true)
                }
                is AppResult.Error -> _registerState.update {
                    it.copy(isLoading = false, errorMessage = result.failure.message)
                }
            }
        }
    }
}
