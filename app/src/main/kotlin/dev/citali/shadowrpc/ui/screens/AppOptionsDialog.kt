package dev.citali.shadowrpc.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
import dev.citali.shadowrpc.R
import dev.citali.shadowrpc.detection.InstalledApp
import dev.citali.shadowrpc.detection.InstalledApps
import dev.citali.shadowrpc.presence.*
import dev.citali.shadowrpc.ui.component.SwitchPreference
import kotlinx.coroutines.launch

/** Draft-based app editor: closing or cancelling never changes saved settings. */
@Composable
fun AppOptionsDialog(app: InstalledApp, initialLabel: String, initiallyEnabled: Boolean, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Wait for the stored value rather than seeding editable state from a loading default.
    val loaded by produceState<AppPresenceOverride?>(null, app.packageName) {
        value = AppPresenceOverrides.load(context, app.packageName)
    }
    val initial = loaded ?: return
    var draft by remember(app.packageName, initial) { mutableStateOf(initial) }
    var label by remember(app.packageName) { mutableStateOf(initialLabel) }
    var enabled by remember(app.packageName) { mutableStateOf(initiallyEnabled) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    val bitmap = remember(app.packageName) { InstalledApps.icon(context, app.packageName)?.toBitmap(128, 128)?.asImageBitmap() }
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.88f).dp
    val inherit = stringResource(R.string.app_options_inherit)

    Dialog(onDismissRequest = { if (!saving) onDismiss() }) {
        Surface(shape = RoundedCornerShape(32.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
            Column(Modifier.fillMaxWidth().heightIn(max = maxHeight).imePadding()) {
                Surface(shape = RoundedCornerShape(32.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        bitmap?.let { Image(it, null, Modifier.size(52.dp).clip(RoundedCornerShape(18.dp))) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.app_options), style = MaterialTheme.typography.headlineSmall)
                            Text(label.ifBlank { app.label }, style = MaterialTheme.typography.bodyMedium)
                        }
                        IconButton(onClick = onDismiss, enabled = !saving) {
                            Icon(Icons.Rounded.Close, stringResource(R.string.action_cancel))
                        }
                    }
                }
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(R.string.app_options_hint), style = MaterialTheme.typography.bodyMedium)
                    OptionCard {
                        SwitchPreference(stringResource(R.string.app_options_share), enabled, { enabled = it }, enabled = !saving)
                    }
                    OutlinedTextField(
                        value = label, onValueChange = { label = it }, enabled = !saving,
                        label = { Text(stringResource(R.string.app_detection_rename)) },
                        placeholder = { Text(app.label) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    )
                    OptionPicker(
                        title = stringResource(R.string.settings_activity_type), value = draft.type,
                        choices = listOf(null to inherit) + listOf(
                            "PLAYING" to stringResource(R.string.activity_type_playing),
                            "LISTENING" to stringResource(R.string.activity_type_listening),
                            "WATCHING" to stringResource(R.string.activity_type_watching),
                            "COMPETING" to stringResource(R.string.activity_type_competing),
                        ), enabled = !saving, onSelect = { draft = draft.copy(type = it) },
                    )
                    LineOption(stringResource(R.string.settings_activity_name), draft.name, draft.nameText, false, !saving) { source, text ->
                        draft = draft.copy(name = source, nameText = text)
                    }
                    LineOption(stringResource(R.string.settings_activity_details), draft.details, draft.detailsText, true, !saving) { source, text ->
                        draft = draft.copy(details = source, detailsText = text)
                    }
                    LineOption(stringResource(R.string.settings_activity_state), draft.state, draft.stateText, true, !saving) { source, text ->
                        draft = draft.copy(state = source, stateText = text)
                    }
                    TextButton(enabled = !saving, onClick = { draft = AppPresenceOverride(); label = "" }) {
                        Text(stringResource(R.string.app_options_reset))
                    }
                    if (error) Text(stringResource(R.string.app_options_save_error), color = MaterialTheme.colorScheme.error)
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss, enabled = !saving) { Text(stringResource(R.string.action_cancel)) }
                    Button(enabled = !saving, onClick = {
                        saving = true
                        scope.launch {
                            try {
                                AppPresenceOverrides.save(context, app.packageName, label, enabled, draft)
                                onDismiss()
                            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                                error = true
                            } finally { saving = false }
                        }
                    }) { Text(stringResource(R.string.action_save)) }
                }
            }
        }
    }
}

@Composable
private fun OptionCard(content: @Composable () -> Unit) {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) { content() }
}

@Composable
private fun OptionPicker(title: String, value: String?, choices: List<Pair<String?, String>>, enabled: Boolean, onSelect: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Surface(onClick = { expanded = true }, enabled = enabled, shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(choices.firstOrNull { it.first == value }?.second.orEmpty(), color = MaterialTheme.colorScheme.primary)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            choices.forEach { (key, title) ->
                DropdownMenuItem(text = { Text(title) }, onClick = { expanded = false; onSelect(key) })
            }
        }
    }
}

@Composable
private fun LineOption(title: String, source: ActivitySource?, text: String, allowNone: Boolean, enabled: Boolean, onChange: (ActivitySource?, String) -> Unit) {
    val choices = listOf(null to stringResource(R.string.app_options_inherit)) + ActivitySource.entries
        .filter { allowNone || it != ActivitySource.NONE }
        .map { it.name to stringResource(when (it) {
            ActivitySource.APP -> R.string.activity_source_app
            ActivitySource.SHADOWRPC -> R.string.app_name
            ActivitySource.PACKAGE -> R.string.activity_source_package
            ActivitySource.CATEGORY -> R.string.activity_source_category
            ActivitySource.CUSTOM -> R.string.activity_source_custom
            ActivitySource.NONE -> R.string.activity_source_none
        }) }
    OptionPicker(title, source?.name, choices, enabled) { key ->
        onChange(key?.let(ActivitySource::valueOf), if (key == ActivitySource.CUSTOM.name) text else "")
    }
    if (source == ActivitySource.CUSTOM) {
        OutlinedTextField(value = text, onValueChange = { onChange(source, it) }, enabled = enabled,
            label = { Text(title) }, supportingText = { Text(stringResource(R.string.activity_custom_help)) },
            singleLine = true, modifier = Modifier.fillMaxWidth())
    }
}
