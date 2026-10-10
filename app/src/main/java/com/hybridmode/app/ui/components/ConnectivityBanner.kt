package com.hybridmode.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hybridmode.app.ui.theme.OfflineAmber
import com.hybridmode.app.ui.theme.OnlineGreen
import com.hybridmode.common.connectivity.ConnectivityStatus

@Composable
fun ConnectivityBanner(
    status: ConnectivityStatus,
    offlineSessionActive: Boolean,
    modifier: Modifier = Modifier,
) {
    val label = when (status) {
        ConnectivityStatus.Online -> "Online"
        ConnectivityStatus.Offline -> {
            if (offlineSessionActive) "Offline - will sync later" else "Offline"
        }
    }
    val color = when (status) {
        ConnectivityStatus.Online -> OnlineGreen
        ConnectivityStatus.Offline -> OfflineAmber
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(color)
            .padding(horizontal = 16.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
        )
    }
}
