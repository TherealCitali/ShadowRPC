package dev.citali.shadowrpc.detection

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

data class InstalledApp(
    val packageName: String,
    val label: String,
    val isSystem: Boolean,
)

object InstalledApps {
    suspend fun load(context: Context): List<InstalledApp> =
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            pm
                .queryIntentActivities(launcherIntent, 0)
                .asSequence()
                .map { it.activityInfo.applicationInfo }
                .distinctBy { it.packageName }
                .filter { it.packageName != context.packageName }
                .map { info ->
                    InstalledApp(
                        packageName = info.packageName,
                        label = pm.getApplicationLabel(info).toString(),
                        isSystem = info.flags and ApplicationInfo.FLAG_SYSTEM != 0,
                    )
                }.sortedBy { it.label.lowercase() }
                .toList()
        }

    fun label(
        context: Context,
        packageName: String,
    ): String =
        runCatching {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        }.getOrDefault(packageName)

    fun icon(
        context: Context,
        packageName: String,
    ): Drawable? = runCatching { context.packageManager.getApplicationIcon(packageName) }.getOrNull()

    /** Localised Play Store category ("Game", "Social", ...) or null when the app declares none. */
    @Suppress("DEPRECATION")
    fun categoryTitle(
        context: Context,
        packageName: String,
    ): String? =
        runCatching {
            val info = context.packageManager.getApplicationInfo(packageName, 0)
            val category =
                if (info.category == ApplicationInfo.CATEGORY_UNDEFINED && info.flags and ApplicationInfo.FLAG_IS_GAME != 0) {
                    ApplicationInfo.CATEGORY_GAME
                } else {
                    info.category
                }
            ApplicationInfo.getCategoryTitle(context, category)?.toString()
        }.getOrNull()

    /** PNG bytes of the launcher icon, square, sized for Discord's large image. */
    fun iconPng(
        context: Context,
        packageName: String,
        sizePx: Int,
    ): ByteArray? {
        val drawable = icon(context, packageName) ?: return null
        val bitmap =
            (drawable as? BitmapDrawable)?.bitmap?.let { Bitmap.createScaledBitmap(it, sizePx, sizePx, true) }
                ?: Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888).also { bmp ->
                    val canvas = Canvas(bmp)
                    drawable.setBounds(0, 0, sizePx, sizePx)
                    drawable.draw(canvas)
                }
        return ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.toByteArray()
        }
    }

    fun isInstalled(
        context: Context,
        packageName: String,
    ): Boolean =
        runCatching {
            // getApplicationInfo(String, Int) is available on every supported API level.
            context.packageManager.getApplicationInfo(packageName, 0)
            true
        }.getOrDefault(false)
}
