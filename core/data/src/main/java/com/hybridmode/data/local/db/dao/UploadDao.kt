package com.hybridmode.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.hybridmode.data.local.db.entity.UploadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UploadDao {
    @Query(
        """
        SELECT * FROM uploads
        WHERE ownerUserId = :ownerUserId
        ORDER BY createdAtEpochMs DESC
        """
    )
    fun observeByOwner(ownerUserId: String): Flow<List<UploadEntity>>

    @Query("SELECT * FROM uploads WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): UploadEntity?

    @Query("SELECT * FROM uploads WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<UploadEntity?>

    @Query(
        """
        SELECT * FROM uploads
        WHERE ownerUserId = :ownerUserId
          AND syncStatus IN ('PendingOffline', 'Failed', 'Uploading')
        ORDER BY createdAtEpochMs ASC
        """
    )
    suspend fun getPendingOrFailed(ownerUserId: String): List<UploadEntity>

    @Query(
        """
        SELECT COUNT(*) FROM uploads
        WHERE ownerUserId = :ownerUserId
          AND syncStatus = 'Synced'
        """
    )
    suspend fun countSynced(ownerUserId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: UploadEntity)

    @Update
    suspend fun update(entity: UploadEntity)

    @Query("DELETE FROM uploads WHERE id = :id")
    suspend fun deleteById(id: String)
}
