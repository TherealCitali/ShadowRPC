/*
 * ShadowRPC (2026)
 * GPL-3.0 License | Contributors: see git history
 * Discord gateway and OAuth code originates from LunarTune (github.com/cognitiveshadows03/LunarTune), GPL-3.0.
 */

package dev.citali.shadowrpc.discord

object GatewayOp {
    const val DISPATCH = 0
    const val HEARTBEAT = 1
    const val IDENTIFY = 2
    const val PRESENCE_UPDATE = 3
    const val VOICE_STATE_UPDATE = 4
    const val RESUME = 6
    const val RECONNECT = 7
    const val INVALID_SESSION = 9
    const val HELLO = 10
    const val HEARTBEAT_ACK = 11
}

val NON_RESUMABLE_CLOSE_CODES: Set<Int> = setOf(4004, 4010, 4011, 4012, 4013, 4014)

object GatewayDefaults {
    const val API_BASE = "https://discord.com/api"
    const val GATEWAY_URL = "wss://gateway.discord.gg"
    const val GATEWAY_VERSION = 9
    const val USER_AGENT = "Discord Embedded/1.9.15780"
    const val HELLO_TIMEOUT_MS = 20_000L
}

object ActivityTypes {
    private val keys = listOf("PLAYING", "STREAMING", "LISTENING", "WATCHING", "CUSTOM", "COMPETING", "HANG")
    private val forward = keys.filter { it.isNotEmpty() }.mapIndexed { i, k -> k to i }.toMap()
    private val reverse = forward.entries.associate { (k, v) -> v to k }

    fun fromString(name: String): Int? = forward[name]

    fun fromInt(value: Int): String? = reverse[value]
}

object IntentsFlags {
    /**
     * ShadowRPC only publishes presence. It never needs DM, call or relationship
     * events, so the IDENTIFY intents are limited to what the Social SDK grants
     * a presence-only application.
     */
    val FLAGS: Map<String, Int> =
        mapOf(
            "USER_PRESENCE" to (1 shl 23),
        )
}

object GatewayCapabilitiesFlags {
    val FLAGS: Map<String, Int> =
        mapOf(
            "DEDUPE_USER_OBJECTS" to (1 shl 4),
            "PRIORITIZED_READY_PAYLOAD" to (1 shl 5),
            "AUTO_CALL_CONNECT" to (1 shl 12),
            "AUTO_LOBBY_CONNECT" to (1 shl 16),
        )
}
