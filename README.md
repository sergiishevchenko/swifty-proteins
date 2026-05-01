# Swifty Proteins (Android)

Android implementation of the Swifty Proteins assignment: authentication, searchable ligand catalog, and interactive 3D molecular visualization.

## Overview

The application is built as a classic Android app using Jetpack Compose and MVVM.  
Main user flow:

1. User opens app and sees splash.
2. User logs in (password or fingerprint).
3. User searches/selects a ligand from local list.
4. App downloads ligand data from RCSB.
5. App parses molecular data and renders interactive 3D scene.
6. User explores model (zoom/rotate/mode switch), taps atoms/bonds for tooltip, measures distances/angles, toggles labels, and can share screenshots/videos.

## Documentation

- [Architecture overview](docs/ARCHITECTURE_OVERVIEW.md)

## Quick Start

1. Open the project in Android Studio and wait for Gradle sync.
2. Pick an emulator or a physical device.
3. Run the `app` configuration.
4. Register a user, log in, choose a ligand, open the 3D view.

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Architecture:** MVVM + Repository
- **DI:** Hilt
- **Local DB:** Room (SQLite)
- **Key-Value storage:** Jetpack DataStore (Preferences)
- **Networking:** Retrofit + OkHttp
- **Biometrics:** AndroidX Biometric (`BiometricPrompt`)
- **3D Rendering:** SceneView (Filament backend, `SurfaceView`)
- **Geometry building:** SceneView geometries (`Sphere`, `Cylinder`) + per-atom `MeshNode`s
- **CIF parsing:** Custom parser (`data/parser/CifParser.kt`)
- **Caching:** CIF cached on disk (`filesDir/cif_cache`)
- **Sharing:** `PixelCopy` screenshots + `FileProvider` share intents
- **Video:** MediaProjection + MediaRecorder (MP4)
- **Concurrency:** Kotlin Coroutines
- **Logging:** OkHttp Logging Interceptor (BASIC)

## Project Structure

```text
app/src/main/java/com/music42/swiftyprotein/
├── MainActivity.kt
├── SwiftyProteinApp.kt
├── di/
│   └── AppModule.kt
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt
│   │   ├── UserDao.kt
│   │   └── entity/User.kt
│   ├── model/
│   │   ├── Atom.kt
│   │   ├── Bond.kt
│   │   └── Ligand.kt
│   ├── parser/
│   │   └── CifParser.kt
│   ├── remote/
│   │   └── RcsbApi.kt
│   └── repository/
│       ├── AuthRepository.kt
│       └── LigandRepository.kt
├── ui/
│   ├── navigation/
│   │   ├── NavGraph.kt
│   │   └── Screen.kt
│   ├── login/
│   │   ├── LoginScreen.kt
│   │   └── LoginViewModel.kt
│   ├── proteinlist/
│   │   ├── ProteinListScreen.kt
│   │   └── ProteinListViewModel.kt
│   ├── proteinview/
│   │   ├── MoleculeSceneBuilder.kt
│   │   ├── ProteinViewScreen.kt
│   │   └── ProteinViewViewModel.kt
│   ├── favorites/
│   ├── compare/
│   ├── settings/
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
└── util/
    ├── BiometricHelper.kt
    └── CpkColors.kt
```

## Data Flow

1. `ProteinListViewModel` reads ligand IDs from `LigandRepository`.
2. On selection, navigation opens `ProteinViewScreen` with `ligandId`.
3. `ProteinViewViewModel` loads ligand via `LigandRepository.fetchLigand(...)`.
4. `LigandRepository` downloads CIF from RCSB through `RcsbApi`.
5. `CifParser` extracts ligand name, atoms, bonds.
6. `MoleculeSceneBuilder` converts model into SceneView mesh nodes.
7. UI renders interactive scene and atom interactions.

## Implemented Features

### Authentication

- Local account registration and login.
- Passwords are stored as a secure hash (bcrypt).
- Fingerprint login via AndroidX Biometric (`BiometricManager` + `BiometricPrompt`).
- Password fallback when biometric is unavailable.
- Error popup on failed authentication.
- Login screen is shown again when app returns from background (security requirement).
- Biometric login is available only for the **last signed-in user** (username must match).

### Ligand Catalog

- Ligand IDs are loaded from local `res/raw/ligands.txt`.
- Search by substring (case-insensitive).
- Empty search state message.
- Loading/error handling for selection and fetch failures.
- List rows can show cached ligand info (formula + atom count) when available.

### Favorites + Compare

- Mark ligands as favorites (persisted locally).
- Favorites screen.
- Compare screen: open two ligands side-by-side from Favorites.

