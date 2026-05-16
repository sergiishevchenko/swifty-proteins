# Flow, StateFlow, and MutableStateFlow

Short guide to Kotlin **cold flows**, **hot state holders**, and how **`MutableStateFlow`** relates to **`StateFlow`**.

## Flow

`Flow<T>` is a **cold** asynchronous sequence:

- **Cold**: nothing runs until something **collects** it. Each collector typically starts its own producer logic (unless you applied operators like `shareIn` that turn it hot).
- **Multi-valued over time**: can emit zero or many values and complete (or fail).
- **Flexible**: maps well to events, streams, database/network paging, etc.

Typical mental model: “a recipe for values” that executes when someone subscribes.

## StateFlow

`StateFlow<T>` is a **hot** flow that **always holds a current value**:

- **Hot**: active independently of collectors (within the scope where it is collected/shared).
- **Always has a value**: new collectors immediately receive the latest state (no “waiting for first emission” in the same sense as many cold flows).
- **Conflation**: rapid updates may collapse—collectors may not see every intermediate value, only what matters “now” (similar in spirit to `LiveData` coalescing UI updates).
- **Equality**: `StateFlow` uses `Any.equals` on updates; assigning/equal value may suppress emission.

Use `StateFlow` when you model **observable state**: UI state, settings snapshot, “current screen,” etc.

## MutableStateFlow

`MutableStateFlow<T>` is the **writable** implementation:

- It **is a** `StateFlow` (you can pass it anywhere a `StateFlow` is needed).
- Exposes **`value`** for synchronous read/write and **`compareAndSet`** for atomic updates.
- In layers exposed to other modules/UI, prefer exposing **`StateFlow<T>`** (read-only) and keeping **`MutableStateFlow`** private inside a repository/view model.

```kotlin
private val _uiState = MutableStateFlow(UiState())
val uiState: StateFlow<UiState> = _uiState.asStateFlow()
```

## Side-by-side differences

| Aspect | `Flow` | `StateFlow` | `MutableStateFlow` |
|--------|--------|-------------|---------------------|
| Temperature | Cold by default | Hot | Hot |
| Current value | Not required | Always present | Always present |
| Typical role | Events/streams | Observable state | Mutable observable state |
| Who updates | Producer inside flow builder | Usually wrapped `MutableStateFlow` | Owner updates `value` / operators |
| Collector behavior | Runs producer per collector (unless shared) | Shares current value with all collectors | Same as `StateFlow` |

## When to use which

- **`Flow`**: one-shot or multi-shot **operations** (network calls, DB queries), **event** streams (clicks mapped to actions), pipelines built with `map`, `filter`, `flatMapLatest`, etc.
- **`StateFlow`**: replaceable snapshot of **state** that the UI or other layers **observe**.
- **`MutableStateFlow`**: internal backing store for that state; update from coroutines or synchronous code, then expose read-only `StateFlow`.

## Related notes

- To convert a cold `Flow` into something state-like and shared, use **`stateIn`** or **`shareIn`** with an appropriate `CoroutineScope` and `SharingStarted` policy—not every `Flow` should become a `StateFlow`.
- **`SharedFlow`** is another hot primitive; unlike `StateFlow`, it does not require a single “current value” (better for events). Choose **`StateFlow`** for state, **`SharedFlow`** for fire-and-forget events.
