package com.hybridmode.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hybridmode.domain.auth.User

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String,
    val displayName: String,
    val createdAtEpochMs: Long,
    val cachedAtEpochMs: Long,
) {
    fun toDomain(): User = User.rehydrate(
        id = id,
        email = email,
        displayName = displayName,
        createdAtEpochMs = createdAtEpochMs,
    )

    companion object {
        fun fromDomain(user: User, cachedAtEpochMs: Long): UserEntity = UserEntity(
            id = user.id,
            email = user.email.value,
            displayName = user.displayName.value,
            createdAtEpochMs = user.createdAtEpochMs,
            cachedAtEpochMs = cachedAtEpochMs,
        )
    }
}
