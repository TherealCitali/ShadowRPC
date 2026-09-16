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
    private val format = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private val _lines = MutableStateFlow<List<LogLine>>(emptyList())
    val lines: StateFlow<List<LogLine>> = _lines

    override fun log(
        priority: Int,
        tag: String?,
        message: String,
        t: Throwable?,
    ) {
        val text = if (t != null) "$message\n${t.stackTraceToString().take(600)}" else message
        val line = LogLine(format.format(Date()), priority, tag, text)
        _lines.value = (_lines.value + line).takeLast(MAX_LINES)
    }

    fun clear() {
        _lines.value = emptyList()
    }
}
