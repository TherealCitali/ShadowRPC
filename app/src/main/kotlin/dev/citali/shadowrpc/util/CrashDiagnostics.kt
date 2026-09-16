package dev.citali.shadowrpc.util

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Process
import android.util.Log
import kotlinx.coroutines.*
import java.io.File
import kotlin.system.exitProcess

/** One private, bounded crash report. Never uploaded or included in Android backups. */
object CrashDiagnostics {
    private var report: File? = null

    fun install(context: Context) {
        val file = File(context.noBackupFilesDir, "last-crash.txt")
        report = file
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                val trace = InMemoryLogTree.redact(error.stackTraceToString().take(24_000))
                file.writeText("${java.util.Date()} — ${thread.name}\n$trace")
            } catch (_: Throwable) {
                // Reporting must never prevent Android's normal crash handling.
            } finally {
                if (previous != null) previous.uncaughtException(thread, error)
                else { Process.killProcess(Process.myPid()); exitProcess(10) }
            }
        }
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching {
                if (file.exists()) {
                    file.readText().take(24_000).chunked(3000).forEach { chunk ->
                        InMemoryLogTree.log(Log.ERROR, "PreviousCrash", chunk, null)
                    }
                }
                if (Build.VERSION.SDK_INT >= 30) {
                    context.getSystemService(ActivityManager::class.java)
                        .getHistoricalProcessExitReasons(context.packageName, 0, 1)
                        .firstOrNull()?.let { exit ->
                            InMemoryLogTree.log(Log.INFO, "PreviousExit",
                                "Android process exit: reason=${exit.reason}, status=${exit.status}, time=${java.util.Date(exit.timestamp)}. " +
                                    "Reasons: 3=low memory, 4=Java crash, 5=native crash, 6=ANR, 10=user requested, 13=other.", null)
                        }
                }
            }
        }
    }

    fun clear() { runCatching { report?.delete() } }
}
