package com.hybridmode.domain.auth

import com.hybridmode.domain.shared.DomainEvent

data class UserRegistered(
    val userId: String,
    override val occurredAtEpochMs: Long,
) : DomainEvent

data class UserRenamed(
    val userId: String,
    val displayName: String,
    override val occurredAtEpochMs: Long,
) : DomainEvent

data class UserLoggedIn(
    val userId: String,
    override val occurredAtEpochMs: Long,
) : DomainEvent

data class UserLoggedOut(
    val userId: String?,
    override val occurredAtEpochMs: Long,
) : DomainEvent
