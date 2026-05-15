# SecureStorage

## Purpose

`SecureStorage` wraps **AndroidX Security** `EncryptedSharedPreferences` to store a small amount of sensitive app data on disk (currently the last logged-in username for convenience on the login screen). Values and preference keys are encrypted; the encryption keys are managed by Android Keystore via a `MasterKey`.

## Location

[`app/src/main/java/com/music42/swiftyprotein/data/security/SecureStorage.kt`](../app/src/main/java/com/music42/swiftyprotein/data/security/SecureStorage.kt)

Injected via Hilt in [`AppModule`](../app/src/main/java/com/music42/swiftyprotein/di/AppModule.kt) and used from [`AuthRepository`](../app/src/main/java/com/music42/swiftyprotein/data/repository/AuthRepository.kt).

## API

| Method | Description |
|--------|-------------|
| `setLastUsername(username)` | Writes `last_username` to encrypted prefs |
| `getLastUsername()` | Reads `last_username`, or `null` if unset |

Passwords are **not** stored here; they live as bcrypt hashes in Room (`User.passwordHash`). See [`AUTH.md`](AUTH.md).

## How encryption is configured

```kotlin
val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()

EncryptedSharedPreferences.create(
    context,
    "secure_prefs",
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)
```

There are **three** algorithm choices in this stack. They protect different layers.

---

## Master key: `AES256_GCM`

**What it is:** The scheme used for the **master key** held in Android Keystore that wraps the actual keys used to encrypt the SharedPreferences file.

**Properties:**

- **AES-256** in **GCM** (Galois/Counter Mode) — authenticated encryption.
- Provides **confidentiality** and **integrity** for key material derived from the master key.
- Tied to the device Keystore; keys are not extractable as plaintext by normal app code.

**Role here:** Root of trust for `EncryptedSharedPreferences`. Without a strong master key, preference encryption would be weak regardless of SIV/GCM on entries.

---

## Preference keys: `AES256_SIV`

**What it is:** **S**ynthetic **IV** (initialization vector) mode — in AndroidX this is **AES-SIV** (RFC 5297 style usage in the library).

**Used for:** Encrypting **preference key strings** (e.g. `"last_username"`).

**Why SIV for keys:**

| Aspect | SIV | GCM (for comparison) |
|--------|-----|----------------------|
| Nonce/IV misuse | **Deterministic** for the same key + plaintext: equal keys encrypt to equal ciphertext | Requires unique nonce per encryption; reuse breaks security |
| Key privacy | Hides **which** preference names exist (keys are not stored in plaintext) | Same, when used correctly |
| Typical use in EncryptedSharedPreferences | **Keys** — short, often stable identifiers | **Values** — larger, unique payloads |

**Intuition:** Preference **names** are fixed, short, and repeated. SIV is chosen so key encryption is robust and does not depend on generating a fresh random nonce for every key on every write in the same way GCM expects for values.

---

## Preference values: `AES256_GCM`

**What it is:** **AES-256-GCM** — standard authenticated encryption with a random IV per encryption.

**Used for:** Encrypting **preference values** (e.g. the username string).

**Why GCM for values:**

- Values change, vary in length, and are written often; GCM is fast and widely used for bulk data.
- Each write gets a fresh IV; integrity is checked on decrypt (tampering is detected).
- Better fit than SIV for arbitrary user/content strings that are not fixed identifiers.

**Intuition:** **Values** behave like typical secret payloads; **GCM** is the usual choice for encrypt-then-MAC-at-tag semantics in mobile APIs.

---

## Summary table

| Layer | Algorithm | Encrypts | Main property |
|-------|-----------|----------|----------------|
| `MasterKey` | `AES256_GCM` | Keystore-backed master key | Root key protection on device |
| `PrefKeyEncryptionScheme` | `AES256_SIV` | SharedPreferences **keys** | Deterministic, safe for stable key names |
| `PrefValueEncryptionScheme` | `AES256_GCM` | SharedPreferences **values** | Authenticated encryption for arbitrary strings |

## Threat model (practical)

**Protects against:**

- Casual inspection of app data on a rooted device or backup (keys and values not readable as plain text).
- Leaking **which** settings exist via cleartext key names (keys are encrypted with SIV).

**Does not replace:**

- Strong password hashing (bcrypt in Room).
- Server-side security (this app is local-only).
- Protection if the device is fully compromised while unlocked (memory / runtime attacks).

## Related storage

| Store | Technology | Contents |
|-------|------------|----------|
| `SecureStorage` | EncryptedSharedPreferences | `last_username` |
| `SettingsRepository` | DataStore Preferences | theme, default vis mode, onboarding, hydrogens |
| `AuthRepository` / Room | SQLite | `users` with `passwordHash` |
| `FavoritesRepository` | Room | favorite ligand IDs |

Enum settings use [`Preferences.readEnum`](PREFERENCES_READ_ENUM.md) on **non-encrypted** DataStore; only credentials-adjacent prefs belong in `SecureStorage`.
