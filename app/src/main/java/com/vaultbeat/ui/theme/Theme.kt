package com.vaultbeat.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VaultDarkColors = darkColorScheme(
    primary = Copper,
    onPrimary = Obsidian,
    secondary = Moss,
    background = Obsidian,
    onBackground = WarmIvory,
    surface = Graphite,
    onSurface = WarmIvory,
    surfaceVariant = RaisedGraphite,
    onSurfaceVariant = MutedIvory,
    outline = MutedIvory.copy(alpha = .45f)
)

private val VaultLightColors = lightColorScheme(
    primary = DeepCopper,
    secondary = Color(0xFF51664B),
    background = Color(0xFFFFFBF5),
    surface = Color(0xFFFFFBF5),
    onBackground = Color(0xFF1B1D1B),
    onSurface = Color(0xFF1B1D1B)
)

@Composable
fun VaultBeatTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) VaultDarkColors else VaultLightColors,
        typography = Typography,
        content = content
    )
}

