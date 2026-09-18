package dev.citali.shadowrpc.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import dev.citali.shadowrpc.R
import dev.citali.shadowrpc.data.*
import dev.citali.shadowrpc.ui.component.*
import dev.citali.shadowrpc.ui.theme.*

@Composable
fun ThemePresetSettings() {
    val (preset, setPreset) = rememberThemePreset()
    val (monet, setMonet) = rememberPreference(Prefs.MiuixMonetKey, false)
    PreferenceGroupTitle(stringResource(R.string.theme_preset))
    PreferenceCard {
        ThemePreset.entries.forEach { choice ->
            PreferenceEntry(title = stringResource(presetLabel(choice)),
                description = stringResource(when (choice) {
                    ThemePreset.MATERIAL_YOU -> R.string.theme_material_summary
                    ThemePreset.MIUI -> R.string.theme_miui_summary
                }),
                onClick = { setPreset(choice) },
                trailing = { RadioButton(selected = preset == choice, onClick = { setPreset(choice) }) })
        }
    }
    if (preset == ThemePreset.MIUI) {
        PreferenceGroupTitle(stringResource(R.string.theme_miui_options))
        PreferenceCard {
            SwitchPreference(title = stringResource(R.string.theme_miui_monet), description = stringResource(R.string.theme_miui_monet_summary), checked = monet, onCheckedChange = setMonet)
        }
    }
}

private fun presetLabel(value: ThemePreset) = when (value) {
    ThemePreset.MATERIAL_YOU -> R.string.theme_material
    ThemePreset.MIUI -> R.string.theme_miui
}
