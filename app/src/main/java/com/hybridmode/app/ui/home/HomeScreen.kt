package com.hybridmode.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.hybridmode.app.ui.components.ConnectivityBanner
import com.hybridmode.app.ui.components.MistBackground
import com.hybridmode.app.ui.components.SoftPanel
import com.hybridmode.app.ui.components.StatusChip
import com.hybridmode.app.ui.responsive.ResponsiveContent
import com.hybridmode.app.ui.responsive.rememberAppDimens
import com.hybridmode.app.ui.theme.Copper500
import com.hybridmode.app.ui.theme.Danger
import com.hybridmode.app.ui.theme.Mist100
import com.hybridmode.app.ui.theme.Mist50
import com.hybridmode.app.ui.theme.OnlineGreen
import com.hybridmode.app.ui.theme.Stone400
import com.hybridmode.app.ui.theme.Stone900
import com.hybridmode.app.ui.util.DateFormats
import com.hybridmode.app.ui.util.ImageRequests
import com.hybridmode.common.connectivity.ConnectivityStatus
import com.hybridmode.common.result.AppResult
import com.hybridmode.domain.reporting.FieldReport
import com.hybridmode.domain.reporting.ReportSyncStatus
import com.hybridmode.domain.application.reporting.ObserveUploadsUseCase
import com.hybridmode.domain.application.reporting.SyncPendingUploadsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenCapture: () -> Unit,
    onOpenReport: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    overviewViewModel: HomeOverviewViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val overview by overviewViewModel.overview.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val dimens = rememberAppDimens()
    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearMessage()
    }
    LaunchedEffect(overview.syncMessage) {
        val message = overview.syncMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        overviewViewModel.clearSyncMessage()
    }

    MistBackground {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "HybridMode",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = Stone900,
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                    ),
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                ConnectivityBanner(
                    status = state.connectivity,
                    offlineSessionActive = state.session != null,
                )

                ResponsiveContent {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(dimens.screenPadding),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        val user = state.profile ?: state.session?.user
                        val online = state.connectivity == ConnectivityStatus.Online

                        SoftPanel {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "Hi, ${user?.displayName?.value ?: "there"}",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                        ),
                                        color = Stone900,
                                    )
                                    Text(
                                        text = user?.email?.value.orEmpty(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Stone400,
                                    )
                                }
                                StatusChip(
                                    text = if (online) "Online" else "Offline",
                                    color = if (online) OnlineGreen else Copper500,
                                )
                            }
                            HorizontalDivider(color = Mist100, thickness = 0.5.dp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                            ) {
                                StatBlock("${overview.total}", "Total")
                                StatBlock("${overview.synced}", "Synced")
                                StatBlock("${overview.waiting}", "Pending")
                            }
                        }

                        if (overview.waiting > 0) {
                            SoftPanel {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "${overview.waiting} waiting to sync",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                            ),
                                            color = Stone900,
                                        )
                                        Text(
                                            if (online) "Ready to upload" else "Connect to sync",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Stone400,
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = overviewViewModel::syncQueued,
                                        enabled = !overview.isSyncing && online,
                                    ) {
                                        Text(if (overview.isSyncing) "…" else "Sync")
                                    }
                                }
                            }
                        }

                        SoftPanel {
                            Text(
                                "Recent",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = Stone900,
                            )
                            if (overview.recent.isEmpty()) {
                                Text(
                                    "No reports yet. Tap + to create one.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Stone400,
                                )
                            } else {
                                overview.recent.forEachIndexed { index, report ->
                                    if (index > 0) {
                                        HorizontalDivider(color = Mist100, thickness = 0.5.dp)
                                    }
                                    RecentReportRow(
                                        report = report,
                                        onClick = { onOpenReport(report.id) },
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(72.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentReportRow(
    report: FieldReport,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val statusColor = when (report.syncStatus) {
        ReportSyncStatus.Synced -> OnlineGreen
        ReportSyncStatus.Failed -> Danger
        ReportSyncStatus.Uploading -> MaterialTheme.colorScheme.primary
        ReportSyncStatus.PendingOffline -> Copper500
    }
    val statusLabel = when (report.syncStatus) {
        ReportSyncStatus.Synced -> "Synced"
        ReportSyncStatus.PendingOffline -> "Pending"
        ReportSyncStatus.Failed -> "Failed"
        ReportSyncStatus.Uploading -> "Uploading"
    }
    val interaction = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true),
                onClick = onClick,
            )
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = ImageRequests.thumbnail(context, report.localImagePath.value),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Mist50),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                report.title.value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                color = Stone900,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                DateFormats.dateTime(report.createdAtEpochMs),
                style = MaterialTheme.typography.labelMedium,
                color = Stone400,
            )
        }
        StatusChip(text = statusLabel, color = statusColor)
    }
}

@Composable
private fun StatBlock(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            color = Stone900,
        )
        Text(label, style = MaterialTheme.typography.labelMedium, color = Stone400)
    }
}

data class HomeOverview(
    val total: Int = 0,
    val synced: Int = 0,
    val waiting: Int = 0,
    val recent: List<FieldReport> = emptyList(),
    val isSyncing: Boolean = false,
    val syncMessage: String? = null,
)

@HiltViewModel
class HomeOverviewViewModel @Inject constructor(
    observeUploadsUseCase: ObserveUploadsUseCase,
    private val syncPendingUploadsUseCase: SyncPendingUploadsUseCase,
) : ViewModel() {

    private val ephemeral = MutableStateFlow(OverviewEphemeral())

    val overview: StateFlow<HomeOverview> = combine(
        observeUploadsUseCase(),
        ephemeral,
    ) { list, epi ->
        HomeOverview(
            total = list.size,
            synced = list.count { it.syncStatus == ReportSyncStatus.Synced },
            waiting = list.count {
                it.syncStatus == ReportSyncStatus.PendingOffline ||
                    it.syncStatus == ReportSyncStatus.Failed
            },
            recent = list.take(RECENT_SUBMISSION_LIMIT),
            isSyncing = epi.isSyncing,
            syncMessage = epi.syncMessage,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeOverview())

    fun syncQueued() {
        viewModelScope.launch {
            ephemeral.update { it.copy(isSyncing = true, syncMessage = null) }
            when (val result = syncPendingUploadsUseCase()) {
                is AppResult.Success -> {
                    ephemeral.update {
                        it.copy(
                            isSyncing = false,
                            syncMessage = result.data.userMessage(),
                        )
                    }
                }
                is AppResult.Error -> ephemeral.update {
                    it.copy(isSyncing = false, syncMessage = result.failure.message)
                }
            }
        }
    }

    fun clearSyncMessage() {
        ephemeral.update { it.copy(syncMessage = null) }
    }
}

private data class OverviewEphemeral(
    val isSyncing: Boolean = false,
    val syncMessage: String? = null,
)

private const val RECENT_SUBMISSION_LIMIT = 5
