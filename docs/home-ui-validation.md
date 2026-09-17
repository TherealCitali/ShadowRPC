# Home UI validation

The home screen uses Material colour roles, not hard-coded screenshot colours.
Android 12+ defaults to the wallpaper palette when no dynamic-colour preference
has been saved. Explicit opt-outs and saved custom seeds remain respected.
Older devices default to the sage seed. Display uses the same default as Theme.

Device checks still required (no Android SDK / JVM 21 in the editing environment):

- Android 12+: fresh preferences, light/dark wallpaper palettes; change wallpaper
  and return to the app; verify surfaces, pills, preview and toggle recolour.
- Display: disable dynamic colour, select a seed, restart, and confirm persistence;
  re-enable dynamic colour; exercise system/light/dark and pure-black modes.
- Android 8–11: sage fallback renders without calling dynamic-colour APIs.
- Home: Welcome, Account, General, Activity Content, App Detection in order;
  no social-link chips or feature grid. Check long names and 200% font size.
- Edit each activity line and verify preview updates and persists after restart.
- Test login/logout, denied usage access, Manage apps navigation, detection toggle.

Static checks: resource XML parses, added string references resolve, git diff
--check passes. Android compilation and screenshot comparison are not verified.

## Drawer and sheet polish

- Drawer width: 78% of window width, capped at 304 dp; current destination is
  highlighted and content scrolls on short windows / large font sizes.
- Preview uses bundled user-supplied `presence_sample.jpg`, also used in picker
  headers. This is illustrative artwork, not an asset uploaded to Discord.
- Activity type/status and line-source pickers use modal bottom sheets. Verify
  back, scrim tap, swipe-down dismissal and custom-template keyboard scrolling.
- Confirm Cancel discards line drafts, Save persists, and selecting a type/status
  persists immediately. Existing supported activity types are unchanged.
- Check spring entrance and preview size changes with animator duration scale
  normal and disabled. Animations use Compose's duration-scale-aware primitives;
  app-wide Material components receive the expressive motion scheme.

## Master RPC switch

- Signed-in account card shows Enable Rich Presence above Sign out, on by
  default to retain existing behaviour. Choice persists across app restarts.
- Off serializes with publishing, clears presence, closes the gateway and blocks
  future updates centrally. Detection remains configured but polling skips app
  lookup / icon upload while paused; the notification indicates paused state.
- On resumes on the next detection poll if detection is enabled. It does not
  silently enable detection or change the allow-list.
- Device checks pending: toggle during an in-flight publish, restart while off,
  refresh while off, resume with an allowed foreground app, logout/login and
  usage access revoked. Confirm no presence is published while paused.

## Per-app options

Long-press an app row (TalkBack action: App options). The floating editor shows
its actual launcher icon, app name and package. It supports display-name edits,
sharing allow-list membership, activity type and all three activity sources.
Each unset field inherits its global counterpart; CUSTOM supports the existing
placeholders. NONE is offered only for details/state. Reset overrides clears
name/type/content drafts but preserves the sharing selection; Save commits.
Cancel, close and outside-tap discard drafts. Save writes all edits atomically.

Pending device checks:
- Two apps with different overrides; verify each foreground app publishes its
  own type and text without changing global preferences or other apps.
- Type-only edits bypass request deduplication; source/template edits are applied
  by the existing ActivityTemplate (including duplicate collapse and limits).
- Change a global setting: inherited per-app fields follow it, overridden fields
  remain unchanged. Reset and save, restart, and confirm inheritance restored.
- Share off removes only that app from the allow-list; on adds it back. Master
  RPC-off remains respected. Save while an app is active updates on a later poll.
- Missing/malformed JSON falls back to defaults, app icon absent, long package
  name, large fonts, short landscape viewport and keyboard visible.

Static XML/resource and whitespace checks pass. No Android build or runtime
verification was possible in the current Java 11 / no Android SDK environment.

## Inline app management

Home now has a single LazyColumn: account / activity settings, App Detection and
Manage apps card, usage-access prompt (if needed), icon/timestamp controls,
optional search field and keyed installed-app rows. The separate App Detection
route and screen have been removed. Long-press still opens App options.

Pending device checks: scroll from Welcome to the last installed app without
nested scrolling issues; search using the button beside Manage apps; toggle
sharing, long-press and save overrides; grant/revoke Usage access and return to
Home; verify there is one detection switch and Back does not open another app
management page. Compilation remains unverified in this environment.

## Foreground detection and notification reliability

Replaced the fixed 60-second resume-event window with a one-day bootstrap and
incremental event tracking. Quiet intervals retain the active package; matching
pause/screen-off events clear it. Non-interactive / locked devices do not share.
Usage-event queries run on IO. Optional icon loading runs independently, so the
first text presence does not wait for Catbox. Poll exceptions log and retry.
Notification now distinguishes no foreground, unselected app, detected app and
Discord publishing failure. The small icon is an alpha-only gamepad vector.

