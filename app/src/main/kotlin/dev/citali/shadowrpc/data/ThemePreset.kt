package dev.citali.shadowrpc.data

/** MANGA is accepted only as a legacy stored value, never offered as a preset. */
enum class ThemePreset {
    MATERIAL_YOU, MIUI;

    companion object {
        fun fromStored(value: String): ThemePreset = when (value) {
            "MIUI", "MANGA" -> MIUI
            else -> MATERIAL_YOU
        }
    }
}
