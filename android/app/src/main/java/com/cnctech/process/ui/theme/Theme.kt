package com.cnctech.process.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class CncColorPalette(
    val primary: Color,
    val onPrimary: Color,
    val background: Color,
    val surface: Color,
    val onSurface: Color,
    val onSurfaceSecondary: Color,
    val muted: Color,
    val border: Color,
    val danger: Color,
    val skeleton: Color,
    val hubGradientTop: Color,
    val iconTintBg: Color,
    val badgeTimeBg: Color,
    val badgeTimeText: Color,
    val badgeProgramsBg: Color,
    val badgeProgramsText: Color,
)

enum class ThemeVariant {
    Industrial,
    Graphite,
    Oled,
    Neon,
    Amber,
    Emerald,
    Violet,
    Light,
}

fun ThemeVariant.displayName(): String = when (this) {
    ThemeVariant.Industrial -> "Индустриальный"
    ThemeVariant.Graphite -> "Графит"
    ThemeVariant.Oled -> "OLED"
    ThemeVariant.Neon -> "Неон"
    ThemeVariant.Amber -> "Янтарь"
    ThemeVariant.Emerald -> "Изумруд"
    ThemeVariant.Violet -> "Ультрафиолет"
    ThemeVariant.Light -> "Светлый"
}

private fun buildPalette(
    background: Long,
    surface: Long,
    border: Long,
    onSurface: Long,
    muted: Long,
    primary: Long,
    onPrimary: Long,
    badgeTimeBg: Long,
    badgeTimeText: Long,
    badgeProgramsBg: Long,
    badgeProgramsText: Long,
    danger: Long,
    skeleton: Long,
    hubGradientTop: Long? = null,
): CncColorPalette {
    val primaryColor = Color(primary)
    return CncColorPalette(
        primary = primaryColor,
        onPrimary = Color(onPrimary),
        background = Color(background),
        surface = Color(surface),
        onSurface = Color(onSurface),
        onSurfaceSecondary = Color(muted),
        muted = Color(muted),
        border = Color(border),
        danger = Color(danger),
        skeleton = Color(skeleton),
        hubGradientTop = Color(hubGradientTop ?: surface),
        iconTintBg = primaryColor.copy(alpha = 0.14f),
        badgeTimeBg = Color(badgeTimeBg),
        badgeTimeText = Color(badgeTimeText),
        badgeProgramsBg = Color(badgeProgramsBg),
        badgeProgramsText = Color(badgeProgramsText),
    )
}

