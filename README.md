<div align="center">

# ShadowRPC

**Discord Rich Presence for whatever you are doing on your phone.**

ShadowRPC watches which app is in the foreground and mirrors it to your Discord profile
as *Playing &lt;game&gt;* — using the same OAuth2 + gateway stack that powers
[LunarTune](https://github.com/cognitiveshadows03/LunarTune)'s "Listening to" presence.

[![Build](https://github.com/TherealCitali/ShadowRPC/actions/workflows/build.yml/badge.svg)](https://github.com/TherealCitali/ShadowRPC/actions/workflows/build.yml)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)
[![Telegram](https://img.shields.io/badge/Telegram-LunarTuneGC-26A5E4?logo=telegram&logoColor=white)](https://t.me/LunarTuneGC)

</div>

## Features

| | Status |
|---|---|
| **App Detection** – pick the apps/games you want to share; ShadowRPC shows the one in the foreground | ✅ |
| Activity type (Playing / Listening / Watching / Competing), activity name, online status | ✅ |
| Optional app icon + elapsed time on the presence | ✅ |
| Material You theming, seed colours, pure black | ✅ |
| Media RPC (now-playing from any player) | 🔜 |
| Custom RPC / Console RPC | 🔜 |

No bot, no Discord user token: you sign in with Discord's OAuth2 (PKCE) and the app talks to
Discord with the resulting scoped token, exactly like a desktop game would.

## Building

1. Create a Discord **Application** at <https://discord.com/developers/applications>
   (a fresh one for ShadowRPC — never reuse another game's ID).
2. Copy `local.properties.example` to `local.properties` and set `DISCORD_APPLICATION_ID`.
   In the Developer Portal add the OAuth2 redirect `discord-<APPLICATION_ID>:/authorize/callback`.
3. `./gradlew assembleDebug` (JDK 21, Android SDK 37).

### CI secrets

| Secret | Purpose |
|---|---|
| `DISCORD_APPLICATION_ID` | Baked into release builds as the OAuth client / presence application id |
| `KEYSTORE` | Base64 of your release `.jks` |
| `KEY_ALIAS`, `KEYSTORE_PASSWORD`, `KEY_PASSWORD` | Keystore credentials |

Without the signing secrets the `Build APK` workflow still runs and uploads an unsigned APK.
Bumping `versionName` in `app/build.gradle.kts` on `main` triggers the `Release` workflow.

## FAQ

**Why does it need Usage access?** That is the only Android API that reports the foreground
app without an accessibility service. It is granted in *Settings → Special app access → Usage access*
and can be revoked anytime; ShadowRPC stops itself when it is.

**Nothing shows up on my profile.** Discord only shows presence from a device that has the
"Share detected activities" toggle on and no other client overriding it. Check *Logs* in the drawer.

**Android 10 support?** Yes — minSdk 26; the detection loop runs in a foreground service.

## Credits

- [LunarTune](https://github.com/cognitiveshadows03/LunarTune) — Discord gateway / OAuth code (GPL-3.0)
- [Kizzy](https://github.com/dead8309/Kizzy) — UI inspiration
- Lead: cognitiveshadows03 · Team: TherealCitali

## License

GPL-3.0 — see [LICENSE](LICENSE).


### Automatic test builds

Every push to `main` or `dev` (including documentation-only changes) runs the APK
build. After a successful signed build, **[Releases](https://github.com/TherealCitali/ShadowRPC/releases)**
gets a prerelease named with the branch and commit, with a directly downloadable
APK and SHA-256 checksum. Rerunning a workflow updates the same commit release;
prereleases do not replace the latest stable release. Pull requests never publish.
A push containing multiple commits builds the pushed branch tip, not each intermediate commit.

The repository must have `KEYSTORE`, `KEY_ALIAS`, `KEYSTORE_PASSWORD` and
`KEY_PASSWORD` configured for signing. Without signing, the unsigned Actions
artifact is retained but prerelease publication fails with an explicit message
rather than distributing a non-installable APK. The version-based stable release
workflow remains separate.
