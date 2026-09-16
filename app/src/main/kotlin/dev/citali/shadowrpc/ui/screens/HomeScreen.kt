package dev.citali.shadowrpc.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.citali.shadowrpc.R
import dev.citali.shadowrpc.data.Prefs
import dev.citali.shadowrpc.data.rememberPreference
import dev.citali.shadowrpc.detection.AppDetectionService
import dev.citali.shadowrpc.presence.PresenceManager
import dev.citali.shadowrpc.ui.component.PreferenceGroupTitle
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
    val (detectionEnabled) = rememberPreference(Prefs.AppDetectionEnabledKey, false)
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
        HomeAppList(
            onNeedLogin = { onNavigate(Routes.ACCOUNT) },
            modifier = Modifier.fillMaxSize().padding(padding),
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
            PresenceSettingsContent(showAdvanced = false)
        }
    }
}
