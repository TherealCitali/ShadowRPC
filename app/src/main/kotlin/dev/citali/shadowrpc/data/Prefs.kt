package dev.citali.shadowrpc.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object Prefs {
    // Discord session (names match LunarTune so the OAuth repository ports unchanged)
    val DiscordTokenKey = stringPreferencesKey("discordToken")
    val DiscordRefreshTokenKey = stringPreferencesKey("discordRefreshToken")
    val DiscordTokenExpiresAtKey = longPreferencesKey("discordTokenExpiresAt")
    val DiscordApplicationIdKey = stringPreferencesKey("discordApplicationId")
    val DiscordUsernameKey = stringPreferencesKey("discordUsername")
    val DiscordNameKey = stringPreferencesKey("discordName")
    val DiscordAvatarUrlKey = stringPreferencesKey("discordAvatarUrl")

    // App detection
    val AppDetectionEnabledKey = booleanPreferencesKey("appDetectionEnabled")
    val AppDetectionPackagesKey = stringSetPreferencesKey("appDetectionPackages")
    val AppDetectionShowIconKey = booleanPreferencesKey("appDetectionShowIcon")
    val AppDetectionTimestampsKey = booleanPreferencesKey("appDetectionTimestamps")

    // Presence
    val ActivityTypeKey = stringPreferencesKey("activityType") // PLAYING / LISTENING / WATCHING / COMPETING
    val ActivityStatusKey = stringPreferencesKey("activityStatus") // online / idle / dnd
    val ActivityNameModeKey = stringPreferencesKey("activityNameMode") // APP (detected app) / SHADOWRPC
    val CustomApplicationIdKey = stringPreferencesKey("customApplicationId")
    val LowResolutionImagesKey = booleanPreferencesKey("lowResolutionImages")

    // Display
    val DarkModeKey = stringPreferencesKey("darkMode") // AUTO / ON / OFF
    val PureBlackKey = booleanPreferencesKey("pureBlack")
    val DynamicColorKey = booleanPreferencesKey("dynamicColor")
    val SeedColorKey = longPreferencesKey("seedColor")
}

enum class DarkMode { AUTO, ON, OFF }
