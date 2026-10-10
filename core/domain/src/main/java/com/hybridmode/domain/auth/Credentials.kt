package com.hybridmode.domain.auth

import com.hybridmode.domain.auth.value.DisplayName
import com.hybridmode.domain.auth.value.Email
import com.hybridmode.domain.shared.ValueObject

data class LoginCredentials(
    val email: Email,
    val password: String,
) : ValueObject {
    init {
        require(password.length >= 8) { "Password must be at least 8 characters" }
    }

    companion object {
        fun of(email: String, password: String) =
            LoginCredentials(Email.create(email), password)
    }
}

data class RegisterCredentials(
    val email: Email,
    val password: String,
    val displayName: DisplayName,
) : ValueObject {
    init {
        require(password.length >= 8) { "Password must be at least 8 characters" }
    }

    companion object {
        fun of(email: String, password: String, displayName: String) =
            RegisterCredentials(Email.create(email), password, DisplayName.create(displayName))
    }
}
