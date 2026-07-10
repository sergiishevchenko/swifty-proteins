# How Swifty Proteins Works

This document describes the user journey and data flow **from signing in to displaying a three-dimensional molecular model**. It is not tied to specific source files.

---

## 1. Launch

When the app opens, the system splash (icon and theme background) appears for about two seconds. Then the main Jetpack Compose UI loads. The navigation start destination is **Login**.

A security rule runs in parallel: if the user sends the app to the background (Home, app switcher) and returns later, they are sent back to Login. An exception is a short **suppress window** after system dialogs (for example screen-capture permission or the share sheet), so the user is not kicked out of molecule viewing mid-share.

---

## 2. Login Screen

The screen supports **sign in** or **register** (mode toggle). Fields: username and password.

On first open, if someone has signed in before, the last username is filled in automatically. It is stored in **encrypted preferences** on the device (not the password, only the name).

### Registration

- Empty fields and passwords shorter than 8 characters are rejected with a clear message.
- If the username is already taken, an error is shown.
- On success, a row is created in the **local SQLite database (Room)**: username and **password hash** (bcrypt). The plain password is never stored.
- The last user is remembered for convenience and biometrics.
- The “onboarding completed” flag is cleared so the new user sees tutorial screens.

### Password Sign-In

- Username and password are checked against the database; wrong credentials show an error dialog.
- On success, the last user is saved again.

### Biometrics (Fingerprint)

Available only when:

- biometrics are set up on the device;
- a user with that name exists in the database;
- the input field shows the **same username** as the last successful sign-in.

Then a fingerprint sign-in button appears. Successful biometrics **do not re-check the password** — the app assumes the user previously signed in with a password and trusts device unlock. Changing the username in the field disables biometrics until it matches the last user again.

---

## 3. Where the User Goes After Sign-In

Navigation picks the next step:

- if the user **just registered** or onboarding is not finished → **onboarding** — several pages with tips on gestures, modes, favorites;
- otherwise → **ligand list**.

After onboarding, the “completed” flag is written to DataStore; on later sign-ins, onboarding is skipped.

The **session** is updated: the current username is kept in memory for app bar titles.

---

## 4. Ligand List

This is a catalog of molecule identifiers (for example `001`, `ATP`), not ready-made 3D models.

### Where the List Comes From

Identifiers are read once from a **bundled text file** in app resources (`ligands.txt`) — about 1200 lines. The network is not used for the list.

### Search

The search field filters the list **in real time**, case-insensitive, by substring in the ID.

### Row Subtitles (Optional)

If a ligand’s CIF was downloaded and cached before, while scrolling the list **lazy** loading pulls short info from cache for visible rows: formula and atom count (without building full 3D).

### Catalog Load Error

If the list file cannot be read, a dialog shows the error with **Retry** and **Close**.

### Favorites

Favorite IDs are stored separately in Room; the star on a row toggles favorite without reloading the whole list.

---

## 5. Selecting a Ligand

The user taps a row. **Downloading the molecule does not start here** — only the intent to open the viewer with that ID. Navigation opens **Protein View (3D)** with the identifier in the route arguments.

This avoids double loading: data is requested once on the viewer screen.

---

## 6. Loading Molecule Data (CIF)

On the 3D screen, loading starts immediately. The user sees a progress indicator and changing stage labels (cache, download, parse, and so on).

### Source

Structure is requested from **RCSB** in **CIF** format using: `https://files.rcsb.org/ligands/view/{ID}.cif`.

### On-Disk Cache

Before the network, the app checks an in-app cache folder (`cif_cache`). If a file for that ID exists and parsing yields atoms, the cache is used and the internet is not needed.

If there is no cache or parsing produced an empty model:

1. HTTP request to RCSB;
2. on success, the response body is saved to cache;
3. then parsing runs.

Typical failures become readable messages: no network, timeout, 404 (ligand not found), empty response, parse error.

On failure the screen shows **Retry** (same load from scratch) and **Back** (return to the list).

For large molecules, a quality warning may appear after a successful load.

---

## 7. Parsing CIF into the App Model

**CIF** (Crystallographic Information File) is a text format from structural chemistry. An RCSB ligand file is not a ready 3D scene but tables of metadata, atom coordinates, and bonds. The parser reads **the full text into memory**, line by line, and builds a **Ligand** object used only by the UI and scene builder.

### Header Fields

- **Compound name** — `_chem_comp.name` (value on the same line or the next).
- **Formula** — `_chem_comp.formula` (same pattern).

