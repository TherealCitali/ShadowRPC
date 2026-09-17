package dev.citali.shadowrpc.ui.screens

import android.content.Intent
import android.graphics.drawable.Drawable
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Slider
import kotlin.math.roundToInt
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.citali.shadowrpc.R
import dev.citali.shadowrpc.data.Prefs
import dev.citali.shadowrpc.data.rememberPreference
import dev.citali.shadowrpc.data.setPref
import dev.citali.shadowrpc.detection.AppDetectionService
import dev.citali.shadowrpc.detection.AppLabels
import dev.citali.shadowrpc.detection.ForegroundAppDetector
import dev.citali.shadowrpc.detection.InstalledApp
import dev.citali.shadowrpc.detection.InstalledApps
import dev.citali.shadowrpc.discord.DiscordOAuthRepository
import dev.citali.shadowrpc.ui.component.ExpressiveSwitch
import dev.citali.shadowrpc.ui.component.GroupedPreferenceCard
import dev.citali.shadowrpc.ui.component.PreferenceGroupTitle
import dev.citali.shadowrpc.ui.component.SwitchPreference
import kotlinx.coroutines.launch

@Composable
fun HomeAppList(
    onNeedLogin: () -> Unit,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    val (enabled) = rememberPreference(Prefs.AppDetectionEnabledKey, false)
    val (watched, setWatched) = rememberPreference(Prefs.AppDetectionPackagesKey, emptySet())
    val (showIcon, setShowIcon) = rememberPreference(Prefs.AppDetectionShowIconKey, false)
    val (timestamps, setTimestamps) = rememberPreference(Prefs.AppDetectionTimestampsKey, true)

    val (graceSeconds, setGraceSeconds) = rememberPreference(Prefs.BackgroundGraceSecondsKey, Prefs.DefaultGraceSeconds)
    var graceDraft by remember(graceSeconds) { mutableStateOf(graceSeconds.coerceIn(0, Prefs.MaxGraceSeconds).toFloat()) }

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

    var query by rememberSaveable { mutableStateOf("") }
    val apps by produceState<List<InstalledApp>>(emptyList()) { value = InstalledApps.load(context) }
    val filtered =
        remember(apps, query, overrides) {
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

    var changingDetection by remember { mutableStateOf(false) }
    fun toggleEnabled(wanted: Boolean) {
        if (changingDetection || (wanted && !hasUsageAccess)) return
        changingDetection = true
        scope.launch {
            try {
                if (wanted && DiscordOAuthRepository.getValidAccessToken(context).isNullOrBlank()) {
                    onNeedLogin()
                    return@launch
                }
                // Commit before starting/stopping: the service reads this on its first poll.
                context.setPref(Prefs.AppDetectionEnabledKey, wanted)
                if (wanted) AppDetectionService.startIfEnabled(context) else AppDetectionService.stop(context)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                timber.log.Timber.tag("DetectionSettings").e(error, "Could not change detection setting")
            } finally {
                changingDetection = false
            }
        }
    }

    LazyColumn(modifier = modifier, contentPadding = PaddingValues(bottom = 32.dp)) {
        item(key = "home_header") { Column { header() } }
        item(key = "detection_controls") {
            Column {
                PreferenceGroupTitle(stringResource(R.string.feature_app_detection))
                GroupedPreferenceCard(first = true) {
                    SwitchPreference(
                        title = stringResource(R.string.app_detection_enable),
                        checked = enabled,
                        onCheckedChange = ::toggleEnabled,
                        enabled = !changingDetection && (enabled || hasUsageAccess),
                        icon = Icons.Outlined.Apps,
                    )
                }
                GroupedPreferenceCard {
                    SwitchPreference(
                        title = stringResource(R.string.app_detection_show_icon),
                        description = stringResource(R.string.app_detection_show_icon_summary),
                        icon = Icons.Outlined.Apps,
                        checked = showIcon,
                        onCheckedChange = setShowIcon,
                    )
                }
                GroupedPreferenceCard {
                    SwitchPreference(
                        title = stringResource(R.string.app_detection_timestamps),
                        description = stringResource(R.string.app_detection_timestamps_summary),
                        icon = Icons.Outlined.Timer,
                        checked = timestamps,
                        onCheckedChange = setTimestamps,
                    )
                }
                GroupedPreferenceCard(last = true) {
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                        Text(stringResource(R.string.grace_title), style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(if (graceDraft.roundToInt() == 0) R.string.grace_none else R.string.grace_seconds, graceDraft.roundToInt()),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        val sliderLabel = stringResource(R.string.grace_title)
                        Slider(
                            modifier = Modifier.semantics { contentDescription = sliderLabel },
                            value = graceDraft,
                            onValueChange = { graceDraft = (it / 5f).roundToInt() * 5f },
                            onValueChangeFinished = { setGraceSeconds(graceDraft.roundToInt()) },
                            valueRange = 0f..Prefs.MaxGraceSeconds.toFloat(),
                            steps = 35,
                        )
                        Text(stringResource(R.string.grace_summary), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        if (!hasUsageAccess) {
            item {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
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

        item(key = "app_search") {
            TextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                shape = RoundedCornerShape(50),
                placeholder = { Text(stringResource(R.string.app_detection_search)) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) IconButton(onClick = { query = "" }) {
                        Icon(Icons.Rounded.Close, stringResource(R.string.apps_clear_search))
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
            )
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

        itemsIndexed(filtered, key = { _, app -> app.packageName }) { index, app ->
            GroupedPreferenceCard(
                first = index == 0,
                last = index == filtered.lastIndex,
                modifier = Modifier.animateItem(),
            ) {
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
        AppOptionsDialog(
            app = app,
            initialLabel = overrides[app.packageName].orEmpty(),
            initiallyEnabled = app.packageName in watched,
            onDismiss = { renaming = null },
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

    val rowColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.surfaceContainerLow,
        animationSpec = spring(stiffness = 350f),
        label = "App selection colour",
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(rowColor)
            .heightIn(min = 88.dp)
            .combinedClickable(onClick = { onCheckedChange(!checked) }, onLongClick = onLongPress, onLongClickLabel = stringResource(R.string.app_options))
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
            Text(displayName ?: app.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                if (displayName != null) "${app.label} · ${app.packageName}" else app.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(12.dp))
        ExpressiveSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
