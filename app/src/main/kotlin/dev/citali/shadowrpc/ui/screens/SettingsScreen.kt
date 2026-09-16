package dev.citali.shadowrpc.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DoNotDisturbOn
import androidx.compose.material.icons.outlined.HighQuality
import androidx.compose.material.icons.outlined.Pin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.citali.shadowrpc.R
import dev.citali.shadowrpc.data.Prefs
import dev.citali.shadowrpc.data.rememberPreference
import dev.citali.shadowrpc.detection.IconHost
import dev.citali.shadowrpc.ui.component.PreferenceEntry
import dev.citali.shadowrpc.ui.component.PreferenceGroupTitle
import dev.citali.shadowrpc.ui.component.ScreenScaffold
import dev.citali.shadowrpc.ui.component.SwitchPreference
import kotlinx.coroutines.launch

private val activityTypes =
    listOf(
        "PLAYING" to R.string.activity_type_playing,
        "LISTENING" to R.string.activity_type_listening,
        "WATCHING" to R.string.activity_type_watching,
        "COMPETING" to R.string.activity_type_competing,
    )

private val activityNameModes =
    listOf(
        "APP" to R.string.activity_name_app,
        "SHADOWRPC" to R.string.activity_name_shadowrpc,
    )

private val activityStatuses =
    listOf(
        "online" to R.string.activity_status_online,
        "idle" to R.string.activity_status_idle,
        "dnd" to R.string.activity_status_dnd,
    )

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val (activityType, setActivityType) = rememberPreference(Prefs.ActivityTypeKey, "PLAYING")
    val (activityStatus, setActivityStatus) = rememberPreference(Prefs.ActivityStatusKey, "online")
    val (nameMode, setNameMode) = rememberPreference(Prefs.ActivityNameModeKey, "APP")
    val (customAppId, setCustomAppId) = rememberPreference(Prefs.CustomApplicationIdKey, "")
    val (lowRes, setLowRes) = rememberPreference(Prefs.LowResolutionImagesKey, false)

    var typeDialog by remember { mutableStateOf(false) }
    var statusDialog by remember { mutableStateOf(false) }
    var nameDialog by remember { mutableStateOf(false) }
    var appIdDialog by remember { mutableStateOf(false) }

    ScreenScaffold(title = stringResource(R.string.drawer_settings), onBack = onBack) {
        PreferenceGroupTitle(stringResource(R.string.settings_general))
        PreferenceEntry(
            title = stringResource(R.string.settings_activity_type),
            description = stringResource(activityTypes.first { it.first == activityType }.second),
            icon = Icons.Outlined.Code,
            onClick = { typeDialog = true },
        )
        PreferenceEntry(
            title = stringResource(R.string.settings_activity_name),
            description = stringResource(activityNameModes.first { it.first == nameMode }.second),
            icon = Icons.Outlined.Badge,
            onClick = { nameDialog = true },
        )
        PreferenceEntry(
            title = stringResource(R.string.settings_activity_status),
            description = stringResource(activityStatuses.first { it.first == activityStatus }.second),
            icon = Icons.Outlined.DoNotDisturbOn,
            onClick = { statusDialog = true },
        )
        PreferenceEntry(
            title = stringResource(R.string.settings_application_id),
            description = customAppId.ifBlank { stringResource(R.string.settings_application_id_summary) },
            icon = Icons.Outlined.Pin,
            onClick = { appIdDialog = true },
        )

        PreferenceGroupTitle(stringResource(R.string.settings_advanced))
        SwitchPreference(
            title = stringResource(R.string.settings_low_res_images),
            description = stringResource(R.string.settings_low_res_images_summary),
            icon = Icons.Outlined.HighQuality,
            checked = lowRes,
            onCheckedChange = setLowRes,
        )
        PreferenceEntry(
            title = stringResource(R.string.settings_clear_icon_cache),
            description = stringResource(R.string.settings_clear_icon_cache_summary),
            icon = Icons.Outlined.DeleteForever,
            onClick = { scope.launch { IconHost.clearCache(context) } },
        )
    }

    if (typeDialog) {
        ChoiceDialog(
            title = stringResource(R.string.settings_activity_type),
            options = activityTypes.map { it.first to stringResource(it.second) },
            selected = activityType,
            onSelect = { setActivityType(it); typeDialog = false },
            onDismiss = { typeDialog = false },
        )
    }
    if (nameDialog) {
        ChoiceDialog(
            title = stringResource(R.string.settings_activity_name),
            options = activityNameModes.map { it.first to stringResource(it.second) },
            selected = nameMode,
            onSelect = { setNameMode(it); nameDialog = false },
            onDismiss = { nameDialog = false },
        )
    }
    if (statusDialog) {
        ChoiceDialog(
            title = stringResource(R.string.settings_activity_status),
            options = activityStatuses.map { it.first to stringResource(it.second) },
            selected = activityStatus,
            onSelect = { setActivityStatus(it); statusDialog = false },
            onDismiss = { statusDialog = false },
        )
    }
    if (appIdDialog) {
        var draft by remember { mutableStateOf(customAppId) }
        AlertDialog(
            onDismissRequest = { appIdDialog = false },
            title = { Text(stringResource(R.string.settings_application_id)) },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it.filter(Char::isDigit) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = { setCustomAppId(draft.trim()); appIdDialog = false }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = { TextButton(onClick = { appIdDialog = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun ChoiceDialog(
    title: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { (value, label) ->
                    PreferenceEntry(
                        title = label,
                        onClick = { onSelect(value) },
                        trailing = { RadioButton(selected = value == selected, onClick = { onSelect(value) }) },
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
        modifier = Modifier.padding(0.dp),
    )
}
