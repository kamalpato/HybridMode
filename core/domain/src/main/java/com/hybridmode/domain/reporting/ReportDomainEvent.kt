package com.hybridmode.domain.reporting

import com.hybridmode.domain.shared.DomainEvent

data class ReportQueuedOffline(
    val reportId: String,
    val ownerUserId: String,
    override val occurredAtEpochMs: Long,
) : DomainEvent

data class ReportUploadStarted(
    val reportId: String,
    override val occurredAtEpochMs: Long,
) : DomainEvent

data class ReportSynced(
    val reportId: String,
    val remoteUrl: String,
    override val occurredAtEpochMs: Long,
) : DomainEvent

data class ReportSyncFailed(
    val reportId: String,
    val message: String?,
    override val occurredAtEpochMs: Long,
) : DomainEvent
