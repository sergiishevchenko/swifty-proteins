# Java vs Kotlin (JVM)

This document compares **Java** and **Kotlin** for developers who work on the JVM—especially **Android**, but the ideas apply to backend and desktop as well. The goal is not to declare a “winner,” but to explain **what each language optimizes for** and **where they differ in everyday code**.

Both languages compile to **JVM bytecode** and run on the same virtual machine. Kotlin was created at JetBrains with **full Java interoperability** in mind: you can have Java and Kotlin in one Gradle module, call Java libraries from Kotlin without adapters, and call Kotlin from Java with a few naming conventions. That is why Android adoption moved gradually: teams could migrate screen by screen instead of rewriting the whole app at once.

---

## Big picture

**Java** is a mature, widely taught language with a enormous ecosystem: libraries, books, hiring pool, and decades of production use. Its syntax grew slowly; modern Java (17+) has improved a lot (records, pattern matching, `var`), but much Android and enterprise code still looks like “classic” Java: classes, interfaces, builders, and explicit null checks.

**Kotlin** is a newer JVM language (2011, stable 1.0 in 2016) designed to be **more concise and safer by default**, especially around **nullability** and **boilerplate**. On Android it became the **recommended language** in 2019; Jetpack Compose, coroutines, and many official samples are Kotlin-first. Kotlin is not a separate runtime—it is another front end to the same JVM (and can also target JS/Native, which this doc does not focus on).

| Topic | Java | Kotlin |
|--------|------|--------|
| Typical style | Imperative OOP, interfaces, inheritance | OOP + functional idioms (lambdas, higher-order functions) |
| Null handling | Everything can be null unless you discipline yourself | Non-null by default; `T?` for nullable |
| Boilerplate | Getters/setters, builders, listeners | `data class`, default parameters, property syntax |
| Async on Android | Threads, executors, callbacks, RxJava | Coroutines + Flow (with Java interop still possible) |

---

## Variables, mutability, and types

In **Java**, you declare a variable with an explicit type, and use `final` when the *reference* must not be reassigned:

```java
String name = "ATP";
final int count = 42;
```

In **Kotlin**, you choose between **`var`** (reassignable) and **`val`** (read-only reference—like `final`). Types are often inferred:

```kotlin
var name = "ATP"      // can reassign name to another String
val count = 42        // cannot reassign count
val explicit: Int = 1
```

Important nuance: **`val` does not make an object immutable**—only the variable cannot point to another instance. A `val list` can still have elements added if the list itself is mutable. For immutable collections, Kotlin’s standard library exposes read-only interfaces (`List`) vs mutable ones (`MutableList`).

Kotlin also has no primitive types in source code the way Java does; `Int`, `Double`, etc. compile to JVM primitives where possible, so performance stays close to Java for typical numeric code.

---

## Null safety: the largest practical difference

In **Java**, any reference type can be `null` unless you use conventions or tools. A missing check often surfaces as a **`NullPointerException` at runtime**—one of the most common crash types on Android.

```java
String title = user.getProfile().getDisplayName(); // any hop may be null
```

You can use `Optional<T>` (Java 8+) or static analysis, but the language does not force a distinction between “maybe null” and “never null” in the type system.

In **Kotlin**, types are **non-null by default**:

```kotlin
var title: String = "Ligand"   // cannot assign null
var nickname: String? = null   // nullable type
```

To use a nullable value you must handle absence explicitly:

```kotlin
val len = nickname?.length           // safe call: null if nickname is null
val display = nickname ?: "Guest"      // Elvis: default if null
nickname?.let { save(it) }           // run block only when non-null
```

The compiler rejects many calls like `nickname.length` without a check. That removes a whole class of bugs before the app ships.

**Interop caveat:** Java types seen from Kotlin are **platform types** (`String!`)—the compiler does not know if the Java API returns null. You still need judgment or annotations (`@Nullable` / `@NonNull`) on the Java side.

---

## Functions and where they live

**Java** ties behavior to classes. “Utility” helpers often end up in `SomethingUtils` with `static` methods. Interfaces cannot hold implementation (until `default` methods in Java 8).

**Kotlin** allows **top-level functions** in a file—no enclosing class required:

```kotlin
// File: Formatters.kt
fun formatLigandId(id: String): String = id.trim().uppercase()
```

It also supports **default parameter values** and **named arguments**, which reduce overload explosions:

```kotlin
fun connect(timeoutMs: Int = 30_000, log: Boolean = false) { ... }
connect(log = true)
```

In Java you would typically add multiple overloads or a builder. For Android listeners and one-off callbacks, Kotlin’s syntax is usually shorter.

---

## Classes, data, and sealed hierarchies

A simple “holder” in **Java** often needs:

- private fields,
- constructor,
- getters (and maybe setters),
- `equals`, `hashCode`, `toString` (or Lombok/`record` in modern Java).

**Kotlin `data class`** generates the usual boilerplate for properties declared in the primary constructor:

```kotlin
data class LigandSummary(val id: String, val name: String)
```

You get `copy`, `equals`, `hashCode`, and destructuring for free—very common for UI state and DTOs in this project’s ViewModels.

For **restricted inheritance** (e.g. UI events, network results), Kotlin offers **`sealed class`** / **`sealed interface`**: all subclasses are known at compile time, so a **`when`** expression can be **exhaustive**—the compiler warns if you forgot a branch. Java is moving in a similar direction with sealed types in newer versions, but Kotlin has had this pattern in Android codebases for years.

