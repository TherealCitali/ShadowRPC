# Privacy Policy

Effective date: 17 September 2026

## Who maintains ShadowRPC

ShadowRPC (Android package dev.citali.shadowrpc) is an open-source app maintained by TherealCitali. It displays selected foreground apps or games as Discord Rich Presence. This policy describes this repository’s app, not modified third-party builds. For privacy questions, contact the maintainer through https://github.com/TherealCitali/ShadowRPC/issues. Issues are public: never post tokens, private logs, or other sensitive information there. You do not need to disclose your real identity to use ShadowRPC.

## Information processed on your device

With Usage access enabled, ShadowRPC reads Android usage events to determine which app is in the foreground. It reads installed-app names, package identifiers, categories and launcher icons to show the app list and create presence. It does not use this permission to read messages, passwords, screen contents or files inside those apps.

Settings stored locally include selected apps, per-app names and presence overrides, appearance choices, RPC preferences and cached icon URLs. Foreground detection runs as a foreground service when enabled. It can retain the last selected app’s presence for about 150 seconds after you leave it; turning master RPC off clears it without waiting for that grace period.

## Discord sign-in and presence

Sign-in opens Discord’s OAuth2 authorization page with PKCE. Requested scopes are openid, identify and sdk.social_layer_presence. ShadowRPC does not ask for your Discord password or scrape your Discord user token. It stores OAuth access/refresh tokens, expiry information and account profile information locally, including your Discord username, display name and avatar URL. Tokens authorize presence updates and refresh your session. Profile images may be fetched from Discord’s CDN.

When RPC is enabled, ShadowRPC sends Discord the configured activity name, details, state, type, status, optional timestamps and image references under ShadowRPC’s application ID (or your configured override). Depending on your templates, this may include app names, package names, categories and custom text. Discord determines who can view the activity under its settings and policies. Other people may copy visible activity; clearing it cannot recall copies.

## Optional public icon uploads

Show app icon is optional. When enabled, an app’s launcher icon is uploaded without a Catbox account to https://catbox.moe/user/api.php. The upload filename includes the Android package name. Catbox returns a publicly accessible URL, which ShadowRPC caches locally and passes to Discord for image registration/display. Catbox and Discord receive ordinary network information, such as your IP address. Do not enable this option for artwork you do not want publicly hosted.

Turning the option off stops using app icons for subsequent updates; clearing saved icon links removes local cached URLs only. It does NOT delete files already uploaded to Catbox. ShadowRPC has no automatic remote-deletion mechanism for these anonymous uploads. Remote retention and removal depend on the provider; no retention period is promised here.

## Logs, crash reports and floating logs

Diagnostics are kept in a bounded in-memory buffer (up to 400 entries). They may contain timestamps, app/package names, configured activity text, connection events, device API/version information and errors. Verbose logging is temporary and adds detail. Common credential patterns are filtered, but redaction is not guaranteed to catch every sensitive value.

A bounded last-crash report is stored in app-private no-backup storage and shown after restart. On Android 11+, the app may read Android’s record of its previous process exit to help diagnose crashes and system stops. There is no automatic diagnostic upload to the maintainer. Clear logs clears the current buffer and requests deletion of the saved crash report; Android’s own exit history is managed by the operating system.

Copy logs writes to your clipboard. Export logs writes a text file to a location you choose through Android’s document picker; that provider may be a cloud service. Exported/copied files are outside ShadowRPC’s control. Review them before sharing and delete them separately if needed.

Floating logs require Display over other apps permission and show ShadowRPC’s own logs in a movable overlay. They do not capture other apps’ screens. The overlay is temporary, can be closed using its close button or notification Stop action, and does not automatically restart after process death or reboot. Visible logs can be seen by anyone viewing or recording your screen.

## Collection, retention and your controls

This version does not include advertising or analytics SDKs and does not run a maintainer-operated data-collection backend. Third-party services receive information necessary for the connections described above. External links open their own services, subject to their policies.

Settings and session data remain locally until replaced or removed. Sign out clears the local Discord session; you can also revoke ShadowRPC under Discord’s Authorized Apps. Disable RPC or detection to stop sharing, revoke Usage access or overlay permission in Android Settings, and use Android’s Clear storage or uninstall to remove app-private data. Account/session cleanup does not delete exported logs or data held by Discord/Catbox. Android backup/transfer behaviour depends on OS and device implementation; the app excludes its DataStore directory from its legacy backup rules and keeps crash reports in no-backup storage. No storage or transmission method is perfectly secure.

## Third-party policies, eligibility and updates

Discord: https://discord.com/privacy and https://discord.com/terms
Catbox: https://catbox.moe/ — consult its current policies before enabling uploads.
GitHub: https://docs.github.com/en/site-policy/privacy-policies/github-general-privacy-statement

Use requires meeting Discord’s minimum-age and eligibility rules in your jurisdiction. ShadowRPC is not intended to collect information from children below that age. Privacy rights vary by jurisdiction; use the contact above to request clarification without publishing personal information. This policy may change with the app; the effective date and repository history identify revisions. Material changes to data practices should be reviewed before using an updated version.
