package dev.citali.shadowrpc.presence

import android.content.Context
import dev.citali.shadowrpc.BuildConfig
import dev.citali.shadowrpc.data.Prefs
import dev.citali.shadowrpc.data.setPref
import dev.citali.shadowrpc.data.pref
import dev.citali.shadowrpc.discord.DiscordStatusDisplayType
import dev.citali.shadowrpc.discord.DiscordActivityType
import dev.citali.shadowrpc.discord.DiscordAssetRegistrar
import dev.citali.shadowrpc.discord.DiscordOAuthRepository
import dev.citali.shadowrpc.discord.DiscordOnlineStatus
import dev.citali.shadowrpc.discord.DiscordPresenceActivity
import dev.citali.shadowrpc.discord.DiscordPresenceAssets
import dev.citali.shadowrpc.discord.DiscordPresenceTimestamps
import dev.citali.shadowrpc.discord.DiscordSocialPresenceClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

/**
 * What ShadowRPC wants Discord to show right now. Producers (app detection,
 * later media / custom RPC) hand one of these to [PresenceManager]; it takes
 * care of the token, the application id override, throttling and clearing.
 */
data class PresenceRequest(
    val name: String,
    val activityType: String? = null,
    val details: String? = null,
    val state: String? = null,
    val largeImage: String? = null,
    val largeText: String? = null,
    val smallImage: String? = null,
    val smallText: String? = null,
    val startEpochSeconds: Long? = null,
    val endEpochSeconds: Long? = null,
)

sealed interface PresenceState {
    data object Idle : PresenceState

    data class Sharing(
        val request: PresenceRequest,
    ) : PresenceState

    data class Error(
        val message: String,
    ) : PresenceState
}

object PresenceManager {
    private const val TAG = "PresenceManager"

    private val mutex = Mutex()
    private var lastSent: PresenceRequest? = null
    private var lastSentAtMs = 0L
    private var lastActivity: DiscordPresenceActivity? = null

    private val _state = MutableStateFlow<PresenceState>(PresenceState.Idle)
    val state: StateFlow<PresenceState> = _state

    /** Discord rate-limits presence updates; identical requests inside this window are dropped. */
    private const val MIN_INTERVAL_MS = 15_000L

    suspend fun update(
        context: Context,
        request: PresenceRequest,
    ) {
        mutex.withLock {
            if (!context.pref(Prefs.RpcEnabledKey, true)) return
            val now = android.os.SystemClock.elapsedRealtime()

            val token = DiscordOAuthRepository.getValidAccessToken(context)
            if (token.isNullOrBlank()) {
                _state.value = PresenceState.Error("Not signed in to Discord")
                return
            }

            val applicationId = resolveApplicationId(context)
            DiscordAssetRegistrar.applicationId = applicationId.toString()

            val activity =
                DiscordPresenceActivity(
                    applicationId = applicationId,
                    name = request.name,
                    statusDisplayType = DiscordStatusDisplayType.Name,
                    type = DiscordActivityType.fromPreference(request.activityType ?: context.pref(Prefs.ActivityTypeKey, "PLAYING")),
                    details = request.details,
                    state = request.state,
                    assets =
                        DiscordPresenceAssets(
                            largeImage = request.largeImage,
                            largeText = request.largeText,
                            smallImage = request.smallImage,
                            smallText = request.smallText,
                        ),
                    timestamps =
                        DiscordPresenceTimestamps(
                            startEpochSeconds = request.startEpochSeconds,
                            endEpochSeconds = request.endEpochSeconds,
                        ),
                    onlineStatus = DiscordOnlineStatus.fromPreference(context.pref(Prefs.ActivityStatusKey, "online")),
                )

            if (activity == lastActivity && DiscordSocialPresenceClient.isStarted && now - lastSentAtMs < MIN_INTERVAL_MS) return
            Timber.tag(TAG).i("Publishing name=%s type=%s (%d)", activity.name, activity.type.name, activity.type.nativeValue)
            DiscordSocialPresenceClient
                .updatePresence(token, activity)
                .onSuccess {
                    lastActivity = activity
                    lastSent = request
                    lastSentAtMs = now
                    _state.value = PresenceState.Sharing(request)
                    Timber.tag(TAG).i("presence -> %s / %s", request.name, request.details)
                }.onFailure { error ->
                    _state.value = PresenceState.Error(error.message ?: "Presence update failed")
                    Timber.tag(TAG).w(error, "presence update failed")
                }
        }
    }

    /** Serialize the master switch with in-flight publishes so Off always wins. */
    suspend fun setEnabled(context: Context, enabled: Boolean) {
        mutex.withLock {
            context.setPref(Prefs.RpcEnabledKey, enabled)
            if (!enabled) {
                clearLocked(context)
                DiscordSocialPresenceClient.close()
            }
        }
    }

    suspend fun clear(context: Context) {
        mutex.withLock { clearLocked(context) }
    }

    private suspend fun clearLocked(context: Context) {
        if (lastSent == null && _state.value is PresenceState.Idle) return
        val token = DiscordOAuthRepository.getValidAccessToken(context)
        DiscordSocialPresenceClient
            .clearPresence(token)
            .onFailure { Timber.tag(TAG).w(it, "clear presence failed") }
        lastActivity = null
        lastSent = null
        lastSentAtMs = 0L
        _state.value = PresenceState.Idle
        Timber.tag(TAG).i("presence cleared")
    }

    suspend fun shutdown(context: Context, shouldClose: () -> Boolean = { true }) {
        mutex.withLock {
            if (!shouldClose()) return
            clearLocked(context)
            // A replacement detection service may have started while clearing.
            if (shouldClose()) DiscordSocialPresenceClient.close()
        }
    }

    private suspend fun resolveApplicationId(context: Context): Long =
        context
            .pref(Prefs.CustomApplicationIdKey, "")
            .trim()
            .toLongOrNull()
            ?.takeIf { it > 0 }
            ?: BuildConfig.DISCORD_APPLICATION_ID_LONG
}
