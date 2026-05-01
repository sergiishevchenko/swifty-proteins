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
- Login screen is shown again when app returns from background.

### Ligand Catalog

- Ligand IDs are loaded from local `res/raw/ligands.txt`.
- Search by substring (case-insensitive).
- Empty search state message.
- Loading/error handling for selection and fetch failures.

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

### Sharing

- Share button captures screenshot with `PixelCopy`.
- Shares PNG or JPEG via `FileProvider`.
- Fallback to text-only share if screenshot capture fails.
- Video recording + share (MP4) via MediaProjection + MediaRecorder.

### UI / UX

- Splash screen with enforced visible duration (~2s).
- Material 3 styling across screens.
- Model loading card with progress indicators and rotating status messages.
- Themed icon and round icon.

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
