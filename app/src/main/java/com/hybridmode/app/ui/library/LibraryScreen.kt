package com.hybridmode.app.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.hybridmode.app.ui.components.MistBackground
import com.hybridmode.app.ui.components.StatusChip
import com.hybridmode.app.ui.responsive.ResponsiveContent
import com.hybridmode.app.ui.responsive.rememberAppDimens
import com.hybridmode.app.ui.theme.Copper500
import com.hybridmode.app.ui.theme.Danger
import com.hybridmode.app.ui.theme.Forest800
import com.hybridmode.app.ui.theme.Mist100
import com.hybridmode.app.ui.theme.Mist50
import com.hybridmode.app.ui.theme.OnlineGreen
import com.hybridmode.app.ui.theme.Stone400
import com.hybridmode.app.ui.theme.Stone600
import com.hybridmode.app.ui.theme.Stone900
import com.hybridmode.app.ui.util.DateFormats
import com.hybridmode.app.ui.util.ImageRequests
import com.hybridmode.domain.reporting.FieldReport
import com.hybridmode.domain.reporting.ReportSyncStatus
import com.hybridmode.domain.application.reporting.ObserveUploadsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import kotlin.math.ceil

private const val PAGE_SIZE = 10

enum class ReportSortOrder {
    NewestFirst,
    OldestFirst,
}

data class ReportsPageState(
    val visibleItems: List<FieldReport> = emptyList(),
    val totalOnDevice: Int = 0,
    val filteredCount: Int = 0,
    val page: Int = 1,
    val totalPages: Int = 1,
    val hasPrevious: Boolean = false,
    val hasNext: Boolean = false,
    val query: String = "",
    val sortOrder: ReportSortOrder = ReportSortOrder.NewestFirst,
    val isLoading: Boolean = true,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onOpenPhoto: (String) -> Unit,
    onOpenCapture: () -> Unit,
    viewModel: ReportsViewModel = hiltViewModel(),
) {
    val pageState by viewModel.pageState.collectAsStateWithLifecycle()
    val dimens = rememberAppDimens()

    MistBackground {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "Reports",
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
        ) { padding ->
            ResponsiveContent {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = dimens.screenPadding),
                ) {
                    ReportsToolbar(
                        query = pageState.query,
                        sortOrder = pageState.sortOrder,
                        onQueryChange = viewModel::onQueryChange,
                        onSortChange = viewModel::onSortChange,
                        modifier = Modifier.padding(top = 2.dp, bottom = 14.dp),
                    )

                    when {
                        pageState.isLoading -> {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    strokeWidth = 2.5.dp,
                                    color = Forest800,
                                )
                            }
                        }
                        pageState.totalOnDevice == 0 -> {
                            ReportsEmptyState(
                                title = "No reports yet",
                                body = "Create a report with a photo. It stays on this device until it syncs.",
                                actionLabel = "New report",
                                onAction = onOpenCapture,
                                buttonMinHeight = dimens.buttonMinHeight,
                            )
                        }
                        pageState.filteredCount == 0 -> {
                            ReportsEmptyState(
                                title = "No results",
                                body = "No report title matches “${pageState.query.trim()}”.",
                                actionLabel = null,
                                onAction = null,
                                buttonMinHeight = dimens.buttonMinHeight,
                            )
                        }
                        else -> {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(bottom = 20.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                items(pageState.visibleItems, key = { it.id }) { report ->
                                    ReportListRow(
                                        report = report,
                                        onClick = { onOpenPhoto(report.id) },
                                    )
                                }
                                if (pageState.hasPrevious || pageState.hasNext) {
                                    item {
                                        ReportsPager(
                                            hasPrevious = pageState.hasPrevious,
                                            hasNext = pageState.hasNext,
                                            onPrevious = viewModel::previousPage,
                                            onNext = viewModel::nextPage,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportsToolbar(
    query: String,
    sortOrder: ReportSortOrder,
    onQueryChange: (String) -> Unit,
    onSortChange: (ReportSortOrder) -> Unit,
    modifier: Modifier = Modifier,
) {
    var sortMenuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 1.dp, shape = RoundedCornerShape(14.dp), clip = false)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            tint = Stone400,
            modifier = Modifier.size(18.dp),
        )
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Stone900),
            cursorBrush = SolidColor(Forest800),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            text = "Search",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Stone400,
                        )
                    }
                    inner()
                }
            },
        )
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(18.dp)
                .background(Mist100),
        )
        Box {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { sortMenuOpen = true }
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Sort,
                    contentDescription = "Sort",
                    tint = Forest800,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = when (sortOrder) {
                        ReportSortOrder.NewestFirst -> "Newest"
                        ReportSortOrder.OldestFirst -> "Oldest"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = Forest800,
                )
            }
            DropdownMenu(
                expanded = sortMenuOpen,
                onDismissRequest = { sortMenuOpen = false },
            ) {
                DropdownMenuItem(
                    text = {
                        Text("Newest first", style = MaterialTheme.typography.bodySmall)
                    },
                    onClick = {
                        onSortChange(ReportSortOrder.NewestFirst)
                        sortMenuOpen = false
                    },
                )
                DropdownMenuItem(
                    text = {
                        Text("Oldest first", style = MaterialTheme.typography.bodySmall)
                    },
                    onClick = {
                        onSortChange(ReportSortOrder.OldestFirst)
                        sortMenuOpen = false
                    },
                )
            }
        }
    }
}

