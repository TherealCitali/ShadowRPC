package dev.citali.shadowrpc.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.citali.shadowrpc.R
import dev.citali.shadowrpc.data.*
import dev.citali.shadowrpc.detection.AppDetectionService
import dev.citali.shadowrpc.presence.PresencePrivacy
import dev.citali.shadowrpc.presence.PresenceManager
import dev.citali.shadowrpc.ui.component.*
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream

@Composable
fun PrivacyConvenienceSettings() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val (clearOnLock, setClearOnLock) = rememberPreference(Prefs.ClearOnLockKey, false)
    val (iconConsent) = rememberPreference(Prefs.IconUploadConsentKey, false)
    var consentDialog by remember { mutableStateOf(false) }
    val (master) = rememberPreference(Prefs.RpcEnabledKey, true)
    val (pauseUntil) = rememberPreference(Prefs.PauseUntilKey, 0L)
    var pauseMenu by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf<Int?>(null) }
    var busy by remember { mutableStateOf(false) }
    val now by produceState(System.currentTimeMillis(), pauseUntil) {
        while (true) { value = System.currentTimeMillis(); delay(1000) }
    }
    fun perform(action: suspend () -> Unit) {
        if (busy) return
        busy = true
        scope.launch {
            try { action() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                status = R.string.convenience_failed
                timber.log.Timber.tag("Settings").w("Privacy/convenience action failed (%s)", error.javaClass.simpleName)
            } finally { busy = false }
        }
    }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) perform {
            withContext(Dispatchers.IO) {
                val text = SettingsBackup.export(context)
                (context.contentResolver.openOutputStream(uri, "wt") ?: error("Cannot open destination"))
                    .bufferedWriter().use { it.write(text) }
            }
            status = R.string.settings_exported
        }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) perform {
            pendingImport = withContext(Dispatchers.IO) {
                val text = (context.contentResolver.openInputStream(uri) ?: error("Cannot open backup")).use { input ->
                    val result = ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        require(result.size() + count <= 1_000_000) { "Backup too large" }
                        result.write(buffer, 0, count)
                    }
                    result.toString("UTF-8")
                }
                SettingsBackup.validate(text)
                text
            }
        }
    }

    PreferenceGroupTitle(stringResource(R.string.privacy_convenience))
    PreferenceCard {
        SwitchPreference(
            title = stringResource(R.string.clear_on_lock),
            description = stringResource(R.string.clear_on_lock_summary),
            checked = clearOnLock,
            onCheckedChange = setClearOnLock,
        )
        SwitchPreference(
            title = stringResource(R.string.icon_upload_permission),
            description = stringResource(R.string.icon_upload_permission_summary),
            checked = iconConsent,
            enabled = !busy,
            onCheckedChange = { allow ->
                if (allow) consentDialog = true else perform {
                    context.setPref(Prefs.IconUploadConsentKey, false)
                    context.setPref(Prefs.AppDetectionShowIconKey, false)
                    PresenceManager.clearForPrivacy()
                }
            },
        )
        PreferenceEntry(
            title = stringResource(R.string.timed_pause),
            description = when {
                pauseUntil == -1L -> stringResource(R.string.pause_manual)
                pauseUntil > now -> stringResource(R.string.pause_remaining, ((pauseUntil - now + 59_999) / 60_000).toInt())
                else -> stringResource(R.string.pause_none)
            },
            enabled = master && !busy,
            onClick = { pauseMenu = true },
        )
        Text(stringResource(R.string.pause_timing_note), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(16.dp))
        if (pauseUntil == -1L || pauseUntil > now) TextButton(enabled = !busy, onClick = {
            perform { PresencePrivacy.resume(context); AppDetectionService.startIfEnabled(context) }
        }) { Text(stringResource(R.string.pause_resume)) }
        PreferenceEntry(title = stringResource(R.string.settings_export), enabled = !busy,
            onClick = { runCatching { exporter.launch("ShadowRPC-settings-${System.currentTimeMillis()}.json") }.onFailure { status = R.string.convenience_failed } })
        PreferenceEntry(title = stringResource(R.string.settings_import), enabled = !busy,
            onClick = { runCatching { importer.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }.onFailure { status = R.string.convenience_failed } })
        Text(stringResource(R.string.settings_backup_summary), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(16.dp))
        PreferenceEntry(title = stringResource(R.string.qs_title), description = stringResource(R.string.qs_help))
        status?.let { Text(stringResource(it), modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.primary) }
    }
    if (consentDialog) IconUploadConsent(
        onAccept = { perform {
            context.setPref(Prefs.IconUploadConsentKey, true)
            context.setPref(Prefs.AppDetectionShowIconKey, true)
            consentDialog = false
        } },
        onDismiss = { consentDialog = false },
    )
    if (pauseMenu) ElasticSheet(onDismissRequest = { pauseMenu = false }, title = { Text(stringResource(R.string.timed_pause)) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(15 to R.string.pause_15, 60 to R.string.pause_60, -1 to R.string.pause_manual).forEach { (minutes, label) ->
                SheetChoice(stringResource(label), false) {
                    pauseMenu = false
                    perform {
                        PresencePrivacy.pause(context, minutes.takeIf { it > 0 })
                        AppDetectionService.startIfEnabled(context)
                    }
                }
            }
        }
    })
    pendingImport?.let { backup ->
        AlertDialog(
            onDismissRequest = { if (!busy) pendingImport = null },
            title = { Text(stringResource(R.string.settings_import)) },
            text = { Text(stringResource(R.string.settings_import_confirm)) },
            confirmButton = { TextButton(enabled = !busy, onClick = {
                perform {
                    withContext(Dispatchers.IO) { SettingsBackup.restore(context, backup) }
                    PresenceManager.clearForPrivacy()
                    pendingImport = null
                    status = R.string.settings_imported
                }
            }) { Text(stringResource(R.string.action_save)) } },
            dismissButton = { TextButton(enabled = !busy, onClick = { pendingImport = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}
