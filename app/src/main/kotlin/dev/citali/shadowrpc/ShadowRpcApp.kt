package dev.citali.shadowrpc

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import dev.citali.shadowrpc.detection.AppDetectionService
import dev.citali.shadowrpc.util.InMemoryLogTree
import timber.log.Timber

class ShadowRpcApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        Timber.plant(InMemoryLogTree)
        dev.citali.shadowrpc.util.CrashDiagnostics.install(this)
        Timber.tag("Startup").i("ShadowRPC %s started; Android %s", BuildConfig.VERSION_NAME, android.os.Build.VERSION.SDK_INT)

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                AppDetectionService.CHANNEL_ID,
                getString(R.string.app_detection_notification_channel),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { setShowBadge(false) },
        )
    }
}
