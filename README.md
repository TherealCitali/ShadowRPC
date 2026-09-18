<div align="center">

 <img src="assets/ShadowRPC-Icon.png" width="160" height="160" alt="App icon" style="border-radius: 22%">

# ShadowRPC

**Discord Rich Presence for whatever you are doing on your phone.**

ShadowRPC watches which app is in the foreground and mirrors it to your Discord profile
as *Playing &lt;game&gt;* — using the same OAuth2 + gateway stack that powers
[LunarTune](https://github.com/cognitiveshadows03/LunarTune)'s "Listening to" presence.

[![Build](https://github.com/TherealCitali/ShadowRPC/actions/workflows/build.yml/badge.svg)](https://github.com/TherealCitali/ShadowRPC/actions/workflows/build.yml)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)
<img src="https://img.shields.io/github/downloads/TherealCitali/ShadowRPC/total?style=for-the-badge&color=6366f1&labelColor=1e1e2e&logo=github" alt="Downloads" />

</div>

## Features

| | Status |
|---|---|
| **App Detection** – pick the apps/games you want to share; ShadowRPC shows the one in the foreground | ✅ |
| Activity type (Playing / Listening / Watching / Competing), activity name, online status | ✅ |
| Optional app icon + elapsed time on the presence | ✅ |
| Material You, Manga (Komi), MIUI / Miuix presets; seed colours and pure black | ✅ |
| Media RPC (now-playing from any player) | 🔜 |
| Custom RPC / Console RPC | 🔜 |

No bot, no Discord user token: you sign in with Discord's OAuth2 (PKCE) and the app talks to
Discord with the resulting scoped token, exactly like a desktop game would.

## FAQ

**Why does it need Usage access?** That is the only Android API that reports the foreground
app without an accessibility service. It is granted in *Settings → Special app access → Usage access*
and can be revoked anytime; ShadowRPC stops itself when it is.

**Nothing shows up on my profile.** Discord only shows presence from a device that has the
"Share detected activities" toggle on and no other client overriding it. Check *Logs* in the drawer.

**Android 10 support?** Yes — minSdk 26; the detection loop runs in a foreground service.

## Credits

- [LunarTune](https://github.com/cognitiveshadows03/LunarTune) — Discord gateway / OAuth code (GPL-3.0)
- ShadowRPC icon artwork supplied by TherealCitali; [original PNGs](assets/) and [asset notes](docs/ICON_ASSETS.md).

- [Komi Store](https://github.com/komi-store/komi-store) — Manga palettes/personality styling (Apache-2.0).
- [InstallerX Revived](https://github.com/wxxsfxyzm/InstallerX-Revived) — MIUI engine routing (GPL-3.0), using [Miuix](https://github.com/miuix-kotlin-multiplatform/miuix) (Apache-2.0).
- [Theme engine details, source revisions and adaptations](docs/THEMES.md).

## License

GPL-3.0 — see [LICENSE](LICENSE).


## Privacy and terms

Read the [Privacy Policy](docs/PRIVACY.md) and [Terms of Use](docs/TERMS.md).
Both are also available offline inside **About**. The app source remains licensed
under GPL-3.0; the terms do not restrict rights granted by that licence.

Prerelease retention: after a successful automatic publication, the workflow keeps
the five most recently published prereleases across the repository and deletes
older prereleases and their assets. Stable releases and drafts are excluded.
Automated `prerelease-<commit SHA>` tags belonging to deleted releases are removed;
other tags are retained. Actions artifacts have their own retention settings.

### Privacy and convenience

Settings includes optional clear-on-lock (bypasses grace), pause for 15 minutes / 1 hour / until resumed, and JSON settings export/import through Android’s file picker. Timer resumption is best-effort on the next detection poll; Android sleep or clock changes can delay it. Master Off still removes the watcher and its notification.

Backups include app selections/custom text but exclude account credentials, logs, icon URLs/consent, runtime toggles and pause deadlines. Import validates recognized fields before a confirmation and atomic apply. Public Catbox icon uploads now require an explicit disclosure/consent, including for existing installations. Revoke consent in Settings; clearing saved icon links only removes local URLs, not remote uploads.

Add **ShadowRPC** from Android Quick Settings → Edit. The tile toggles master RPC after unlocking and preserves detection preferences and selected apps. App Detection must already be enabled to start sharing. Build commit/date appear in About and exported logs.
