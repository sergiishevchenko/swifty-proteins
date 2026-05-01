# Settings, Theme, Onboarding

## Goal

Provide basic app preferences:

- Theme: System / Light / Dark
- Default 3D visualization mode
- Onboarding completion flag

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