Pending device regression checks (not executed here):
- Android 8/10/12+: open a selected app for >60 seconds; keep presence active.
- Start detection while an app is already open; switch selected/unselected apps,
  launcher, lock/unlock and activities within the same package.
- Block Catbox: text still publishes, polling continues and icons retry later.
- Revoke usage access, disconnect network, refresh/restart service; inspect Logs
  for foreground package changes vs Discord publishing errors.
- Inspect status-bar glyph and notification header in light/dark themes. Android
  may hide silent status icons via system settings; the app does not override it.

The screenshots reported during this fix still show the removed separate App
Detection screen, so verify installation of the newest build before retesting.

## Live and floating logs

Telegram drawer entry replaced with Logs; the earlier Logs entry was removed to
avoid duplication. Live Logs has optional auto-follow, copy and clear. The
400-entry in-memory buffer is synchronized and filters common credential formats
before display/export (not a guarantee against every possible secret format).

Floating logs are explicitly user-started after SYSTEM_ALERT_WINDOW permission.
The non-focusable overlay shows the latest 80 entries, supports dragging by its
header and closing with ×, and has a separate low-importance foreground service
notification with Stop. No stored enabled preference, START_NOT_STICKY, no boot
restart; removing the app task stops it. It does not depend on detection running.

Pending device checks: permission denied/granted/revoked; switching to other
apps; drag to screen edges; rotate; close and notification Stop; task removal;
process death/reboot; Android 8/12/14+; notification permission denied; live log
updates with detection enabled; copy/clear and disable follow to read history.
Manifest/resource and whitespace checks passed; build/runtime remain unverified.

## Logs / unexpected process exit follow-up

Logs now uses a compact top bar and a weighted log viewport rather than the
large-title scaffold that could consume the available height. A Startup and
Logs-open message makes an idle session visible. Overlay foreground promotion
and rendering failures are handled and surfaced; detection foreground promotion
and start requests are guarded, and boot work is off the broadcast main thread.

One redacted Java crash report (up to 24 KB) is kept in noBackupFilesDir and shown
as PreviousCrash after relaunch. Android 11+ additionally reports the most recent
system process exit as PreviousExit. Reports are local only; Clear logs removes
the saved crash report. This cannot catch SIGKILL or guarantee survival against
OEM battery management. No specific device crash cause is confirmed yet.

Validation: resource/delimiter checks and git diff --check passed. Attempted
:app:compileDebugKotlin, but Gradle cannot run under this environment's Java 11.
Pending device tests: Logs visible at large font sizes; float permission denial,
service promotion denial, overlay close; boot restart; intentional debug crash
followed by relaunch/copy/clear; distinguish crash from low-memory/user exit.

## Background presence grace and notification artwork

Leaving a selected app retains the existing presence for 150 seconds (cleared on
the next 3-second poll after expiry). The deadline uses elapsedRealtime so wall
clock changes do not stretch it. Repeated background polls do not reset it.
Returning to the same app cancels the deadline and preserves its original elapsed
timestamp. Another selected app replaces it immediately. Removing the previous
app from the allow-list clears it on the next poll; RPC Off, detection Stop and
logout keep their existing clear/shutdown paths. Grace is not persisted across
service/process restarts. Screen-off is treated as background for this grace.

Notification glyph is derived from the user-provided character/orb artwork as a
white alpha-only image at five Android densities; dark background removed. Full
artwork is separately supplied as the large icon for detection and floating logs.
Android controls large-icon placement and status-bar tint.

Static resource references and alpha extrema checked. Device checks pending:
background at 119/150/180 seconds; same-app return at 100 seconds; switch to a
second selected app; unselect/RPC Off during grace; screen lock; light/dark
notification visibility. Build and runtime behaviour remain unverified here.

## Detected-app status name and late artwork

PresenceManager explicitly selects Name for Discord's status display; the gateway
serializer now includes status_display_type, which was previously ignored.
Discord clients decide the exact placement of the Playing heading. Existing
activity-name/source and per-app overrides remain respected; application ID is
unchanged. A configured ShadowRPC state line is not silently removed.

Grace-period polls now refresh the retained app through the existing deduplication
path, allowing a late app-icon upload to reach Discord after the user leaves the
app. Session timestamp and grace deadline remain unchanged. Icon-ready/failure
messages aid diagnosis; Show app icon still controls optional Catbox uploads.

Pending device checks: open a selected app and immediately switch to Discord;
verify icon appears when upload/registration completes, grace still expires,
and master Off/unselect still clears. Check activity heading on Android/desktop.
Static diff check passed; compilation and Discord rendering not verified here.

## Settings mismatch and automatic-stop investigation

Confirmed code issues fixed:
- App options Save/Cancel lived below the scrollable form. They now remain in a
  fixed footer; the screenshot's displayed choices may have been unsaved drafts.
- Detection enable was written in a separate launched preference coroutine before
  service startup. Await the DataStore write before starting/stopping; debounce
  the toggle. This prevents first-poll reads of the old disabled preference.