@Composable
private fun ReportsEmptyState(
    title: String,
    body: String,
    actionLabel: String?,
    onAction: (() -> Unit)?,
    buttonMinHeight: Dp,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Mist50),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Description,
                contentDescription = null,
                tint = Forest800.copy(alpha = 0.7f),
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = Stone900,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = Stone600,
            modifier = Modifier.fillMaxWidth(0.92f),
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = buttonMinHeight),
            ) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun ReportsPager(
    hasPrevious: Boolean,
    hasNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (hasPrevious) {
            PagerControl(
                label = "Previous",
                leading = true,
                onClick = onPrevious,
            )
        }
        if (hasPrevious && hasNext) {
            Spacer(Modifier.width(20.dp))
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(14.dp)
                    .background(Mist100),
            )
            Spacer(Modifier.width(20.dp))
        }
        if (hasNext) {
            PagerControl(
                label = "Next",
                leading = false,
                onClick = onNext,
            )
        }
    }
}

@Composable
private fun PagerControl(
    label: String,
    leading: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true),
                onClick = onClick,
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (leading) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                contentDescription = null,
                tint = Forest800,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = Forest800,
        )
        if (!leading) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = Forest800,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun ReportListRow(
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
            .shadow(elevation = 1.dp, shape = RoundedCornerShape(16.dp), clip = false)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true),
                onClick = onClick,
            )
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = ImageRequests.thumbnail(context, report.localImagePath.value),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Mist50),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = report.title.value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                color = Stone900,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = DateFormats.dateTime(report.createdAtEpochMs),
                style = MaterialTheme.typography.labelMedium,
                color = Stone400,
            )
            if (report.description.value.isNotBlank()) {
                Text(
                    text = report.description.value,
                    style = MaterialTheme.typography.bodySmall,
                    color = Stone600,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        StatusChip(text = statusLabel, color = statusColor)
    }
}

@OptIn(kotlinx.coroutines.FlowPreview::class)
@HiltViewModel
class ReportsViewModel @Inject constructor(
    observeUploadsUseCase: ObserveUploadsUseCase,
) : ViewModel() {

    private val page = MutableStateFlow(1)
    private val query = MutableStateFlow("")
    private val sortOrder = MutableStateFlow(ReportSortOrder.NewestFirst)

    private val allReports: StateFlow<List<FieldReport>?> = observeUploadsUseCase()
        .map<List<FieldReport>, List<FieldReport>?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val debouncedQuery = query
        .debounce(180)
        .distinctUntilChanged()

    val pageState: StateFlow<ReportsPageState> = combine(
        allReports,
        page,
        query,
        debouncedQuery,
        sortOrder,
    ) { all, currentPage, displayQuery, filterQuery, order ->
        if (all == null) {
            return@combine ReportsPageState(
                query = displayQuery,
                sortOrder = order,
                isLoading = true,
            )
        }
        val matched = all.filter { report ->
            val q = filterQuery.trim()
            q.isEmpty() || report.title.value.contains(q, ignoreCase = true)
        }
        val filtered = when (order) {
            ReportSortOrder.NewestFirst -> matched
            ReportSortOrder.OldestFirst -> matched.asReversed()
        }
        val totalPages = ceil(filtered.size / PAGE_SIZE.toDouble()).toInt().coerceAtLeast(1)
        val safePage = currentPage.coerceIn(1, totalPages)
        val from = (safePage - 1) * PAGE_SIZE
        ReportsPageState(
            visibleItems = filtered.drop(from).take(PAGE_SIZE),
            totalOnDevice = all.size,
            filteredCount = filtered.size,
            page = safePage,
            totalPages = totalPages,
            hasPrevious = safePage > 1,
            hasNext = safePage < totalPages,
            query = displayQuery,
            sortOrder = order,
            isLoading = false,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ReportsPageState(),
    )

    fun onQueryChange(value: String) {
        query.value = value
        page.value = 1
    }

    fun onSortChange(order: ReportSortOrder) {
        sortOrder.value = order
        page.value = 1
    }

    fun nextPage() {
        if (!pageState.value.hasNext) return
        page.update { it + 1 }
    }

    fun previousPage() {
        if (!pageState.value.hasPrevious) return
        page.update { (it - 1).coerceAtLeast(1) }
    }
}
