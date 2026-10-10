package com.hybridmode.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ColorScheme = lightColorScheme(
    primary = Forest800,
    onPrimary = Color.White,
    primaryContainer = Mist100,
    onPrimaryContainer = Forest950,
    secondary = Stone600,
    onSecondary = Color.White,
    secondaryContainer = Mist100,
    onSecondaryContainer = Stone900,
    tertiary = Copper500,
    onTertiary = Color.White,
    background = Mist50,
    onBackground = Stone900,
    surface = Color.White,
    onSurface = Stone900,
    surfaceVariant = Mist100,
    onSurfaceVariant = Stone600,
    outline = Stone400.copy(alpha = 0.55f),
    error = Danger,
    onError = Color.White,
)

@Composable
fun HybridModeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColorScheme,
        typography = Typography,
        content = content,
    )
}
