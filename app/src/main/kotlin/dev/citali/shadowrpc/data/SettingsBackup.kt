package dev.citali.shadowrpc.data

import android.content.Context
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

/** Explicit allow-list: never serializes accounts, tokens, consent, caches or running state. */
object SettingsBackup {
    private val booleans = listOf(Prefs.MiuixMonetKey, Prefs.ClearOnLockKey, Prefs.AppDetectionShowIconKey, Prefs.AppDetectionTimestampsKey,
        Prefs.LowResolutionImagesKey, Prefs.PureBlackKey, Prefs.DynamicColorKey)
    private val strings = listOf(Prefs.ThemePresetKey, Prefs.DarkModeKey, Prefs.ActivityTypeKey, Prefs.ActivityStatusKey,
        Prefs.ActivityNameSourceKey, Prefs.ActivityDetailsSourceKey, Prefs.ActivityStateSourceKey,
        Prefs.ActivityNameCustomKey, Prefs.ActivityDetailsCustomKey, Prefs.ActivityStateCustomKey,
        Prefs.AppLabelOverridesKey, Prefs.AppPresenceOverridesKey, Prefs.CustomApplicationIdKey)

    suspend fun export(context: Context): String {
        return encode(context.dataStore.data.first())
    }

    internal fun encode(p: Preferences): String {
        val values = JSONObject()
        booleans.forEach { key -> p[key]?.let { values.put(key.name, it) } }
        strings.forEach { key -> p[key]?.let { values.put(key.name, it) } }
        if (values.optString(Prefs.ThemePresetKey.name) == "MANGA") values.put(Prefs.ThemePresetKey.name, "MIUI")
        p[Prefs.SeedColorKey]?.let { values.put(Prefs.SeedColorKey.name, it) }
        p[Prefs.BackgroundGraceSecondsKey]?.let { values.put(Prefs.BackgroundGraceSecondsKey.name, it) }
        values.put(Prefs.AppDetectionPackagesKey.name, JSONArray(p[Prefs.AppDetectionPackagesKey].orEmpty().toList()))
        return JSONObject().put("format", "ShadowRPC-settings").put("version", 1).put("settings", values).toString(2)
    }

    private fun validPackage(value: String) = value.length in 1..255 && value.matches(Regex("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)*"))

    private fun checkNesting(text: String) {
        var depth = 0
        var quoted = false
        var escaped = false
        for (c in text) {
            if (quoted) {
                if (escaped) escaped = false
                else if (c == '\\') escaped = true
                else if (c == '"') quoted = false
            } else when (c) {
                '"' -> quoted = true
                '{', '[' -> { depth++; require(depth <= 16) { "Backup nesting too deep" } }
                '}', ']' -> depth--
            }
        }
    }

