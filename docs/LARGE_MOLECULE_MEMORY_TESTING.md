## Large molecule memory/performance testing (LOD & safeguards)

This document explains how to **manually test** the app’s large-molecule safeguards:

- **LOD (Level of Detail)**: lower sphere mesh resolution when atom count is high.
- **Auto-degrade**: if a ligand is considered large, the app reduces the most expensive visualization settings.
- **User warning**: the UI informs the user that quality was reduced for performance.

These mitigations live primarily in:

- [`app/src/main/java/com/music42/swiftyprotein/ui/proteinview/MoleculeSceneBuilder.kt`](../app/src/main/java/com/music42/swiftyprotein/ui/proteinview/MoleculeSceneBuilder.kt)
- [`app/src/main/java/com/music42/swiftyprotein/ui/proteinview/ProteinViewViewModel.kt`](../app/src/main/java/com/music42/swiftyprotein/ui/proteinview/ProteinViewViewModel.kt)

---

### Known large ligands (from `ligands.txt`)

The following ligand IDs were sampled from [`app/src/main/res/raw/ligands.txt`](../app/src/main/res/raw/ligands.txt) by downloading their CIF files from RCSB and counting `_chem_comp_atom` rows:

- **`CDL`**: ~**256 atoms**
- **`15P`**: ~**244 atoms**

These are good candidates to reliably trigger the “large molecule” behavior.

---

### Expected behavior (functional check)

1. Open `CDL` or `15P` from the ligand list.
2. Expected on the Protein (3D) screen:
   - A dialog titled **“Large molecule”** appears.
   - If the current visualization mode is **Space Fill**, it is automatically changed to **Wireframe**.
   - Hydrogens are not enabled automatically for large ligands.
   - The model remains interactive (no freeze) and the UI stays responsive.

Notes:
- The exact atom threshold is defined in [`ProteinViewViewModel.LARGE_MOLECULE_THRESHOLD`](../app/src/main/java/com/music42/swiftyprotein/ui/proteinview/ProteinViewViewModel.kt) (currently 200).
- The LOD mesh resolution thresholds are in [`MoleculeSceneBuilder`](../app/src/main/java/com/music42/swiftyprotein/ui/proteinview/MoleculeSceneBuilder.kt) (`LOD_THRESHOLD_MED`, `LOD_THRESHOLD_LOW`).

---

### Memory testing with Android Studio Profiler (recommended)

This is the most practical way to validate that the app does not “leak” memory when repeatedly building/destroying 3D scenes.

#### Setup

- Prefer a **real device** for performance and memory realism.
- Use Android Studio → **Profiler**.

#### Procedure

1. Start the app in **debug**.
2. Open **Profiler** and select the app process.
3. Perform the following loop 5–10 times:
   - Open `CDL` (or `15P`) → wait until the 3D view is visible
   - Rotate/zoom for ~10 seconds
   - Go back to the ligand list
4. Watch the memory chart:
   - It can rise during scene creation, but should **not grow unbounded** across repeated open/close cycles.
5. Trigger GC from Profiler (when available) and confirm memory drops at least partially.

#### What is considered a failure

- Memory grows by a similar amount on each open/close cycle and **never stabilizes**.
- The app eventually crashes with **OutOfMemoryError** or becomes unusably slow after several cycles.

---

### Stress tests (no profiler)

If you cannot use the Profiler, you can still do a coarse stress test:

1. Open `CDL`.
2. Switch visualization modes several times (Balls → Fill → Wire → Sticks).
3. Toggle labels/measurement (Balls only).
4. Trigger screenshot share and video recording.
5. Repeat steps 1–4 with `15P`.

Expected:
- No crashes.
- No increasing jank after multiple opens.

---

### Troubleshooting notes

- If you cannot find `CDL` / `15P` in the list, verify they exist in [`app/src/main/res/raw/ligands.txt`](../app/src/main/res/raw/ligands.txt).
- If RCSB fetch fails, verify network access and that `https://files.rcsb.org/ligands/view/{ID}.cif` is reachable (example: `https://files.rcsb.org/ligands/view/CDL.cif`).
