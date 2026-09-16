package dev.citali.shadowrpc.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
        Color(0xFFB69DF8), // lavender (default)
        Color(0xFFC9B8C8), // mauve
        Color(0xFF8FC7F0), // sky
        Color(0xFFB4C0F5), // periwinkle
        Color(0xFFF2A7B9), // rose
        Color(0xFFA6D8B1), // mint
        Color(0xFFF5C77E), // amber
        Color(0xFF9EDCE0), // teal
    )

@Composable
fun ShadowRpcTheme(content: @Composable () -> Unit) {
    val (darkMode) = rememberEnumPreference(Prefs.DarkModeKey, DarkMode.AUTO)
    val (pureBlack) = rememberPreference(Prefs.PureBlackKey, false)
    val (dynamicColor) = rememberPreference(Prefs.DynamicColorKey, false)
    val (seedArgb) = rememberPreference(Prefs.SeedColorKey, SeedColors.first().toArgb().toLong())

    val systemDark = isSystemInDarkTheme()
    val darkTheme =
        when (darkMode) {
            DarkMode.AUTO -> systemDark
            DarkMode.ON -> true
            DarkMode.OFF -> false
        }
    val context = LocalContext.current
    val canUseDynamic = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val base: ColorScheme =
        if (canUseDynamic) {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            val seed = Color(seedArgb.toInt())
            remember(seed, darkTheme) {
                dynamicColorScheme(seedColor = seed, isDark = darkTheme, style = PaletteStyle.TonalSpot)
            }
        }
    val scheme =
        remember(base, darkTheme, pureBlack) {
            if (darkTheme && pureBlack) {
                base.copy(
                    background = Color.Black,
                    surface = Color.Black,
                    surfaceContainerLowest = Color.Black,
                    surfaceContainerLow = Color(0xFF0B0B0B),
                    surfaceContainer = Color(0xFF121212),
                    surfaceContainerHigh = Color(0xFF1A1A1A),
                    surfaceContainerHighest = Color(0xFF222222),
                )
            } else {
                base
            }
        }

    MaterialTheme(colorScheme = scheme, typography = ShadowTypography, content = content)
}
