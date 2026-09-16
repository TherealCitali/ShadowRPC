package dev.citali.shadowrpc.detection

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process

object ForegroundAppDetector {
    /** Usage access is a special permission; it is granted in Settings, not via a runtime dialog. */
    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
        val mode =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
            }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private var queriedUntil = 0L
    private var foreground: String? = null
    private var activity: String? = null

    @Synchronized
    fun reset() {
        queriedUntil = 0L
        foreground = null
        activity = null
    }

    /** Bootstrap from a full day, then retain state across quiet polling intervals. */
    @Synchronized
    fun currentForegroundPackage(context: Context): String? {
        val usm = context.getSystemService(UsageStatsManager::class.java) ?: return null
        val end = System.currentTimeMillis()
        if (end < queriedUntil) reset() // Wall-clock adjustment.
        val start = if (queriedUntil == 0L) end - 86_400_000L else queriedUntil
        val events = usm.queryEvents(start, end) ?: return null
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            // RESUMED/PAUSED have the same numeric values as MOVE_TO_FOREGROUND/BACKGROUND
            // on API 26–28, so these branches cover both platform generations.
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    foreground = event.packageName
                    activity = event.className
                }
                UsageEvents.Event.ACTIVITY_PAUSED -> {
                    if (foreground == event.packageName && (activity == event.className || event.className == null)) {
                        foreground = null
                        activity = null
                    }
                }
                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                    foreground = null
                    activity = null
                }
            }
        }
        queriedUntil = end
        val power = context.getSystemService(android.os.PowerManager::class.java)
        val keyguard = context.getSystemService(android.app.KeyguardManager::class.java)
        if (power?.isInteractive == false || keyguard?.isKeyguardLocked == true) return null
        return foreground
    }
}
