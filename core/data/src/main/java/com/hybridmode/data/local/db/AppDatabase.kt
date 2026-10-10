package com.hybridmode.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.hybridmode.data.local.db.dao.UploadDao
import com.hybridmode.data.local.db.dao.UserDao
import com.hybridmode.data.local.db.entity.UploadEntity
import com.hybridmode.data.local.db.entity.UserEntity

@Database(
    entities = [UserEntity::class, UploadEntity::class],
    version = 3,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun uploadDao(): UploadDao
}