If fields are missing, name and formula stay empty; rendering is unaffected, but formula may still show in the ligand list when CIF is already cached.

### Atoms: `loop_` Block

In CIF, repeated data uses `loop_` blocks: **column headers** (lines starting with `_`), then **data rows** until a blank line, `#`, a new `loop_`, or `data_`.

The parser finds a loop whose headers include the `_chem_comp_atom.` prefix. Required columns:

| Meaning | Typical CIF name |
|--------|-------------------|
| Atom ID in file | `_chem_comp_atom.atom_id` |
| Chemical element | `_chem_comp_atom.type_symbol` |
| X, Y, Z (Å) | `pdbx_model_Cartn_x/y/z_ideal` or fallback `model_Cartn_x/y/z` |

Each data row is split into **tokens** respecting quotes and spaces (values like `'C1'` parse correctly). Each valid row becomes an **Atom**: string `id`, element symbol, three `float` coordinates.

The **element symbol** comes from `type_symbol`; if missing or invalid, from `atom_id` (for example `C1` → carbon). Two-letter elements are supported (Cl, Br, Fe…). As a last resort, carbon `C` is used so the scene can still build.

Rows with non-numeric coordinates are skipped. If **no atoms** remain after the loop, loading fails (“failed to parse”) even when the file downloaded successfully.

### Bonds: Second `loop_` Block

Similarly, a loop with `_chem_comp_bond.`:

- `atom_id_1`, `atom_id_2` — which two atoms are connected (same IDs as in the atom table);
- `value_order` — PDB bond order: `SING`, `DOUB`, `TRIP`, `AROM`; unknown values are treated as single.

A bond whose atom is missing from the filtered list (for example hidden hydrogen) is simply **not drawn** when building the scene — that is expected.

### In-Memory Model

**Ligand** contains:

- `id` — code from navigation (for example `001`);
- `name`, `formula` — from CIF;
- `atoms` — full atom list from the file;
- `bonds` — all bonds from the file.

Network and parser are not called again until the user taps **Retry** or opens another ligand.

---

## 8. Preparing 3D: Viewer Settings

The viewer receives `ligandId` from navigation and **in parallel** starts two tasks: CIF loading (section 6) and reading **user settings** from DataStore (not from encrypted storage — that holds only the login username).

### Settings Applied Before the First Frame

| Setting | Effect when opening the screen |
|-----------|--------------------------------|
| Default visualization mode | Initial scene style |
| Show hydrogens by default | Whether H/D atoms are included for rendering |

Other app settings (light/dark/system theme, onboarding flag) affect this screen indirectly through the global theme and 3D background color.

### Four Display Modes

1. **Ball-and-stick (Balls)** — atom spheres plus bond cylinders; main assignment mode. Atom element labels and distance/angle measurement are available here.
2. **Space fill** — each sphere radius scales by **Van der Waals** radius: a “inflated” molecule; **bonds are not drawn** (overlapping spheres only).
3. **Sticks only** — **no atom spheres**, only bond cylinders between atom centers.
4. **Wireframe** — very thin cylinders; each bond is always one “rod”, regardless of double/triple bond in data.

The user can switch mode **after** load — the scene is **rebuilt** (new meshes), not just a camera change.

### Hydrogens

By default atoms **H** and **D** (deuterium) are **excluded** from drawing: less clutter and faster GPU. If “show hydrogens” is on and the file has them, they appear in the scene; if filtering leaves no atoms (rare), the full list is used so the model does not vanish.

### Large Molecule Rules

Threshold: **more than 200 atoms** in the parsed Ligand:

- a quality warning is shown;
- hydrogens are **forced off** even if enabled in settings;
- if **Space fill** was selected, it is automatically switched to **Wireframe** to avoid too many overlapping spheres hurting performance.

### Loading UI Indicator

While `ligand == null`, the screen does not show 3D, only a centered Material card:

- spinner and label “Loading ligand {ID}”;
- **current stage** (`loadingStage`): “Reading cache”, “Downloading CIF”, “Parsing ligand”, “Preparing scene”, and so on — passed from the data layer during download/read;
- **linear progress** 0…1 from the same callbacks.

To avoid a flash on fast cache hits, success is not shown until at least ~**350 ms** after the request started.

### What Can Change on a Loaded Model (Without Re-Fetching CIF)

- visualization mode and hydrogens → **3D node rebuild**;
- atom labels, atom/bond selection, measurements, zoom, model animation, sharing — **on top of** the loaded Ligand; RCSB data is untouched.

