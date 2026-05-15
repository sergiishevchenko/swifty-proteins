# Preferences.readEnum

## Purpose

`Preferences.readEnum` is a small extension on Jetpack **DataStore Preferences** that reads a persisted enum value from a `stringPreferencesKey` and maps it back to a Kotlin `enum class`, with a safe fallback when the key is missing or the stored string is invalid.

## Location

[`app/src/main/java/com/music42/swiftyprotein/data/settings/SettingsRepository.kt`](../app/src/main/java/com/music42/swiftyprotein/data/settings/SettingsRepository.kt)

```kotlin
private inline fun <reified T : Enum<T>> Preferences.readEnum(
    key: Preferences.Key<String>,
    default: T
): T
```

## Signature

| Parameter | Type | Role |
|-----------|------|------|
| `key` | `Preferences.Key<String>` | DataStore key where the enum is stored as `enum.name` |
| `default` | `T` | Value returned when the key is absent or parsing fails |
| `T` | `Enum<T>` (reified) | Target enum type, resolved at compile time via `enumValueOf` |

## Behavior

1. Read `this[key]` from the current `Preferences` snapshot.
2. If the value is `null`, return `default`.
3. Otherwise call `enumValueOf<T>(raw)` inside `runCatching`.
4. On success, return the parsed enum constant.
5. On failure (unknown name, typo, renamed enum constant after an app upgrade), return `default`.

Enums are **written** elsewhere as strings, e.g. `mode.name` in `dataStore.edit { it[Keys.THEME_MODE] = mode.name }`.

## Why `inline` and `reified`

- **`reified T`**: `enumValueOf` needs the concrete enum class at runtime. Reification keeps the type argument available inside the function body (normal generics are erased on the JVM).
- **`inline`**: Required for `reified` type parameters in Kotlin; also avoids allocating a lambda when used from `map { prefs -> ... }`.

## Usage in this project

| Key | Enum | Default |
|-----|------|---------|
| `theme_mode` | `ThemeMode` | `ThemeMode.SYSTEM` |
| `default_visualization_mode` | `VisualizationMode` | `VisualizationMode.BALL_AND_STICK` |

Both are loaded when building `AppSettings` from `context.dataStore.data`.

## Design notes

- **Fail-safe**: A corrupted or outdated stored string does not crash settings load; the user gets the default and can change the setting again.
- **Not for SecureStorage**: User credentials and other secrets use [`SecureStorage`](SECURE_STORAGE.md), not DataStore enum helpers.
- **Private to `SettingsRepository`**: The helper is not part of a shared utilities module; duplicate it only if another repository needs the same pattern.

## Example

```kotlin
// Persist
context.dataStore.edit { it[Keys.THEME_MODE] = ThemeMode.DARK.name }

// Read
val mode = prefs.readEnum(Keys.THEME_MODE, ThemeMode.SYSTEM)
// ThemeMode.DARK, or ThemeMode.SYSTEM if missing / invalid
```
