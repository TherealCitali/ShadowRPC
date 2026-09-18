package dev.citali.shadowrpc.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.citali.shadowrpc.BuildConfig
import dev.citali.shadowrpc.R
import dev.citali.shadowrpc.data.Prefs
import dev.citali.shadowrpc.data.rememberPreference
import dev.citali.shadowrpc.detection.AppDetectionService
import dev.citali.shadowrpc.discord.DiscordAuthCoordinator
import dev.citali.shadowrpc.discord.DiscordAuthorizationSession
import dev.citali.shadowrpc.discord.DiscordOAuthRepository
import dev.citali.shadowrpc.presence.PresenceManager
import dev.citali.shadowrpc.ui.component.SwitchPreference
import dev.citali.shadowrpc.ui.component.ScreenScaffold
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun AccountScreen(onBack: () -> Unit) {
    ScreenScaffold(title = stringResource(R.string.account_title), onBack = onBack) {
        AccountContent()
    }
}

@Composable
fun AccountContent() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val (rpcEnabled) = rememberPreference(Prefs.RpcEnabledKey, true)
    var changingRpc by remember { mutableStateOf(false) }
    val (token) = rememberPreference(Prefs.DiscordTokenKey, "")
    val (username) = rememberPreference(Prefs.DiscordUsernameKey, "")
    val (name) = rememberPreference(Prefs.DiscordNameKey, "")
    val (avatarUrl) = rememberPreference(Prefs.DiscordAvatarUrlKey, "")
    var error by remember { mutableStateOf<String?>(null) }
    val hasAppId = BuildConfig.DISCORD_APPLICATION_ID_LONG > 0L

    // Finish the PKCE flow when the browser redirects back into DiscordOAuthCallbackActivity.
    LaunchedEffect(Unit) {
        DiscordAuthCoordinator.redirects.collectLatest { uri ->
            val session = PendingAuth.session ?: return@collectLatest
            PendingAuth.session = null
            DiscordOAuthRepository
                .completeAuthorization(context, session, uri)
                .onFailure { error = it.message }
                .onSuccess { error = null }
        }
    }

    Column {
        Card(
            shape = androidx.compose.foundation.shape.themeShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (avatarUrl.isNotBlank()) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp).clip(CircleShape),
                        )
                        Spacer(Modifier.width(16.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (token.isBlank()) {
                                stringResource(R.string.account_not_linked)
                            } else {
                                name.ifBlank { username }
                            },
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        if (token.isNotBlank() && username.isNotBlank()) {
                            Text(
                                "@$username",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                Surface(
                    shape = androidx.compose.foundation.shape.themeShape(24.dp),
                    color = if (error != null || !hasAppId) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = when {
                            !hasAppId -> stringResource(R.string.account_missing_app_id)
                            error != null -> stringResource(R.string.account_login_failed, error.orEmpty())
                            token.isNotBlank() && !rpcEnabled -> stringResource(R.string.rpc_paused)
                            token.isNotBlank() -> stringResource(R.string.home_account_ready)
                            else -> stringResource(R.string.account_login_hint)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (error != null || !hasAppId) MaterialTheme.colorScheme.onErrorContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
                if (token.isNotBlank()) {
                    Surface(
                        shape = androidx.compose.foundation.shape.themeShape(24.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        SwitchPreference(
                            title = stringResource(R.string.rpc_enable),
                            checked = rpcEnabled,
                            enabled = !changingRpc,
                            onCheckedChange = { wanted ->
                                changingRpc = true
                                scope.launch {
                                    try {
                                        PresenceManager.setEnabled(context, wanted)
                                        if (wanted) AppDetectionService.startIfEnabled(context)
                                    } finally {
                                        changingRpc = false
                                    }
                                }
                            },
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }
                if (token.isBlank()) {
                    Button(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        enabled = hasAppId,
                        onClick = {
                            val session = DiscordOAuthRepository.createAuthorizationSession()
                            PendingAuth.session = session
                            context.startActivity(Intent(Intent.ACTION_VIEW, session.authorizationUri))
                        },
                    ) { Text(stringResource(R.string.account_login)) }
                } else {
                    OutlinedButton(modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), onClick = {
                        scope.launch {
                            AppDetectionService.stop(context)
                            PresenceManager.shutdown(context)
                            DiscordOAuthRepository.clearSession(context)
                        }
                    }) { Text(stringResource(R.string.account_logout)) }
                }
            }
        }

    }
}

/** Kept outside composition so the PKCE verifier survives rotation while the browser is open. */
private object PendingAuth {
    var session: DiscordAuthorizationSession? = null
}
