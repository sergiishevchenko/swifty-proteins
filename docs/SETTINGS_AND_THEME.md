# Settings, Theme, Onboarding

## Goal

Provide basic app preferences:

- Theme: System / Light / Dark
- Default 3D visualization mode
- Show hydrogens by default (when opening a ligand)
- Onboarding completion flag
- Logout (returns to Login)

## Key files

- [`app/src/main/java/com/music42/swiftyprotein/ui/settings/SettingsScreen.kt`](../app/src/main/java/com/music42/swiftyprotein/ui/settings/SettingsScreen.kt)
- [`app/src/main/java/com/music42/swiftyprotein/ui/settings/SettingsViewModel.kt`](../app/src/main/java/com/music42/swiftyprotein/ui/settings/SettingsViewModel.kt)
- [`app/src/main/java/com/music42/swiftyprotein/data/settings/SettingsRepository.kt`](../app/src/main/java/com/music42/swiftyprotein/data/settings/SettingsRepository.kt)
- [`app/src/main/java/com/music42/swiftyprotein/data/settings/AppSettings.kt`](../app/src/main/java/com/music42/swiftyprotein/data/settings/AppSettings.kt)
- [`app/src/main/java/com/music42/swiftyprotein/ui/onboarding/OnboardingScreen.kt`](../app/src/main/java/com/music42/swiftyprotein/ui/onboarding/OnboardingScreen.kt)
- [`app/src/main/java/com/music42/swiftyprotein/ui/theme/Theme.kt`](../app/src/main/java/com/music42/swiftyprotein/ui/theme/Theme.kt)

## Storage

Settings are persisted using **Jetpack DataStore (Preferences)**.

## Theme application

The chosen theme mode is applied at the app root composable so that it affects all screens.

## Default visualization mode

The user-selected default visualization mode is stored and used when opening the main 3D viewer.

## Show hydrogens by default

Stored as `show_hydrogens_by_default` in DataStore. When enabled, the 3D viewer opens with hydrogen/deuterium atoms visible (when the ligand contains them). The user can still toggle hydrogens per ligand in the protein view.

## Onboarding

`onboarding_completed` is reset to `false` on registration so new users see the walkthrough. After completion, the flag is set and onboarding is skipped on subsequent logins.

**Replay from Settings:** «Show onboarding again» calls `SettingsViewModel.replayOnboarding()` (sets `onboarding_completed` to `false`), then navigates to `OnboardingScreen` with Settings removed from the back stack. Completing the tour sets the flag back to `true` and returns to the ligand list.

## CIF cache

Downloaded ligand CIF files live under `filesDir/cif_cache/`. Settings shows the total cache size and a **Clear cache** action that deletes the directory. `LigandRepository.cacheCleared` notifies `ProteinListViewModel` to reset in-memory `cachedInfo` so list subtitles disappear until files are downloaded again.

## Logout

The Settings top bar (and the same pattern on ligand list, favorites, compare, and protein view) shows the current username and a logout icon.

Logout is **not** immediate: [`NavGraph.kt`](../app/src/main/java/com/music42/swiftyprotein/ui/navigation/NavGraph.kt) shows [`LogoutConfirmDialog.kt`](../app/src/main/java/com/music42/swiftyprotein/ui/navigation/LogoutConfirmDialog.kt) first. After confirmation, `SessionViewModel.logout()` clears the in-memory session and navigation returns to Login with an empty back stack.

This is separate from the **background → Login** policy in [`NAVIGATION.md`](NAVIGATION.md) (resume after Home does not use the confirmation dialog). Encrypted `last_username` is kept for biometric convenience; see [`SECURE_STORAGE.md`](SECURE_STORAGE.md).