fun paletteFor(variant: ThemeVariant): CncColorPalette = when (variant) {
    ThemeVariant.Industrial -> buildPalette(
        background = 0xFF12161D,
        surface = 0xFF1A1F29,
        border = 0xFF2A3140,
        onSurface = 0xFFF2F4F8,
        muted = 0xFF8B93A3,
        primary = 0xFF3B82F6,
        onPrimary = 0xFFFFFFFF,
        badgeTimeBg = 0xFF0F3D2E,
        badgeTimeText = 0xFF4ADE80,
        badgeProgramsBg = 0xFF1E2B4A,
        badgeProgramsText = 0xFF7DD3FC,
        danger = 0xFFD64545,
        skeleton = 0xFF232937,
    )
    ThemeVariant.Graphite -> buildPalette(
        background = 0xFF191A1C,
        surface = 0xFF232527,
        border = 0xFF33363A,
        onSurface = 0xFFE8E9EB,
        muted = 0xFF9A9EA5,
        primary = 0xFF9CA3AF,
        onPrimary = 0xFF111214,
        badgeTimeBg = 0xFF2A2C2F,
        badgeTimeText = 0xFFC7CAD0,
        badgeProgramsBg = 0xFF2A2C2F,
        badgeProgramsText = 0xFF9CA3AF,
        danger = 0xFFD64545,
        skeleton = 0xFF2A2C2F,
    )
    ThemeVariant.Oled -> buildPalette(
        background = 0xFF000000,
        surface = 0xFF0D0D0D,
        border = 0xFF242424,
        onSurface = 0xFFFFFFFF,
        muted = 0xFF8A8A8A,
        primary = 0xFFEF4444,
        onPrimary = 0xFFFFFFFF,
        badgeTimeBg = 0xFF241313,
        badgeTimeText = 0xFFFCA5A5,
        badgeProgramsBg = 0xFF1A1A1A,
        badgeProgramsText = 0xFFC7C7C7,
        danger = 0xFFEF4444,
        skeleton = 0xFF141414,
    )
    ThemeVariant.Neon -> buildPalette(
        background = 0xFF081018,
        surface = 0xFF0E1C26,
        border = 0xFF164152,
        onSurface = 0xFFE3FBFF,
        muted = 0xFF7FB8C9,
        primary = 0xFF22D3EE,
        onPrimary = 0xFF04222B,
        badgeTimeBg = 0xFF0B3B3A,
        badgeTimeText = 0xFF5EEAD4,
        badgeProgramsBg = 0xFF0E2B3A,
        badgeProgramsText = 0xFF67E8F9,
        danger = 0xFFEF4444,
        skeleton = 0xFF15242E,
    )
    ThemeVariant.Amber -> buildPalette(
        background = 0xFF1A1410,
        surface = 0xFF241C15,
        border = 0xFF3A2D1E,
        onSurface = 0xFFFBF1E6,
        muted = 0xFFB3A086,
        primary = 0xFFF59E0B,
        onPrimary = 0xFF221703,
        badgeTimeBg = 0xFF2A2210,
        badgeTimeText = 0xFFFCD34D,
        badgeProgramsBg = 0xFF2A1F14,
        badgeProgramsText = 0xFFFDBA74,
        danger = 0xFFEF4444,
        skeleton = 0xFF2E2419,
    )
    ThemeVariant.Emerald -> buildPalette(
        background = 0xFF0D1A15,
        surface = 0xFF12241C,
        border = 0xFF1F3A2C,
        onSurface = 0xFFEAFFF4,
        muted = 0xFF8FBBA4,
        primary = 0xFF10B981,
        onPrimary = 0xFF04140C,
        badgeTimeBg = 0xFF0F3324,
        badgeTimeText = 0xFF6EE7B7,
        badgeProgramsBg = 0xFF132B22,
        badgeProgramsText = 0xFF86EFAC,
        danger = 0xFFEF4444,
        skeleton = 0xFF17291F,
    )
    ThemeVariant.Violet -> buildPalette(
        background = 0xFF150F1E,
        surface = 0xFF1E1729,
        border = 0xFF332545,
        onSurface = 0xFFF3ECFF,
        muted = 0xFFA996C4,
        primary = 0xFF8B5CF6,
        onPrimary = 0xFFFFFFFF,
        badgeTimeBg = 0xFF231A3A,
        badgeTimeText = 0xFFC4B5FD,
        badgeProgramsBg = 0xFF2A1A3A,
        badgeProgramsText = 0xFFE0A3FF,
        danger = 0xFFEF4444,
        skeleton = 0xFF241A33,
    )
    ThemeVariant.Light -> buildPalette(
        background = 0xFFF5F7FA,
        surface = 0xFFFFFFFF,
        border = 0xFFE2E5EA,
        onSurface = 0xFF16181C,
        muted = 0xFF667085,
        primary = 0xFF2563EB,
        onPrimary = 0xFFFFFFFF,
        badgeTimeBg = 0xFFDCFCE7,
        badgeTimeText = 0xFF166534,
        badgeProgramsBg = 0xFFDBEAFE,
        badgeProgramsText = 0xFF1E40AF,
        danger = 0xFFD64545,
        skeleton = 0xFFECECEC,
        hubGradientTop = 0xFFF7F9FC,
    )
}

val LocalCncColors = staticCompositionLocalOf { paletteFor(ThemeVariant.Light) }

val CncPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalCncColors.current.primary

val CncOnPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalCncColors.current.onPrimary

val CncBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalCncColors.current.background

val CncSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalCncColors.current.surface

val CncOnSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalCncColors.current.onSurface

val CncOnSurfaceSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalCncColors.current.onSurfaceSecondary

val CncMuted: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalCncColors.current.muted

val CncBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalCncColors.current.border

val CncDanger: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalCncColors.current.danger

val CncSkeleton: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalCncColors.current.skeleton

val CncHubGradientTop: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalCncColors.current.hubGradientTop

val CncIconTintBg: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalCncColors.current.iconTintBg

val CncBadgeTimeBg: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalCncColors.current.badgeTimeBg

val CncBadgeTimeText: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalCncColors.current.badgeTimeText

val CncBadgeProgramsBg: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalCncColors.current.badgeProgramsBg

val CncBadgeProgramsText: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalCncColors.current.badgeProgramsText

@Composable
fun CncTheme(variant: ThemeVariant, content: @Composable () -> Unit) {
    val palette = paletteFor(variant)
    CompositionLocalProvider(LocalCncColors provides palette) {
        MaterialTheme(
            colorScheme = lightColorScheme(
                primary = palette.primary,
                onPrimary = palette.onPrimary,
                background = palette.background,
                onBackground = palette.onSurface,
                surface = palette.surface,
                onSurface = palette.onSurface,
                error = palette.danger,
                onError = Color.White,
            ),
            content = content,
        )
    }
}
