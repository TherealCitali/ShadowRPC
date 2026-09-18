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
import dev.citali.shadowrpc.ui.theme.manga.MangaAccent

@Composable
fun ThemePresetSettings() {
    val (preset, setPreset) = rememberEnumPreference(Prefs.ThemePresetKey, ThemePreset.MATERIAL_YOU)
    val (paper, setPaper) = rememberEnumPreference(Prefs.MangaPaperKey, MangaPaperMode.AUTO)
    val (accent, setAccent) = rememberEnumPreference(Prefs.MangaAccentKey, MangaAccent.CRIMSON)
    val (decor, setDecor) = rememberPreference(Prefs.ThemeDecorationsKey, true)
    val (monet, setMonet) = rememberPreference(Prefs.MiuixMonetKey, false)
    var paperDialog by remember { mutableStateOf(false) }
    var accentDialog by remember { mutableStateOf(false) }
    PreferenceGroupTitle(stringResource(R.string.theme_preset))
    PreferenceCard {
        ThemePreset.entries.forEach { choice ->
            PreferenceEntry(title = stringResource(presetLabel(choice)),
                description = stringResource(when (choice) {
                    ThemePreset.MATERIAL_YOU -> R.string.theme_material_summary
                    ThemePreset.MANGA -> R.string.theme_manga_summary
                    ThemePreset.MIUI -> R.string.theme_miui_summary
                }),
                onClick = { setPreset(choice) },
                trailing = { RadioButton(selected = preset == choice, onClick = { setPreset(choice) }) })
        }
    }
    if (preset == ThemePreset.MANGA) {
        PreferenceGroupTitle(stringResource(R.string.theme_manga_options))
        PreferenceCard {
            PreferenceEntry(title = stringResource(R.string.theme_paper), description = stringResource(paperLabel(paper)), onClick = { paperDialog = true })
            PreferenceEntry(title = stringResource(R.string.theme_accent), description = stringResource(accentLabel(accent)), onClick = { accentDialog = true })
            SwitchPreference(title = stringResource(R.string.theme_decorations), description = stringResource(R.string.theme_decorations_summary), checked = decor, onCheckedChange = setDecor)
        }
    }
    if (preset == ThemePreset.MIUI) {
        PreferenceGroupTitle(stringResource(R.string.theme_miui_options))
        PreferenceCard {
            SwitchPreference(title = stringResource(R.string.theme_miui_monet), description = stringResource(R.string.theme_miui_monet_summary), checked = monet, onCheckedChange = setMonet)
        }
    }
    if (paperDialog) ElasticSheet(onDismissRequest = { paperDialog = false }, title = { Text(stringResource(R.string.theme_paper)) }, text = {
        Column { MangaPaperMode.entries.forEach { choice -> SheetChoice(stringResource(paperLabel(choice)), paper == choice) { setPaper(choice); paperDialog = false } } }
    })
    if (accentDialog) ElasticSheet(onDismissRequest = { accentDialog = false }, title = { Text(stringResource(R.string.theme_accent)) }, text = {
        Column { MangaAccent.entries.forEach { choice -> SheetChoice(stringResource(accentLabel(choice)), accent == choice) { setAccent(choice); accentDialog = false } } }
    })
}

private fun presetLabel(value: ThemePreset) = when (value) {
    ThemePreset.MATERIAL_YOU -> R.string.theme_material
    ThemePreset.MANGA -> R.string.theme_manga
    ThemePreset.MIUI -> R.string.theme_miui
}
private fun paperLabel(value: MangaPaperMode) = when (value) {
    MangaPaperMode.AUTO -> R.string.theme_paper_auto
    MangaPaperMode.DAY -> R.string.theme_paper_day
    MangaPaperMode.NIGHT -> R.string.theme_paper_night
    MangaPaperMode.NORD -> R.string.theme_paper_nord
}
private fun accentLabel(value: MangaAccent) = when (value) {
    MangaAccent.MONO -> R.string.theme_accent_mono
    MangaAccent.CRIMSON -> R.string.theme_accent_crimson
    MangaAccent.COBALT -> R.string.theme_accent_cobalt
    MangaAccent.SUN -> R.string.theme_accent_sun
    MangaAccent.FROST -> R.string.theme_accent_frost
}
