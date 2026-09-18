// SPDX-License-Identifier: GPL-3.0-only
// ShadowRPC adaptations of Komi personality tokens and InstallerX Revived theme routing.
// InstallerX portions: Copyright (C) 2025-2026 InstallerX Revived contributors.
// See docs/THEMES.md and bundled licenses for source revisions and modifications.
package dev.citali.shadowrpc.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.citali.shadowrpc.ui.theme.manga.*
import top.yukonga.miuix.kmp.theme.Colors
import top.yukonga.miuix.kmp.theme.TextStyles
import kotlin.math.roundToInt

enum class ThemePreset { MATERIAL_YOU, MANGA, MIUI }
enum class MangaPaperMode { AUTO, DAY, NIGHT, NORD }

@Immutable
data class ThemeTokens(
    val preset: ThemePreset = ThemePreset.MATERIAL_YOU,
    val decorations: Boolean = true,
    val isDark: Boolean = false,
) {
    val manga get() = preset == ThemePreset.MANGA
    val miui get() = preset == ThemePreset.MIUI
    fun corner(default: Dp): Dp = when (preset) {
        ThemePreset.MANGA -> 0.dp
        ThemePreset.MIUI -> minOf(default, 16.dp)
        else -> default
    }
}
val LocalThemeTokens = staticCompositionLocalOf { ThemeTokens() }

@Composable
fun themeShape(default: Dp): RoundedCornerShape = RoundedCornerShape(LocalThemeTokens.current.corner(default))

@Composable
fun themeBorder(): BorderStroke? = if (LocalThemeTokens.current.manga) BorderStroke(3.dp, MaterialTheme.colorScheme.outline) else null

/** Komi hard-shadow panel treatment; no blur, offscreen framebuffer or animation. */
@Composable
fun Modifier.themePanel(): Modifier {
    val t = LocalThemeTokens.current
    val ink = if (t.isDark) Color.Black else MaterialTheme.colorScheme.onSurface
    return if (!t.manga || !t.decorations) this else drawWithCache {
        val offset = 6.dp.toPx()
        onDrawBehind { translate(offset, offset) { drawRect(ink) } }
    }
}

/** Komi paper-grid treatment, adapted to a cached repeating tile (one draw call). */
@Composable
fun Modifier.themePaper(): Modifier {
    val t = LocalThemeTokens.current
    val ink = MaterialTheme.colorScheme.onBackground
    return if (!t.manga || !t.decorations) this else drawWithCache {
        val side = 26.dp.toPx().roundToInt().coerceAtLeast(1)
        val tile = ImageBitmap(side, side)
        val canvas = Canvas(tile)
        val paint = Paint().apply { color = ink.copy(alpha = if (t.isDark) 0.06f else 0.05f); strokeWidth = 1.dp.toPx() }
        canvas.drawLine(Offset.Zero, Offset(side.toFloat(), 0f), paint)
        canvas.drawLine(Offset.Zero, Offset(0f, side.toFloat()), paint)
        val brush = ShaderBrush(ImageShader(tile, TileMode.Repeated, TileMode.Repeated))
        onDrawBehind { drawRect(brush) }
    }
}

fun PersonalityColors.toMaterialScheme(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primary, onPrimary = onPrimary, primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
        secondary = primary, onSecondary = onPrimary, secondaryContainer = primaryContainer, onSecondaryContainer = onPrimaryContainer,
        tertiary = primary, onTertiary = onPrimary, tertiaryContainer = primaryContainer, onTertiaryContainer = onPrimaryContainer,
        background = background, onBackground = onBackground, surface = surface, onSurface = onSurface,
        surfaceVariant = surfaceVariant, onSurfaceVariant = onSurfaceVariant, surfaceTint = primary,
        surfaceContainerLowest = background, surfaceContainerLow = surface, surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh, surfaceContainerHighest = surfaceContainerHigh,
        surfaceBright = surfaceContainerHigh, surfaceDim = background,
        outline = outline, outlineVariant = outlineVariant, error = error, onError = onError,
        inverseSurface = onSurface, inverseOnSurface = surface, inversePrimary = primary, scrim = scrim,
    )
}

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
