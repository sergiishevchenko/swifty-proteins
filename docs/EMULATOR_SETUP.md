# Emulator setup for SwiftyProtein

This project runs best on a **Google Play** emulator image (not AOSP-only images). Prefer a **stable API** (for example **API 34/35**) over very new preview images, because Google sign-in, Play services, and biometrics are more reliable there.

## Create a recommended AVD

- Android Studio → **Device Manager** → **Create Device**
- Pick a phone profile (for example **Pixel 7**)
- System image:
  - **Google Play Store** (services show as **Google Play**)
  - **API 34 or 35**, **arm64-v8a** (matches most Apple Silicon dev machines)
- Finish the wizard and boot the emulator once to let Play Store / Play services finish updating in the background

## Network (ligand downloads)

The app needs working HTTPS access to download ligand data.

- Emulator **Settings → Network & internet** should show connectivity
- Prefer **automatic time**:
  - **Settings → System → Date & time**
  - Enable **Use network-provided time** and **Use network-provided time zone**
- If downloads fail with “no internet” despite basic connectivity:
  - Try toggling **Airplane mode** off/on
  - Reboot the emulator (**Cold boot** from Device Manager)
  - Avoid broken **Private DNS** profiles while debugging (if you changed it, set it back to **Automatic**)

## Permissions the app will request (and why)

Declared in `AndroidManifest.xml`:

- **INTERNET**: download ligand data
- **USE_BIOMETRIC**: fingerprint login
- **POST_NOTIFICATIONS** (Android 13+): required for the **media projection foreground service notification** used by screen recording
- **FOREGROUND_SERVICE** + **FOREGROUND_SERVICE_MEDIA_PROJECTION**: Android requirement for **screen recording** via `MediaProjection`

On first use of recording-related flows, accept the system prompts the app triggers.

## Biometric login (fingerprint)

The login screen shows **Login with Fingerprint** only when:

- the device reports biometrics as available, and
- the username field matches the **last successful login** stored for this install

Setup on emulator:

- Android Studio emulator → **Extended controls** (…) → **Fingerprint**
- Enroll at least one fingerprint for the virtual device
- In the app:
  - log in once with **username + password** (this establishes the “last user” baseline)
  - return to login and use fingerprint when eligible

If the button does not appear:

- confirm you are not in **Register** mode
- confirm the username matches the last logged-in user
- confirm the AVD image supports biometrics (Google Play images generally do)

## Screen recording and sharing

Screen recording uses `MediaProjection` and a **foreground service** (you may see a persistent notification while recording).

Tips:

- On **Android 13+**, grant **Notifications** when prompted (or enable in **Settings → Apps → SwiftyProtein → Notifications**)
- If recording fails right after installing an experimental/preview system image, test the same flow on **API 34/35 Google Play**

Screenshots in the 3D viewer are captured without `MediaProjection` (PixelCopy-based), but **sharing** still opens the system share sheet.

## App data reset (when things get “stuck”)

If auth, secure prefs, or downloads behave inconsistently after many experiments:

- **Settings → Apps → SwiftyProtein → Storage & cache → Clear storage**
- or uninstall/reinstall the debug build

Note: **Wipe Data** on the AVD resets the entire emulator user profile (accounts, fingerprints, etc.).

## Quick sanity checklist before reporting a device-specific bug

- Google Play system image (not AOSP-only)
- Stable API (avoid preview API unless you are intentionally testing previews)
- Time sync enabled
- Notifications permission granted (Android 13+) if testing recording
- Fingerprint enrolled in emulator extended controls
- Try **Cold boot** after large emulator updates