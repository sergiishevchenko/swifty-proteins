## Xiaomi real-device setup (USB) for Android Studio

This guide explains how to connect a **Xiaomi / Redmi / POCO** phone (MIUI or HyperOS) to **Android Studio** using a **USB cable**, so the device appears as a run target.

---

### Prerequisites

- **A data-capable USB cable** (many cables are charge-only).
- **Android Studio** installed.
- **Android SDK Platform-Tools** available (Android Studio includes `adb`).

---

### 1) Enable Developer options on Xiaomi (MIUI / HyperOS)

1. Open **Settings → About phone**.
2. Tap **MIUI version** (MIUI) or **OS version** (HyperOS) **7–10 times**.
3. You should see a message like **“You are now a developer”** / **“Developer options enabled”**.

---

### 2) Enable USB debugging

1. Open **Settings → Additional settings → Developer options**.
   - On some devices it may be under **Settings → System → Developer options**.
2. Enable:
   - **USB debugging**
3. (Recommended on Xiaomi) Also enable if present:
   - **Install via USB**
   - **USB debugging (Security settings)**

If you are prompted for permissions/warnings, confirm them.

---

### 3) Connect the phone by cable and authorize the computer

1. Connect the phone to your computer via USB.
2. **Unlock the phone**.
3. If a popup appears: **“Allow USB debugging?”** → tap **Allow**.
   - Optional: check **“Always allow from this computer”** to avoid repeated prompts.
4. If you see a USB mode notification, choose **File transfer (MTP)** (when available).

---

### 4) Verify the connection with `adb`

In **Android Studio → Terminal**, run:

```bash
adb devices
```

Expected result: your device is listed with status **`device`**.

Common statuses:

- **`unauthorized`**: the phone has not authorized this computer yet.
  - Unlock the phone and accept the “Allow USB debugging?” prompt, then run `adb devices` again.
- **`offline`**: usually a flaky cable/port/hub or an ADB server hiccup.

If needed, restart ADB:

```bash
adb kill-server
adb start-server
adb devices
```

---

### 5) Run from Android Studio on the physical device

1. Open your project in Android Studio.
2. Use the device selector near the Run button (or **Run → Select Device**).
3. Pick your phone and press **Run**.

---

## Troubleshooting (most common fixes)

- **Device not listed in Android Studio**
  - Confirm `adb devices` shows the phone as `device`.
  - Switch USB cable (try a known data cable).
  - Try another USB port (avoid hubs/adapters if possible).
  - Re-plug the cable, unlock phone, re-accept debugging prompt.

- **`adb devices` shows nothing**
  - Ensure **USB debugging** is enabled in Developer options.
  - Set USB mode to **File transfer (MTP)**.
  - Try `adb kill-server && adb start-server`.

- **`unauthorized`**
  - On the phone: accept the authorization dialog.
  - If the dialog doesn’t appear:
    - Toggle **USB debugging** off/on.
    - In **Developer options**, use **Revoke USB debugging authorizations**, then reconnect and re-authorize.

- **Still stuck**
  - Reboot the phone and retry.
  - Ensure the phone is not in a “charge only” USB mode.
