package dev.citali.shadowrpc.detection

import android.content.Context
import androidx.datastore.preferences.core.edit
import dev.citali.shadowrpc.data.Prefs
import dev.citali.shadowrpc.data.dataStore
import dev.citali.shadowrpc.presence.ActivitySubject
import kotlinx.coroutines.flow.first
import org.json.JSONObject

/**
 * Per-app display names ("BGMI" instead of "Battlegrounds Mobile India"). Stored
 * as one JSON object { package: label } so the list stays a single preference.
 */
object AppLabels {
    fun parse(json: String?): Map<String, String> {
        if (json.isNullOrBlank()) return emptyMap()
        return runCatching {
            val obj = JSONObject(json)
            buildMap {
                for (key in obj.keys()) put(key, obj.getString(key))
            }
        }.getOrDefault(emptyMap())
    }

    suspend fun overrides(context: Context): Map<String, String> = parse(context.dataStore.data.first()[Prefs.AppLabelOverridesKey])

    /** Empty or blank [label] removes the override. */
    suspend fun setOverride(
        context: Context,
        packageName: String,
        label: String?,
    ) {
        context.dataStore.edit { prefs ->
            val map = parse(prefs[Prefs.AppLabelOverridesKey]).toMutableMap()
            val clean = label?.trim().orEmpty()
            if (clean.isEmpty()) map.remove(packageName) else map[packageName] = clean
            prefs[Prefs.AppLabelOverridesKey] = JSONObject(map).toString()
        }
    }

    suspend fun effectiveLabel(
        context: Context,
        packageName: String,
    ): String = overrides(context)[packageName] ?: InstalledApps.label(context, packageName)

    suspend fun subject(
        context: Context,
        packageName: String,
    ): ActivitySubject =
        ActivitySubject(
            appLabel = effectiveLabel(context, packageName),
            packageName = packageName,
            category = InstalledApps.categoryTitle(context, packageName),
        )
}
