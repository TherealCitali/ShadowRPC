package dev.citali.shadowrpc.ui.theme

import android.os.Build
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import dev.citali.shadowrpc.data.*
import top.yukonga.miuix.kmp.utils.MiuixOverscrollFactory

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
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (darkMode) {
        DarkMode.AUTO -> systemDark
        DarkMode.ON -> true
        DarkMode.OFF -> false
    }
    val context = LocalContext.current
    LaunchedEffect(context) { RetiredThemeSettings.clear(context) }
    val canUseDynamic = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val seed = Color(seedArgb.toInt())
    val base = if (canUseDynamic) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        remember(seed, darkTheme) {
            dynamicColorScheme(seedColor = seed, isDark = darkTheme, style = PaletteStyle.TonalSpot)
        }
    }
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
    // Retain only Miuix's edge elasticity. No Miuix theme, colors, shapes,
    // typography, controls or indication are installed by this provider.
    CompositionLocalProvider(LocalOverscrollFactory provides MiuixOverscrollFactory) {
        MaterialTheme(
            colorScheme = base.withPureBlack(darkTheme && pureBlack),
            typography = ShadowTypography,
            motionScheme = MotionScheme.expressive(),
            content = content,
        )
    }
}

fun ColorScheme.withPureBlack(enabled: Boolean): ColorScheme = if (!enabled) this else copy(
    background = Color.Black, surface = Color.Black, surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0B0B0B), surfaceContainer = Color(0xFF121212),
    surfaceContainerHigh = Color(0xFF1A1A1A), surfaceContainerHighest = Color(0xFF222222),
)
