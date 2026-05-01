## Manual test plan (SwiftyProtein)

This checklist is designed to verify the **mandatory** and **bonus** requirements from the subject PDF, using both an **emulator** and a **real Android device**.

Recommended environment notes:

- **Emulator**: Google Play system image (prefer stable API 34/35). See [Emulator setup](EMULATOR_SETUP.md).
- **Real device**: required for performance/UX verification and realistic biometric behavior. USB debugging on Xiaomi / MIUI: [Xiaomi device (USB)](XIAOMI_DEVICE_USB_SETUP.md).

---

### 0) Pre-flight / sanity

- **Install**
  - Install a fresh debug build.
  - Expected: app launches and shows the splash for ~1–2 seconds then transitions into the app.

- **Clear state (when needed)**
  - Android Settings → Apps → SwiftyProtein → Storage & cache → Clear storage.
  - Expected: app behaves like first launch (onboarding may reappear; no users/favorites in DB).

---

### 1) Launch screen (mandatory)

- **Splash screen is “clean branded”, not a fake loading screen**
  - Cold start the app (force stop first).
  - Expected: standard Android splash (icon + themed background), then app UI. No “loading forever” look.

---

### 2) Authentication (mandatory)

#### 2.1 Register (username + password)

- **Register flow**
  - Open Login.
  - Switch to Register mode.
  - Try invalid inputs:
    - empty username/password
    - password < 8 characters
  - Expected: user-friendly error dialog/message, no crash.
  - Try valid input (new username + valid password).
  - Expected: user is created and can proceed into the app.

#### 2.2 Password login

- **Login with wrong password**
  - Expected: “Invalid credentials” (or similar) without app crash.

- **Login with correct password**
  - Expected: login succeeds, you see the main content (ligand list).

#### 2.3 Biometric login (mandatory)

Emulator setup required: enroll a fingerprint in emulator “Extended controls → Fingerprint”.

- **Biometric availability rules**
  - Login once with username+password.
  - Return to Login screen.
  - Expected:
    - “Login with Fingerprint” is visible when biometrics are supported and the username field matches the **last signed-in user**.
    - If the typed username differs from last user, biometrics should be unavailable and the UI should guide the user to use password.

- **Biometric success**
  - Tap “Login with Fingerprint”.
  - Trigger fingerprint success.
  - Expected: user is authenticated and enters the app.

- **Biometric failure**
  - Trigger fingerprint failure (or use a wrong fingerprint attempt).
  - Expected: a clear error dialog appears (no silent failure).

---

### 3) “Always show Login” policy (mandatory)

This is a security requirement: when the app returns from background, Login should be shown again.

- **Background → return**
  - While in ligand list or 3D view, press Home and return to the app.
  - Expected: app shows Login.

- **Exception: system dialogs must NOT force login**
  - Open a system flow and cancel it:
    - Share sheet (screenshot share)
    - Share sheet (video share)
    - MediaProjection permission dialog (video recording) and cancel
  - Expected: returning from the dialog does **not** redirect to Login.

---

### 4) Ligand list (mandatory)

- **List shows ligand IDs from [`ligands.txt`](../app/src/main/res/raw/ligands.txt)**
  - Scroll.
  - Expected: smooth scrolling; no UI lockups.

- **Search (real-time, case-insensitive, by ligand id)**
  - Type lower/upper case queries.
  - Expected: list filters immediately; results are relevant.

- **Select ligand**
  - Tap a ligand.
  - Expected: loading indicator appears, then navigation to 3D view when loaded.

- **Network error handling**
  - Disable network and select a ligand.
  - Expected: user-friendly “no internet” error.
  - Re-enable network and retry.
  - Expected: loads successfully.

- **404 / missing ligand**
  - If you can reproduce a 404 from the RCSB endpoint, verify the message.
  - Expected: user-friendly “not found” message.

---

### 5) Protein (3D) view (mandatory + many bonuses)

#### 5.1 Rendering baseline (mandatory)

- **Molecule is visible**
  - Open any ligand.
  - Expected:
    - atom colors follow CPK rules
    - ball-and-stick model is visible (spheres + cylinders)
    - background matches theme (light/dark)

#### 5.2 Gestures (mandatory)

- **Rotate**
  - One-finger drag.
  - Expected: molecule rotates smoothly.

- **Pinch zoom**
  - Two-finger pinch.
  - Expected: zoom changes smoothly and remains usable.

#### 5.3 Atom info on tap (mandatory)

- **Select atom**
  - Tap an atom.
  - Expected: tooltip/popup with atom info appears.

