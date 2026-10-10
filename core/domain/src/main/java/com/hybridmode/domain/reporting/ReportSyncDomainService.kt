package com.hybridmode.domain.reporting

/**
 * Domain service: rules that span connectivity + report aggregate state.
 */
object ReportSyncDomainService {
    fun canAttemptRemoteSync(online: Boolean, report: FieldReport): Boolean =
        online && (report.canRetry() || report.syncStatus == ReportSyncStatus.Uploading)
}
