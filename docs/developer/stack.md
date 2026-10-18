# The stack

What GetFit is built with, why each piece was chosen, and what was considered instead. Versions are pinned in [`gradle/libs.versions.toml`](../../gradle/libs.versions.toml); this page explains them. "Decision #N" refers to the decision register in [`docs/initial_design.md`](../initial_design.md). The "Main alternatives" column names the usual competitors and why they lose here; it is not a record of formal evaluations, except where a decision number says so.

## Guiding constraints

Five constraints shaped every choice below:

1. **Local-first, single user, but sync-ready.** No backend today, and nothing that would block one later.
2. **The data must outlive the app's bugs.** Explicit migrations, transactional writes, backups.
3. **Modern, first-party Android.** Prefer Google/JetBrains-maintained libraries that will be supported for years.
4. **Testable on the JVM.** Almost every test runs without a device.
5. **Portable domain logic.** Calculations stay in pure Kotlin, ready for Kotlin Multiplatform.

## Platform and language

| Choice | Version | Why | Main alternatives |
| --- | --- | --- | --- |
| **Kotlin** in explicit style | 2.4.21 | The Android standard. The explicit style (explicit types, block bodies; decision #1) keeps code readable without an IDE and reviewable line by line | Idiomatic terse Kotlin (rejected for reviewability) |
| **Android, minSdk 34** | min 34, target and compile 37 | Android 14 is the first version with Health Connect built in, which is central to the Body tab. One owner, one modern phone, so there is no reason to support older versions. compileSdk moved to 37 because current Compose and androidx.core require it (decision #2, amended) | minSdk 26–28 for reach (unnecessary for a personal app) |
| **JDK 17 bytecode**, Gradle on JBR 25 | | 17 is the Android toolchain baseline; Gradle runs on Android Studio's bundled runtime | |

## Build

| Choice | Version | Why | Main alternatives |
| --- | --- | --- | --- |
| **Gradle Kotlin DSL** | Gradle 9.8.1 | Type-checked build scripts, the same language as the app | Groovy DSL |
| **Android Gradle Plugin with built-in Kotlin** | AGP 9.4.1 | AGP 9 compiles Kotlin itself, so there is no separate `kotlin-android` plugin to keep in step | |
| **Convention plugins** in `build-logic/` | | One place for shared configuration; module build files shrink to one to five lines | `buildSrc` (slower invalidation), copy-paste (drift) |
| **Version catalog** | | Every version in one file; upgrades are single deliberate commits | Inline versions |
| **KSP** for code generation | 2.3.12 | Hilt and Room both support it, and it is much faster than kapt, which is in maintenance mode | kapt |

## UI

| Choice | Version | Why | Main alternatives |
| --- | --- | --- | --- |
| **Jetpack Compose + Material 3** | BOM 2026.09.00 | Google's current UI toolkit. Declarative state fits a data-dense app, and Material 3 gives accessible defaults that are then made flat and dense | Views/XML (legacy) |
| **Navigation 3** | 1.2.0 | The back stack is a plain list the app owns ([`TopLevelBackStack`](../../app/src/main/kotlin/com/jrussell/fitness/navigation/TopLevelBackStack.kt)), which makes per-tab history and the Settings overlay easy to build and unit-test. Its decorators can be applied per tab (`rememberDecoratedNavEntries`), so hidden tabs keep their state ([`TabNavDisplay`](../../app/src/main/kotlin/com/jrussell/fitness/ui/TabNavDisplay.kt)). Keys are `@Serializable` objects (decision #8) | Navigation Compose 2.x (graph DSL and route strings; harder to own the stack), Voyager or Decompose (third-party) |
| **Inter** | 4.1, bundled | Highly legible at small sizes, with tabular figures (`tnum`) so numbers align; open license (decision #31) | IBM Plex Sans (the other finalist), Roboto (the default) |
| **Material Symbols** as vector drawables | | Consistent outlined and filled pairs. Importing only the icons used keeps the APK small, unlike the full icon font or the large `material-icons-extended` artifact | `material-icons-extended` |
| **Vico** (`compose-m3`) for charts | 3.3.1 | Compose-native and actively maintained. It is used only inside [`MetricChart`](../../core/ui/src/main/kotlin/com/jrussell/fitness/core/ui/chart/MetricChart.kt) in `:core:ui`; features pass app-owned `ChartSeries` values, so Vico can be replaced without touching a screen (decision #32). The app sets a one-day x step and fixed axis ranges itself, because Vico infers the step from the gaps between points and dropped date labels for weekly weigh-ins | MPAndroidChart (View-based, unmaintained), a custom Canvas |

## Architecture and dependency injection

| Choice | Version | Why | Main alternatives |
| --- | --- | --- | --- |
| **Hilt** | 2.60.1 | Compile-time-checked DI with first-class ViewModel, WorkManager and Navigation integration. Missing bindings fail the build, not the user | Koin (runtime resolution: errors only at runtime), manual DI (boilerplate at this module count) |
| **hilt-work** | 1.4.0 | `@HiltWorker` and `HiltWorkerFactory`, so the backup worker gets its dependencies like any other class | A hand-written `WorkerFactory` |
| **Fine-grained modules** | ~24 | Compiled layer boundaries, incremental builds, portable pure modules (decision #4; see [Architecture](02-architecture.md)) | A single module, or modules split by layer only |
| **Lifecycle ViewModel + StateFlow** | 2.11.0 | The standard Android state holder; survives rotation; `collectAsStateWithLifecycle` stops work in the background | |

## Data

| Choice | Version | Why | Main alternatives |
| --- | --- | --- | --- |
| **Room** (SQLite) | 2.8.5 | First-party; compile-time-verified SQL; `Flow` queries; exported schema JSON plus migration testing, which is critical when the phone holds the only copy of the data | SQLDelight (strong, but Room's tooling and migration helper won); a key-value store (the data is relational) |
| **Preferences DataStore** | 1.2.1 | Asynchronous, transactional replacement for SharedPreferences, used for UI-only preferences | Proto DataStore (schema overhead for two enums), SharedPreferences (blocking I/O) |
| **kotlin.uuid.Uuid**, UUIDv7 and v5 | stdlib | Merge-safe IDs that need no server (decision #35). v7 is time-ordered and index-friendly; v5 is deterministic across devices. Standard-library, so no extra dependency | Auto-increment `Long` (collides across devices when syncing), a UUID library |
| **kotlin.time.Clock / Instant** | stdlib | Stable since Kotlin 2.3; injectable for tests | `java.time` (not Multiplatform), `kotlinx.datetime.Instant` (superseded by the stdlib type) |
| **kotlinx-datetime** | 0.8.0 | `LocalDate`, `TimeZone` and conversions in pure Kotlin modules (decision #5) | `java.time` (JVM-only, which blocks Multiplatform) |
| **kotlinx.serialization** | 1.11.0 | Navigation keys and the JSON export format (`ExportBundle`, with the built-in `Uuid` and `Instant` serializers); parameter blobs later (convention 8) | Moshi, Gson (reflection-based) |
| **Coroutines and Flow** | 1.11.0 | The Android standard for async work; Room and DataStore speak it natively | RxJava |
| **WorkManager** | 2.12.0 | The daily backup snapshot (spec: "Daily, while charging"): persisted, constraint-aware periodic work that survives restarts and reboots, scheduled by `WorkManagerBackupScheduler` | `JobScheduler` directly (no persistence or unique-work API), `AlarmManager` (exact alarms are restricted and drain battery) |
| **androidx.sqlite** | 2.6.2 | The SupportSQLite helper Room runs on; used directly to keep a copy of a corrupt `user.db` and to write `VACUUM INTO` snapshots | |
| **Storage Access Framework** | platform | The backup folder and export/restore documents are picked by the user once with the system picker, so GetFit needs no storage permission and works with any provider, including Google Drive | Direct file paths (need broad storage permissions), the Drive REST API (needs sign-in and a cloud project) |

## Testing

| Choice | Version | Why | Main alternatives |
| --- | --- | --- | --- |
| **JUnit 5** (pure modules) / **JUnit 4** (Android modules) | 5.14.4 / 4.13.2 | JUnit 5 is the modern runner; Robolectric and AndroidX test rules are built on JUnit 4 (decision #33) | JUnit 4 everywhere |
| **Robolectric** | 4.17 | Runs Room, Context and Compose semantics on the JVM in seconds, with no emulator needed for unit tests | Instrumented tests only (slow, need a device) |
| **AssertK** | 0.28.1 | Kotlin-first fluent assertions with good failure messages, and Multiplatform-friendly | Truth, Hamcrest, kotlin.test |
| **Turbine** | 1.2.1 | Readable testing of `Flow` emissions | Collecting into lists by hand |
| **Hand-written fakes** | | Tests assert real behavior; no mocking DSL to learn or maintain | MockK, Mockito |

## Quality tooling

| Choice | Version | Why | Main alternatives |
| --- | --- | --- | --- |
| **ktlint** via ktlint-gradle | 14.2.0 plugin | Formatting is automatic and not debated | Spotless |
| **detekt 2** | 2.0.0-alpha.6 | Static analysis, plus a custom rule set encoding GetFit's own rules. Version 2 because the 1.23.x Gradle plugin predates AGP 9 (it relies on the removed legacy `BaseExtension`); 2.x is the line that works with this toolchain. One limitation: plugin config isn't validated, which `GetFitConfigTest` covers | detekt 1.23 (incompatible with AGP 9), Android Lint alone (cannot express these rules) |
| **Konsist** | 0.18.1 | Architecture rules written as ordinary Kotlin tests over the source | ArchUnit (bytecode-based, Java-centric) |
| **Dependency Analysis Gradle Plugin** | 3.9.0 | Catches misdeclared dependencies (`api` vs `implementation`) that would leak types or slow builds | Manual review |
| **Android Lint** | with AGP | Android-specific correctness and accessibility | |

## What is deliberately *not* used

- **A backend or cloud database.** Local-first by design; a sync seam exists for later (decision #35).
- **Dynamic color (Material You).** Chart colors must be stable and meaningful.
- **kapt, Gson and RxJava.** Superseded by KSP, kotlinx.serialization and coroutines.
- **A mocking framework.** Fakes keep tests honest.
- **`fallbackToDestructiveMigration`.** Banned by a custom detekt rule; a lost database is not acceptable.

## Upgrading

Bump the version in `libs.versions.toml` in its own commit, then run `./gradlew check buildHealth assembleDebug assembleRelease`. The SDK level is the one coupled upgrade: `compileSdk` follows what the AndroidX libraries require, and `targetSdk` moves with it (decision #2).
