// SPDX-License-Identifier: GPL-3.0-only
// Routing adapted from InstallerX Revived; Copyright (C) 2025-2026 InstallerX Revived contributors.
// Modified for ShadowRPC preferences, preset bridge and Montserrat; see docs/THEMES.md.
package dev.citali.shadowrpc.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import dev.citali.shadowrpc.ui.theme.manga.*
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import dev.citali.shadowrpc.data.DarkMode
import dev.citali.shadowrpc.data.Prefs
import dev.citali.shadowrpc.data.rememberEnumPreference
import dev.citali.shadowrpc.data.rememberPreference

/** Seed colours offered in Display, in the order they appear in the picker. */
val SeedColors: List<Color> =
    listOf(
        Color(0xFFBFC9AD), // sage (fallback default, matching the LunarTune reference)
        Color(0xFFB69DF8), // lavender
        Color(0xFFC9B8C8), // mauve
        Color(0xFF8FC7F0), // sky
        Color(0xFFB4C0F5), // periwinkle
        Color(0xFFF2A7B9), // rose
        Color(0xFFA6D8B1), // mint
        Color(0xFFF5C77E), // amber
        Color(0xFF9EDCE0), // teal
    )

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ShadowRpcTheme(content: @Composable () -> Unit) {
    val (darkMode) = rememberEnumPreference(Prefs.DarkModeKey, DarkMode.AUTO)
    val (pureBlack) = rememberPreference(Prefs.PureBlackKey, false)
    val (dynamicColor) = rememberPreference(Prefs.DynamicColorKey, Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
    val (seedArgb) = rememberPreference(Prefs.SeedColorKey, SeedColors.first().toArgb().toLong())
    val (preset) = rememberEnumPreference(Prefs.ThemePresetKey, ThemePreset.MATERIAL_YOU)
    val (paperMode) = rememberEnumPreference(Prefs.MangaPaperKey, MangaPaperMode.AUTO)
    val (accent) = rememberEnumPreference(Prefs.MangaAccentKey, MangaAccent.CRIMSON)
    val (decorations) = rememberPreference(Prefs.ThemeDecorationsKey, true)
    val (monet) = rememberPreference(Prefs.MiuixMonetKey, false)
    val systemDark = isSystemInDarkTheme()
    val requestedDark = when (darkMode) {
        DarkMode.AUTO -> systemDark
        DarkMode.ON -> true
        DarkMode.OFF -> false
    }
    val paper = when (paperMode) {
        MangaPaperMode.AUTO -> if (requestedDark) MangaPaper.NIGHT else MangaPaper.DAY
        MangaPaperMode.DAY -> MangaPaper.DAY
        MangaPaperMode.NIGHT -> MangaPaper.NIGHT
        MangaPaperMode.NORD -> MangaPaper.NORD
    }
    val darkTheme = if (preset == ThemePreset.MANGA) paper != MangaPaper.DAY else requestedDark
    val context = LocalContext.current
    val canUseDynamic = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val seed = Color(seedArgb.toInt())
    val tokens = remember(preset, decorations, darkTheme) { ThemeTokens(preset, decorations, darkTheme) }
    val shapes = remember(tokens) {
        androidx.compose.material3.Shapes(
            extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(tokens.corner(4.dp)),
            small = androidx.compose.foundation.shape.RoundedCornerShape(tokens.corner(8.dp)),
            medium = androidx.compose.foundation.shape.RoundedCornerShape(tokens.corner(12.dp)),
            large = androidx.compose.foundation.shape.RoundedCornerShape(tokens.corner(16.dp)),
            extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(tokens.corner(28.dp)),
        )
    }
    // InstallerX's movable-content routing keeps navigation/forms alive across engine switches.
    val preserved = remember { movableContentOf<@Composable () -> Unit> { it() } }
    val view = LocalView.current
    val activity = androidx.activity.compose.LocalActivity.current
    SideEffect {
        activity?.window?.let { window ->
            androidx.core.view.WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    CompositionLocalProvider(LocalThemeTokens provides tokens) {
        if (preset == ThemePreset.MIUI) {
            // Stable Miuix controller; same default-vs-Monet route as InstallerX Revived.
            val mode = if (monet) {
                if (darkTheme) ColorSchemeMode.MonetDark else ColorSchemeMode.MonetLight
            } else if (darkTheme) ColorSchemeMode.Dark else ColorSchemeMode.Light
            val key = if (canUseDynamic) androidx.compose.ui.res.colorResource(android.R.color.system_accent1_500) else seed
            val controller = remember(mode, key, darkTheme) {
                ThemeController(colorSchemeMode = mode, keyColor = key, isDark = darkTheme)
            }
            val raw = controller.currentColors()
            val colors = if (darkTheme && pureBlack) raw.copy(background = Color.Black, surface = Color.Black) else raw
            MiuixTheme(colors = colors, textStyles = remember { miuixTextStyles() }) {
                MaterialTheme(colorScheme = colors.toMaterialScheme(darkTheme).withPureBlack(darkTheme && pureBlack),
                    shapes = shapes, typography = ShadowTypography, motionScheme = MotionScheme.standard()) { preserved(content) }
            }
        } else {
            val base = if (preset == ThemePreset.MANGA) {
                remember(paper, accent) { mangaColors(paper, accent).toMaterialScheme() }
            } else if (canUseDynamic) {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                remember(seed, darkTheme) { dynamicColorScheme(seedColor = seed, isDark = darkTheme, style = PaletteStyle.TonalSpot) }
            }
            MaterialTheme(colorScheme = base.withPureBlack(darkTheme && pureBlack), shapes = shapes,
                typography = ShadowTypography,
                motionScheme = if (preset == ThemePreset.MANGA) MotionScheme.standard() else MotionScheme.expressive()) { preserved(content) }
        }
    }
}
