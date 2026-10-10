package com.hybridmode.app.ui.upload

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hybridmode.common.connectivity.ConnectivityStatus
import com.hybridmode.common.result.AppResult
import com.hybridmode.domain.model.UploadSchema
import com.hybridmode.domain.model.UploadSyncStatus
import com.hybridmode.domain.usecase.connectivity.ObserveConnectivityUseCase
import com.hybridmode.domain.usecase.upload.SubmitUploadUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UploadFormState(
    val title: String = "",
    val description: String = "",
    val imageUri: String? = null,
    val mimeType: String = "image/jpeg",
)

data class UploadUiState(
    val form: UploadFormState = UploadFormState(),
    val connectivity: ConnectivityStatus = ConnectivityStatus.Offline,
    val activeSchema: UploadSchema = UploadSchema.Offline,
    val isSubmitting: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class UploadViewModel @Inject constructor(
    observeConnectivityUseCase: ObserveConnectivityUseCase,
    private val submitUploadUseCase: SubmitUploadUseCase,
) : ViewModel() {

    private val form = MutableStateFlow(UploadFormState())
    private val ephemeral = MutableStateFlow(Ephemeral())

    val uiState: StateFlow<UploadUiState> = combine(
        form,
        observeConnectivityUseCase(),
        ephemeral,
    ) { formState, connectivity, epi ->
        UploadUiState(
            form = formState,
            connectivity = connectivity,
            activeSchema = if (connectivity == ConnectivityStatus.Online) {
                UploadSchema.Online
            } else {
                UploadSchema.Offline
            },
            isSubmitting = epi.isSubmitting,
            message = epi.message,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UploadUiState(),
    )

    fun onTitleChange(value: String) {
        form.update { it.copy(title = value) }
    }

    fun onDescriptionChange(value: String) {
        form.update { it.copy(description = value) }
    }

    fun onImagePicked(uri: String, mimeType: String?) {
        form.update {
            it.copy(
                imageUri = uri,
                mimeType = mimeType?.takeIf { type -> type.isNotBlank() } ?: "image/jpeg",
            )
        }
    }

    fun clearImage() {
        form.update { it.copy(imageUri = null) }
    }

    fun submit(onSuccess: () -> Unit = {}) {
        val current = form.value
        viewModelScope.launch {
            ephemeral.update { it.copy(isSubmitting = true, message = null) }
            when (
                val result = submitUploadUseCase(
                    title = current.title,
                    description = current.description,
                    sourceUri = current.imageUri.orEmpty(),
                    mimeType = current.mimeType,
                )
            ) {
                is AppResult.Success -> {
                    form.value = UploadFormState()
                    val message = when (result.data.syncStatus) {
                        UploadSyncStatus.Synced -> "Report uploaded successfully."
                        UploadSyncStatus.PendingOffline ->
                            "Report saved offline. It will sync when you are online."
                        UploadSyncStatus.Failed ->
                            "Saved offline, but upload failed. Retry from Reports."
                        UploadSyncStatus.Uploading -> "Uploading…"
                    }
                    ephemeral.update {
                        it.copy(isSubmitting = false, message = message)
                    }
                    onSuccess()
                }
                is AppResult.Error -> ephemeral.update {
                    it.copy(isSubmitting = false, message = result.failure.message)
                }
            }
        }
    }

    fun clearMessage() {
        ephemeral.update { it.copy(message = null) }
    }

    private data class Ephemeral(
        val isSubmitting: Boolean = false,
        val message: String? = null,
    )
}
