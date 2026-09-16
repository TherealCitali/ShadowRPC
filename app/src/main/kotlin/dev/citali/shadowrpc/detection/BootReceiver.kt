package dev.citali.shadowrpc.detection

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.*
import timber.log.Timber

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pending = goAsync()
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                try {
                    AppDetectionService.startIfEnabled(context.applicationContext)
                } catch (error: Exception) {
                    Timber.tag("BootReceiver").e(error, "Detection boot restart failed")
                } finally { pending.finish() }
            }
        }
    }
}
