package com.hybridmode.domain.auth.value

import com.hybridmode.domain.shared.ValueObject

@JvmInline
value class Email private constructor(val value: String) : ValueObject {
    companion object {
        fun create(raw: String): Email {
            val normalized = raw.trim().lowercase()
            require(normalized.length >= 5 && normalized.contains("@")) { "Invalid email" }
            return Email(normalized)
        }

        fun rehydrate(raw: String): Email = Email(raw.trim().lowercase())
    }

    override fun toString(): String = value
}

@JvmInline
value class UserId private constructor(val value: String) : ValueObject {
    companion object {
        fun create(raw: String): UserId {
            require(raw.isNotBlank()) { "User id required" }
            return UserId(raw)
        }

        fun rehydrate(raw: String): UserId = UserId(raw)
    }

    override fun toString(): String = value
}

@JvmInline
value class DisplayName private constructor(val value: String) : ValueObject {
    companion object {
        fun create(raw: String): DisplayName {
            val cleaned = raw.trim()
            require(cleaned.length >= 2) { "Display name too short" }
            return DisplayName(cleaned)
        }

        fun rehydrate(raw: String): DisplayName = DisplayName(raw)
    }

    override fun toString(): String = value
}
