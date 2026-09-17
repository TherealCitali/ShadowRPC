package dev.citali.shadowrpc.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import dev.citali.shadowrpc.BuildConfig
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.Button
import dev.citali.shadowrpc.ui.component.SwitchPreference
import dev.citali.shadowrpc.util.FloatingLogsService
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.citali.shadowrpc.R
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.foundation.layout.fillMaxSize
import dev.citali.shadowrpc.util.InMemoryLogTree

@Composable
fun LogsScreen(onBack: () -> Unit) {
    val lines by InMemoryLogTree.lines.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val verbose by InMemoryLogTree.verbose.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var exporting by remember { mutableStateOf(false) }
    var exportResult by remember { mutableStateOf<Int?>(null) }
    val exportLogs = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) {
            // Snapshot once the user chooses a destination; file I/O stays off the UI thread.
            val snapshot = InMemoryLogTree.lines.value
            val text = "ShadowRPC ${BuildConfig.VERSION_NAME} / Android ${android.os.Build.VERSION.SDK_INT}\n" +
                "Build ${BuildConfig.BUILD_COMMIT} / ${BuildConfig.BUILD_DATE}\nExported ${java.util.Date()} — ${snapshot.size} buffered entries\n\n" +
                snapshot.joinToString("\n") { "${it.time} [${it.priority}] ${it.tag ?: "-"}: ${it.message}" }
            exporting = true
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        val stream = context.contentResolver.openOutputStream(uri, "wt")
                            ?: error("Cannot open export destination")
                        stream.bufferedWriter(Charsets.UTF_8).use { it.write(InMemoryLogTree.redact(text)) }
                    }
                    exportResult = R.string.logs_export_success
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    exportResult = R.string.logs_export_failed
                } finally { exporting = false }
            }
        }
    }
    val floating by FloatingLogsService.running.collectAsStateWithLifecycle()
    val serviceError by FloatingLogsService.error.collectAsStateWithLifecycle()
    var follow by remember { mutableStateOf(true) }
    var overlayError by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) { timber.log.Timber.tag("Logs").i("Live log viewer opened") }
    LaunchedEffect(lines.lastOrNull(), follow) {
        if (follow && lines.isNotEmpty()) listState.scrollToItem(lines.lastIndex)
    }

    Scaffold(
        topBar = { TopAppBar(
        title = { Text(stringResource(R.string.drawer_logs)) },
        navigationIcon = { IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back))
        } },
        actions = {
            IconButton(enabled = !exporting, onClick = {
                runCatching { exportLogs.launch("ShadowRPC-logs-${System.currentTimeMillis()}.txt") }
                    .onFailure { exportResult = R.string.logs_export_failed }
            }) { Icon(Icons.Outlined.FileDownload, stringResource(R.string.logs_export)) }
            IconButton(onClick = {
                val text = lines.joinToString("\n") { "${it.time} ${it.tag ?: "-"}: ${it.message}" }
                context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("ShadowRPC logs", text))
            }) { Icon(Icons.Outlined.ContentCopy, contentDescription = stringResource(R.string.logs_copy)) }
            IconButton(onClick = { dev.citali.shadowrpc.util.CrashDiagnostics.clear(); InMemoryLogTree.clear() }) {
                Icon(Icons.Outlined.DeleteSweep, contentDescription = stringResource(R.string.logs_clear))
            }
        },
        ) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
        SwitchPreference(
            title = stringResource(R.string.logs_follow), checked = follow,
            onCheckedChange = { follow = it },
        )
        SwitchPreference(
            title = stringResource(R.string.logs_verbose), checked = verbose,
            onCheckedChange = { InMemoryLogTree.verbose.value = it },
        )
        exportResult?.let { Text(stringResource(it), style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp)) }
        Button(
            modifier = Modifier.padding(horizontal = 16.dp),
            onClick = {
                runCatching {
                    when {
                        floating -> FloatingLogsService.stop(context)
                        Settings.canDrawOverlays(context) -> FloatingLogsService.start(context)
                        else -> context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
                    }
                }.onFailure { overlayError = true }
            },
        ) { Text(stringResource(if (floating) R.string.floating_logs_stop else R.string.floating_logs_start)) }
        Text(stringResource(R.string.logs_overlay_help), style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        if (overlayError || serviceError) Text(stringResource(R.string.floating_logs_error), color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(horizontal = 16.dp))
        LazyColumn(state = listState, modifier = Modifier.fillMaxWidth().weight(1f)) {
        if (lines.isEmpty()) {
            item {
            Text(
                stringResource(R.string.logs_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
            }
        } else {
                items(lines) { line ->
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        Text(
                            "${line.time}  ${line.tag ?: ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color =
                                when (line.priority) {
                                    Log.ERROR -> MaterialTheme.colorScheme.error
                                    Log.WARN -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                        )
                        Text(line.message, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}
}
