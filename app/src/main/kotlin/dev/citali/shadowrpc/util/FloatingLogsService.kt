package dev.citali.shadowrpc.util

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.*
import android.widget.*
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import dev.citali.shadowrpc.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber

/** Session-only overlay. No preferences, boot receiver or sticky restart. */
class FloatingLogsService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var panel: LinearLayout? = null
    private lateinit var windows: WindowManager

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == STOP || !Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (panel != null) return START_NOT_STICKY
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, getString(R.string.floating_logs), NotificationManager.IMPORTANCE_LOW))
        val stop = PendingIntent.getService(this, 0, Intent(this, FloatingLogsService::class.java).setAction(STOP), PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_shadow)
            .setContentTitle(getString(R.string.floating_logs))
            .setContentText(getString(R.string.floating_logs_temporary))
            .setOngoing(true).setSilent(true)
            .addAction(0, getString(R.string.floating_logs_stop), stop).build()
        if (Build.VERSION.SDK_INT >= 34) startForeground(ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(ID, notification)
        try {
            showPanel()
            _running.value = true
            Timber.tag("FloatingLogs").i("Temporary log overlay opened")
        } catch (error: Exception) {
            Timber.tag("FloatingLogs").e(error, "Cannot display overlay")
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun showPanel() {
        windows = getSystemService(WindowManager::class.java)
        val width = minOf(dp(320), resources.displayMetrics.widthPixels - dp(24)).coerceAtLeast(1)
        val height = minOf(dp(240), resources.displayMetrics.heightPixels / 2)
        val params = WindowManager.LayoutParams(
            width, height, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.TOP or Gravity.LEFT; x = dp(12); y = dp(100) }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(4), dp(8), dp(8))
            background = GradientDrawable().apply { setColor(Color.rgb(25, 28, 25)); cornerRadius = dp(20).toFloat() }
            elevation = dp(12).toFloat()
        }
        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val title = TextView(this).apply {
            text = getString(R.string.floating_logs_drag)
            setTextColor(Color.WHITE); textSize = 14f
            setPadding(dp(8), 0, 0, 0)
        }
        val close = Button(this).apply {
            text = "×"; contentDescription = getString(R.string.floating_logs_stop)
            setOnClickListener { stopSelf() }
        }
        header.addView(title, LinearLayout.LayoutParams(0, dp(48), 1f))
        header.addView(close, LinearLayout.LayoutParams(dp(48), dp(48)))
        val scroll = ScrollView(this)
        val text = TextView(this).apply {
            setTextColor(Color.rgb(220, 235, 220)); textSize = 11f; typeface = Typeface.MONOSPACE
        }
        scroll.addView(text)
        root.addView(header)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        var startX = 0; var startY = 0; var touchX = 0f; var touchY = 0f
        title.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> { startX = params.x; startY = params.y; touchX = event.rawX; touchY = event.rawY; true }
                MotionEvent.ACTION_MOVE -> {
                    params.x = (startX + event.rawX - touchX).toInt().coerceIn(0, (resources.displayMetrics.widthPixels - width).coerceAtLeast(0))
                    params.y = (startY + event.rawY - touchY).toInt().coerceIn(0, (resources.displayMetrics.heightPixels - height).coerceAtLeast(0))
                    runCatching { windows.updateViewLayout(root, params) }.onFailure { stopSelf() }
                    true
                }
                MotionEvent.ACTION_UP -> { view.performClick(); true }
                else -> false
            }
        }
        windows.addView(root, params)
        panel = root
        scope.launch {
            InMemoryLogTree.lines.collect { lines ->
                val follow = !scroll.canScrollVertically(1)
                text.text = if (lines.isEmpty()) getString(R.string.logs_empty) else lines.takeLast(80)
                    .joinToString("\n") { "${it.time} ${it.tag.orEmpty()}: ${it.message}" }
                if (follow) scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) { stopSelf() }

    override fun onDestroy() {
        scope.cancel()
        panel?.let { runCatching { windows.removeView(it) } }
        panel = null
        _running.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "floating_logs"
        private const val ID = 1002
        private const val STOP = "dev.citali.shadowrpc.STOP_FLOATING_LOGS"
        private val _running = MutableStateFlow(false)
        val running: StateFlow<Boolean> = _running
        fun start(context: Context) {
            if (Settings.canDrawOverlays(context)) ContextCompat.startForegroundService(context, Intent(context, FloatingLogsService::class.java))
        }
        fun stop(context: Context) { context.stopService(Intent(context, FloatingLogsService::class.java)) }
    }
}
