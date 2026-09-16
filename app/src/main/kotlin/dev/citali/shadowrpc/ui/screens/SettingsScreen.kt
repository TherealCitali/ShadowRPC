package dev.citali.shadowrpc.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DoNotDisturbOn
import androidx.compose.material.icons.outlined.HighQuality
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.Pin
import androidx.compose.material.icons.outlined.ShortText
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.citali.shadowrpc.R
import dev.citali.shadowrpc.data.Prefs
import dev.citali.shadowrpc.data.rememberEnumPreference
import dev.citali.shadowrpc.data.rememberPreference
import dev.citali.shadowrpc.detection.IconHost
import dev.citali.shadowrpc.presence.ActivityContent
import dev.citali.shadowrpc.presence.ActivitySource
import dev.citali.shadowrpc.presence.ActivitySubject
import dev.citali.shadowrpc.presence.ActivityTemplate
import dev.citali.shadowrpc.ui.component.ElasticSheet
import dev.citali.shadowrpc.ui.component.SheetChoice
import androidx.compose.foundation.layout.Arrangement
import dev.citali.shadowrpc.ui.component.PreferenceCard
import dev.citali.shadowrpc.ui.component.PreferenceEntry
import dev.citali.shadowrpc.ui.component.PreferenceGroupTitle
import dev.citali.shadowrpc.ui.component.PresencePreview
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

private val activityStatuses =
    listOf(
        "online" to R.string.activity_status_online,
        "idle" to R.string.activity_status_idle,
        "dnd" to R.string.activity_status_dnd,
    )

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    ScreenScaffold(title = stringResource(R.string.drawer_settings), onBack = onBack) {
        PresenceSettingsContent()
    }
}

