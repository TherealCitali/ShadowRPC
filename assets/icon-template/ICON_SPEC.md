# ShadowRPC icon — what to deliver

The supplied fox-mask artwork is now installed; see [current asset notes](../../docs/ICON_ASSETS.md).
The guidance below is retained for future layered/vector source updates.

## 1. Adaptive launcher icon (Android 8+) — the important one

Two **separate layers**, each a **1024×1024 px** square (= 108×108 dp at 9.48 px/dp):

| Layer | File | Rules |
|---|---|---|
| Foreground | `ic_launcher_foreground.png` (transparent PNG) **or** an SVG | All art inside the **72 dp safe circle** (purple in `adaptive_icon_zones.png`); the main glyph ideally inside 48 dp (red). Launchers crop to a circle, squircle, rounded square or teardrop *and* may zoom/parallax the layers — anything outside the safe zone gets cut. |
| Background | `ic_launcher_background.png` **or** a single hex colour | Full bleed, flat or a subtle gradient. No details near the edges. Must not contain the glyph. |

Design tips that matter for this app:
- **Single strong silhouette** that reads at 48 px: a shadow/crescent shape, a presence dot, a controller glyph — one idea, not three.
- Avoid thin lines (< 4 dp) and text; both disappear at launcher size.
- Test against **dark and light backgrounds** — the icon sits on both wallpapers.
- Don't use Discord's logo/Clyde or a game's logo (trademark; also the Social SDK terms).

## 2. Monochrome / themed icon (Android 13+ "Themed icons")

`ic_launcher_monochrome` — **one-colour** flat silhouette, same 108 dp canvas and 72 dp safe zone, ideally an **SVG / VectorDrawable**. Android recolours it to the wallpaper palette, so no gradients, no opacity tricks. Usually the foreground glyph reduced to a solid shape.

## 3. Discord Developer Portal app icon

`discord_app_icon.png` — **512×512 px**, square, opaque. Discord displays it **circle-cropped** in the profile "Playing …" card and rounded in the app list, so keep the subject centered with ~10 % margin. This is what users see next to "Playing ShadowRPC".

## 4. Optional: Rich-Presence large image (default when the detected app has no icon)

`presence_default.png` — **512×512** (Discord minimum), same rules as above.

## 5. Nice-to-have

- `icon_source.svg` / `.fig` — the master vector so future sizes can be regenerated.
- A **splash** variant: the foreground glyph alone on transparent, 288×288 dp (2732 px); used by the Android 12 splash screen, which masks to a circle with a 2/3-diameter safe area.

## Formats & delivery

- PNG (24-bit + alpha) or SVG. **No JPEG for foreground/monochrome** (no alpha).
- Drop them into `assets/icon-src/` in a PR/commit; I'll generate the density buckets (mdpi…xxxhdpi), the legacy round icons for Android 7, and the vector drawable.
