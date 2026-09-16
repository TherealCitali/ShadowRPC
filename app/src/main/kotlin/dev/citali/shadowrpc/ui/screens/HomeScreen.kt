package dev.citali.shadowrpc.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.citali.shadowrpc.R
import dev.citali.shadowrpc.data.Prefs
import dev.citali.shadowrpc.data.rememberPreference
import dev.citali.shadowrpc.detection.AppDetectionService
import dev.citali.shadowrpc.detection.ForegroundAppDetector
import dev.citali.shadowrpc.discord.DiscordOAuthRepository
import dev.citali.shadowrpc.presence.PresenceManager
import dev.citali.shadowrpc.ui.component.PreferenceCard
import dev.citali.shadowrpc.ui.component.PreferenceEntry
import dev.citali.shadowrpc.ui.component.PreferenceGroupTitle
import dev.citali.shadowrpc.ui.component.SwitchPreference
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onOpenDrawer: () -> Unit,
    onNavigate: (String) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val (displayName) = rememberPreference(Prefs.DiscordNameKey, "")
    val (detectionEnabled, setDetectionEnabled) = rememberPreference(Prefs.AppDetectionEnabledKey, false)
    val serviceRunning by AppDetectionService.running.collectAsStateWithLifecycle()
    val loginRequired = stringResource(R.string.app_detection_login_required)

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = if (displayName.isBlank()) {
                    stringResource(R.string.home_welcome)
                } else {
                    stringResource(R.string.home_welcome_name, displayName)
                },
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.padding(start = 32.dp, end = 24.dp, top = 40.dp, bottom = 28.dp),
            )
            PreferenceGroupTitle(stringResource(R.string.account_title))
            AccountContent()
            PresenceSettingsContent(showAdvanced = false) {
                PreferenceGroupTitle(stringResource(R.string.feature_app_detection))
                PreferenceCard {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        SwitchPreference(
                            title = stringResource(R.string.app_detection_enable),
                            checked = detectionEnabled && serviceRunning,
                            onCheckedChange = ::setAppDetection,
                            icon = Icons.Outlined.Apps,
                        )
                    }
                    PreferenceEntry(
                        title = stringResource(R.string.home_manage_apps),
                        description = stringResource(R.string.home_manage_apps_summary),
                        onClick = { onNavigate(Routes.APP_DETECTION) },
                    )
                }
            }
        }
    }
}
