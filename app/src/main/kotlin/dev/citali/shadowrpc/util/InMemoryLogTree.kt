package dev.citali.shadowrpc.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LogLine(
    val time: String,
    val priority: Int,
    val tag: String?,
    val message: String,
)

/** Keeps the last few hundred log lines so the Logs screen can show them without adb. */
object InMemoryLogTree : Timber.Tree() {
    private const val MAX_LINES = 400
    val verbose = MutableStateFlow(false)

    override fun isLoggable(tag: String?, priority: Int): Boolean =
        verbose.value || priority >= android.util.Log.INFO
    private val format = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private val _lines = MutableStateFlow<List<LogLine>>(emptyList())
    val lines: StateFlow<List<LogLine>> = _lines

    @Synchronized
    public override fun log(
        priority: Int,
        tag: String?,
        message: String,
        t: Throwable?,
    ) {
        if (!isLoggable(tag, priority)) return
        val text = if (t != null) "$message\n${t.stackTraceToString().take(600)}" else message
        val line = LogLine(format.format(Date()), priority, tag, redact(text).take(4000))
        _lines.value = (_lines.value + line).takeLast(MAX_LINES)
    }

    /** Strip common credential formats before storage, display or clipboard export. */
    fun redact(text: String): String = text
        .replace(Regex("(?i)Bearer\\s+[^\\s\"&]+"), "Bearer [redacted]")
        .replace(Regex("(?i)((?:access_token|refresh_token|id_token|client_secret|code_verifier|session_id|session|authorization|token|code)[\"\\s]*[:=][\"\\s]*)[^\\s\"&,}]+"), "$1[redacted]")
        .replace(Regex("gh[pousr]_[A-Za-z0-9]+"), "[redacted]")

    @Synchronized
    fun clear() {
        _lines.value = emptyList()
    }
}
