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