---

## 9. Building the 3D Scene

When Ligand is in the screen state, a separate step turns abstract atoms and bonds into a **3D node tree** for **Filament** (Google’s low-level renderer) via the **SceneView** wrapper.

### When the Scene Is Rebuilt

The molecule graph is rebuilt when any of these change:

- the Ligand itself (different ID or reload);
- visualization mode;
- “show hydrogens” flag.

One **parent node** holds all spheres and cylinders. Two maps are filled in parallel: **which mesh node is which Atom**, **which node is which Bond** (for taps and highlighting).

### Centering and Coordinates

First, atoms for rendering are chosen (with H/D filter). Their coordinates are **averaged** to get the geometric center. Each scene position = atom coordinate minus that center (plus optional focus/pan offsets from “move model” gestures).

The molecule ends up near scene origin `(0,0,0)` and the camera looks at that point.

### Atoms: Geometry and Materials

If the mode is not sticks-only / wire without spheres in the sticks sense:

For each atom a **MeshNode** with **sphere** geometry is created:

- **Ball-and-stick**: shared sphere radius; H/D use a smaller radius (~55%).
- **Space fill**: radius = base × (element VDW / carbon VDW) — oxygen larger, hydrogen smaller.
- **Color** — **CPK** table: N blue, O red, C gray, S yellow, Cl green, Fe orange, and many more; unknown elements are neutral gray.
- **Material** — colored, non-metallic, moderate roughness (~0.6), slight specular.
- A **collision shape** slightly larger than the sphere makes taps easier.

**LOD:** above 100 atoms, fewer sphere segments; above 200, coarser (8×8 instead of 16×16). This greatly reduces GPU load on large ligands.

The builder can optionally highlight **one element** (brighten those atoms, dim others) — the current UI uses this indirectly when selecting an atom (see below).

### Bonds: Cylinders and Order

Bonds are drawn unless the mode is **Space fill** (spheres only).

For each bond, positions of both atoms are taken. If either atom was filtered out, the bond is skipped. Near-zero length is skipped too.

- **Single** — one cylinder between centers.
- **Double and aromatic** — **two** parallel cylinders offset perpendicular to the bond axis.
- **Triple** — **three** cylinders (center plus two side).

Each cylinder is effectively **two halves** of different color: from atom 1 to midpoint — first atom’s color; midpoint to atom 2 — second atom’s color. The rod visually “blends” between elements.

In **wireframe**, all bonds are **one thin line** (small radius), without doubling/tripling.

Each cylinder also has a mesh for **tap** (bond selection and length hint).

### Camera Before First Gesture

Over all visible atoms, a **bounding sphere radius** is estimated (distance from center plus visual atom radius for the mode). The camera is placed at ~4.5× that radius at a fixed angle, looking at the center. The user then changes distance with pinch, ± buttons, or mouse wheel on the emulator, roughly within 0.2×…6× of the base distance.

### Model animation

The **Animation** button (Play / Stop in the action row) rotates the existing **parent node** around the vertical axis at ~30°/s. Rotation is updated in the SceneView `onFrame` callback using frame delta time, so motion stays smooth regardless of FPS. Stopping animation leaves the model at its current angle; **Reset view** also clears rotation.

Manual gestures (orbit, zoom, pan) are independent: animation changes `parentNode.rotation`, while gestures move the camera or pan offset.

### Video recording orbit

During ~5 s screen recording, the **camera** (not the model) auto-orbits horizontally so the shared MP4 shows motion. This is separate from the Animation toggle.

### Highlighting Without Full Rebuild

When the user **selects an atom**, a pass **changes only materials** on existing spheres:

- selected atom — near white / brightened;
- atoms of the **same element** — dimmed;
- others — base CPK.

In **measurement** mode, the last two picked atoms get an orange tint.

For **bonds**: selected bond — bright cyan; bonds used for angle measurement — orange; others — original two-color gradient.

Interaction stays fast: thousands of meshes are not recreated, only recolored.

---

## 10. Drawing on Screen

The 3D part is not “a picture in Compose” but a full **Surface/OpenGL** inside a **SceneView** widget, embedded in Compose via **AndroidView** (bridge between declarative UI and classic Views).

### Three Screen States

1. **Loading** — only a Material progress card over empty space (section 8); SceneView does not show the molecule yet or is not created.
2. **Error** — centered text and Retry / Back; no 3D.
3. **Success** — SceneView fills the area under the app bar, with Compose layers on top.

