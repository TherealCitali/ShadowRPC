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
