package dev.citali.shadowrpc.presence

import android.app.KeyguardManager
import android.content.Context
import android.os.PowerManager
import dev.citali.shadowrpc.data.Prefs
import dev.citali.shadowrpc.data.pref
import dev.citali.shadowrpc.data.setPref

object PresencePrivacy {
    suspend fun isSuppressed(context: Context): Boolean {
        val until = context.pref(Prefs.PauseUntilKey, 0L)
        if (until == -1L || until > System.currentTimeMillis()) return true
        return context.pref(Prefs.ClearOnLockKey, false) &&
            (context.getSystemService(KeyguardManager::class.java)?.isKeyguardLocked == true ||
                context.getSystemService(PowerManager::class.java)?.isInteractive == false)
    }

    suspend fun pause(context: Context, minutes: Int?) {
        require(minutes == null || minutes in 1..1440)
        context.setPref(Prefs.PauseUntilKey, minutes?.let { System.currentTimeMillis() + it * 60_000L } ?: -1L)
        PresenceManager.clearForPrivacy()
    }

    suspend fun resume(context: Context) { context.setPref(Prefs.PauseUntilKey, 0L) }
}
