package dev.citali.shadowrpc.data

import androidx.datastore.preferences.core.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class SettingsBackupTest {
    private fun backup(settings: JSONObject) = JSONObject().put("format", "ShadowRPC-settings").put("version", 1).put("settings", settings).toString()

    @Test fun exportExcludesSensitiveAndRuntimeState() {
        val prefs = mutablePreferencesOf(
            Prefs.DiscordTokenKey to "private-token", Prefs.DiscordRefreshTokenKey to "private-refresh",
            Prefs.DiscordUsernameKey to "private-user", Prefs.IconUploadConsentKey to true,
            Prefs.PauseUntilKey to -1L, Prefs.RpcEnabledKey to true, Prefs.AppDetectionEnabledKey to true,
            stringPreferencesKey("iconUrl:com.example.app") to "https://example.com/icon.png",
            Prefs.AppDetectionPackagesKey to setOf("com.example.app"), Prefs.BackgroundGraceSecondsKey to 15,
        )
        val exported = SettingsBackup.encode(prefs)
        val values = SettingsBackup.validate(exported)
        assertEquals(setOf("appDetectionPackages", "backgroundGraceSeconds"), values.keys().asSequence().toSet())
        assertFalse(exported.contains("private-"))
    }

    @Test fun importPreservesAccountConsentAndRunningState() {
        val prefs = mutablePreferencesOf(Prefs.DiscordTokenKey to "original", Prefs.RpcEnabledKey to false,
            Prefs.AppDetectionEnabledKey to false, Prefs.IconUploadConsentKey to false, Prefs.PauseUntilKey to -1L)
        SettingsBackup.applyTo(prefs, backup(JSONObject().put("discordToken", "injected").put("rpcEnabled", true)
            .put("appDetectionEnabled", true).put("iconUploadConsentV1", true).put("pauseUntil", 0)
            .put("backgroundGraceSeconds", 30).put("appDetectionPackages", org.json.JSONArray(listOf("com.example.app")))))
        assertEquals("original", prefs[Prefs.DiscordTokenKey])
        assertEquals(false, prefs[Prefs.RpcEnabledKey])
        assertEquals(false, prefs[Prefs.AppDetectionEnabledKey])
        assertEquals(false, prefs[Prefs.IconUploadConsentKey])
        assertEquals(-1L, prefs[Prefs.PauseUntilKey])
        assertEquals(30, prefs[Prefs.BackgroundGraceSecondsKey])
        assertEquals(setOf("com.example.app"), prefs[Prefs.AppDetectionPackagesKey])
    }

    @Test fun materialAppearanceRoundTrips() {
        val original = mutablePreferencesOf(Prefs.DarkModeKey to "ON", Prefs.PureBlackKey to true,
            Prefs.DynamicColorKey to false, Prefs.SeedColorKey to 0xFFB69DF8L)
        val restored = mutablePreferencesOf()
        SettingsBackup.applyTo(restored, SettingsBackup.encode(original))
        original.asMap().forEach { (key, value) -> assertEquals(value, restored.asMap()[key]) }
    }

    @Test fun retiredThemesCannotReturnThroughBackups() {
        listOf("MIUI", "MANGA").forEach { oldTheme ->
            val prefs = mutablePreferencesOf(Prefs.RpcEnabledKey to false, Prefs.DiscordTokenKey to "original",
                Prefs.DarkModeKey to "ON", Prefs.SeedColorKey to 0xFFB69DF8L,
                stringPreferencesKey("themePreset") to oldTheme, booleanPreferencesKey("miuixMonet") to true)
            val exported = SettingsBackup.encode(prefs)
            assertFalse(exported.contains("themePreset"))
            assertFalse(exported.contains("miuixMonet"))
            RetiredThemeSettings.clear(prefs)
            SettingsBackup.applyTo(prefs, backup(JSONObject().put("themePreset", oldTheme)
                .put("miuixMonet", true).put("mangaPaper", "NORD").put("themeDecorations", true)))
            assertEquals(false, prefs[Prefs.RpcEnabledKey])
            assertEquals("original", prefs[Prefs.DiscordTokenKey])
            assertEquals("ON", prefs[Prefs.DarkModeKey])
            assertEquals(0xFFB69DF8L, prefs[Prefs.SeedColorKey])
            assertFalse(prefs.asMap().keys.any { it.name in setOf("themePreset", "miuixMonet", "mangaPaper", "themeDecorations") })
        }
    }

    @Test fun malformedSettingsFailBeforeMutation() {
        val bad = listOf(
            JSONObject().put("clearOnLock", "true"), JSONObject().put("backgroundGraceSeconds", -1),
            JSONObject().put("backgroundGraceSeconds", 181), JSONObject().put("backgroundGraceSeconds", 1.5),
            JSONObject().put("activityType", "BOGUS"), JSONObject().put("appDetectionPackages", "not-an-array"),
            JSONObject().put("appLabelOverrides", "{\"com.example.app\":42}"),
            JSONObject().put("appPresenceOverrides", "{\"com.example.app\":{\"type\":\"BOGUS\"}}"),
        )
        bad.forEach { values ->
            val prefs = mutablePreferencesOf(Prefs.BackgroundGraceSecondsKey to 15)
            assertTrue(runCatching { SettingsBackup.applyTo(prefs, backup(values)) }.isFailure)
            assertEquals(15, prefs[Prefs.BackgroundGraceSecondsKey])
        }
        assertTrue(runCatching { SettingsBackup.validate("[".repeat(50) + "]".repeat(50)) }.isFailure)
        assertTrue(runCatching { SettingsBackup.validate(" ".repeat(1_000_001)) }.isFailure)
        assertTrue(runCatching { SettingsBackup.validate(backup(JSONObject()).replace("\"version\":1", "\"version\":2")) }.isFailure)
    }
}
