# ShadowRPC icon assets

## Artwork and attribution

The artwork was supplied by TherealCitali. LunarTune’s icon was used as a design reference, as stated by the contributor. This attribution does not imply LunarTune endorsement or independently establish the provenance/licensing of any third-party elements within the supplied artwork.

Original PNGs, kept byte-for-byte unchanged:

- `Assets/ShadowRPC-Icon.png`: rounded composition with alpha.
- `Assets/ShadowRPC-Icon-Square.png`: square composition.

## Android assets

`tools/generate_icons.py` (Python + Pillow) generates mdpi through xxxhdpi launcher, round and adaptive-foreground PNGs; splash art; notification artwork; white-alpha notification icons; and the themed-icon vector. Run it from any directory with `python tools/generate_icons.py`.

Adaptive icons use the supplied full square composition on the existing black backing. Because the supplied images are flattened, there is no separate movable fox-mask layer. Android launcher masks may crop peripheral text, textures and character details. The central mask is retained; full-colour artwork is not redrawn. The monochrome/themed and small notification icons are deliberately simplified fox-mask silhouettes because Android tints these assets to one colour.

The supplied round composition is inset into a transparent splash canvas for Android splash masking. The notification’s large artwork also uses the supplied round composition. Actual detected-app icons and the illustrative presence preview are unchanged.

`assets/discord_app_icon_512.png` is a square 512px export for manual upload to the ShadowRPC application in the Discord Developer Portal. Changing this repository file does not update Discord’s registered application icon remotely.

## Screenshots

The five screenshot PNGs previously in the repository root are now in `assets/screenshot/`, retaining their original filenames and bytes. This includes `Untitled7_20260917181855.png`, which is also a phone UI screenshot. Their content is unchanged and may show earlier branding/reference UI.

## Validation

Checked generated dimensions and formats, original/source and moved-screenshot byte parity, XML parsing, README local links, and unchanged presence-preview assets. Full Android build and launcher/device visual checks remain pending; the local environment has Java 11 and no configured Android SDK.
