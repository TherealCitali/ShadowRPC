package dev.citali.shadowrpc.detection

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.citali.shadowrpc.data.Prefs
import dev.citali.shadowrpc.data.dataStore
import dev.citali.shadowrpc.data.pref
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Discord can only show presence images it can fetch from a public URL, so a
 * launcher icon has to be hosted somewhere first. Uploaded links are cached per
 * package in DataStore ("Delete saved icon links" clears them).
 *
 * The default host is catbox.moe's keyless anonymous upload. Swap [upload] for
 * another provider if you prefer; nothing else depends on it.
 */
object IconHost {
    private const val TAG = "IconHost"
    private const val CATBOX_ENDPOINT = "https://catbox.moe/user/api.php"
    private const val CACHE_PREFIX = "iconUrl:"

    private val client =
        OkHttpClient
            .Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

    private fun cacheKey(packageName: String) = stringPreferencesKey(CACHE_PREFIX + packageName)

    suspend fun urlFor(
        context: Context,
        packageName: String,
    ): String? =
        withContext(Dispatchers.IO) {
            if (!context.pref(Prefs.IconUploadConsentKey, false)) return@withContext null
            val cached = context.dataStore.data.first()[cacheKey(packageName)]
            if (!cached.isNullOrBlank()) return@withContext cached

            val lowRes = context.pref(Prefs.LowResolutionImagesKey, false)
            val png = InstalledApps.iconPng(context, packageName, sizePx = if (lowRes) 128 else 512)
                ?: return@withContext null
            // Check again at the upload boundary after icon extraction.
            if (!context.pref(Prefs.IconUploadConsentKey, false)) return@withContext null
            val url = runCatching { upload(packageName, png) }
                .onFailure { Timber.tag(TAG).w(it, "icon upload failed for %s", packageName) }
                .getOrNull() ?: return@withContext null

            context.dataStore.edit { it[cacheKey(packageName)] = url }
            url
        }

    suspend fun clearCache(context: Context) {
        context.dataStore.edit { prefs ->
            prefs
                .asMap()
                .keys
                .map { it.name }
                .filter { it.startsWith(CACHE_PREFIX) }
                .forEach { prefs.remove(stringPreferencesKey(it)) }
        }
    }

    private fun upload(
        packageName: String,
        png: ByteArray,
    ): String {
        val body =
            MultipartBody
                .Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("reqtype", "fileupload")
                .addFormDataPart(
                    "fileToUpload",
                    "$packageName.png",
                    png.toRequestBody("image/png".toMediaType()),
                ).build()
        val request = Request.Builder().url(CATBOX_ENDPOINT).post(body).build()
        client.newCall(request).execute().use { response ->
            val text = response.body.string().trim()
            check(response.isSuccessful && text.startsWith("https://")) { "upload failed: HTTP ${response.code} $text" }
            return text
        }
    }
}
