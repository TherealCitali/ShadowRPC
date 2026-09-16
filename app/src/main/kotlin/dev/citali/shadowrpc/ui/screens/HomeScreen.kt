package dev.citali.shadowrpc.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.QuestionMark
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.citali.shadowrpc.R
import dev.citali.shadowrpc.data.Prefs
import dev.citali.shadowrpc.data.rememberPreference
import dev.citali.shadowrpc.detection.AppDetectionService
import dev.citali.shadowrpc.detection.ForegroundAppDetector
import dev.citali.shadowrpc.discord.DiscordOAuthRepository
import dev.citali.shadowrpc.presence.PresenceManager
import kotlinx.coroutines.launch

private enum class Feature(
    val titleRes: Int,
    val icon: ImageVector,
    val route: String?,
) {
    AppDetection(R.string.feature_app_detection, Icons.Outlined.Apps, Routes.APP_DETECTION),
    MediaRpc(R.string.feature_media_rpc, Icons.Outlined.Movie, null),
    CustomRpc(R.string.feature_custom_rpc, Icons.Outlined.QuestionMark, null),
    ConsoleRpc(R.string.feature_console_rpc, Icons.Outlined.SportsEsports, null),
    Experimental(R.string.feature_experimental, Icons.Outlined.Extension, null),
}

@Composable
fun HomeScreen(
    onOpenDrawer: () -> Unit,
    onNavigate: (String) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val (displayName) = rememberPreference(Prefs.DiscordNameKey, "")
    val (detectionEnabled, setDetectionEnabled) = rememberPreference(Prefs.AppDetectionEnabledKey, false)
    val serviceRunning by AppDetectionService.running.collectAsStateWithLifecycle()
    val comingSoon = stringResource(R.string.coming_soon)
    val cantLoad = stringResource(R.string.cant_load_link)
    val loginRequired = stringResource(R.string.app_detection_login_required)

    fun openLink(url: String) {
        runCatching { uriHandler.openUri(url) }.onFailure { scope.launch { snackbarHostState.showSnackbar(cantLoad) } }
    }

    val onComingSoonToggle: (Boolean) -> Unit = { scope.launch { snackbarHostState.showSnackbar(comingSoon) } }

    fun setAppDetection(wanted: Boolean) {
        if (!wanted) {
            setDetectionEnabled(false)
            AppDetectionService.stop(context)
            return
        }
        if (!ForegroundAppDetector.hasUsageAccess(context)) {
            onNavigate(Routes.APP_DETECTION)
            return
        }
        scope.launch {
            val token = DiscordOAuthRepository.getValidAccessToken(context)
            if (token.isNullOrBlank()) {
                snackbarHostState.showSnackbar(loginRequired)
                onNavigate(Routes.ACCOUNT)
            } else {
                setDetectionEnabled(true)
                AppDetectionService.start(context)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Rounded.Menu, contentDescription = stringResource(R.string.action_menu))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            PresenceManager.clear(context)
                            if (detectionEnabled) AppDetectionService.start(context)
                        }
                    }) {
                        Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.action_refresh))
                    }
                    IconButton(onClick = { onNavigate(Routes.ACCOUNT) }) {
                        Icon(Icons.Rounded.Person, contentDescription = stringResource(R.string.action_account))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets.navigationBars,
    ) { padding: PaddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item(span = { GridItemSpan(2) }) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text =
                            if (displayName.isBlank()) {
                                stringResource(R.string.home_welcome)
                            } else {
                                stringResource(R.string.home_welcome_name, displayName)
                            },
                        style = MaterialTheme.typography.displaySmall,
                        modifier = Modifier.padding(top = 32.dp, bottom = 32.dp),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        LinkChip(stringResource(R.string.home_chip_discord)) { openLink("https://discord.com/app") }
                        LinkChip(stringResource(R.string.home_chip_telegram)) { openLink("https://t.me/LunarTuneGC") }
                        LinkChip(stringResource(R.string.home_chip_github)) { openLink("https://github.com/TherealCitali/ShadowRPC") }
                    }
                    Text(
                        text = stringResource(R.string.home_features),
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(top = 36.dp, bottom = 8.dp),
                    )
                }
            }

            items(Feature.entries, key = { it.name }) { feature ->
                val detectionOn = detectionEnabled && serviceRunning
                val stateLabel =
                    when (feature) {
                        Feature.AppDetection -> stringResource(if (detectionOn) R.string.state_on else R.string.state_off)
                        Feature.MediaRpc -> stringResource(R.string.state_off)
                        else -> null
                    }
                val onToggle: ((Boolean) -> Unit)? =
                    when (feature) {
                        Feature.AppDetection -> ::setAppDetection
                        Feature.MediaRpc -> onComingSoonToggle
                        else -> null
                    }
                FeatureCard(
                    title = stringResource(feature.titleRes),
                    icon = feature.icon,
                    stateLabel = stateLabel,
                    checked = feature == Feature.AppDetection && detectionOn,
                    onToggle = onToggle,
                    onClick = {
                        val route = feature.route
                        if (route != null) onNavigate(route) else scope.launch { snackbarHostState.showSnackbar(comingSoon) }
                    },
                )
            }
        }
    }
}

@Composable
private fun LinkChip(
    label: String,
    onClick: () -> Unit,
) {
    AssistChip(
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.titleMedium) },
        shape = RoundedCornerShape(16.dp),
        colors =
            AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                labelColor = MaterialTheme.colorScheme.onSurface,
            ),
        border = null,
        modifier = Modifier.height(56.dp),
    )
}

/**
 * Feature tile: icon top-left, title, optional Off/On label + switch bottom row.
 * Corners mirror the screenshots (large radius, one flattened corner per tile).
 */
@Composable
private fun FeatureCard(
    title: String,
    icon: ImageVector,
    stateLabel: String?,
    checked: Boolean,
    onToggle: ((Boolean) -> Unit)?,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp, bottomStart = 40.dp, bottomEnd = 12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 24.dp, end = 20.dp, top = 24.dp, bottom = 20.dp),
        ) {
            Column {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(36.dp),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            if (stateLabel != null || onToggle != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stateLabel.orEmpty(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (onToggle != null) {
                        Switch(checked = checked, onCheckedChange = onToggle)
                    }
                }
            }
        }
    }
}