- Presence deduplication ignored changes to global type/status/application ID.
  Compare the resolved activity instead, using monotonic time, and bypass dedupe
  for disconnected gateways. Log the effective published name/type.
- Recheck enabled detection on every Activity onStart, not just onCreate. No
  runBlocking on this path. Explicit Off remains off. Usage permission loss stops
  detection without silently clearing the desired-enabled preference.
- Bound publish calls to 45 seconds; timeout retries instead of cancelling the
  poll loop. Avoid an old service teardown closing a replacement's connection.
- Add a 30-second polling heartbeat and service-destroy message. Saved per-app
  type/source and effective outgoing name/type have separate log entries.

No screenshot proves an OS process kill. Prior-crash/exit diagnostics remain the
way to distinguish a crash, background kill, stopped poller and disconnected
Discord transport. Android may still kill/force-stop apps; no promise of an
unkillable service. Pending device checks: rapid toggle, persist/reopen app
options, global vs per-app Listening, disconnect/reconnect, idle -> selected app,
permission revoke/regrant, service restart and large-font fixed footer.

Static delimiter and whitespace checks passed. Compilation and device execution
remain unverified in this Java 11/no Android SDK editing environment.

## Activity-name compatibility attempt

Removed the optional status_display_type wire field to match LunarTune's gateway
payload (it is not a profile-heading control). On an established connection,
changing app ID/name/type now sends an empty activity list before the replacement.
Normal artwork/text/timestamp refreshes do not clear. Failed clear triggers the
existing bounded reconnect loop; no application-ID spoofing or auth changes.

Diagnostic logs distinguish "Presence queued (not server acknowledgement)" from
"Server self-presence observed". The latter is emitted only if the existing
session receives a self PRESENCE_UPDATE matching the current application ID;
no extra permissions/subscriptions and no other-user presence logging. Such an
event is not guaranteed for OAuth sessions; absence proves nothing.

User test: disable other RPC publishers temporarily; save per-app Playing,
Detected app name, None details/state, no display-name override; keep global
Application ID override blank. Open selected app, then check Discord and copy the
Saved/Publishing/Presence queued lines. Repeat with Listening. Compare with a
second Discord client. Android's card layout may differ from member-list status.
This is a compatibility attempt, not a confirmed rendering fix; device testing
and Android compilation remain pending.

## Log export and noise control

Logs toolbar Export uses Android CreateDocument (text/plain); no broad storage
permission or clipboard required. On destination selection, snapshot the bounded
buffer, redact, then write UTF-8 on IO with success/failure feedback. Picker cancel
writes nothing. Exports contain app version, Android API and timestamp. Partial
files may remain if the provider fails or the activity is destroyed mid-write.

Verbose defaults Off per process. INFO/WARN/ERROR remain visible; normal gateway
dispatch/heartbeat, poll heartbeat, unchanged refresh and queued-send chatter
requires Verbose. Enable before reproducing to retain those diagnostics; turning
it on does not reconstruct discarded history. Errors and changed effective
name/type remain normal-level logs. Buffer stays capped at 400 entries.
READY no longer logs account/session identifiers; redaction includes session keys.
Master RPC toggle now logs the explicit user request to distinguish that path
from unexplained service/process termination.

Pending device checks: export/cancel/provider failure, large buffer, UTF-8 text,
Clear then export, verbose off/on during gateway traffic, review file before
sharing. Resource and whitespace checks pass; build/device tests not run here.

## Notification Stop RPC preserves detection settings

Notification Stop now calls PresenceManager.setEnabled(false), serializing with
in-flight publishing and persisting only RpcEnabledKey. It clears presence and
the grace session without changing AppDetectionEnabledKey or selected packages.
The service remains paused with its notification; enabling master RPC resumes
configured detection. The existing notification action identifier is preserved
so already-posted notifications receive the corrected behaviour after update.
The App Detection switch remains the explicit way to disable detection itself.

Static diff/handler checks passed. Pending device checks: Stop RPC while active,
during background grace and during upload; both on-screen toggles reflect master
Off / detection unchanged; app selections survive restart; master On resumes.

## Home app-list visual polish

Removed Manage apps explanatory/search-button row. Detection, icon and timestamp
switches form a close-spaced card group with rounded outside corners. An always
visible rounded search field (search/clear icons) sits above the app rows.
Each app is a lazy keyed card; first/last visible results receive rounded outer
corners. Filtering animates insertion/removal/placement, selection colours use a
spring, and shared switches crossfade check/cross thumb icons. Dynamic colour
roles are retained; RPC/detection behaviour is unchanged. Long press still opens
App options. App/package labels are capped at two lines for predictable spacing.

Static resources/diff checks pass. Device checks pending: light/dark/dynamic theme,
search by app/package/override, clear/no results, single-result corners, long-press,
large font, TalkBack, animations disabled via system settings and toggle persistence.
No Android screenshot render or build verification performed in this environment.
