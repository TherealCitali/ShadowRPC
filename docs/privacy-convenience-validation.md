# Privacy and convenience validation

## Automated coverage included

`SettingsBackupTest` covers allowed export fields, exclusion of credentials/runtime state/icon links, account/consent/master preservation during import, malformed types, numeric bounds, enum values, nested overrides, file size and nesting limits. Run `./gradlew :app:testDebugUnitTest` with the project's JDK 21 / Android SDK toolchain. These JVM tests and the Android build were not executed in the editing environment (Java 11 only, no Android SDK).

Static checks performed: XML parsing and unique strings, newly referenced resource existence, legal asset/docs parity, consent gates in Home and the upload boundary, backup exclusion allow-list, tile manifest binding permission, unchanged selected-app/detection-toggle semantics, and whitespace/diff integrity.

## Device/CI checks still required

- Publish a selected app, lock during grace and during an in-flight update: clear without grace; stay suppressed while locked; resume on unlock. Repeat screen-off rapidly, then master Off: no resurrected watcher/notification.
- Pause 15 minutes / 1 hour / manually. Close/reopen, sleep/wake and reboot: retain deadline/manual pause, never publish while suppressed. Resume and check refreshed elapsed session. Master Off must win over timer expiry.
- On a fresh install and an upgrade with Show icon already On, verify no Catbox requests before accepting. Decline still permits text-only RPC. Accept, revoke, and check subsequent payloads omit icons. An already-started anonymous upload cannot be undone.
- Export to local/cloud SAF providers; cancel picker; import a valid backup and cancel confirmation. Confirm apply preserves session, detection/master toggles and consent. Reject invalid/oversized/deeply nested files without partial writes or logging raw backup contents.
- Quick Settings: add/edit tile, sign-out click opens app, locked click requests unlock, toggle master repeatedly and from Home/notification; tile reflects persisted master state. Off removes foreground notification while keeping selections/detection enabled. On respects saved detection and Usage access. Test API 26, 29, 34+ activity-launch/service rules.
- Check Montserrat, compact/large font Settings layout, long translations, all dialogs and document picker restoration. Rotation during file operations may cancel them; errors are reported and import confirmation is not applied silently.
- About/exported logs: Actions commit short SHA and UTC date, local builds explicitly show `local`.
