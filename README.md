# Swifty Protein (Android)

Swifty Protein is an Android app for browsing ligands from the RCSB Protein Data Bank and visualizing them in interactive 3D.

## Features

- Fingerprint + password authentication (with local account storage in Room)
- Splash screen and app icon aligned with the molecular theme
- Ligand list loaded from `ligands.txt`
- Search bar to quickly filter ligands
- Network loading/error handling when fetching ligand data from RCSB
- 3D molecule rendering with SceneView (Filament backend)
- CPK coloring for atom types
- Ball-and-Stick model (mandatory) + extra visualization modes
- Atom info tooltip on tap (symbol, element name, atom id)
- Share ligand model screenshot from the Protein View screen

## Tech Stack

- Kotlin + Jetpack Compose
- Hilt (DI)
- Room (local database)
- Retrofit + OkHttp (network)
- AndroidX Biometric
- SceneView / Filament (3D rendering)

## Project Structure

- `app/src/main/java/com/music42/swiftyprotein/ui`
  - `login` - authentication screen + state
  - `proteinlist` - ligand list + search
  - `proteinview` - 3D viewer + atom interactions + share
- `app/src/main/java/com/music42/swiftyprotein/data`
  - `local` - Room database and DAO
  - `remote` - RCSB API interface
  - `parser` - CIF parsing logic
  - `repository` - app data/business logic
- `app/src/main/res/raw/ligands.txt` - bundled ligand IDs

## Requirements

- Android Studio (latest stable recommended)
- Android SDK 35
- Min SDK 26
- JDK 17

## Getting Started

1. Clone or open the project in Android Studio.
2. Let Gradle sync and download dependencies.
3. Run the `app` configuration on an emulator or physical Android device.
4. Register a user account, then log in.
5. Open a ligand from the list to view it in 3D.

## Notes

- Internet access is required to download ligand `.cif` files from RCSB.
- Biometric login appears only on devices that support biometrics and after at least one account is created.
- If biometric auth is unavailable, password login remains available.

## Data Source

- RCSB PDB ligand files: `https://files.rcsb.org/ligands/download/{ID}.cif`

