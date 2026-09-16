package dev.citali.shadowrpc.detection

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import dev.citali.shadowrpc.MainActivity
import dev.citali.shadowrpc.R
import dev.citali.shadowrpc.data.Prefs
import dev.citali.shadowrpc.data.dataStore
import dev.citali.shadowrpc.data.pref
import dev.citali.shadowrpc.data.setPref
import dev.citali.shadowrpc.presence.AppPresenceOverrides
import dev.citali.shadowrpc.presence.ActivityContent
import dev.citali.shadowrpc.presence.ActivityTemplate
import dev.citali.shadowrpc.presence.PresenceManager
import dev.citali.shadowrpc.presence.PresenceRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import dev.citali.shadowrpc.presence.PresenceState
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber

/**
 * Polls the foreground app while App Detection is enabled and mirrors it to
 * Discord. Runs as a foreground service so the poller survives Doze on
 * Android 10 devices; the notification doubles as the "what am I sharing" view.
 */
class AppDetectionService : LifecycleService() {
    private var pollJob: Job? = null
    private var iconJob: Job? = null
    private val iconUrls = mutableMapOf<String, String>()
    private val iconAttempts = mutableMapOf<String, Long>()
    private var lastDetected: String? = null
    private var sharedPackage: String? = null
    private var sharedSinceEpochSeconds: Long = 0L

