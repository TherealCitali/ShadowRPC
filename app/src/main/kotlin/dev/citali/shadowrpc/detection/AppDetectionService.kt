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
import dev.citali.shadowrpc.presence.ActivityContent
import dev.citali.shadowrpc.presence.ActivityTemplate
import dev.citali.shadowrpc.presence.PresenceManager
import dev.citali.shadowrpc.presence.PresenceRequest
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
    private var sharedPackage: String? = null
    private var sharedSinceEpochSeconds: Long = 0L

    override fun onCreate() {
        super.onCreate()
        _running.value = true
        startForegroundCompat(buildNotification(getString(R.string.app_detection_notification_idle)))
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
        if (pollJob == null) pollJob = lifecycleScope.launch { pollLoop() }
        return START_STICKY
    }

    private suspend fun pollLoop() {
        while (lifecycleScope.isActive) {
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
            val foreground = ForegroundAppDetector.currentForegroundPackage(this)
            val target = foreground?.takeIf { it in watched }

            when {
                target == null && sharedPackage != null -> {
                    PresenceManager.clear(this)
                    sharedPackage = null
                    updateNotification(getString(R.string.app_detection_notification_idle))
                }

                target != null -> {
                    if (target != sharedPackage) {
                        sharedPackage = target
                        sharedSinceEpochSeconds = System.currentTimeMillis() / 1000L
                    }
                    publish(target)
                }
            }
            delay(POLL_INTERVAL_MS)
        }
    }

    private suspend fun publish(packageName: String) {
        val subject = AppLabels.subject(this, packageName)
        val text = ActivityTemplate.resolve(ActivityContent.load(this), subject, getString(R.string.app_name))
        val showIcon = pref(Prefs.AppDetectionShowIconKey, false)
        val timestamps = pref(Prefs.AppDetectionTimestampsKey, true)
        val icon = if (showIcon) IconHost.urlFor(this, packageName) else null

        PresenceManager.update(
            this,
            PresenceRequest(
                name = text.name,
                details = text.details,
                state = text.state,
                largeImage = icon,
                largeText = subject.appLabel,
                startEpochSeconds = if (timestamps) sharedSinceEpochSeconds else null,
            ),
        )
        updateNotification(
            if (pref(Prefs.RpcEnabledKey, true)) getString(R.string.app_detection_notification_active, subject.appLabel)
            else getString(R.string.rpc_paused),
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
            ContextCompat.startForegroundService(context, Intent(context, AppDetectionService::class.java))
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
