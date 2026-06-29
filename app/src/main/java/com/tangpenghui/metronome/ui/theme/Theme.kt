package com.tangpenghui.metronome.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val MetronomeColorScheme = darkColorScheme(
    primary = AccentGreen, onPrimary = BgPrimary,
    secondary = AccentBlue, onSecondary = BgPrimary,
    background = BgPrimary, onBackground = TextPrimary,
    surface = BgCard, onSurface = TextPrimary,
    error = AccentRed
)

@Composable
fun MetronomeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = MetronomeColorScheme, typography = MetronomeTypography, content = content)
}
