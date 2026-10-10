package com.hybridmode.domain.reporting

enum class ReportSyncStatus {
    PendingOffline,
    Uploading,
    Synced,
    Failed,
}

enum class ReportCaptureMode {
    Online,
    Offline,
}
