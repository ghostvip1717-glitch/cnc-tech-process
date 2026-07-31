package com.cnctech.process.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Fixed light palette (matches web). Do NOT inherit system dark text on light cards.
val CncPrimary = Color(0xFF2481CC)
val CncOnPrimary = Color(0xFFFFFFFF)
val CncBackground = Color(0xFFFFFFFF)
val CncSurface = Color(0xFFFFFFFF)
val CncOnSurface = Color(0xFF16181C)
val CncOnSurfaceSecondary = Color(0xFF555555)
val CncMuted = Color(0xFF666666)
val CncBorder = Color(0xFFE5E5E5)
val CncDanger = Color(0xFFD64545)
val CncSkeleton = Color(0xFFECECEC)
val CncHubGradientTop = Color(0xFFF7F9FC)
val CncIconTintBg = Color(0xFF2481CC).copy(alpha = 0.14f)

private val LightColors = lightColorScheme(
    primary = CncPrimary,
    onPrimary = CncOnPrimary,
    background = CncBackground,
    onBackground = CncOnSurface,
    surface = CncSurface,
    onSurface = CncOnSurface,
    error = CncDanger,
    onError = Color.White,
)

@Composable
fun CncTheme(content: @Composable () -> Unit) {
    // Always light scheme so text never blends with light cards in system dark mode.
    @Suppress("UNUSED_VARIABLE")
    val ignoredDark = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = LightColors,
        content = content,
    )
}
