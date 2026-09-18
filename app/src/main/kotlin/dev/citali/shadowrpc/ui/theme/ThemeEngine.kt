// SPDX-License-Identifier: GPL-3.0-only
// Theme routing adapted from InstallerX Revived.
// Copyright (C) 2025-2026 InstallerX Revived contributors.
// ShadowRPC adaptations documented in docs/THEMES.md.
package dev.citali.shadowrpc.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.citali.shadowrpc.data.ThemePreset
import top.yukonga.miuix.kmp.theme.Colors
import top.yukonga.miuix.kmp.theme.TextStyles

@Immutable
data class ThemeTokens(val preset: ThemePreset = ThemePreset.MATERIAL_YOU) {
    val miui get() = preset == ThemePreset.MIUI
    fun corner(default: Dp): Dp = if (miui) minOf(default, 16.dp) else default
}
val LocalThemeTokens = staticCompositionLocalOf { ThemeTokens() }

@Composable
fun themeShape(default: Dp): RoundedCornerShape = RoundedCornerShape(LocalThemeTokens.current.corner(default))

/** Bridge actual Miuix engine roles to screens which still use Material components. */
fun Colors.toMaterialScheme(dark: Boolean): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primary, onPrimary = onPrimary, primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
        secondary = secondary, onSecondary = onSecondary, secondaryContainer = secondaryContainer, onSecondaryContainer = onSecondaryContainer,
        tertiary = primary, onTertiary = onPrimary, tertiaryContainer = tertiaryContainer, onTertiaryContainer = onTertiaryContainer,
        background = background, onBackground = onBackground, surface = surface, onSurface = onSurface,
        surfaceVariant = surfaceVariant, onSurfaceVariant = onSurfaceSecondary, surfaceTint = primary,
        surfaceContainerLowest = background, surfaceContainerLow = surface, surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh, surfaceContainerHighest = surfaceContainerHighest,
        surfaceBright = surfaceContainerHigh, surfaceDim = background,
        outline = outline, outlineVariant = dividerLine, error = error, onError = onError,
        errorContainer = errorContainer, onErrorContainer = onErrorContainer,
        inverseSurface = onSurface, inverseOnSurface = surface, inversePrimary = primary,
    )
}

fun ColorScheme.withPureBlack(enabled: Boolean): ColorScheme = if (!enabled) this else copy(
    background = Color.Black, surface = Color.Black, surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0B0B0B), surfaceContainer = Color(0xFF121212),
    surfaceContainerHigh = Color(0xFF1A1A1A), surfaceContainerHighest = Color(0xFF222222),
)

fun miuixTextStyles() = ShadowTypography.let { t ->
    TextStyles(main = t.bodyLarge, paragraph = t.bodyLarge, body1 = t.bodyLarge, body2 = t.bodyMedium,
        button = t.labelLarge, footnote1 = t.bodySmall, footnote2 = t.labelSmall,
        headline1 = t.headlineLarge, headline2 = t.headlineMedium, subtitle = t.titleMedium,
        title1 = t.displaySmall, title2 = t.headlineLarge, title3 = t.headlineMedium, title4 = t.titleLarge)
}
