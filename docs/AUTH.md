# Authentication

## Goal

Provide a simple local account system with:

- Register (username + password)
- Login with password
- Optional biometric login via AndroidX Biometric
- Always show Login again when returning from background

## Key files

- `app/src/main/java/com/music42/swiftyprotein/ui/login/LoginScreen.kt`
- `app/src/main/java/com/music42/swiftyprotein/ui/login/LoginViewModel.kt`
- `app/src/main/java/com/music42/swiftyprotein/data/repository/AuthRepository.kt`
- `app/src/main/java/com/music42/swiftyprotein/data/local/entity/User.kt`
- `app/src/main/java/com/music42/swiftyprotein/data/local/UserDao.kt`
- `app/src/main/java/com/music42/swiftyprotein/util/BiometricHelper.kt`
- `app/src/main/java/com/music42/swiftyprotein/MainActivity.kt` (return-to-login policy)

## Data model

- Users are stored locally in **Room (SQLite)**.
- Passwords are stored as a **secure hash** (bcrypt).

## Can I view passwords in the database?

No. The app does **not** store plaintext passwords. Only `passwordHash` is stored.

If you want to inspect what is stored:

- Android Studio: `App Inspection → Database Inspector` → open `swifty_protein_db` → table `users` → column `passwordHash`
- ADB (copy DB out of app sandbox):

```bash
adb shell run-as com.music42.swiftyprotein ls -l databases
adb exec-out run-as com.music42.swiftyprotein cat databases/swifty_protein_db > swifty_protein_db
```

## Biometric flow (high level)

1. UI checks biometric availability via `LoginViewModel`.
2. If available, UI can start a biometric prompt.
3. On success, the app logs in the user (username must exist).
4. On failure, a user-friendly error dialog is shown.

## Return to Login when app backgrounds

`MainActivity` sets an internal flag when the user leaves the app, and forces a navigation back to Login on resume.

Note: some OS dialogs (e.g., MediaProjection permission) behave like background/foreground transitions. The app suppresses login-redirect for that specific case.
