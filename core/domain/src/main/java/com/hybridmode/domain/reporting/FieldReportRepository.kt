package com.hybridmode.domain.reporting

import com.hybridmode.common.result.AppResult
import kotlinx.coroutines.flow.Flow

interface FieldReportRepository {
    fun observeReports(): Flow<List<FieldReport>>

    fun observeReport(id: String): Flow<FieldReport?>

    suspend fun submit(input: NewFieldReport): AppResult<FieldReport>

    suspend fun syncPending(): AppResult<FieldReportSyncReport>

    suspend fun retry(reportId: String): AppResult<FieldReport>

    suspend fun delete(reportId: String): AppResult<Unit>
}
