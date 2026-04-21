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
6. User explores model (zoom/rotate/mode switch), taps atoms for tooltip, and can share screenshot.

## Implemented Features

### Authentication

- Local account registration and login.
- SHA-256 password hashing before DB storage.
- Fingerprint login via AndroidX Biometric (`BiometricManager` + `BiometricPrompt`).
- Password fallback when biometric is unavailable.
- Error popup on failed authentication.
- Login screen is shown again when app returns from background.

### Ligand Catalog

- Ligand IDs are loaded from local `res/raw/ligands.txt`.
- Search by substring (case-insensitive).
- Empty search state message.
- Loading/error handling for selection and fetch failures.

### 3D Viewer

- 3D rendering with SceneView (Filament backend).
- CPK atom coloring.
- Visualization modes:
  - Ball & Stick (default)
  - Space Fill
  - Sticks
- Atom selection with tooltip popup (symbol, element name, atom ID).
- Dismiss tooltip by tapping elsewhere.
- Zoom controls (`+` / `-`) and pointer scroll zoom.
- Camera orbit/rotate interactions.

### Sharing

- Share button captures screenshot with `PixelCopy`.
- Shares PNG via `FileProvider`.
- Fallback to text-only share if screenshot capture fails.

### UI / UX

- Splash screen with enforced visible duration (~2s).
- Material 3 styling across screens.
- Model loading card with progress indicators and rotating status messages.
- Themed icon and round icon.

## Compliance Snapshot

- Mandatory requirements: implemented.
- Bonus requirement (alternative visualization modes): implemented.
- Data source: RCSB website is used.
- Data format note: ligand endpoint is `.cif` (details below).

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Architecture:** MVVM + Repository
- **DI:** Hilt
- **Local DB:** Room
- **Networking:** Retrofit + OkHttp
- **Biometrics:** AndroidX Biometric
- **3D Engine:** SceneView (Filament)
- **Concurrency:** Kotlin Coroutines

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

## Molecular Rendering Details

- Atoms are rendered as spheres.
- Bonds are rendered as cylinders.
- Double/triple/aromatic bonds are rendered with parallel sticks.
- Bonds are color-split by connected atoms (half-and-half coloring).
- Hydrogen and deuterium are filtered out for rendering if non-hydrogen atoms exist.
- Sphere nodes are touchable and include collision shapes.

## Atom Picking Logic

Atom tap detection uses screen-space nearest-hit logic:

1. Capture touch down/up positions and timing.
2. Treat interaction as tap only if movement and duration stay under thresholds.
3. Project each atom node world position to normalized view coordinates.
4. Select closest atom under selection radius threshold.
5. If no atom is close enough, dismiss tooltip.

Tooltip is shown via Compose `Popup` to guarantee proper z-order over the 3D surface.

## Authentication Details

- User table: `id`, `username`, `passwordHash`.
- Registration validation:
  - non-empty username/password
  - minimum password length
  - unique username
- Login validation compares SHA-256 hash.
- Fingerprint login requires:
  - device biometric capability
  - existing users
  - entered username (must exist)

## Sharing Details

- Capture source: activity window via `PixelCopy`.
- Output file: `cache/shared_images/ligand_<ID>.png`.
- Share transport: `ACTION_SEND` + `FileProvider`.
- Fallback: text share with RCSB ligand URL.

## Data Source and Format

### RCSB Endpoint Used

- `https://files.rcsb.org/ligands/download/{ID}.cif`

### Why CIF is used

For RCSB ligand/chemical component downloads, CIF/mmCIF is the official structured format endpoint used in this app.

### Quick CIF example (ligand)

```text
data_HEM
_chem_comp.id        HEM
_chem_comp.name      "PROTOPORPHYRIN IX CONTAINING FE"
...
loop_
_chem_comp_atom.atom_id
_chem_comp_atom.type_symbol
_chem_comp_atom.model_Cartn_x
...
```

### PDB vs CIF (high level)

| Aspect | PDB (legacy) | mmCIF/CIF (modern) |
|---|---|---|
| Layout | Fixed-width lines | Named fields + tables (`loop_`) |
| Parsing | Position-based | Key/column-based |
| Extensibility | Limited | High |

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
