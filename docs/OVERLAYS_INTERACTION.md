# Overlays & Interaction

This project uses several UI overlays on top of a SceneView `SurfaceView`.

## Key files

- [`app/src/main/java/com/music42/swiftyprotein/ui/proteinview/ProteinViewScreen.kt`](../app/src/main/java/com/music42/swiftyprotein/ui/proteinview/ProteinViewScreen.kt)
- [`app/src/main/java/com/music42/swiftyprotein/ui/proteinview/ProteinViewViewModel.kt`](../app/src/main/java/com/music42/swiftyprotein/ui/proteinview/ProteinViewViewModel.kt)

## Why `Popup` is used

SceneView renders into a `SurfaceView`, which can cause z-order issues with regular Compose overlays.
For consistent overlay rendering above the 3D surface, the app uses Compose `Popup` windows for:

- tooltips (atom/bond)
- labels overlay
- mode banners
- top-right action buttons

## Atom tooltip

- Shown when the user taps an atom.
- Dismissed by tapping the tooltip (or tapping elsewhere, depending on logic).
- Rendered as a pill banner in the bottom-left corner.

## Bond tooltip

- Shown when the user taps a bond.
- Displays bond order/type and bond length.
- Rendered as a pill banner in the bottom-left corner.

## Measurement mode (Balls only)

- Toggle via the **straighten** icon (`Icons.Default.Straighten`) in the top-right action row.
- **Distance:** tap **2 atoms** → distance between them (Å). Only the last two atom picks are kept.
- **Angle:** tap **2 bonds** that share one atom (common vertex) → angle at that atom (°). Atom and bond picks are mutually exclusive (selecting bonds clears atom picks and vice versa).
- A pill **bottom overlay** (`MeasurementOverlay`) shows the current result, a **Reset** control (clears picks), and tapping the app icon on the left exits measurement mode.
- A top banner **“MEASURE MODE”** appears while the tool is active.

## Labels overlay (Balls only)

- Toggle via the labels icon.
- Labels are projected from atom world positions to view coordinates each frame (throttled).
- The label overlay is a non-touchable window overlay so it does not block interaction with the 3D view.

## Mode banners

- "MEASURE MODE", "LABELS MODE", and "Switch to Balls mode" appear as pill banners at the top.
- Styled consistently (app icon + text).
