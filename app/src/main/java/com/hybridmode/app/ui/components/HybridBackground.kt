package com.hybridmode.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.hybridmode.app.ui.theme.Forest800
import com.hybridmode.app.ui.theme.Forest900
import com.hybridmode.app.ui.theme.Forest950
import com.hybridmode.app.ui.theme.Mist50
import com.hybridmode.app.ui.theme.Mist100

@Composable
fun MistBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Mist100,
                        Mist50,
                        Color.White,
                    )
                )
            ),
        content = content,
    )
}

@Composable
fun BrandHeroBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Forest950,
                        Forest900,
                        Forest900,
                        Forest800.copy(alpha = 0.95f),
                    )
                )
            ),
        content = content,
    )
}