/** Shared by Home and Settings so edits and preview use the same preferences. */
@Composable
fun PresenceSettingsContent(
    showAdvanced: Boolean = true,
    afterActivityContent: @Composable () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val (activityType, setActivityType) = rememberPreference(Prefs.ActivityTypeKey, "PLAYING")
    val (activityStatus, setActivityStatus) = rememberPreference(Prefs.ActivityStatusKey, "online")
    val (customAppId, setCustomAppId) = rememberPreference(Prefs.CustomApplicationIdKey, "")
    val (lowRes, setLowRes) = rememberPreference(Prefs.LowResolutionImagesKey, false)
    val (timestamps) = rememberPreference(Prefs.AppDetectionTimestampsKey, true)

    val (nameSource, setNameSource) = rememberEnumPreference(Prefs.ActivityNameSourceKey, ActivitySource.APP)
    val (detailsSource, setDetailsSource) = rememberEnumPreference(Prefs.ActivityDetailsSourceKey, ActivitySource.APP)
    val (stateSource, setStateSource) = rememberEnumPreference(Prefs.ActivityStateSourceKey, ActivitySource.NONE)
    val (nameCustom, setNameCustom) = rememberPreference(Prefs.ActivityNameCustomKey, "")
    val (detailsCustom, setDetailsCustom) = rememberPreference(Prefs.ActivityDetailsCustomKey, "")
    val (stateCustom, setStateCustom) = rememberPreference(Prefs.ActivityStateCustomKey, "")
    var editingLine by remember { mutableStateOf<ActivityLine?>(null) }

    val appName = stringResource(R.string.app_name)
    val sampleSubject =
        ActivitySubject(
            appLabel = stringResource(R.string.settings_preview_sample_app),
            packageName = "com.example.game",
            category = stringResource(R.string.settings_preview_sample_category),
        )
    val preview =
        remember(nameSource, detailsSource, stateSource, nameCustom, detailsCustom, stateCustom, sampleSubject, appName) {
            ActivityTemplate.resolve(
                ActivityContent(nameSource, detailsSource, stateSource, nameCustom, detailsCustom, stateCustom),
                sampleSubject,
                appName,
            )
        }

    var typeDialog by remember { mutableStateOf(false) }
    var statusDialog by remember { mutableStateOf(false) }
    var appIdDialog by remember { mutableStateOf(false) }

    Column {
        PreferenceGroupTitle(stringResource(R.string.settings_general))
        PreferenceCard {
            PreferenceEntry(
                title = stringResource(R.string.settings_activity_type),
                pillDescription = true,
                description = stringResource(activityTypes.first { it.first == activityType }.second),
                icon = Icons.Outlined.Code,
                onClick = { typeDialog = true },
            )
            PreferenceEntry(
                title = stringResource(R.string.settings_activity_status),
                pillDescription = true,
                description = stringResource(activityStatuses.first { it.first == activityStatus }.second),
                icon = Icons.Outlined.DoNotDisturbOn,
                onClick = { statusDialog = true },
            )
            PreferenceEntry(
                title = stringResource(R.string.settings_application_id),
                pillDescription = true,
                description = customAppId.ifBlank { stringResource(R.string.settings_application_id_summary) },
                icon = Icons.Outlined.Pin,
                onClick = { appIdDialog = true },
            )
        }
        PreferenceGroupTitle(stringResource(R.string.settings_activity_content))
        PreferenceCard {
            Text(
                text = stringResource(R.string.settings_activity_content_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            PreferenceEntry(
                title = stringResource(R.string.settings_activity_name),
                pillDescription = true,
                description = sourceLabel(nameSource, nameCustom),
                icon = Icons.Outlined.Badge,
                onClick = { editingLine = ActivityLine.NAME },
            )
            PreferenceEntry(
                title = stringResource(R.string.settings_activity_details),
                pillDescription = true,
                description = sourceLabel(detailsSource, detailsCustom),
                icon = Icons.Outlined.Notes,
                onClick = { editingLine = ActivityLine.DETAILS },
            )
            PreferenceEntry(
                title = stringResource(R.string.settings_activity_state),
                pillDescription = true,
                description = sourceLabel(stateSource, stateCustom),
                icon = Icons.Outlined.ShortText,
                onClick = { editingLine = ActivityLine.STATE },
            )
            PresencePreview(
                activityTypeLabel = stringResource(activityTypes.first { it.first == activityType }.second),
                name = preview.name,
                details = preview.details,
                state = preview.state,
                showTimestamp = timestamps,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            )
        }
        afterActivityContent()
        if (showAdvanced) {
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
    if (statusDialog) {
        ChoiceDialog(
            title = stringResource(R.string.settings_activity_status),
            options = activityStatuses.map { it.first to stringResource(it.second) },
            selected = activityStatus,
            onSelect = { setActivityStatus(it); statusDialog = false },
            onDismiss = { statusDialog = false },
        )
    }
    editingLine?.let { line ->
        val (source, template) =
            when (line) {
                ActivityLine.NAME -> nameSource to nameCustom
                ActivityLine.DETAILS -> detailsSource to detailsCustom
                ActivityLine.STATE -> stateSource to stateCustom
            }
        ActivityLineDialog(
            title =
                stringResource(
                    when (line) {
                        ActivityLine.NAME -> R.string.settings_activity_name
                        ActivityLine.DETAILS -> R.string.settings_activity_details
                        ActivityLine.STATE -> R.string.settings_activity_state
                    },
                ),
            allowNone = line != ActivityLine.NAME,
            source = source,
            template = template,
            sampleSubject = sampleSubject,
            appName = appName,
            onDismiss = { editingLine = null },
            onSave = { newSource, newTemplate ->
                when (line) {
                    ActivityLine.NAME -> { setNameSource(newSource); setNameCustom(newTemplate) }
                    ActivityLine.DETAILS -> { setDetailsSource(newSource); setDetailsCustom(newTemplate) }
                    ActivityLine.STATE -> { setStateSource(newSource); setStateCustom(newTemplate) }
                }
                editingLine = null
            },
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
    ElasticSheet(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                options.forEach { (value, label) ->
                    SheetChoice(label, value == selected) { onSelect(value) }
                }
            }
        },
    )
}

private enum class ActivityLine { NAME, DETAILS, STATE }

@Composable
private fun sourceLabel(
    source: ActivitySource,
    template: String,
): String =
    when (source) {
        ActivitySource.APP -> stringResource(R.string.activity_source_app)
        ActivitySource.SHADOWRPC -> stringResource(R.string.app_name)
        ActivitySource.PACKAGE -> stringResource(R.string.activity_source_package)
        ActivitySource.CATEGORY -> stringResource(R.string.activity_source_category)
        ActivitySource.CUSTOM -> template.ifBlank { stringResource(R.string.activity_source_custom) }
        ActivitySource.NONE -> stringResource(R.string.activity_source_none)
    }

@Composable
private fun ActivityLineDialog(
    title: String,
    allowNone: Boolean,
    source: ActivitySource,
    template: String,
    sampleSubject: ActivitySubject,
    appName: String,
    onDismiss: () -> Unit,
    onSave: (ActivitySource, String) -> Unit,
) {
    var draftSource by remember { mutableStateOf(source) }
    var draftTemplate by remember { mutableStateOf(template) }
    val options = ActivitySource.entries.filter { allowNone || it != ActivitySource.NONE }
    val livePreview =
        ActivityTemplate.render(draftSource, draftTemplate, sampleSubject, appName)
            ?: stringResource(R.string.activity_source_none)

    ElasticSheet(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                options.forEach { option ->
                    SheetChoice(sourceLabel(option, ""), option == draftSource) {
                        draftSource = option
                    }
                }
                if (draftSource == ActivitySource.CUSTOM) {
                    OutlinedTextField(
                        value = draftTemplate,
                        onValueChange = { draftTemplate = it },
                        singleLine = true,
                        label = { Text(stringResource(R.string.activity_source_custom)) },
                        placeholder = { Text(stringResource(R.string.activity_custom_placeholder)) },
                        supportingText = { Text(stringResource(R.string.activity_custom_help)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    )
                }
                Text(
                    text = stringResource(R.string.settings_preview_line, livePreview),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp, start = 4.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(draftSource, draftTemplate.trim()) }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