### How Filament Renders a Frame

SceneView holds **engine**, **scene**, **camera**, **lighting**. Each frame (`onFrame`):

- camera position updates (gestures, zoom, optional recording orbit);
- if model animation is on, `parentNode.rotation` updates around Y using elapsed frame time;
- camera `lookAt` center `(0,0,0)`;
- if needed, **focus offset** eases in after double-tap on an atom (molecule shifts slightly so the picked atom is easier to see);
- at end of frame, **label overlay** redraw is requested if labels are on.

The scene background is cleared with the **app theme** color (light or dark) so 3D does not look like a foreign black patch.

The view is **on top** with transparency (`TRANSLUCENT`) so Compose and popups can stack correctly.

### SceneView Gestures

- **One finger drag** — orbit camera around center (SceneView manipulator).
- **Two-finger pinch** — change camera distance (zoom).
- **Two-finger pan** — pan model (`panOffset`).
- **Tap** — hit-test nearest atom sphere; short repeat tap on same atom may focus camera; tap empty space clears selection.
- **Tap on bond cylinder** — select bond for hint (order, length in ångströms).
- **Wheel / scroll** (often on emulator) — extra zoom.

After zoom and rotation, atom labels **sync** to the frame: overlay redraw is posted on the next animation frame (`postOnAnimation`) so text does not lag behind the molecule.

### Atom Labels (Balls Mode Only)

A separate transparent **View** in a `Popup`, sized to the SceneView area, **not consuming touches** (gestures pass through to 3D).

On `onDraw` for each atom:

- **world position** of the sphere mesh node;
- camera maps it to **normalized screen coordinates** (`worldToView`);
- **element symbol** (O, C, N…) is drawn in pixels with Android graphics.

On every camera move, labels recompute — 2D text over 3D, not Filament textures.

### Compose Over 3D

In the same `Box` over SceneView, Material 3 UI:

- **app bar** — ligand name, logout;
- **top-right actions** — record video, **animation (Play/Stop)**, measure, labels, hydrogens, share;
- **bottom bar** — visualization mode chips (Balls / Fill / Sticks / Wire);
- **left column** — zoom `+`, `-`, reset view;
- **popup card** for selected atom or bond (element, name, ID, bond length);
- **measurement lines and arcs** — distance between two atoms or angle between two bonds sharing an atom (Balls only);
- **mode banner** — short hint when switching Balls / Fill / Sticks / Wire;
- **large molecule dialog**.

Theme (light/dark) sets Material colors **and** Filament clear color so scene edges match the rest of the app.

### Screenshot and Video

When SceneView is created, a reference is kept for **PixelCopy** (PNG/JPEG screenshot) and **MediaProjection** (MP4 recording). During system share dialogs the app temporarily **does not** treat the user as backgrounded, to avoid returning to Login mid-recording.

---

## 11. Interaction After Rendering

On a loaded model the user can:

- **rotate and zoom** — gestures and buttons;
- **animation** — toggle continuous Y-axis model rotation (Play / Stop);
- **tap atom/bond** — card with element, name, bond length;
- **display modes** — ball-and-stick, space fill, sticks, wireframe;
- **hydrogens** — show/hide (unless disabled for very large molecules);
- **favorites** — same Room record as in the list;
- **screenshot and video** — PixelCopy snapshot, MediaProjection video with temporary Login suppress;
- **logout** — app bar icon → confirm dialog → in-memory session cleared and navigation to Login (last username on disk for biometrics is **not** cleared).

---

## 12. End-to-End: Sign-In to Picture

1. Splash → Login.  
2. Register or password (optional fingerprint) → Room check + save last username.  
3. Onboarding (if needed) → ID list from `ligands.txt`.  
4. Tap ID → 3D screen with ligandId.  
5. CIF cache or RCSB download → save under `cif_cache`.  
6. CIF parser → atoms and bonds.  
7. Scene builder → spheres and cylinders in Filament/SceneView.  
8. Camera and gestures → user sees and rotates the model.

---

## 13. What Is Stored Where

| What | Where |
|-----|-----|
| Users and password hashes | Local SQLite |
| Last sign-in username | Encrypted SharedPreferences |
| Theme, onboarding, default 3D mode, hydrogens | DataStore |
| Favorite ligand IDs | SQLite |
| Downloaded CIF files | App files directory |
| Full ligand ID list | Bundled raw resource |

The internet is needed for the **first** load of a given ligand (if there is no valid cache), not for the list or sign-in.