### 3D Viewer

- 3D rendering with SceneView (Filament backend).
- CPK atom coloring.
- Visualization modes:
  - Balls (Ball & Stick, default)
  - Fill (Space Fill)
  - Sticks
  - Wire (Wireframe)
- Atom selection with tooltip popup (element, name, atom ID).
- Bond selection with tooltip popup (order/type + bond length).
- Dismiss tooltip by tapping elsewhere.
- Zoom controls (`+` / `-`) and pointer scroll zoom.
- Camera orbit/rotate interactions.
- Measurement mode (Balls only): pick 2 atoms for distance (Å), 3 atoms for angle (°).
- Atom labels (Balls only): overlay labels that track atoms while moving/zooming.
- Large-molecule handling: adaptive sphere resolution (LOD) + warning + automatic downgrade for heavy scenes.

### Sharing

- Share button captures screenshot with `PixelCopy`.
- Shares PNG or JPEG via `FileProvider`.
- Fallback to text-only share if screenshot capture fails.
- Video recording + share (MP4) via MediaProjection + MediaRecorder.

### UI / UX

- Splash screen with enforced visible duration (~2s).
- Material 3 styling across screens.
- Model loading card with progress indicators and rotating status messages.
- Onboarding: multi-step walkthrough (gestures, modes/sharing, favorites/settings).
- Settings: theme, default visualization mode, and “show hydrogens by default”.
- Themed icon and round icon.

## Screenshots & demo

Assets live in [`screenshots/`](screenshots/). They follow the main flow: auth and onboarding, catalog, settings, **001** in every visualization mode, export, large-molecule handling, and a screen recording. All screenshots use the same display width (`300px`, capped with `max-width: 100%` on small screens). **Screen recording:** GitHub’s README sanitizer removes HTML `<video>` tags, so there is no in-page player here—use the preview link below (opens the MP4 in the browser or downloads it, depending on your client).

### Launch, sign-in, and onboarding

<p align="center">
  <img src="screenshots/splash.png" alt="Splash screen" width="300" style="max-width: 100%; height: auto;"/>
  <img src="screenshots/registration.png" alt="Registration and login" width="300" style="max-width: 100%; height: auto;"/>
</p>

<p align="center"><em>Left to right: splash; registration/login.</em></p>

<p align="center">
  <img src="screenshots/biometric.png" alt="Biometric prompt" width="300" style="max-width: 100%; height: auto;"/>
  <img src="screenshots/biometric2.png" alt="Biometric state" width="300" style="max-width: 100%; height: auto;"/>
</p>

<p align="center"><em>Left to right: fingerprint sign-in prompt; another biometric state (e.g. after switching user or changing device biometrics).</em></p>

<table align="center" style="border-collapse: collapse; margin-left: auto; margin-right: auto;">
  <tr>
    <td style="padding: 0 4px; vertical-align: top;"><img src="screenshots/tour.png" alt="Onboarding page 1" width="300" style="max-width: 100%; height: auto; display: block;"/></td>
    <td style="padding: 0 4px; vertical-align: top;"><img src="screenshots/tour2.png" alt="Onboarding page 2" width="300" style="max-width: 100%; height: auto; display: block;"/></td>
    <td style="padding: 0 4px; vertical-align: top;"><img src="screenshots/tour3.png" alt="Onboarding page 3" width="300" style="max-width: 100%; height: auto; display: block;"/></td>
  </tr>
</table>

<p align="center"><em>Multi-step onboarding (one row): gestures/camera; modes and sharing; favorites and settings.</em></p>

### Catalog, search, favorites, compare

<p align="center">
  <img src="screenshots/ligands.png" alt="Ligand list" width="300" style="max-width: 100%; height: auto;"/>
  <img src="screenshots/search.png" alt="Search ligands" width="300" style="max-width: 100%; height: auto;"/>
</p>

<p align="center"><em>Local ligand list; substring search.</em></p>

<p align="center">
  <img src="screenshots/favorites.png" alt="Favorites" width="300" style="max-width: 100%; height: auto;"/>
  <img src="screenshots/compare.png" alt="Compare two ligands" width="300" style="max-width: 100%; height: auto;"/>
</p>

<p align="center"><em>Favorites; side-by-side compare from favorites.</em></p>

### Settings

<p align="center">
  <img src="screenshots/settings.png" alt="Settings" width="300" style="max-width: 100%; height: auto;"/>
</p>

<p align="center"><em>Theme, default visualization mode, default hydrogen visibility.</em></p>

### 3D viewer (ligand 001): modes and tools