    override fun onCreate() {
        super.onCreate()
        ForegroundAppDetector.reset()
        try {
            startForegroundCompat(buildNotification(getString(R.string.app_detection_notification_idle)))
            _running.value = true
            Timber.tag(TAG).i("Detection service started")
        } catch (error: Exception) {
            _running.value = false
            Timber.tag(TAG).e(error, "Android rejected detection foreground service")
            stopSelf()
        }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_STOP) {
            lifecycleScope.launch {
                setPref(Prefs.AppDetectionEnabledKey, false)
                stopSelf()
            }
            return START_NOT_STICKY
        }
        if (!_running.value) return START_NOT_STICKY
        if (pollJob?.isActive != true) pollJob = lifecycleScope.launch { pollLoop() }
        return START_STICKY
    }

    private suspend fun pollLoop() {
        while (lifecycleScope.isActive) {
            try {
                val enabled = pref(Prefs.AppDetectionEnabledKey, false)
                if (!enabled) {
                    stopSelf()
                    return
                }
                if (!ForegroundAppDetector.hasUsageAccess(this)) {
                    Timber.tag(TAG).w("usage access revoked; stopping")
                    setPref(Prefs.AppDetectionEnabledKey, false)
                    stopSelf()
                    return
                }

                if (!pref(Prefs.RpcEnabledKey, true)) {
                    sharedPackage = null
                    updateNotification(getString(R.string.rpc_paused))
                    delay(POLL_INTERVAL_MS)
                    continue
                }

                val watched = pref(Prefs.AppDetectionPackagesKey, emptySet())
                val foreground = withContext(Dispatchers.IO) { ForegroundAppDetector.currentForegroundPackage(this@AppDetectionService) }
                if (foreground != lastDetected) {
                    Timber.tag(TAG).i("foreground=%s, selected=%s", foreground, foreground in watched)
                    lastDetected = foreground
                }
                val target = foreground?.takeIf { it in watched }

                when {
                    target == null -> {
                        if (sharedPackage != null) PresenceManager.clear(this)
                        sharedPackage = null
                        updateNotification(
                            if (foreground == null) getString(R.string.detection_no_foreground)
                            else if (watched.isEmpty()) getString(R.string.detection_no_selection)
                            else getString(R.string.detection_not_selected, InstalledApps.label(this, foreground)),
                        )
                    }

                    target != null -> {
                        if (target != sharedPackage) {
                            sharedPackage = target
                            sharedSinceEpochSeconds = System.currentTimeMillis() / 1000L
                        }
                        publish(target)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Timber.tag(TAG).e(error, "detection poll failed; retrying")
                updateNotification(getString(R.string.detection_poll_error))
            }
            delay(POLL_INTERVAL_MS)
        }
    }

    private suspend fun publish(packageName: String) {
        val subject = AppLabels.subject(this, packageName)
        val overrides = AppPresenceOverrides.load(this, packageName)
        val text = ActivityTemplate.resolve(overrides.applyTo(ActivityContent.load(this)), subject, getString(R.string.app_name))
        val showIcon = pref(Prefs.AppDetectionShowIconKey, false)
        val timestamps = pref(Prefs.AppDetectionTimestampsKey, true)
        // Optional artwork must never block foreground polling / initial text presence.
        val icon = if (showIcon) iconUrls[packageName] else null
        if (showIcon && icon == null && iconJob?.isActive != true &&
            System.currentTimeMillis() - (iconAttempts[packageName] ?: 0L) > 60_000L
        ) {
            iconAttempts[packageName] = System.currentTimeMillis()
            iconJob = lifecycleScope.launch {
                try {
                    IconHost.urlFor(this@AppDetectionService, packageName)?.let { iconUrls[packageName] = it }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    Timber.tag(TAG).w(error, "optional icon unavailable")
                }
            }
        }
        if (PresenceManager.state.value !is PresenceState.Sharing) {
            updateNotification(getString(R.string.detection_detected, subject.appLabel))
        }
        PresenceManager.update(
            this,
            PresenceRequest(
                name = text.name,
                activityType = overrides.type,
                details = text.details,
                state = text.state,
                largeImage = icon,
                largeText = subject.appLabel,
                startEpochSeconds = if (timestamps) sharedSinceEpochSeconds else null,
            ),
        )
        updateNotification(
            when {
                !pref(Prefs.RpcEnabledKey, true) -> getString(R.string.rpc_paused)
                PresenceManager.state.value is PresenceState.Error -> getString(R.string.detection_publish_error, subject.appLabel)
                else -> getString(R.string.app_detection_notification_active, subject.appLabel)
            },
        )
    }

    private var lastNotificationText: String? = null

    private fun updateNotification(text: String) {
        if (text == lastNotificationText) return
        lastNotificationText = text
        getSystemService(android.app.NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun buildNotification(text: String): Notification {
        val open =
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val stop =
            PendingIntent.getForegroundService(
                this,
                1,
                Intent(this, AppDetectionService::class.java).setAction(ACTION_STOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        return NotificationCompat
            .Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_shadow)
            .setContentTitle(getString(R.string.feature_app_detection))
            .setContentText(text)
            .setContentIntent(open)
            .addAction(0, getString(R.string.app_detection_notification_stop), stop)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // dataSync services are capped at 6 h/day from Android 15; specialUse is not.
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onDestroy() {
        iconJob?.cancel()
        pollJob?.cancel()
        pollJob = null
        _running.value = false
        // Best effort: clear the presence so the profile does not keep showing a closed app.
        val appContext = applicationContext
        GlobalScope.launch { runCatching { PresenceManager.shutdown(appContext) } }
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "app_detection"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_STOP = "dev.citali.shadowrpc.action.STOP_APP_DETECTION"
        private const val POLL_INTERVAL_MS = 3_000L
        private const val TAG = "AppDetectionService"

        private val _running = MutableStateFlow(false)
        val running: StateFlow<Boolean> = _running

        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, AppDetectionService::class.java))
            } catch (error: Exception) {
                Timber.tag(TAG).e(error, "Android rejected detection start; open ShadowRPC and retry")
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AppDetectionService::class.java))
        }

        /** Restart after boot / process death if the user left detection on and access is still granted. */
        fun startIfEnabled(context: Context) {
            val enabled = runBlocking { context.dataStore.data.map { it[Prefs.AppDetectionEnabledKey] ?: false }.first() }
            if (enabled && ForegroundAppDetector.hasUsageAccess(context)) start(context)
        }
    }
}
