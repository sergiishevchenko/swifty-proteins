# Navigation

## Goal

Define the screen graph and the “always go back to Login” security behavior.

## Key files

- `app/src/main/java/com/music42/swiftyprotein/ui/navigation/NavGraph.kt`
- `app/src/main/java/com/music42/swiftyprotein/ui/navigation/Screen.kt`
- `app/src/main/java/com/music42/swiftyprotein/ui/AppRoot.kt`
- `app/src/main/java/com/music42/swiftyprotein/MainActivity.kt`

## Screens (typical)

- Login
- Onboarding
- Protein list (ligand catalog)
- Protein view (3D)
- Favorites
- Compare (two ligands)
- Settings

## Background → Login behavior

`MainActivity` flags backgrounding via `onUserLeaveHint()` and requests a Login redirect on `onResume()`.
The navigation layer observes that flag and clears the back stack, returning to Login.