    fun validate(text: String): JSONObject {
        require(text.length <= 1_000_000) { "Backup too large" }
        checkNesting(text)
        val root = JSONObject(text)
        require(root.optString("format") == "ShadowRPC-settings" && root.opt("version") == 1) { "Unsupported backup" }
        val values = root.getJSONObject("settings")
        booleans.forEach { if (values.has(it.name)) require(values.get(it.name) is Boolean) }
        strings.forEach { if (values.has(it.name)) {
            require(values.get(it.name) is String)
            if (it != Prefs.AppLabelOverridesKey && it != Prefs.AppPresenceOverridesKey)
                require(values.getString(it.name).length <= 16_384)
        } }
        fun checkChoice(key: Preferences.Key<String>, choices: Set<String>) {
            if (values.has(key.name)) require(values.getString(key.name) in choices)
        }
        // Old backups remain usable, but cannot re-enable the removed theme.
        if (values.optString(Prefs.ThemePresetKey.name) == "MANGA") values.put(Prefs.ThemePresetKey.name, "MIUI")
        checkChoice(Prefs.ThemePresetKey, setOf("MATERIAL_YOU", "MIUI"))
        checkChoice(Prefs.DarkModeKey, setOf("AUTO", "ON", "OFF"))
        checkChoice(Prefs.ActivityTypeKey, setOf("PLAYING", "LISTENING", "WATCHING", "COMPETING"))
        checkChoice(Prefs.ActivityStatusKey, setOf("online", "idle", "dnd"))
        val sources = setOf("APP", "SHADOWRPC", "PACKAGE", "CATEGORY", "CUSTOM", "NONE")
        checkChoice(Prefs.ActivityNameSourceKey, sources - "NONE")
        checkChoice(Prefs.ActivityDetailsSourceKey, sources)
        checkChoice(Prefs.ActivityStateSourceKey, sources)
        listOf(Prefs.AppLabelOverridesKey, Prefs.AppPresenceOverridesKey).forEach {
            if (values.has(it.name) && values.getString(it.name).isNotBlank()) {
                val nested = values.getString(it.name)
                checkNesting(nested)
                val overrides = JSONObject(nested)
                require(overrides.length() <= 10_000)
                overrides.keys().forEach { pkg ->
                    require(validPackage(pkg))
                    if (it == Prefs.AppLabelOverridesKey) {
                        require(overrides.get(pkg) is String && overrides.getString(pkg).length <= 16_384)
                    } else {
                        val entry = overrides.getJSONObject(pkg)
                        entry.keys().forEach { field ->
                            require(field in setOf("type", "name", "details", "state", "nameText", "detailsText", "stateText"))
                            require(entry.get(field) is String && entry.getString(field).length <= 16_384)
                            val v = entry.getString(field)
                            when (field) {
                                "type" -> require(v in setOf("PLAYING", "LISTENING", "WATCHING", "COMPETING"))
                                "name" -> require(v in sources - "NONE")
                                "details", "state" -> require(v in sources)
                            }
                        }
                    }
                }
            }
        }
        if (values.has(Prefs.CustomApplicationIdKey.name)) {
            val id = values.getString(Prefs.CustomApplicationIdKey.name)
            require(id.isBlank() || (id.toLongOrNull() ?: 0) > 0)
        }
        if (values.has(Prefs.SeedColorKey.name)) {
            val color = values.get(Prefs.SeedColorKey.name)
            require(color is Int || color is Long)
            require(values.getLong(Prefs.SeedColorKey.name) in Int.MIN_VALUE.toLong()..0xFFFFFFFFL)
        }
        if (values.has(Prefs.BackgroundGraceSecondsKey.name)) {
            require(values.get(Prefs.BackgroundGraceSecondsKey.name) is Int)
            require(values.getInt(Prefs.BackgroundGraceSecondsKey.name) in 0..Prefs.MaxGraceSeconds)
        }
        if (values.has(Prefs.AppDetectionPackagesKey.name)) {
            val packages = values.getJSONArray(Prefs.AppDetectionPackagesKey.name)
            require(packages.length() <= 10_000)
            for (i in 0 until packages.length()) {
                require(packages.get(i) is String && validPackage(packages.getString(i)))
            }
        }
        return values
    }

    suspend fun restore(context: Context, text: String) {
        validate(text)
        context.dataStore.edit { p -> applyTo(p, text) }
    }

    internal fun applyTo(p: MutablePreferences, text: String) {
        val values = validate(text)
        run {
            booleans.forEach { if (values.has(it.name)) p[it] = values.getBoolean(it.name) }
            strings.forEach { if (values.has(it.name)) p[it] = values.getString(it.name) }
            if (values.has(Prefs.SeedColorKey.name)) p[Prefs.SeedColorKey] = values.getLong(Prefs.SeedColorKey.name)
            if (values.has(Prefs.BackgroundGraceSecondsKey.name)) p[Prefs.BackgroundGraceSecondsKey] = values.getInt(Prefs.BackgroundGraceSecondsKey.name)
            values.optJSONArray(Prefs.AppDetectionPackagesKey.name)?.let { a ->
                p[Prefs.AppDetectionPackagesKey] = (0 until a.length()).map { a.getString(it) }.toSet()
            }
        }
    }
}
