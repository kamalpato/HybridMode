package com.hybridmode.app.ui.upload

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import coil.compose.AsyncImage
import com.hybridmode.app.ui.components.ConnectivityBanner
import com.hybridmode.app.ui.components.MistBackground
import com.hybridmode.app.ui.components.SoftPanel
import com.hybridmode.app.ui.responsive.ResponsiveContent
import com.hybridmode.app.ui.responsive.rememberAppDimens
import com.hybridmode.app.ui.theme.Mist50
import com.hybridmode.app.ui.theme.Stone400
import com.hybridmode.app.ui.theme.Stone600
import com.hybridmode.app.ui.theme.Stone900
import com.hybridmode.app.ui.util.ImageRequests
import com.hybridmode.domain.reporting.ReportCaptureMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(
    onBack: () -> Unit,
    onFinished: () -> Unit = {},
    viewModel: UploadViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val dimens = rememberAppDimens()
    val density = LocalDensity.current
    val previewWidthPx = with(density) {
        LocalConfiguration.current.screenWidthDp.dp.roundToPx()
    }
    val previewHeightPx = with(density) { dimens.heroImageHeight.roundToPx() }
    val canSubmit = !state.isSubmitting &&
        state.form.title.trim().length >= 2 &&
        state.form.imageUri != null

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri != null) {
            val mime = context.contentResolver.getType(uri)
            viewModel.onImagePicked(uri.toString(), mime)
        }
    }

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
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
                            text = "New report",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = Stone900,
                        )
                    },
                    navigationIcon = {
                        TextButton(onClick = onBack) { Text("Cancel") }
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
                    offlineSessionActive = true,
                )

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    ResponsiveContent {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .imePadding()
                                .verticalScroll(rememberScrollState())
                                .padding(dimens.screenPadding),
                            verticalArrangement = Arrangement.spacedBy(dimens.sectionSpacing),
                        ) {
                            SoftPanel {
                                Text(
                                    text = "New report",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                    color = Stone900,
                                )
                                Text(
                                    text = if (state.activeSchema == ReportCaptureMode.Online) {
                                        "You are online. This report will upload now."
                                    } else {
                                        "You are offline. This report will be saved and synced later."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Stone600,
                                )
                            }

                            SoftPanel {
                                Text(
                                    "1. Photo",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                    color = Stone900,
                                )
                                val previewUri = state.form.imageUri
                                if (previewUri != null) {
                                    AsyncImage(
                                        model = ImageRequests.uri(
                                            context = context,
                                            uri = previewUri,
                                            widthPx = previewWidthPx,
                                            heightPx = previewHeightPx,
                                        ),
                                        contentDescription = "Selected photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(dimens.heroImageHeight)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Mist50),
                                    )
                                    Row {
                                        TextButton(onClick = viewModel::clearImage) {
                                            Text("Remove")
                                        }
                                        TextButton(
                                            onClick = {
                                                picker.launch(
                                                    PickVisualMediaRequest(
                                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                                    )
                                                )
                                            },
                                        ) {
                                            Text("Change image")
                                        }
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(132.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Mist50),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Image,
                                                contentDescription = null,
                                                tint = Stone400,
                                                modifier = Modifier.size(28.dp),
                                            )
                                            Text(
                                                "No photo selected",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Stone400,
                                            )
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            picker.launch(
                                                PickVisualMediaRequest(
                                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                                )
                                            )
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = dimens.buttonMinHeight),
                                    ) {
                                        Text("Choose from gallery")
                                    }
                                }
                            }

                            SoftPanel {
                                Text(
                                    "2. Details",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                    color = Stone900,
                                )
                                OutlinedTextField(
                                    value = state.form.title,
                                    onValueChange = viewModel::onTitleChange,
                                    label = { Text("Title") },
                                    placeholder = { Text("e.g. Site visit - Block A") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                OutlinedTextField(
                                    value = state.form.description,
                                    onValueChange = viewModel::onDescriptionChange,
                                    label = { Text("Description") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp),
                                )
                            }

                            SoftPanel {
                                Text(
                                    "3. Submit",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                    color = Stone900,
                                )
                                Button(
                                    onClick = { viewModel.submit(onSuccess = onFinished) },
                                    enabled = canSubmit,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = dimens.buttonMinHeight),
                                ) {
                                    if (state.isSubmitting) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                        )
                                    } else {
                                        Text(
                                            if (state.activeSchema == ReportCaptureMode.Online) {
                                                "Submit"
                                            } else {
                                                "Save offline"
                                            },
                                        )
                                    }
                                }
                                if (!canSubmit && !state.isSubmitting) {
                                    Text(
                                        text = "Title and photo are required.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Stone400,
                                    )
                                }
                            }
                            Spacer(Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }
}