<p align="center">
  <img src="screenshots/ligand_001.png" alt="Balls and stick" width="300" style="max-width: 100%; height: auto;"/>
  <img src="screenshots/ligand_001_fill.png" alt="Space fill" width="300" style="max-width: 100%; height: auto;"/>
</p>

<p align="center"><em>Ball &amp; Stick (default) and Space Fill.</em></p>

<p align="center">
  <img src="screenshots/ligand_001_sticks.png" alt="Sticks mode" width="300" style="max-width: 100%; height: auto;"/>
  <img src="screenshots/ligand_001_wire.png" alt="Wireframe" width="300" style="max-width: 100%; height: auto;"/>
</p>

<p align="center"><em>Sticks and Wireframe.</em></p>

<p align="center">
  <img src="screenshots/ligand_001_hydrogens.png" alt="Hydrogens" width="300" style="max-width: 100%; height: auto;"/>
  <img src="screenshots/ligand_001_labels.png" alt="Atom labels" width="300" style="max-width: 100%; height: auto;"/>
</p>

<p align="center"><em>Hydrogens on; atom labels (Balls mode).</em></p>

<p align="center">
  <img src="screenshots/ligand_001_tap_atom.png" alt="Tap atom tooltip" width="300" style="max-width: 100%; height: auto;"/>
</p>

<p align="center"><em>Atom tap: tooltip with element, name, and atom id.</em></p>

<p align="center">
  <img src="screenshots/ligand_001_measure.png" alt="Measure mode" width="300" style="max-width: 100%; height: auto;"/>
  <img src="screenshots/ligand_001_measure_distance.png" alt="Distance measurement" width="300" style="max-width: 100%; height: auto;"/>
  <img src="screenshots/ligand_001_measure_angle.png" alt="Angle measurement" width="300" style="max-width: 100%; height: auto;"/>
</p>

<p align="center"><em>Measurement mode; distance between two atoms (Å); angle through three atoms (°).</em></p>

### Export and large structures

<p align="center">
  <img src="screenshots/ligand_001_export_format.png" alt="Export format screen" width="300" style="max-width: 100%; height: auto;"/>
  <img src="screenshots/export_format_ligand_001.png" alt="Export format choice" width="300" style="max-width: 100%; height: auto;"/>
</p>

<p align="center"><em>Screenshot share: choosing PNG or JPEG before sending.</em></p>

<p align="center">
  <img src="screenshots/large%20molecule.png" alt="Large molecule warning" width="300" style="max-width: 100%; height: auto;"/>
</p>

<p align="center"><em>Heavy ligand: warning and automatic scene simplification (LOD) for stability and memory.</em></p>

### Screen recording

<p align="center">
  <a href="https://raw.githubusercontent.com/sergiishevchenko/swifty-proteins/main/screenshots/ligand_001.mp4" title="Open MP4 (browser usually plays it)"><img src="screenshots/ligand_001.png" alt="Screen recording: tap to open ligand_001.mp4" width="300" style="max-width: 100%; height: auto;"/></a>
</p>

<p align="center"><strong><a href="https://raw.githubusercontent.com/sergiishevchenko/swifty-proteins/main/screenshots/ligand_001.mp4">▶ Play screen recording (MP4)</a></strong></p>

<p align="center"><em>Flow: catalog → ligand 001 → 3D. Repo-relative copy: <a href="screenshots/ligand_001.mp4"><code>screenshots/ligand_001.mp4</code></a>. For a true inline player inside README, upload the MP4 in GitHub’s web editor (drag-and-drop into the README); GitHub will insert a <code>user-images.githubusercontent.com</code> URL that renders as a player.</em></p>

## Compliance Snapshot

- Mandatory requirements: implemented.
- Bonus requirement (alternative visualization modes): implemented.
- Data source: RCSB website is used.
- Data source: RCSB ligand CIF endpoint: `https://files.rcsb.org/ligands/view/{ID}.cif`

## Known Limitations

- Labels and measurement tools are available only in **Balls** mode.

## Requirements

- Android Studio (latest stable recommended)
- Android SDK 35
- Min SDK 26
- JDK 17
- Internet connection

## Build Configuration

- `compileSdk = 35`
- `targetSdk = 35`
- `minSdk = 26`
- Java/Kotlin target: 17
- `applicationId = com.music42.swiftyprotein`

## Run Locally (Android Studio)

1. Open project in Android Studio.
2. Let Gradle sync complete.
3. Select emulator/device.
4. Run `app` configuration.
5. Register account and log in.
6. Select ligand and open 3D screen.

## Run From CLI

From project root:

```bash
./gradlew assembleDebug
./gradlew installDebug
```
