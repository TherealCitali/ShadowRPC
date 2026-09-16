package dev.citali.shadowrpc.presence

import android.content.Context
import androidx.datastore.preferences.core.edit
import dev.citali.shadowrpc.data.Prefs
import dev.citali.shadowrpc.data.dataStore
import kotlinx.coroutines.flow.first
import org.json.JSONObject

data class AppPresenceOverride(
    val type: String? = null,
    val name: ActivitySource? = null,
    val details: ActivitySource? = null,
    val state: ActivitySource? = null,
    val nameText: String = "",
    val detailsText: String = "",
    val stateText: String = "",
) {
    fun applyTo(global: ActivityContent) = global.copy(
        nameSource = name ?: global.nameSource,
        detailsSource = details ?: global.detailsSource,
        stateSource = state ?: global.stateSource,
        nameTemplate = if (name != null) nameText else global.nameTemplate,
        detailsTemplate = if (details != null) detailsText else global.detailsTemplate,
        stateTemplate = if (state != null) stateText else global.stateTemplate,
    )
}

object AppPresenceOverrides {
    val types = listOf("PLAYING", "LISTENING", "WATCHING", "COMPETING")

    fun parse(json: String, packageName: String): AppPresenceOverride = runCatching {
        val obj = JSONObject(json).optJSONObject(packageName) ?: return@runCatching AppPresenceOverride()
        fun source(key: String) = ActivitySource.entries.firstOrNull { it.name == obj.optString(key) }
        AppPresenceOverride(
            type = obj.optString("type").takeIf { it in types },
            name = source("name")?.takeUnless { it == ActivitySource.NONE },
            details = source("details"), state = source("state"),
            nameText = obj.optString("nameText"), detailsText = obj.optString("detailsText"), stateText = obj.optString("stateText"),
        )
    }.getOrDefault(AppPresenceOverride())

    suspend fun load(context: Context, packageName: String) =
        parse(context.dataStore.data.first()[Prefs.AppPresenceOverridesKey].orEmpty(), packageName)

    /** Save all per-app edits atomically, preserving other packages and global settings. */
    suspend fun save(context: Context, packageName: String, label: String, enabled: Boolean, value: AppPresenceOverride) {
        context.dataStore.edit { prefs ->
            val root = runCatching { JSONObject(prefs[Prefs.AppPresenceOverridesKey].orEmpty()) }.getOrDefault(JSONObject())
            if (value == AppPresenceOverride()) root.remove(packageName) else root.put(packageName, JSONObject().apply {
                put("type", value.type); put("name", value.name?.name); put("details", value.details?.name); put("state", value.state?.name)
                put("nameText", value.nameText); put("detailsText", value.detailsText); put("stateText", value.stateText)
            })
            prefs[Prefs.AppPresenceOverridesKey] = root.toString()
            val labels = runCatching { JSONObject(prefs[Prefs.AppLabelOverridesKey].orEmpty()) }.getOrDefault(JSONObject())
            if (label.isBlank()) labels.remove(packageName) else labels.put(packageName, label.trim())
            prefs[Prefs.AppLabelOverridesKey] = labels.toString()
            val watched = prefs[Prefs.AppDetectionPackagesKey].orEmpty()
            prefs[Prefs.AppDetectionPackagesKey] = if (enabled) watched + packageName else watched - packageName
        }
        timber.log.Timber.tag("AppOptions").i("Saved %s: type=%s nameSource=%s shared=%s", packageName, value.type ?: "global", value.name?.name ?: "global", enabled)
    }
}
