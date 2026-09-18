package dev.citali.shadowrpc.data

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class PreferenceState<T>(
    private val state: State<T>,
    val set: (T) -> Unit,
) {
    val value: T get() = state.value

    operator fun component1(): T = value

    operator fun component2(): (T) -> Unit = set
}

@Composable
fun <T> rememberPreference(
    key: Preferences.Key<T>,
    defaultValue: T,
): PreferenceState<T> {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state =
        produceState(initialValue = defaultValue, key) {
            context.dataStore.data
                .map { it[key] ?: defaultValue }
                .collect { value = it }
        }
    return remember(key, state) {
        PreferenceState(state) { newValue ->
            scope.launch { context.dataStore.edit { it[key] = newValue } }
        }
    }
}

@Composable
inline fun <reified E : Enum<E>> rememberEnumPreference(
    key: Preferences.Key<String>,
    defaultValue: E,
): PreferenceState<E> {
    val raw = rememberPreference(key, defaultValue.name)
    return remember(raw) {
        PreferenceState(
            state =
                object : State<E> {
                    override val value: E
                        get() = enumValues<E>().firstOrNull { it.name == raw.value } ?: defaultValue
                },
            set = { raw.set(it.name) },
        )
    }
}

suspend fun <T> Context.pref(
    key: Preferences.Key<T>,
    defaultValue: T,
): T = dataStore.data.map { it[key] ?: defaultValue }.first()

suspend fun <T> Context.setPref(
    key: Preferences.Key<T>,
    value: T,
) {
    dataStore.edit { it[key] = value }
}

/** Resolve retired Manga selections immediately, then persist MIUI without resetting other settings. */
@Composable
fun rememberThemePreset(): PreferenceState<ThemePreset> {
    val raw = rememberPreference(Prefs.ThemePresetKey, ThemePreset.MATERIAL_YOU.name)
    androidx.compose.runtime.LaunchedEffect(raw.value) {
        if (raw.value == "MANGA") raw.set(ThemePreset.MIUI.name)
    }
    return remember(raw) {
        PreferenceState(object : State<ThemePreset> {
            override val value get() = ThemePreset.fromStored(raw.value)
        }, set = { raw.set(it.name) })
    }
}
