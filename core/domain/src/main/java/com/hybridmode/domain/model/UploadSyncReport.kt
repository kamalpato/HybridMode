package com.hybridmode.domain.model

data class UploadSyncReport(
    val pendingAttempted: Int,
    val synced: Int,
    val failed: Int,
    val alreadySyncedTotal: Int,
) {
    fun userMessage(): String = when {
        pendingAttempted == 0 && alreadySyncedTotal > 0 ->
            "Everything is already synced."
        pendingAttempted == 0 ->
            "Nothing waiting to sync."
        synced > 0 && failed == 0 ->
            "Uploaded $synced report(s)."
        synced > 0 && failed > 0 ->
            "Uploaded $synced report(s). $failed still need attention."
        else ->
            "Could not upload $failed report(s). Check your connection and try again."
    }
}
