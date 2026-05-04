## Performance (60 FPS) and memory leak testing

This document explains how to verify two quality requirements:

- **Rendering smoothness**: the app stays at **60 FPS** (i.e. most frames are under ~16.7 ms) during normal interaction.
- **Memory safety**: the app does **not leak memory** across repeated navigation and repeated creation/destruction of heavy UI (especially the 3D scene).

It is intentionally written as a **repeatable manual procedure** you can follow on both an emulator and a real device.

---

### 1) 60 FPS requirement: what it means in practice

Android UI is rendered in frames. On a 60 Hz display the budget is:

- \(1000 / 60 \approx 16.7\) ms per frame

The app can still be considered “60 FPS” if **most** frames are under budget and occasional spikes are rare (no constant stutter).

Recommended acceptance rule of thumb (manual testing):

- **Pass**: interactions feel smooth and frame times are mostly < 16.7 ms, with only occasional spikes.
- **Fail**: continuous jank, obvious stutter when rotating/zooming in 3D, or frequent long frames (e.g. > 32 ms).

---

### 2) Measuring smoothness (recommended): Android Studio Profiler

This is the most practical, no-code way to validate smoothness.

#### Setup

- Prefer a **real device** for realistic GPU/CPU behavior.
- Use a **release-like build** when possible (debug can add overhead), but debug is acceptable for a first pass.
- Close other heavy apps on the device.

#### Procedure (system trace / performance capture)

1. Run the app.
2. Open Android Studio → **Profiler**.
3. Start a **System Trace** (or “Performance” capture, depending on Android Studio version).
4. While tracing, do a 30–60 second interaction script:
   - Open a ligand.
   - Rotate the molecule continuously for ~10 seconds.
   - Zoom in/out for ~10 seconds.
   - Toggle visualization modes a few times.
   - Go back to list and open another ligand.
5. Stop capture.
6. Inspect **frame timeline / jank**:
   - Look for frequent long frames.
   - Confirm the interaction period does not show consistent “jank bars”.

What to record in a test note:

- Device model + Android version.
- Ligand ID used.
- Whether the trace shows frequent long frames, and during which interactions.

---

### 3) Measuring smoothness (quick): Developer Options overlay

This is useful when you want an on-device “sanity check”.

#### Setup

On the device/emulator:

- Enable **Developer options**.
- Enable **Profile HWUI rendering** → **On screen as bars**.

#### Procedure

1. Open the app and go to the 3D protein view.
2. Rotate/zoom for ~15 seconds.
3. Watch the bars:
   - If bars frequently exceed the target line, you are missing the frame budget often.

Notes:

- This is a coarse tool, but it quickly highlights obvious jank regressions.

---

### 4) Measuring smoothness (CLI): `dumpsys gfxinfo` (optional)

You can also check graphics stats via adb:

- `adb shell dumpsys gfxinfo com.music42.swiftyprotein framestats`

Focus on:

- a large number of “janky” frames
- frequent long frame durations during the interaction window

Because parsing output varies by Android version, treat this as supplemental evidence.

---

### 5) Memory leak testing: what a leak looks like

A leak usually shows up as:

- memory grows after each repeated action (open/close, navigate, rotate) and **never stabilizes**
- after forcing GC, memory does **not** drop meaningfully
- eventually the app becomes slow or crashes with OOM

For this app, the highest-risk area is repeated creation/destruction of the **3D scene**.

Related doc:

- See [`LARGE_MOLECULE_MEMORY_TESTING.md`](LARGE_MOLECULE_MEMORY_TESTING.md) for a large-ligand scenario and a recommended profiler loop.

---

### 6) Memory testing (recommended): Android Studio Memory Profiler

#### Setup

- Prefer a **real device** (better memory realism).
- Use Android Studio → **Profiler** → **Memory**.

#### Procedure (repeat loop)

1. Start the app in debug.
2. Attach Profiler → select the app process → open **Memory**.
3. Perform a loop 10 times:
   - Open a ligand → wait for 3D view
   - Interact (rotate/zoom) ~5–10 seconds
   - Go back to the list
4. During the loop, watch the memory chart:
   - memory can go up during scene creation
   - it should **not** increase by a similar amount each cycle forever
5. Trigger **GC** from Profiler (when available) at least twice:
   - after cycle 5
   - after cycle 10
6. If the chart suggests growth, take a **Heap Dump**:
   - one around cycle 2–3 (baseline)
   - one around cycle 10
7. Compare heap dumps:
   - look for large retained object sets that keep growing (views, bitmaps, scene/engine objects)

Pass criteria (manual):

- memory rises and falls (“sawtooth”), and does not trend upward without bound
- after GC, memory drops at least partially
- repeated open/close does not progressively degrade responsiveness

Fail criteria:

- clear upward trend after each cycle that GC does not correct
- heap dumps show growing retained objects tied to screens that should have been destroyed
- the app slows down heavily or crashes

---

### 7) Memory leak testing (optional): add LeakCanary (if desired)

If you want automated leak detection during development, LeakCanary can report leaked activities/fragments/views.

This repo currently does not require LeakCanary. If you decide to add it:

- keep it **debug-only**
- reproduce leaks by repeatedly entering/exiting the 3D screen and watching for LeakCanary reports

---

### 8) Recommended test matrix (minimal)

- **Device**: 1 real device + 1 emulator
- **Ligands**:
  - one “normal” ligand (fast)
  - one “large” ligand (see `LARGE_MOLECULE_MEMORY_TESTING.md`)
- **Scenarios**:
  - rotate/zoom continuous for 30 seconds
  - open/close 3D screen 10 times
  - switch visualization modes 10 times
