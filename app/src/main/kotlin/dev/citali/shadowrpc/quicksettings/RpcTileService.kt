package dev.citali.shadowrpc.quicksettings

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import dev.citali.shadowrpc.MainActivity
import dev.citali.shadowrpc.R
import dev.citali.shadowrpc.data.*
import dev.citali.shadowrpc.detection.AppDetectionService
import dev.citali.shadowrpc.presence.PresenceManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import timber.log.Timber

class RpcTileService : TileService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var listening: Job? = null
    private var changing = false

    override fun onStartListening() {
        super.onStartListening()
        listening?.cancel()
        listening = scope.launch {
            dataStore.data.collect { prefs ->
                qsTile?.apply {
                    val enabled = prefs[Prefs.RpcEnabledKey] ?: true
                    val signedIn = !prefs[Prefs.DiscordTokenKey].isNullOrBlank()
                    val until = prefs[Prefs.PauseUntilKey] ?: 0L
                    val paused = until == -1L || until > System.currentTimeMillis()
                    state = if (enabled && signedIn) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                    label = getString(R.string.app_name)
                    if (Build.VERSION.SDK_INT >= 29) subtitle = getString(if (!signedIn) R.string.qs_sign_in else if (enabled && paused) R.string.rpc_paused else if (enabled) R.string.state_on else R.string.state_off)
                    updateTile()
                }
            }
        }
    }
    override fun onStopListening() { listening?.cancel(); super.onStopListening() }
    override fun onClick() {
        super.onClick()
        unlockAndRun {
            if (!changing) {
                changing = true
                scope.launch {
                    try {
                        if (pref(Prefs.DiscordTokenKey, "").isBlank()) {
                            openApp()
                            return@launch
                        }
                        val wanted = !pref(Prefs.RpcEnabledKey, true)
                        if (!wanted) AppDetectionService.stop(this@RpcTileService)
                        PresenceManager.setEnabled(this@RpcTileService, wanted)
                        if (wanted) AppDetectionService.startIfEnabled(this@RpcTileService)
                    } catch (cancelled: CancellationException) { throw cancelled }
                    catch (error: Exception) { Timber.tag("QuickSettings").e(error, "RPC tile action failed") }
                    finally { changing = false }
                }
            }
        }
    }
    @Suppress("DEPRECATION")
    private fun openApp() {
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= 34) startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
        else startActivityAndCollapse(intent)
    }
    override fun onDestroy() { scope.cancel(); super.onDestroy() }
}
