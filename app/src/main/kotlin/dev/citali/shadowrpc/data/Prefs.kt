package dev.citali.shadowrpc.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
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

    const val DefaultGraceSeconds = 15
    const val MaxGraceSeconds = 180
    val BackgroundGraceSecondsKey = intPreferencesKey("backgroundGraceSeconds")

    // App detection
    val AppDetectionEnabledKey = booleanPreferencesKey("appDetectionEnabled")
    val AppDetectionPackagesKey = stringSetPreferencesKey("appDetectionPackages")
    val AppDetectionShowIconKey = booleanPreferencesKey("appDetectionShowIcon")
    val AppDetectionTimestampsKey = booleanPreferencesKey("appDetectionTimestamps")

    // Presence
    val AppPresenceOverridesKey = stringPreferencesKey("appPresenceOverrides")
    val RpcEnabledKey = booleanPreferencesKey("rpcEnabled")
    val ActivityTypeKey = stringPreferencesKey("activityType") // PLAYING / LISTENING / WATCHING / COMPETING
    val ActivityStatusKey = stringPreferencesKey("activityStatus") // online / idle / dnd
    // Presence text lines, each with an ActivitySource (APP / SHADOWRPC / PACKAGE / CATEGORY / CUSTOM / NONE)
    val ActivityNameSourceKey = stringPreferencesKey("activityNameSource")
    val ActivityDetailsSourceKey = stringPreferencesKey("activityDetailsSource")
    val ActivityStateSourceKey = stringPreferencesKey("activityStateSource")
    val ActivityNameCustomKey = stringPreferencesKey("activityNameCustom") // templates for CUSTOM
    val ActivityDetailsCustomKey = stringPreferencesKey("activityDetailsCustom")
    val ActivityStateCustomKey = stringPreferencesKey("activityStateCustom")
    val AppLabelOverridesKey = stringPreferencesKey("appLabelOverrides") // JSON object: package -> display name
    val CustomApplicationIdKey = stringPreferencesKey("customApplicationId")
    val LowResolutionImagesKey = booleanPreferencesKey("lowResolutionImages")

    // Display
    val DarkModeKey = stringPreferencesKey("darkMode") // AUTO / ON / OFF
    val PureBlackKey = booleanPreferencesKey("pureBlack")
    val DynamicColorKey = booleanPreferencesKey("dynamicColor")
    val SeedColorKey = longPreferencesKey("seedColor")
}

enum class DarkMode { AUTO, ON, OFF }
