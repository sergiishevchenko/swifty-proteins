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

## Logout

The Settings top bar shows the current username and a logout action. Logout clears the session and navigates back to Login (same security model as returning from background).
