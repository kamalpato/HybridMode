package com.hybridmode.app.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import coil.compose.AsyncImage
import com.hybridmode.app.ui.components.MistBackground
import com.hybridmode.app.ui.components.SoftPanel
import com.hybridmode.app.ui.components.StatusChip
import com.hybridmode.app.ui.responsive.ResponsiveContent
import com.hybridmode.app.ui.responsive.rememberAppDimens
import com.hybridmode.app.ui.theme.Copper500
import com.hybridmode.app.ui.theme.Mist100
import com.hybridmode.app.ui.theme.Mist50
import com.hybridmode.app.ui.theme.OnlineGreen
import com.hybridmode.app.ui.theme.Stone400
import com.hybridmode.app.ui.theme.Stone600
import com.hybridmode.app.ui.theme.Stone900
import com.hybridmode.app.ui.util.DateFormats
import com.hybridmode.app.ui.util.ImageRequests
import com.hybridmode.common.connectivity.ConnectivityStatus
import com.hybridmode.common.result.AppResult
import com.hybridmode.domain.reporting.FieldReport
import com.hybridmode.domain.reporting.ReportCaptureMode
import com.hybridmode.domain.reporting.ReportSyncStatus
import com.hybridmode.domain.application.connectivity.ObserveConnectivityUseCase
import com.hybridmode.domain.application.reporting.DeleteUploadUseCase
import com.hybridmode.domain.application.reporting.ObserveUploadUseCase
import com.hybridmode.domain.application.reporting.RetryUploadUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoDetailScreen(
    photoId: String,
    onBack: () -> Unit,
    viewModel: PhotoDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(photoId) { viewModel.bind(photoId) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val dimens = rememberAppDimens()
    val density = LocalDensity.current
    val screenWidthPx = with(density) {
        LocalConfiguration.current.screenWidthDp.dp.roundToPx()
    }
    val heroHeightPx = with(density) { dimens.heroImageHeight.roundToPx() }

    LaunchedEffect(state.message) {
        val msg = state.message ?: return@LaunchedEffect
        snackbar.showSnackbar(msg)
        viewModel.clearMessage()
    }

    MistBackground {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "Report details",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = Stone900,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                    ),
                )
            },
            snackbarHost = { SnackbarHost(snackbar) },
        ) { padding ->
            val upload = state.upload
            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
                return@Scaffold
            }
            if (upload == null) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(dimens.screenPadding),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        "This report is no longer available.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Button(
                        onClick = onBack,
                        modifier = Modifier.heightIn(min = dimens.buttonMinHeight),
                    ) {
                        Text("Back to reports")
                    }
                }
                return@Scaffold
            }

            ResponsiveContent {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(dimens.screenPadding),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    AsyncImage(
                        model = ImageRequests.file(
                            context = context,
                            path = upload.localImagePath.value,
                            widthPx = screenWidthPx,
                            heightPx = heroHeightPx,
                        ),
                        contentDescription = "Report photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(dimens.heroImageHeight)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Mist50),
                    )

                    SoftPanel {
                        Text(
                            upload.title.value,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = Stone900,
                        )
                        Text(
                            text = "ID ${upload.id.take(8).uppercase()}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Stone400,
                        )
                    }

                    if (upload.description.value.isNotBlank()) {
                        SoftPanel {
                            Text(
                                "Description",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = Stone900,
                            )
                            HorizontalDivider(color = Mist100, thickness = 0.5.dp)
                            Text(
                                upload.description.value,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Stone600,
                            )
                        }
                    }

                    SoftPanel {
                        val chipColor = when (upload.syncStatus) {
                            ReportSyncStatus.Synced -> OnlineGreen
                            ReportSyncStatus.Failed -> MaterialTheme.colorScheme.error
                            else -> Copper500
                        }
                        StatusChip(text = detailStatus(upload), color = chipColor)
                        HorizontalDivider(color = Mist100, thickness = 0.5.dp)
                        DetailMeta("Created", DateFormats.dateTime(upload.createdAtEpochMs))
                        DetailMeta(
                            "Mode",
                            when (upload.captureMode) {
                                ReportCaptureMode.Online -> "Online"
                                ReportCaptureMode.Offline -> "Offline"
                            },
                        )
                        DetailMeta(
                            "Synced",
                            upload.syncedAtEpochMs?.let { DateFormats.dateTime(it) } ?: "Pending",
                        )
                        upload.remoteUrl?.takeIf { it.isNotBlank() }?.let { url ->
                            DetailMeta("Server URL", url)
                        }
                        upload.errorMessage?.takeIf { it.isNotBlank() }?.let { note ->
                            DetailMeta("Note", note)
                        }
                    }

                    if (state.online && upload.canRetry()) {
                        Button(
                            onClick = viewModel::sendNow,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = dimens.buttonMinHeight),
                        ) {
                            Text("Upload now")
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.remove { onBack() } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = dimens.buttonMinHeight),
                    ) {
                        Text("Delete")
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun DetailMeta(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Stone400,
            modifier = Modifier.width(96.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = Stone900,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun detailStatus(upload: FieldReport): String = when (upload.syncStatus) {
    ReportSyncStatus.Synced -> "Synced"
    ReportSyncStatus.PendingOffline -> "Pending"
    ReportSyncStatus.Failed -> "Failed"
    ReportSyncStatus.Uploading -> "Uploading"
}

data class PhotoDetailUiState(
    val upload: FieldReport? = null,
    val isLoading: Boolean = true,
    val online: Boolean = false,
    val message: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PhotoDetailViewModel @Inject constructor(
    private val observeUploadUseCase: ObserveUploadUseCase,
    observeConnectivityUseCase: ObserveConnectivityUseCase,
    private val retryUploadUseCase: RetryUploadUseCase,
    private val deleteUploadUseCase: DeleteUploadUseCase,
) : ViewModel() {

    private val photoId = MutableStateFlow("")
    private val message = MutableStateFlow<String?>(null)
    private val resolved = MutableStateFlow(false)

    private val uploadFlow = photoId.flatMapLatest { id ->
        resolved.value = false
        if (id.isBlank()) {
            flowOf(null)
        } else {
            observeUploadUseCase(id).map { upload ->
                resolved.value = true
                upload
            }
        }
    }

    val uiState: StateFlow<PhotoDetailUiState> = combine(
        uploadFlow,
        observeConnectivityUseCase(),
        message,
        resolved,
    ) { upload, connectivity, msg, ready ->
        PhotoDetailUiState(
            upload = upload,
            isLoading = photoId.value.isNotBlank() && !ready,
            online = connectivity == ConnectivityStatus.Online,
            message = msg,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        PhotoDetailUiState(isLoading = true),
    )

    fun bind(id: String) {
        resolved.value = false
        photoId.value = id
    }

    fun sendNow() {
        val id = photoId.value
        viewModelScope.launch {
            when (val result = retryUploadUseCase(id)) {
                is AppResult.Success -> message.value = "Report uploaded successfully."
                is AppResult.Error -> message.value = result.failure.message
            }
        }
    }

    fun remove(onDone: () -> Unit) {
        val id = photoId.value
        viewModelScope.launch {
            when (val result = deleteUploadUseCase(id)) {
                is AppResult.Success -> onDone()
                is AppResult.Error -> message.value = result.failure.message
            }
        }
    }

    fun clearMessage() {
        message.value = null
    }
}
