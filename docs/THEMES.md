# Theme engines and presets

Appearance offers **Material You** and **MIUI / Miuix**. Montserrat and the existing MIUI controller/components are retained. Existing Material You and MIUI preferences are unchanged.

## Removal and migration

The Manga preset has been removed, including paper palettes, grid rendering, hard shadows, ink borders and settings. Legacy stored `MANGA` values resolve to MIUI immediately, then persist as `MIUI`; old backups also map to MIUI. Old paper/accent/decoration fields are ignored and no longer exported. The migration changes only theme selection, not RPC, detection, accounts or saved apps.

## Engines

- **Material You:** MaterialKolor seed colors, optional Android 12+ wallpaper colors, dark/system mode and pure black.
- **MIUI / Miuix:** InstallerX-derived routing backed by Miuix 0.9.3. Native light/dark palette by default, optional Monet using saved seed colors or Android 12+ wallpaper colors. Actual Miuix switches and a color-role bridge for the existing Material screens. Rounded group radii remain 16dp.

Both preserve live navigation state when switching. No new UI libraries or additional presets have been added during Manga removal.

## Source provenance and licenses

### Removed Manga theme (historical provenance)

The Komi Store Manga preset and its ported renderer/palette files have been removed at the user's request. Historical source: https://github.com/komi-store/komi-store at `e26fb15f88293df0df06fc1c03414a4f8dbbacd7`. Its Apache-2.0 license remains bundled for provenance; there is no active Manga theme.

### InstallerX Revived (GPL-3.0-only)

Repository: https://github.com/wxxsfxyzm/InstallerX-Revived

Revision: `8ede27250d04b73631b59464724c240378406cdd`

Copyright (C) 2025–2026 InstallerX Revived contributors.

`app/src/main/java/com/rosan/installer/ui/theme/InstallerTheme.kt` and `Shape.kt`: default/Monet routing, theme controller selection, movable-content preservation and segmented-card concept adapted into `Theme.kt` and `ThemeEngine.kt`. ShadowRPC retains its own DataStore keys, Material UI, Montserrat, default motion and MaterialKolor setup. Installer-specific view models/pages, blur, color-spec chooser and animated color interpolation are not imported.

Bundled license: `app/src/main/assets/licenses/InstallerX-Revived-GPL-3.0.txt`.

### Miuix (Apache-2.0)

Repository: https://github.com/miuix-kotlin-multiplatform/miuix

Dependency: `top.yukonga.miuix.kmp:miuix-ui-android:0.9.3` (core/squircle transitively).

Source inspected at `v0.9.3`, commit `c36fab72391801d1e3ea5a00f966bf16bac28d4c`.

Copyright 2025, compose-miuix-ui contributors.

Bundled license: `app/src/main/assets/licenses/Miuix-Apache-2.0.txt`.

## Validation

Regression tests cover Material/MIUI round-trips and retired Manga selections/backups mapping to MIUI without changing account or running-state preferences. Unknown preset values in backups still fail validation; obsolete Manga-only fields are ignored and no longer exported. Device checks remain necessary for theme switching, restart, dark/system/pure-black mode and legacy imports. No memory-kill workaround is implied.
