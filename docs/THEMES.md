# Material You and elastic overscroll

ShadowRPC uses **Material You only**. There is no theme-preset selector, MIUI palette/controller, Miuix switch, or Manga rendering path. Montserrat, the original rounded Material surfaces, expressive controls, seed colors, Android 12+ wallpaper colors, dark/system mode and pure black are retained.

## The one Miuix behavior retained

`ShadowRpcTheme` provides the actual Miuix `MiuixOverscrollFactory` through Compose Foundation’s `LocalOverscrollFactory`. This supplies spring-based edge elasticity to default Compose scrollable components such as the Home app list, settings’ vertical scrolling, and the horizontal color picker. Each scrollable creates its own effect; no additional nested-scroll modifier is layered on top. Existing sheet entrance behavior is unchanged.

The app does **not** wrap content in `MiuixTheme` or install its colors, typography, shapes, controls, overscroll-unrelated indications, or Monet controller. The `miuix-ui-android:0.9.3` dependency remains because the upstream factory lives in that module. Release shrinking can remove unreachable library code; the entire dependency is not claimed to be removed.

No new background work or service is introduced. These are foreground scroll interactions, not a solution for Android low-memory process kills.

## Upgrade and backup compatibility

- On theme composition, remove only retired settings: `themePreset`, `miuixMonet`, `mangaPaper`, `mangaAccent`, `themeDecorations`.
- Preserve the existing Material seed, dynamic-color flag, dark mode and pure-black choice, as well as every account/RPC/detection preference.
- Older backups remain accepted. Retired keys are ignored via the existing allow-list and no longer exported, so they cannot restore a deleted theme.

## Sources and licenses

Retained overscroll: https://github.com/miuix-kotlin-multiplatform/miuix, version `v0.9.3`, commit `c36fab72391801d1e3ea5a00f966bf16bac28d4c`.

Implementation inspected: `miuix-ui/src/commonMain/kotlin/top/yukonga/miuix/kmp/utils/OverscrollFactory.kt` (spring physics shared with the library’s scrolling utilities).

Copyright 2025, compose-miuix-ui contributors. Apache-2.0; bundled at `app/src/main/assets/licenses/Miuix-Apache-2.0.txt`.

Historical provenance for removed theme ports: Komi Store at `e26fb15f88293df0df06fc1c03414a4f8dbbacd7` (Apache-2.0), InstallerX Revived at `8ede27250d04b73631b59464724c240378406cdd` (GPL-3.0-only, Copyright 2025–2026 InstallerX Revived contributors). Their license copies remain bundled for provenance; their presets are no longer active.

## Validation

Regression tests cover Material appearance round-trips, cleanup of retired settings, and legacy MIUI/Manga imports without account/runtime-state changes. Static checks verify that the only remaining Miuix API referenced by the app is the overscroll factory. CI build/signing and tests are checked separately. Device checks remain needed: overscroll at both ends of Home and Settings, horizontal seed picker, sheet scrolling/dismissal, rapid flings, nested scroll handling and API 29 behavior.