- **Dismiss**
  - Tap outside / dismiss action.
  - Expected: tooltip disappears.

#### 5.4 Visualization modes (bonus)

- **Switch modes without reloading**
  - Switch between:
    - Ball-and-stick
    - Space fill
    - Sticks
    - Wireframe
  - Expected:
    - switch is immediate
    - no crashes / corrupted scene

#### 5.5 Atom highlighting (bonus)

- **Tap highlight behavior**
  - Tap an atom.
  - Expected:
    - selected atom becomes brighter (highlighted)
    - same-element atoms are shaded darker
    - other atoms keep normal CPK color

#### 5.6 Bond info (bonus)

- **Tap a bond**
  - Tap a bond segment.
  - Expected: tooltip shows bond type and length.

#### 5.7 Measurement tool (bonus)

Note: measurement/labels are intended only for Ball-and-stick mode.

- **Enable measurement**
  - Switch to Ball-and-stick mode.
  - Enable measurement.
  - Tap 2 atoms.
  - Expected: distance shown (Å).
  - Tap 3 atoms.
  - Expected: angle shown (°).

- **Non-ball mode behavior**
  - Switch to a non-ball mode and try enabling measurement.
  - Expected: user hint instructs to switch to Ball mode; tool is not active.

#### 5.8 Atom labels (bonus)

- **Enable labels**
  - In Ball-and-stick mode, toggle labels.
  - Expected: labels appear inside/near atoms and remain interactive-safe (no UI lockups).

- **Camera movement**
  - Rotate/zoom with labels enabled.
  - Expected: labels track atom positions (no “stuck” labels).

#### 5.9 Center on atom (bonus)

- **Double-tap an atom**
  - Expected: camera recenters (smooth animation), selected atom becomes center focus.

---

### 6) Sharing (mandatory + bonus)

#### 6.1 Screenshot share (mandatory + bonus formats)

- **Share screenshot**
  - Tap share and choose PNG.
  - Expected:
    - system share sheet opens
    - image contains the 3D view (and overall screen content)
    - no crash
  - Repeat with JPEG.

#### 6.2 Video recording (bonus)

Best verified on a real device (emulators can be flaky).

- **Record**
  - Start recording.
  - Accept the system MediaProjection prompt.
  - Wait for the app to finish (auto-rotate video).
  - Expected: an MP4 is produced and share sheet opens.

- **Cancel permission**
  - Start recording and cancel the MediaProjection prompt.
  - Expected: no crash; you remain in the 3D view (no forced login).

---

### 7) Favorites (bonus)

- **Toggle favorite**
  - From ligand list, toggle favorite star.
  - Expected: state persists after navigating away/back.

- **Favorites screen**
  - Open favorites list.
  - Expected: only favorites are shown; list is scrollable and responsive.

---

### 8) Compare view (bonus)

- **Enter compare**
  - Select two favorites and open Compare.
  - Expected: two molecules are shown side-by-side.

- **Zoom + Reset**
  - Use zoom in/out buttons on each panel.
  - Use Reset view.
  - Expected: each panel zooms independently; reset returns to initial camera framing with the model fitted.

---

### 9) Settings & onboarding (bonus)

- **Onboarding**
  - Clear app storage and relaunch.
  - Expected: onboarding appears once; after completing, it should not appear on next launch.

- **Theme**
  - Settings → switch theme modes (System/Light/Dark).
  - Expected: app UI updates; 3D background tint matches theme.

- **Default visualization mode**
  - Change default mode in Settings.
  - Reopen a ligand.
  - Expected: the chosen default mode is applied.

---

### 10) Caching / offline (bonus)

- **Cache behavior**
  - Open a ligand once with network on.
  - Disable network.
  - Open the same ligand again.
  - Expected: it loads from cache (no network required).

---

### 11) Performance verification (mandatory “smoothness” + bonus perf)

These checks should be done on a **real device**.

- **3D smoothness**
  - Open a ligand with many atoms.
  - Rotate/zoom continuously for ~15–30 seconds.
  - Expected: no obvious stutter; no UI jank; no crashes.

- **Overlay stability**
  - With labels/measurement enabled in Ball mode, rotate/zoom.
  - Expected: overlays remain stable (no freezing; correct positioning).

---

### Emulator vs real-device guidance

- **Prefer emulator for**:
  - functional checks (navigation, list, parsing, caching, most UI flows)
  - basic biometric wiring (fingerprint prompt mechanics)

- **Prefer real device for**:
  - performance / smoothness verification
  - reliable MediaProjection video capture behavior
  - real biometric behavior (OEM implementations vary)