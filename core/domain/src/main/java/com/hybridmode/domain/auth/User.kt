package com.hybridmode.domain.auth

import com.hybridmode.domain.auth.value.DisplayName
import com.hybridmode.domain.auth.value.Email
import com.hybridmode.domain.auth.value.UserId
import com.hybridmode.domain.shared.AggregateRoot

class User private constructor(
    val userId: UserId,
    val email: Email,
    val displayName: DisplayName,
    val createdAtEpochMs: Long,
) : AggregateRoot() {

    val id: String get() = userId.value

    companion object {
        fun register(
            id: String,
            email: String,
            displayName: String,
            createdAtEpochMs: Long,
        ): User {
            val user = User(
                userId = UserId.create(id),
                email = Email.create(email),
                displayName = DisplayName.create(displayName),
                createdAtEpochMs = createdAtEpochMs,
            )
            user.record(UserRegistered(user.id, createdAtEpochMs))
            return user
        }

        fun rehydrate(
            id: String,
            email: String,
            displayName: String,
            createdAtEpochMs: Long,
        ): User = User(
            userId = UserId.rehydrate(id),
            email = Email.rehydrate(email),
            displayName = DisplayName.rehydrate(displayName),
            createdAtEpochMs = createdAtEpochMs,
        )
    }

    fun rename(displayName: String): User {
        val next = User(userId, email, DisplayName.create(displayName), createdAtEpochMs)
        next.record(UserRenamed(id, next.displayName.value, System.currentTimeMillis()))
        return next
    }

    override fun equals(other: Any?): Boolean =
        other is User && other.userId == userId

    override fun hashCode(): Int = userId.hashCode()
}
