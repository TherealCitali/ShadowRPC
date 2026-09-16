package dev.citali.shadowrpc.ui.screens

import android.content.Intent
import android.graphics.drawable.Drawable
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.citali.shadowrpc.R
import dev.citali.shadowrpc.data.Prefs
import dev.citali.shadowrpc.data.rememberPreference
import dev.citali.shadowrpc.detection.AppDetectionService
import dev.citali.shadowrpc.detection.AppLabels
import dev.citali.shadowrpc.detection.ForegroundAppDetector
import dev.citali.shadowrpc.detection.InstalledApp
import dev.citali.shadowrpc.detection.InstalledApps
import dev.citali.shadowrpc.discord.DiscordOAuthRepository
import dev.citali.shadowrpc.ui.component.MasterSwitchCard
import dev.citali.shadowrpc.ui.component.ScreenScaffold
import dev.citali.shadowrpc.ui.component.SwitchPreference
import kotlinx.coroutines.launch

@Composable
fun AppDetectionScreen(
    onBack: () -> Unit,
    onNeedLogin: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    val (enabled, setEnabled) = rememberPreference(Prefs.AppDetectionEnabledKey, false)
    val (watched, setWatched) = rememberPreference(Prefs.AppDetectionPackagesKey, emptySet())
    val (showIcon, setShowIcon) = rememberPreference(Prefs.AppDetectionShowIconKey, false)
    val (timestamps, setTimestamps) = rememberPreference(Prefs.AppDetectionTimestampsKey, true)

    var hasUsageAccess by remember { mutableStateOf(ForegroundAppDetector.hasUsageAccess(context)) }
    // Re-check when the user comes back from the system Usage access page.
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) hasUsageAccess = ForegroundAppDetector.hasUsageAccess(context)
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val (overridesJson) = rememberPreference(Prefs.AppLabelOverridesKey, "")
    val overrides = remember(overridesJson) { AppLabels.parse(overridesJson) }
    var renaming by remember { mutableStateOf<InstalledApp?>(null) }

    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val apps by produceState<List<InstalledApp>>(emptyList()) { value = InstalledApps.load(context) }
    val filtered =
        remember(apps, query) {
            if (query.isBlank()) {
                apps
            } else {
                apps.filter {
                    it.label.contains(query, true) ||
                        it.packageName.contains(query, true) ||
                        overrides[it.packageName]?.contains(query, true) == true
                }
            }
        }

    fun toggleEnabled(wanted: Boolean) {
        if (!wanted) {
            setEnabled(false)
            AppDetectionService.stop(context)
            return
        }
        if (!hasUsageAccess) return
        scope.launch {
            val token = DiscordOAuthRepository.getValidAccessToken(context)
            if (token.isNullOrBlank()) {
                onNeedLogin()
            } else {
                setEnabled(true)
                AppDetectionService.start(context)
            }
        }
    }

    ScreenScaffold(
        title = stringResource(R.string.feature_app_detection),
        onBack = onBack,
        scrollable = false,
        actions = {
            IconButton(onClick = { searching = !searching; if (!searching) query = "" }) {
                Icon(if (searching) Icons.Rounded.Close else Icons.Rounded.Search, contentDescription = stringResource(R.string.app_detection_search))
            }
        },
    ) {
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            item {
                MasterSwitchCard(
                    title = stringResource(R.string.app_detection_enable),
                    checked = enabled,
                    onCheckedChange = { toggleEnabled(it) },
                    enabled = hasUsageAccess,
                )
                Spacer(Modifier.height(8.dp))
            }

            if (!hasUsageAccess) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(stringResource(R.string.app_detection_permission_title), style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(6.dp))
                            Text(stringResource(R.string.app_detection_permission_body), style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.height(14.dp))
                            Button(onClick = {
                                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            }) { Text(stringResource(R.string.app_detection_grant)) }
                        }
                    }
                }
            }

            item {
                SwitchPreference(
                    title = stringResource(R.string.app_detection_show_icon),
                    description = stringResource(R.string.app_detection_show_icon_summary),
                    icon = Icons.Outlined.Apps,
                    checked = showIcon,
                    onCheckedChange = setShowIcon,
                )
                SwitchPreference(
                    title = stringResource(R.string.app_detection_timestamps),
                    description = stringResource(R.string.app_detection_timestamps_summary),
                    icon = Icons.Outlined.Timer,
                    checked = timestamps,
                    onCheckedChange = setTimestamps,
                )
            }

            if (searching) {
                item {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        placeholder = { Text(stringResource(R.string.app_detection_search)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }

            if (filtered.isEmpty() && apps.isNotEmpty()) {
                item {
                    Text(
                        stringResource(R.string.app_detection_no_apps),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }

            items(filtered, key = { it.packageName }) { app ->
                AppRow(
                    app = app,
                    displayName = overrides[app.packageName],
                    checked = app.packageName in watched,
                    onCheckedChange = { on ->
                        setWatched(if (on) watched + app.packageName else watched - app.packageName)
                    },
                    onLongPress = { renaming = app },
                )
            }
        }
    }

    renaming?.let { app ->
        var draft by remember(app) { mutableStateOf(overrides[app.packageName].orEmpty()) }
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text(stringResource(R.string.app_detection_rename)) },
            text = {
                Column {
                    Text(app.label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        singleLine = true,
                        placeholder = { Text(app.label) },
                        supportingText = { Text(stringResource(R.string.app_detection_rename_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { AppLabels.setOverride(context, app.packageName, draft) }
                    renaming = null
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun AppRow(
    app: InstalledApp,
    displayName: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onLongPress: () -> Unit,
) {
    val context = LocalContext.current
    val icon: Drawable? = remember(app.packageName) { InstalledApps.icon(context, app.packageName) }
    val bitmap = remember(icon) { icon?.toBitmap(96, 96)?.asImageBitmap() }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = { onCheckedChange(!checked) }, onLongClick = onLongPress)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        if (bitmap != null) {
            Image(bitmap = bitmap, contentDescription = null, modifier = Modifier
                .size(48.dp)
                .clip(CircleShape))
        } else {
            Spacer(Modifier.size(48.dp))
        }
        Spacer(Modifier.width(18.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(displayName ?: app.label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Normal)
            Text(
                if (displayName != null) "${app.label} · ${app.packageName}" else app.packageName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
