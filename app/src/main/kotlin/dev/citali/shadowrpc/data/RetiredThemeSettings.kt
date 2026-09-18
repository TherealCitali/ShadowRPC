package dev.citali.shadowrpc.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey

/** Remove retired preset choices only; preserve Material colors and all RPC/account settings. */
object RetiredThemeSettings {
    suspend fun clear(context: Context) {
        context.dataStore.edit { clear(it) }
    }

    internal fun clear(prefs: MutablePreferences) {
        listOf("themePreset", "mangaPaper", "mangaAccent").forEach { prefs.remove(stringPreferencesKey(it)) }
        listOf("miuixMonet", "themeDecorations").forEach { prefs.remove(booleanPreferencesKey(it)) }
    }
}
