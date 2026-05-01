# 3D Rendering

## Goal

Render a ligand in 3D with multiple visualization modes and interactive camera controls.

## Key files

- [`app/src/main/java/com/music42/swiftyprotein/ui/proteinview/ProteinViewScreen.kt`](../app/src/main/java/com/music42/swiftyprotein/ui/proteinview/ProteinViewScreen.kt)
- [`app/src/main/java/com/music42/swiftyprotein/ui/proteinview/ProteinViewViewModel.kt`](../app/src/main/java/com/music42/swiftyprotein/ui/proteinview/ProteinViewViewModel.kt)
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
- scroll wheel zoom (mouse/trackpad)
