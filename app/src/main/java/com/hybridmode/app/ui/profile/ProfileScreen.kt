package com.hybridmode.app.ui.profile

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hybridmode.app.ui.components.ConnectivityBanner
import com.hybridmode.app.ui.components.MistBackground
import com.hybridmode.app.ui.components.SoftPanel
import com.hybridmode.app.ui.home.HomeViewModel
import com.hybridmode.app.ui.responsive.ResponsiveContent
import com.hybridmode.app.ui.responsive.rememberAppDimens
import com.hybridmode.app.ui.theme.Forest800
import com.hybridmode.app.ui.theme.Mist100
import com.hybridmode.app.ui.theme.Mist50
import com.hybridmode.app.ui.theme.Stone400
import com.hybridmode.app.ui.theme.Stone600
import com.hybridmode.app.ui.theme.Stone900

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val dimens = rememberAppDimens()

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
                            text = "Profile",
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
                        verticalArrangement = Arrangement.spacedBy(dimens.sectionSpacing),
                    ) {
                        val user = state.profile ?: state.session?.user

                        SoftPanel {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Mist50),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Person,
                                        contentDescription = null,
                                        tint = Forest800,
                                        modifier = Modifier.size(26.dp),
                                    )
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = user?.displayName?.value ?: "User",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.SemiBold,
                                        ),
                                        color = Stone900,
                                    )
                                    Text(
                                        text = user?.email?.value.orEmpty().ifBlank { "-" },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Stone400,
                                    )
                                }
                            }
                        }

                        SoftPanel {
                            Text(
                                "How to use",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = Stone900,
                            )
                            HorizontalDivider(color = Mist100, thickness = 0.5.dp)
                            Text(
                                "1. Login with your email and password\n" +
                                    "2. Submit reports with a photo\n" +
                                    "3. Offline reports sync when you are back online",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Stone600,
                            )
                        }

                        Button(
                            onClick = viewModel::logout,
                            enabled = !state.isBusy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = dimens.buttonMinHeight),
                        ) {
                            Text("Logout")
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}
