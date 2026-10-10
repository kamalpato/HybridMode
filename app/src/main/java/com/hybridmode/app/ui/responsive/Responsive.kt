package com.hybridmode.app.ui.responsive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class AppWidthSize {
    Compact,
    Medium,
    Expanded,
}

data class AppDimens(
    val widthSize: AppWidthSize,
    val screenPadding: Dp,
    val contentMaxWidth: Dp,
    val sectionSpacing: Dp,
    val gridMinSize: Dp,
    val heroImageHeight: Dp,
    val buttonMinHeight: Dp,
    val useNavigationRail: Boolean,
)

@Composable
fun rememberWindowWidthSize(): AppWidthSize {
    val widthDp = LocalConfiguration.current.screenWidthDp
    return remember(widthDp) {
        when {
            widthDp < 600 -> AppWidthSize.Compact
            widthDp < 840 -> AppWidthSize.Medium
            else -> AppWidthSize.Expanded
        }
    }
}

@Composable
fun rememberAppDimens(): AppDimens {
    val widthSize = rememberWindowWidthSize()
    val fontScale = LocalDensity.current.fontScale.coerceIn(1f, 1.6f)
    val heightDp = LocalConfiguration.current.screenHeightDp

    return remember(widthSize, fontScale, heightDp) {
        val base = when (widthSize) {
            AppWidthSize.Compact -> AppDimens(
                widthSize = widthSize,
                screenPadding = 16.dp,
                contentMaxWidth = 560.dp,
                sectionSpacing = 14.dp,
                gridMinSize = 148.dp,
                heroImageHeight = if (heightDp < 700) 180.dp else 220.dp,
                buttonMinHeight = 48.dp,
                useNavigationRail = false,
            )
            AppWidthSize.Medium -> AppDimens(
                widthSize = widthSize,
                screenPadding = 24.dp,
                contentMaxWidth = 720.dp,
                sectionSpacing = 16.dp,
                gridMinSize = 170.dp,
                heroImageHeight = 260.dp,
                buttonMinHeight = 52.dp,
                useNavigationRail = true,
            )
            AppWidthSize.Expanded -> AppDimens(
                widthSize = widthSize,
                screenPadding = 32.dp,
                contentMaxWidth = 840.dp,
                sectionSpacing = 18.dp,
                gridMinSize = 190.dp,
                heroImageHeight = 300.dp,
                buttonMinHeight = 52.dp,
                useNavigationRail = true,
            )
        }
        base.copy(
            buttonMinHeight = (base.buttonMinHeight.value * fontScale).dp.coerceAtLeast(48.dp),
            screenPadding = (base.screenPadding.value * (1f + (fontScale - 1f) * 0.25f)).dp,
        )
    }
}

@Composable
fun ResponsiveContent(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val dimens = rememberAppDimens()
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .widthIn(max = dimens.contentMaxWidth),
            content = content,
        )
    }
}
