package dev.citali.shadowrpc.ui.screens

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import dev.citali.shadowrpc.R
import dev.citali.shadowrpc.data.DarkMode
import dev.citali.shadowrpc.data.Prefs
import dev.citali.shadowrpc.data.rememberEnumPreference
import dev.citali.shadowrpc.data.rememberPreference
import dev.citali.shadowrpc.ui.component.PreferenceEntry
import dev.citali.shadowrpc.ui.component.ScreenScaffold
import dev.citali.shadowrpc.ui.component.SwitchPreference
import dev.citali.shadowrpc.ui.theme.SeedColors

@Composable
fun DisplayScreen(onBack: () -> Unit) {
    val (darkMode, setDarkMode) = rememberEnumPreference(Prefs.DarkModeKey, DarkMode.AUTO)
    val (pureBlack, setPureBlack) = rememberPreference(Prefs.PureBlackKey, false)
    val (dynamicColor, setDynamicColor) = rememberPreference(Prefs.DynamicColorKey, Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
    val (seed, setSeed) = rememberPreference(Prefs.SeedColorKey, SeedColors.first().toArgb().toLong())
    var darkDialog by remember { mutableStateOf(false) }

    ScreenScaffold(title = stringResource(R.string.drawer_display), onBack = onBack) {
        // Preview card, like the illustration panel in the screenshot
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(220.dp),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                ThemePreviewArt()
            }
        }
        Spacer(Modifier.height(24.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
        ) {
            items(SeedColors) { color ->
                val selected = !dynamicColor && color.toArgb().toLong() == seed
                SeedSwatch(color = color, selected = selected) {
                    setDynamicColor(false)
                    setSeed(color.toArgb().toLong())
                }
            }
        }
        Spacer(Modifier.height(20.dp))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SwitchPreference(
                title = stringResource(R.string.display_dynamic_color),
                description = stringResource(R.string.display_dynamic_color_summary),
                icon = Icons.Outlined.Palette,
                checked = dynamicColor,
                onCheckedChange = setDynamicColor,
            )
        }
        PreferenceEntry(
            title = stringResource(R.string.display_dark_theme),
            description =
                stringResource(
                    when (darkMode) {
                        DarkMode.AUTO -> R.string.display_follow_system
                        DarkMode.ON -> R.string.display_on
                        DarkMode.OFF -> R.string.display_off
                    },
                ),
            icon = Icons.Outlined.DarkMode,
            onClick = { darkDialog = true },
        )
        SwitchPreference(
            title = stringResource(R.string.display_pure_black),
            description = stringResource(R.string.display_pure_black_summary),
            checked = pureBlack,
            onCheckedChange = setPureBlack,
            enabled = darkMode != DarkMode.OFF,
        )
    }

    if (darkDialog) {
        AlertDialog(
            onDismissRequest = { darkDialog = false },
            title = { Text(stringResource(R.string.display_dark_theme)) },
            text = {
                Column {
                    DarkMode.entries.forEach { mode ->
                        val label =
                            stringResource(
                                when (mode) {
                                    DarkMode.AUTO -> R.string.display_follow_system
                                    DarkMode.ON -> R.string.display_on
                                    DarkMode.OFF -> R.string.display_off
                                },
                            )
                        PreferenceEntry(
                            title = label,
                            onClick = { setDarkMode(mode); darkDialog = false },
                            trailing = { RadioButton(selected = mode == darkMode, onClick = { setDarkMode(mode); darkDialog = false }) },
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { darkDialog = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun SeedSwatch(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val scheme = remember(color, isDark) { dynamicColorScheme(seedColor = color, isDark = isDark, style = PaletteStyle.TonalSpot) }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .size(84.dp)
            .clickable(onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            // four-quadrant swatch: primary / secondary / tertiary / primaryContainer
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .border(if (selected) 2.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()) {
                        Box(Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .background(scheme.primary))
                        Box(Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .background(scheme.secondaryContainer))
                    }
                    Row(modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()) {
                        Box(Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .background(scheme.tertiaryContainer))
                        Box(Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .background(scheme.primaryContainer))
                    }
                }
                if (selected) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(scheme.primary.copy(alpha = 0.55f)),
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null, tint = scheme.onPrimary)
                    }
                }
            }
        }
    }
}

/** Small abstract "trees" scene drawn with the current palette, standing in for the illustration. */
@Composable
private fun ThemePreviewArt() {
    val scheme = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        listOf(
            scheme.secondaryContainer to 64.dp,
            scheme.primary to 96.dp,
            scheme.tertiaryContainer to 72.dp,
            scheme.primaryContainer to 56.dp,
            scheme.errorContainer to 48.dp,
        ).forEach { (color, size) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(color))
                Box(Modifier
                    .size(width = 3.dp, height = 40.dp)
                    .background(scheme.onSurfaceVariant.copy(alpha = 0.6f)))
            }
        }
    }
}
