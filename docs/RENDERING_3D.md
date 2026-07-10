# 3D Rendering

## Goal

Render a ligand in 3D with multiple visualization modes and interactive camera controls.

## Key files

- [`app/src/main/java/com/music42/swiftyprotein/ui/proteinview/ProteinViewScreen.kt`](../app/src/main/java/com/music42/swiftyprotein/ui/proteinview/ProteinViewScreen.kt)
- [`app/src/main/java/com/music42/swiftyprotein/ui/proteinview/ProteinViewViewModel.kt`](../app/src/main/java/com/music42/swiftyprotein/ui/proteinview/ProteinViewViewModel.kt)
- [`app/src/main/java/com/music42/swiftyprotein/ui/proteinview/MoleculeViewer.kt`](../app/src/main/java/com/music42/swiftyprotein/ui/proteinview/MoleculeViewer.kt)
- [`app/src/main/java/com/music42/swiftyprotein/ui/proteinview/MoleculeSceneBuilder.kt`](../app/src/main/java/com/music42/swiftyprotein/ui/proteinview/MoleculeSceneBuilder.kt)
- [`app/src/main/java/com/music42/swiftyprotein/util/CpkColors.kt`](../app/src/main/java/com/music42/swiftyprotein/util/CpkColors.kt)

## Rendering framework

- **SceneView** is used as the high-level 3D framework.
- SceneView uses **Filament** as the rendering backend.

## Scene building

`MoleculeSceneBuilder` converts a `Ligand` model into a SceneView node hierarchy:

- Atoms → `Sphere` geometry, one `MeshNode` per atom (touchable)
- Bonds → `Cylinder` geometry segments between atom pairs
- Atom colors follow **CPK coloring** via `CpkColors`

Hydrogen/deuterium are filtered out for rendering when non-hydrogen atoms exist.

## Visualization modes

Implemented modes (UI labels on the main viewer):

- **Balls**: Ball & Stick (spheres + cylinders)
- **Fill**: Space filling (bigger spheres, no bonds)
- **Sticks**: thin sticks with minimal/no spheres
- **Wire**: wireframe-like bonds only

Switching modes rebuilds the scene graph for the current ligand.

## Camera controls

The viewer supports:

- orbit/rotation via SceneView camera manipulator
- pinch zoom (touch)
- two-finger pan (touch)
- scroll wheel zoom (mouse/trackpad)
- zoom `+` / `-` buttons and **Reset view** (camera distance, pan, focus, and model rotation)

## Model animation (Play / Stop)

The **Animation** button in the top-right action row toggles continuous rotation of the **already loaded** molecule node (`parentNode`), not a new scene instance.

- **Axis:** vertical (Y).
- **Speed:** ~30°/s, driven by frame delta time in the SceneView `onFrame` render loop (`frameTimeNanos`) for smooth motion across different frame rates.
- **Implementation:** accumulated angle is applied as `parentNode.rotation = Rotation(0f, angle, 0f)` each frame.
- **Gestures:** zoom, pan, and manual camera orbit continue to work while animation is on.
- **Stop:** the model stays at the last orientation (no reset).
- **Reset view:** clears model rotation along with camera and pan.
- **Lifecycle:** `ProteinViewViewModel.stopAnimation()` runs on screen dispose and in `onCleared()` so the render loop does not keep updating after the screen is gone.

State is held in `ProteinViewUiState.isAnimationEnabled` and toggled via `ProteinViewViewModel.toggleAnimation()`.

## Video recording camera orbit

Screen recording (~5 s MP4) uses a **separate** camera auto-orbit (`autoRotate` in `MoleculeViewer`): the camera circles the scene center on the horizontal plane while recording. This is independent of the user-facing Animation toggle.
