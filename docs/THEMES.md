# Theme engines and presets

Appearance → Theme preset offers **Material You**, **Manga**, and **MIUI / Miuix**. Existing installs default to Material You with their existing settings. Montserrat remains the UI font in every preset.

## Engines and controls

- **Material You:** original MaterialKolor seed engine, system wallpaper colours on Android 12+, dark/system mode and pure black.
- **Manga:** Komi's actual paper/ink palette model and accent resolver, adapted into ShadowRPC's shared theme tokens and Material color roles. Day, Night and Nord papers; Auto follows the dark-mode setting. Accents: Mono, Crimson, Cobalt, Sun and Frost. Square panels, 3dp ink outlines, 6dp hard shadows and a static paper grid. Decorations can be disabled. Explicit paper choices override the general dark-mode choice. Pure black overrides dark paper surfaces when enabled.
- **MIUI / Miuix:** InstallerX's default-versus-Monet controller routing, backed by the real Miuix `ThemeController` dependency (stable 0.9.3, not InstallerX's snapshot). Native light/dark palette by default; optional Monet uses the saved seed on Android 10, or wallpaper colours on Android 12+ when enabled. Shared switches use Miuix's actual component. Group radii adapt to 16dp. Miuix color roles are bridged into the existing Material screens/dialogs; this is not a wholesale copy of InstallerX's installer UI.

The engine provides colors, shape tokens, panel borders, decorations, typography and component routing. Material role mapping covers existing screens, while shared explicit card/sheet shapes honor the selected preset. Existing navigation/form state is preserved using InstallerX's movable-content routing approach. No theme operation changes master RPC, detection settings, icons sent to Discord, or account credentials.

Theme choice and its options are included in the settings-backup allow-list with enum/boolean validation. No extra permissions, background service, network theme fetch, blur shader or continuously animated wallpaper is introduced. The paper grid uses a tiny cached repeating tile; only foreground Compose UI uses the engines.

## Source provenance and licenses

### Komi Store (Apache-2.0)

Repository: https://github.com/komi-store/komi-store

Revision: `e26fb15f88293df0df06fc1c03414a4f8dbbacd7`

Source tree: `core/presentation/src/commonMain/kotlin/zed/rainxch/core/presentation/personality/`.

- `manga/MangaColors.kt`, `MangaAccentSwatch.kt`, `MangaAccent.kt`, `MangaPaper.kt`, `model/PersonalityColors.kt`: ported with package/import changes, notices added, original palette values retained under `ui/theme/manga/`.
- `utils/PersonalityThemeProvider.kt`, `manga/MangaShape.kt`, `MangaShadow.kt` and `manga/decoration/{GridPaper,InkModifiers}.kt`: role mapping, shape/shadow values and paper treatment adapted in `ThemeEngine.kt`; grid implementation changed to a cached tile. Typography deliberately remains Montserrat. Komi app resources, business logic, navigation and font-routing subsystem are not copied.

Bundled license: `app/src/main/assets/licenses/Komi-Store-Apache-2.0.txt`.

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

Added backup round-trip/invalid-theme unit coverage. Static validation covers XML/resource names, theme-key allow-lists, source notices and pinned dependency. CI compilation is checked separately; device checks still needed: API 29 and API 31+, every preset in light/dark/pure-black, all paper/accent combinations, switching while on Appearance without navigation reset, relaunch/persistence, imports, large fonts/TalkBack, and scrolling under memory pressure. This UI work does not prevent Android low-memory process kills.