---

## Strings and collections

**Java** builds strings with `+`, `StringBuilder`, or `String.format`.

**Kotlin** uses **string templates**:

```kotlin
val message = "Loaded ligand $ligandId in ${elapsedMs}ms"
```

Collections on the JVM are largely the same underlying types (`ArrayList`, etc.). Kotlin’s stdlib wraps them with **read-only vs mutable** types in APIs: returning `List<T>` signals “callers should not mutate,” while internal code may use `MutableList<T>`. That is a design hint in the type system, not a different runtime list.

---

## Lambdas, SAM, and higher-order functions

Since **Java 8**, a lambda can implement a **single-abstract-method (SAM)** interface:

```java
button.setOnClickListener(v -> loadLigand());
```

**Kotlin** treats lambdas as a first-class language feature. If the last argument is a function, you can put it **outside** the parentheses:

```kotlin
button.setOnClickListener { loadLigand() }
```

Kotlin also has **`inline` functions**: for small higher-order helpers, the compiler can copy the lambda body at the call site and avoid allocating a function object—useful in tight loops (with the usual “don’t inline everything” discipline).

---

## Extension functions

**Java** has no extension methods. You add `StringUtils`, `ViewHelper`, or static imports.

**Kotlin** lets you add functions **as if** they belonged to the type:

```kotlin
fun String.normalizedLigandId(): String = trim().uppercase()
// usage:
val id = rawInput.normalizedLigandId()
```

Under the hood this is still a static function with the receiver as the first parameter; it improves readability for DSLs (including Compose) and domain-specific helpers.

---

## Asynchronous code and Android

**Java** on Android historically used:

- **Threads** and thread pools (easy to leak, hard to cancel),
- **Callbacks** (nested “callback hell”),
- libraries like **RxJava** or **Guava** `ListenableFuture`,
- on the server, **CompletableFuture** and, in newer JDKs, **virtual threads (Project Loom)**.

**Kotlin coroutines** model async work as **`suspend` functions** that can pause without blocking a thread, plus structured scopes (`viewModelScope`, `lifecycleScope`) so cancellation propagates when a screen is destroyed:

```kotlin
viewModelScope.launch {
    val ligand = repository.fetchLigand(id) // suspend, main-safe with right dispatcher
    _uiState.update { it.copy(ligand = ligand) }
}
```

**Flow** (Kotlin) is the usual counterpart to reactive streams for state over time—similar in spirit to Rx `Observable`, but integrated with coroutines. This project uses coroutines and Flow in repositories and ViewModels; the same app could be written in Java with other async stacks, but official Android guidance today assumes Kotlin.

---

## Control flow and exceptions

**Kotlin `when`** is an enhanced `switch` that works as both statement and **expression** (returns a value), and supports type checks with **smart casts**:

```kotlin
when (result) {
    is Success -> show(result.data)
    is Error -> showError(result.message)
}
```

**Java** has improved `switch` and pattern matching in recent versions; older codebases still use long `if/else` chains.

**Checked exceptions** exist in **Java** (`throws IOException` must be handled or declared). **Kotlin does not have checked exceptions**; when calling Java APIs that declare checked exceptions, Kotlin still requires you to handle them, but Kotlin-only APIs typically use unchecked exceptions or `Result` types. That reduces ceremony but places more responsibility on API designers.

---

## Java and Kotlin in the same project

This is normal on Android:

- Legacy screens in **Java**, new features in **Kotlin**.
- All **Android SDK** and most libraries are Java-first; Kotlin calls them directly.
- Kotlin **standard library** and **reflection** add a small amount to APK size; in practice it is rarely a reason to avoid Kotlin on modern apps.

Calling **Kotlin from Java** requires knowing a few rules:

- Top-level functions appear as `MyFileKt.functionName()`.
- A Kotlin `object` is accessed via `MyObject.INSTANCE`.
- Use `@JvmStatic`, `@JvmName`, `@JvmOverloads` when you need Java-friendly APIs.

Calling **Java from Kotlin** is usually seamless; watch **nullability** at boundaries.

---

## Performance and tooling

For equivalent algorithms, **runtime performance is usually in the same ballpark**—both compile to JVM bytecode. Kotlin’s `inline` can reduce allocation in specific hot paths; careless use of coroutines or boxing can cost either language.

**Tooling:** IntelliJ IDEA and Android Studio support both languages. Refactoring, find usages, and navigation are strong for Kotlin in Android projects. Gradle needs the Kotlin plugin and aligned `jvmTarget` / Java compatibility settings (this repo uses Java 17 and Kotlin JVM target 17).

---

## What to choose for new work

**Prefer Kotlin** when you start new Android code, use Jetpack Compose, coroutines, Flow, or want less boilerplate and stricter null checks. That matches current Google documentation and this project’s codebase.

**Stay on or mix Java** when you maintain a large Java-only codebase, your team standardizes on Java, or you publish a JVM library meant for the widest audience without pulling in Kotlin as a dependency for consumers.

Neither language replaces understanding of **Android lifecycles**, **threading**, and **architecture**—they only change how clearly and safely you express those ideas in source code.

---

## Further reading

- [Kotlin docs: Comparison to Java](https://kotlinlang.org/docs/comparison-to-java.html)
- [Kotlin docs: Calling Java from Kotlin](https://kotlinlang.org/docs/java-interop.html)
- [Kotlin docs: Calling Kotlin from Java](https://kotlinlang.org/docs/java-to-kotlin-interop.html)
