package com.hybridmode.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hybridmode.domain.model.User

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String,
    val displayName: String,
    val createdAtEpochMs: Long,
    val cachedAtEpochMs: Long,
) {
    fun toDomain(): User = User(
        id = id,
        email = email,
        displayName = displayName,
        createdAtEpochMs = createdAtEpochMs,
    )

    companion object {
        fun fromDomain(user: User, cachedAtEpochMs: Long): UserEntity = UserEntity(
            id = user.id,
            email = user.email,
            displayName = user.displayName,
            createdAtEpochMs = user.createdAtEpochMs,
            cachedAtEpochMs = cachedAtEpochMs,
        )
    }
}
