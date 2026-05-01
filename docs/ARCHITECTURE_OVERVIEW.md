# Architecture Overview

This project is an Android (Kotlin) implementation of the **Swifty Proteins** assignment. It uses a Compose + MVVM + Repository architecture and renders molecules in 3D via **SceneView (Filament)**.

## Documents

- [Authentication](AUTH.md)
- [Navigation](NAVIGATION.md)
- [Data layer](DATA_LAYER.md)
- [CIF parser](CIF_PARSER.md)
- [3D rendering](RENDERING_3D.md)
- [Overlays & interaction](OVERLAYS_INTERACTION.md)
- [Sharing & video](SHARING_AND_VIDEO.md)
- [Favorites & compare](FAVORITES_AND_COMPARE.md)
- [Settings & theme](SETTINGS_AND_THEME.md)

## High-level modules

- **UI / Navigation**: Compose screens + navigation graph.
- **Authentication**: local users, login/register, biometric prompt.
- **Ligand catalog**: load ligand IDs from [`ligands.txt`](../app/src/main/res/raw/ligands.txt), search, select.
- **Data layer**: download CIF from RCSB, cache to disk, parse to domain model.
- **3D viewer**: build a scene graph (atoms/bonds) and handle interaction.
- **Overlays**: tooltips, labels overlay, measurement overlay, mode banners.
- **Sharing**: screenshot (PixelCopy) and video recording (MediaProjection).
- **Favorites + Compare**: Room-persisted favorites and a two-panel compare view.
- **Settings**: theme and default visualization mode stored in DataStore.

## Main user flow

1. **Splash** → **Login**
2. Login/register (biometric optional).
3. **Protein list**: search ligand IDs loaded from [`ligands.txt`](../app/src/main/res/raw/ligands.txt).
4. Select a ligand:
   - download CIF from RCSB
   - cache CIF to disk
   - parse CIF to `Ligand` (atoms + bonds)
5. **Protein view**: render 3D model and use overlays:
   - select atom/bond → tooltip
   - (Balls mode) labels + measurement
   - share screenshot/video
6. Optional: add to favorites, open favorites list, compare two ligands.
