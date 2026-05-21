# Navigation

## Goal

Define the screen graph and the “always go back to Login” security behavior.

## Key files

- [`app/src/main/java/com/music42/swiftyprotein/ui/navigation/NavGraph.kt`](../app/src/main/java/com/music42/swiftyprotein/ui/navigation/NavGraph.kt)
- [`app/src/main/java/com/music42/swiftyprotein/ui/navigation/LogoutConfirmDialog.kt`](../app/src/main/java/com/music42/swiftyprotein/ui/navigation/LogoutConfirmDialog.kt)
- [`app/src/main/java/com/music42/swiftyprotein/ui/navigation/Screen.kt`](../app/src/main/java/com/music42/swiftyprotein/ui/navigation/Screen.kt)
- [`app/src/main/java/com/music42/swiftyprotein/ui/AppRoot.kt`](../app/src/main/java/com/music42/swiftyprotein/ui/AppRoot.kt)
- [`app/src/main/java/com/music42/swiftyprotein/MainActivity.kt`](../app/src/main/java/com/music42/swiftyprotein/MainActivity.kt)

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

## Explicit logout

Several screens expose a logout icon in the top bar (ligand list, favorites, compare, settings, protein view). They all receive the same callback from `SwiftyProteinNavHost`:

1. `onLogoutRequest` — shows `LogoutConfirmDialog` (“Log out?”).
2. **Log out** — `SessionViewModel.logout()` (clears in-memory session username) and `navController.navigate(Login)` with `popUpTo(0) { inclusive = true }`.
3. **Cancel** or dismiss — dialog closes; user stays on the current screen.

`last_username` in [`SecureStorage`](SECURE_STORAGE.md) is **not** cleared on logout so biometric login still works when the same username is entered again. See [`AUTH.md`](AUTH.md).
