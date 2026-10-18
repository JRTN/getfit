# GetFit — Design Document v1

Oct 13, 2026 · @John

## Overview

GetFit is a local-first Android app that replaces Cronometer, Strong and a body-composition projection calculator with one data-oriented tool. Version 1 targets a single user on one Galaxy S25. The schema, module boundaries and interfaces are built so that multi-device sync, multiple users and an iOS port can be added without rewrites.

**Goals**

- Nutrition logging at Cronometer depth (micronutrients, recipes, supplements, hydration), with a flat, dense, professional UI.
- Strength-training logging at Strong parity (templates, sets, rest timer, previous performance, PRs).
- Biometrics and sleep imported from Health Connect (Fitbit Versa 4) or entered manually.
- A projection engine: BMR/TDEE, required intake for a target weight and date, feasibility verdicts, and weight / fat mass / lean mass / expenditure trajectories.
- Any metric charted against any other, with every chart also viewable as a table.
- Light and dark themes, color-vision-safe palettes and full TalkBack support from v1.

**Non-goals for v1**

- Cloud sync, accounts, a backend, or Play Store publication (interfaces exist; implementations are no-ops).
- iOS, Wear OS, social features, AI photo logging.
- Direct cloud integrations with Fitbit/Google Health API or Garmin (both currently closed to new developers; Health Connect is the only path).

**Guiding rules**

1. Data first: store raw observations; derive everything else at query time.
2. History is immutable: versioned foods, versioned targets and versioned settings never re-evaluate past days.
3. Every user-owned row is sync-ready from its first insert.
4. Pure-Kotlin domain modules never import Android.

## Decision register

All ground-floor decisions are closed. Later sections give the detail behind each.

| # | Area | Decision |
| --- | --- | --- |
| 1 | Language | Kotlin, written in an explicit style (explicit types, block bodies, named classes) |
| 2 | SDK levels | minSdk 34, targetSdk 37, compileSdk 37 (amended 2026-10-16 from 36: current Compose and androidx.core require compileSdk 37) |
| 3 | IDE | Android Studio |
| 4 | Modules | Fine-grained: `:app`, `:feature:*`, `:core:*`; convention plugins in `build-logic` |
| 5 | Date/time | kotlinx-datetime; `Instant` → `Long` epoch ms, `LocalDate` → `Int` epoch day in Room; primitives in `:core:projection` |
| 6 | Repository & CI | Private GitHub repo; GitHub Actions runs unit tests, lint and static analysis on every push |
| 7 | Identifiers | `applicationId` `com.jrussell.fitness`; debug `com.jrussell.fitness.debug`; display name "GetFit" (changeable) |
| 8 | Navigation | Jetpack Navigation 3 |
| 9 | Food edits | Immutable versions in a lineage; scoped, undoable rebind of past entries to the latest version |
| 10 | Reference foods | Read-only `reference.db` for search; copy-on-first-use into `user.db`; UUIDv5 deterministic IDs |
| 11 | Units | Canonical `Double` at full precision plus `enteredValue` / `enteredUnit` on user-entered rows |
| 12 | Day boundary | Configurable day-start hour (versioned setting); `localDate` fixed at creation, user-editable |
| 13 | Day completeness | `DayLog` with explicit flag; heuristic proposes the default |
| 14 | Health Connect storage | Incremental copy into Room using the changes token |
| 15 | Health Connect write-back | Read-only in v1 |
| 16 | Source priority | Manual entry wins; otherwise a per-metric preferred-source list stored as data |
| 17 | Sleep attribution | A session belongs to its wake date |
| 18 | Biometric types | Data table, user-definable, with reference ranges |
| 19 | Exercise identity | Separate definitions per variant, grouped by `movementPatternId` |
| 20 | Effort metric | RIR stored; displayed as RPE or RIR per preference |
| 21 | Export | `VACUUM INTO` SQLite copy (scheduled) plus versioned JSON (manual) |
| 22 | Food catalog | Bundle USDA Foundation, SR Legacy, FNDDS; network lookup for branded/barcode items, cached on use |
| 23 | Nutrient set | Store every nutrient a source provides; display a configurable subset |
| 24 | Energy accounting | Four modes plus exercise-credit fraction, versioned by effective date; all estimates always displayed |
| 25 | Hydration | Dedicated `HydrationEntry`; goal applies to direct water only; water also exists as a food |
| 26 | Exercise library | Open dataset seeded by `externalKey`; custom exercises; non-destructive overrides; JSON import |
| 27 | e1RM | Epley default, user-selectable formula; always computed at query time |
| 28 | Trend weight | Exponential moving average with gap-adjusted smoothing factor; internal only (amended 2026-10-18: the displayed weight trend is the least-squares rate per week, see the navigation and Home spec) |
| 29 | Projection defaults | All parameters configurable with conservative defaults; glycogen transient deferred to v2 |
| 30 | Navigation layout | Four bottom tabs: Home, Nutrition, Training, Body; Settings in the top bar (amended 2026-10-17 from five tabs with Today and Trends; see #38) |
| 31 | Typeface | Inter (chosen 2026-10-16), bundled, tabular figures for all numbers |
| 32 | Charts | Vico, wrapped behind an app-owned `ChartSpec` abstraction |
| 33 | Test stack | JUnit 5 (pure modules), JUnit 4 + Robolectric (Android modules), AssertK, Turbine, hand-written fakes |
| 34 | Multi-user readiness | `ownerId` on every user-owned row; `CurrentOwnerProvider` returns the single local profile |
| 35 | Sync readiness | UUIDv7 keys, tombstones, global revision counter, per-remote cursor, `NoOpSyncGateway` |
| 36 | Versioning and branching | Semver tags `vX.Y.Z` on `master`; trunk-based with short-lived branches; 0.x minor bump per newly useful capability; 1.0.0 = this design complete (added 2026-10-16) |
| 37 | Default accent and M1 split | Violet accent `#6941C6` (light) / `#C3B1FF` (dark), chosen from mockups; M1 delivered as four plans: M1a design system → M1b data foundation → M1c weight, metrics and charts → M1d backup; `v0.1.0` after M1d (added 2026-10-16) |
| 38 | Navigation and Home | Read-only Home (opens on Today; range presets Today, 7 days, 30 days, From…; averages for daily targets, tallies for period targets; eaten food only, plan shown as a projection) hosting the cross-domain chart builder (overlay scaled, overlay real units, compare two with a one-day shift) and pinned charts and measurements; Nutrition, Training and Body each have Log / Trends views (Body also Plan, for projections); charts on Today show the last 7 days. Full design: `docs/superpowers/specs/2026-10-17-navigation-home-design.md` (added 2026-10-17) |

## Platform, SDK levels and tooling

The app targets Android 17 (API 37) and installs on Android 14 (API 34) and later, the first version with Health Connect built in.

| Setting | Value | Reason |
| --- | --- | --- |
| `minSdk` | 34 | Health Connect is part of the OS; no separate APK, availability check or pre-14 rationale intent filter |
| `targetSdk` | 37 | Exceeds Google Play's API 36 requirement (in force since 2026-08-31, [source](https://developer.android.com/google/play/requirements/target-sdk)); raised from 36 on 2026-10-16 together with `compileSdk` |
| `compileSdk` | 37 | Matches target; required by Compose 1.12 and androidx.core 1.19 |
| Release `applicationId` | `com.jrussell.fitness` | Permanent; brand-neutral so the display name can change freely |
| Debug `applicationId` | `com.jrussell.fitness.debug` | Debug installs never touch the release build's database |
| Display name | GetFit / GetFit (Debug) | `app_name` string resource, changeable in any release |

**API 36+ behaviors designed for from the start** (Android 17 behavior changes to be reviewed as they affect features)

- Edge-to-edge is mandatory: `enableEdgeToEdge()` in the activity, insets handled by `Scaffold` / `WindowInsets`, status-bar icon contrast flips with the theme.
- `onBackPressed()` is no longer delivered: all back handling uses `BackHandler` / `PredictiveBackHandler`.

**Release signing**

- One release keystore, generated once, stored outside the repository with a backup in a password manager.
- Losing it means an update cannot be installed over the existing app; the only path is uninstall, which deletes the app's data.
- If CI ever builds releases, the keystore and passwords are injected from GitHub Actions secrets.

**Toolchain**

| Concern | Choice |
| --- | --- |
| IDE | Android Studio (IntelliJ keymap imported) |
| Language | Kotlin 2.x, K2 compiler |
| UI | Jetpack Compose, Material 3 (restyled), Navigation 3 |
| DI | Hilt |
| Persistence | Room (KMP-capable), DataStore |
| Background work | WorkManager; foreground service for the rest timer |
| Annotation processing | KSP (Room, Hilt); no kapt |
| Serialization | kotlinx.serialization (JSON export, parameter blobs, navigation keys) |
| Date/time | kotlinx-datetime and the standard-library `Instant` its current release designates |
| IDs | `kotlin.uuid.Uuid` wrapped in value classes |
| Charts | Vico behind `ChartSpec` |
| Logging | Logging interface in `:core:model` (or Kermit); local crash-log file |
| Static analysis | ktlint (or ktfmt), detekt, Android Lint, Konsist, Dependency Analysis Gradle Plugin |
| Minification | R8 enabled on release from the first release build |

## Module architecture

The project uses fine-grained Gradle modules in five layers, with the pure-Kotlin layer kept free of Android so it can become a Kotlin Multiplatform module without rewrites.

&#91;embedded content: Module graph · 5 layers, dependencies point downward\]

Dependencies run downward, except that `:core:data` points up to the domain interfaces it implements; feature modules never reach storage.

### Dependency rules

1. Pure-Kotlin modules (`:core:model`, `:core:domain`, `:core:projection`, `:core:importers`) apply `app.jvm.library` and depend on no Android module.
2. `:feature:*` modules depend only on `:core:domain`, `:core:model`, `:core:ui` and `:core:designsystem`. ViewModels receive repository interfaces through Hilt.
3. No feature module depends on another. Cross-feature navigation uses callbacks (`onOpenWorkout: (WorkoutId) -> Unit`) supplied by `:app`.
4. `:core:data` is the only module that depends on `:core:database`, `:core:reference`, `:core:datastore`, `:core:healthconnect` and `:core:backup`.
5. `:app` depends on everything and is the only module that binds implementations to interfaces.

Gradle enforces rules 1–4 structurally; Konsist tests and the Dependency Analysis Gradle Plugin catch finer violations.

### Module inventory

| Module | Kind | Responsibility |
| --- | --- | --- |
| `:app` | Android app | `MainActivity`, Navigation 3 `NavDisplay`, Hilt `Application`, bindings, cross-feature navigation |
| `:feature:home` | Android feature | Read-only Home: range control, summary cards (nutrition, training, energy, body), pinned charts and measurements, cross-domain chart builder |
| `:feature:nutrition` | Android feature | Diary, food search, food and recipe editors, targets, hydration |
| `:feature:training` | Android feature | Live workout, templates, exercise library, history, rest-timer service |
| `:feature:body` | Android feature | Log / Trends / Plan: measurements, sleep, measurement types, Health Connect inspector, body charts, projection plans |
| `:feature:settings` | Android feature | Profile, units, day start, energy mode, theme and palette, backup, source priority, crash log |
| `:core:model` | Pure Kotlin | Domain types, typed IDs, quantities, enums |
| `:core:domain` | Pure Kotlin | Repository interfaces, use cases, rules, metric contract |
| `:core:projection` | Pure Kotlin | Simulator, solver, feasibility, smoothing, calibration |
| `:core:importers` | Pure Kotlin | Strong and Cronometer CSV parsers, JSON export format |
| `:core:database` | Android library | `user.db`: entities, DAOs, converters, migrations, schema JSON |
| `:core:reference` | Android library | `reference.db` asset and read-only DAOs |
| `:core:datastore` | Android library | UI preferences |
| `:core:data` | Android library | Repository implementations, mapping, stamping, metric providers, owner provider |
| `:core:healthconnect` | Android library | Health Connect client, mapping, import worker |
| `:core:sync` | Android library | `SyncGateway`, `NoOpSyncGateway`, `SyncWorker` |
| `:core:backup` | Android library | Snapshot export, JSON export/import, scheduled export worker |
| `:core:designsystem` | Android library (Compose) | Theme, tokens, palettes, typography, base components |
| `:core:ui` | Android library (Compose) | Chart component, chart-builder UI, range chips, data table, multi-select scaffold, unit-formatted text |
| `:core:testing` | Test fixtures | Fakes, test dispatchers, fixture builders |

## Build infrastructure

Every module build file is about five lines because all configuration lives in convention plugins inside a `build-logic` included build.

**Convention plugins**

| Plugin ID | Applies | Used by |
| --- | --- | --- |
| `app.android.application` | AGP application, Kotlin, SDK levels, build types, R8, signing config, versionCode | `:app` |
| `app.android.library` | AGP library, Kotlin, SDK levels, namespace derived from module path | all Android `:core:*` |
| `app.android.library.compose` | library + Compose compiler plugin + Compose BOM | `:core:designsystem`, `:core:ui` |
| `app.android.feature` | library + compose + Hilt + Navigation 3 + standard `:core:*` dependencies | all `:feature:*` |
| `app.jvm.library` | Kotlin/JVM, JUnit 5; becomes `app.kmp.library` at iOS time | `:core:model`, `:core:domain`, `:core:projection`, `:core:importers` |
| `app.android.room` | KSP, Room, `room.schemaLocation`, Robolectric test deps | `:core:database`, `:core:reference` |
| `app.hilt` | KSP, Hilt plugin and runtime | `:app`, `:core:data`, feature modules |
| `app.quality` | ktlint/ktfmt, detekt (with custom rules), Android Lint config | every module |

**Gradle settings**

- Version catalog `gradle/libs.versions.toml` is the single source of versions.
- Type-safe project accessors: `implementation(projects.core.domain)`.
- Configuration cache and build cache enabled from the first commit.
- Namespace convention: `com.jrussell.fitness.<module path with dots>`, e.g. `com.jrussell.fitness.feature.nutrition`.
- `versionCode` derived from the git commit count in CI; `versionName` is semver set by hand.

**Versioning and branching** (decision 36)

- **Trunk:** `master` always builds and passes `check`. Work happens on short-lived branches cut from `master` (one per milestone task or fix, e.g. `m1-body-weight`), merged back when green, then deleted. No `develop` branch.
- **Releases are annotated tags** on `master`, written `vX.Y.Z` (e.g. `v1.2.11`). `versionName` shows `X.Y.Z` and is bumped by hand in the tagged commit; `versionCode` stays the commit count, which only increases on `master`.
- **Before 1.0:**
  - `0.0.x` is the skeleton (M0 = `v0.0.1`).
  - `0.1.0` is the first basic usable app, the M1 exit (enter weights, trend chart and table, backup and restore).
  - Each further capability that becomes useful bumps the minor version (0.2.0, 0.3.0, …), roughly one per milestone.
  - Fixes and small improvements bump the patch version.
- **1.0.0** is this design fully implemented (M6 exit: GetFit replaces Cronometer and Strong).
- **After 1.0:** the minor version is for new features, the patch version for fixes, and the major version for large shifts such as sync, multiple users or iOS.
- **Release branches only on demand:** to fix an older release while `master` has moved on, branch `release/X.Y` from its tag, tag the fix `vX.Y.Z+1` there, and merge the fix back to `master`.

**Custom static-analysis rules**

- detekt: no `Color(` literal outside `:core:designsystem`; no hardcoded `Dispatchers.*` outside the dispatcher module; no `fallbackToDestructiveMigration`.
- Konsist (runs as a unit test): no `android.*` import in pure modules; ViewModels depend only on interfaces from `:core:domain`; no feature module references another feature's package.

**Continuous integration (GitHub Actions)**

1. Trigger: every push and pull request.
2. Set up JDK 17 and Gradle with caching.
3. `./gradlew check` — unit tests (JVM and Robolectric), Konsist, detekt, ktlint, Android Lint.
4. `./gradlew buildHealth` — dependency analysis report; fails on misdeclared dependencies.
5. `./gradlew assembleDebug` — confirms the app builds.
6. Upload test reports and lint results as workflow artifacts.

Release builds stay local until there is a reason to automate them.

## Code conventions

These conventions run through every module and are fixed before the first entity is written.

**1. Typed identifiers.** Every entity ID is a value class over `kotlin.uuid.Uuid`, so IDs of different kinds cannot be swapped at compile time. New rows use UUIDv7 (time-ordered); reference foods use UUIDv5 derived from `"usda:<fdcId>"`.

```kotlin
@JvmInline
value class FoodId(val value: Uuid)

@JvmInline
value class DiaryEntryId(val value: Uuid)

interface IdGenerator {
    fun newTimeOrderedId(): Uuid          // UUIDv7
    fun nameBasedId(name: String): Uuid   // UUIDv5, fixed app namespace
}
```

**2. Typed quantities.** Physical quantities are value classes that store one canonical unit and convert explicitly. Room stores the raw `Double`; mappers in `:core:data` convert.

```kotlin
@JvmInline
value class Mass private constructor(val kilograms: Double) {
    companion object {
        private const val KILOGRAMS_PER_POUND: Double = 0.45359237
        fun ofKilograms(value: Double): Mass { return Mass(value) }
        fun ofPounds(value: Double): Mass { return Mass(value * KILOGRAMS_PER_POUND) }
    }
    fun toPounds(): Double { return kilograms / KILOGRAMS_PER_POUND }
}
```

Canonical units: kilograms (`Mass`), kilocalories (`Energy`), milliliters (`Volume`), seconds (`Duration`), meters (`Distance`). Nutrient amounts use `NutrientAmount(value: Double, unit: NutrientUnit)` because nutrient units vary (g, mg, µg, IU).

**3. Layer separation.** Room `@Entity` classes live only in `:core:database` and `:core:reference`. Domain models in `:core:model` are plain data classes with no annotations. `:core:data` maps between them, mirroring the JPA-entity-versus-domain-object split.

**4. Stable nutrient keys.** `NutrientDefinition` is identified by a stable string key (`"protein"`, `"vitamin_d"`, `"magnesium"`). Source-specific numbers (USDA nutrient number, Health Connect field name) are mapping columns, never identity.

**5. Read and write shape.** Repositories expose reads as `Flow<T>` and writes as `suspend` functions. Every write runs inside `database.withTransaction { }` together with its revision stamp.

**6. Injected dispatchers.** Coroutine dispatchers are injected through qualifiers (`@IoDispatcher`, `@DefaultDispatcher`); no code references `Dispatchers.IO` directly, so tests run deterministically.

**7. Error handling.** Expected failures are values of a sealed type in `:core:domain`; exceptions are reserved for programming errors.

```kotlin
sealed interface Outcome<out T> {
    data class Success<T>(val value: T) : Outcome<T>
    data class Failure(val error: DomainError) : Outcome<Nothing>
}

sealed interface DomainError {
    data class Validation(val field: String, val reason: String) : DomainError
    data class PermissionDenied(val permission: String) : DomainError
    data class ImportParse(val line: Int, val reason: String) : DomainError
    data class NotFound(val entity: String, val id: String) : DomainError
}
```

**8. Serialization.** kotlinx.serialization for the JSON export format, parameter blobs (progression rules, target formulas, bulk-operation parameters) and Navigation 3 keys.

**9. Strings and formatting.** Every user-visible string is a resource, including plurals. Numbers are formatted with locale-aware formatters. Display units are a presentation concern only.

**10. Logging and crashes.** Code logs through an interface in `:core:model`. An uncaught-exception handler writes the stack trace to an app-private file, viewable and exportable from Settings.

**11. Derived values are never stored.** e1RM, PRs, daily nutrient totals, trend weight and expenditure estimates are computed at query time from raw observations. Changing a formula setting recomputes all history instantly.

## Data architecture

The app uses two SQLite databases and one settings store. Only `user.db` and the settings store hold user data; `reference.db` is replaceable at any time.

| Store | Module | Contents | Backed up | Synced | Migrated |
| --- | --- | --- | --- | --- | --- |
| `user.db` | `:core:database` | All user-owned data, every food actually logged, seeded exercise library and nutrient definitions | Yes | Yes (future) | Yes, explicit migrations only |
| `reference.db` | `:core:reference` | Bundled USDA catalog for search | No | No | Never; replaced wholesale when the bundled dataset version changes |
| DataStore | `:core:datastore` | UI preferences: theme mode, palette, display units, chart defaults | Yes | Optional (future) | Proto/preferences schema |

Settings that change how past data is evaluated (day-start hour, energy accounting mode, targets) are **not** in DataStore. They are versioned rows in `user.db` with an `effectiveFrom` date, so they sync, back up and never re-evaluate history.

### Ownership categories

Every table belongs to exactly one category, and the category determines its columns.

| Category | `ownerId` | Sync columns | Examples |
| --- | --- | --- | --- |
| Reference | none | none | `reference.db` tables; `NutrientDefinition`; `MeasurementType` built-ins |
| Mixed catalog | nullable (null = built-in) | on owned rows | `ExerciseDefinition`, `MeasurementType` |
| User-owned | non-null | yes | diary, workouts, measurements, sleep, plans, targets, settings, profile, foods |

### Standard columns on user-owned tables

| Column | Type (Kotlin → SQLite) | Purpose |
| --- | --- | --- |
| `id` | `Uuid` → TEXT | Primary key, UUIDv7 (UUIDv5 for copied reference foods) |
| `ownerId` | `Uuid` → TEXT | Owning profile; leads every composite index |
| `createdAt` | `Instant` → INTEGER (epoch ms) | Creation time |
| `updatedAt` | `Instant` → INTEGER | Last modification; last-writer-wins key for future sync |
| `deletedAt` | `Instant?` → INTEGER NULL | Tombstone; DAOs filter `deletedAt IS NULL` |
| `localRevision` | `Long` → INTEGER | Value of the global sequence at the last write |
| `originDeviceId` | `Uuid` → TEXT | Device that made the last write; sync tie-breaker. The device ID lives in a file in `noBackupFilesDir`, never in `user.db` or DataStore, so a restored backup gets a fresh ID |

### Revision stamping

A single-row `SyncSequence(id = 1, value)` table is incremented inside every write transaction. Repositories never write an entity without stamping it.

```kotlin
suspend fun <T> stampedWrite(block: suspend (revision: Long, now: Instant) -> T): T {
    return database.withTransaction {
        val revision: Long = syncSequenceDao.incrementAndGet()
        val now: Instant = clock.now()
        block(revision, now)
    }
}
```

### Owner scoping

- DAOs take `ownerId` as a parameter on every query; repositories obtain it from the injected `CurrentOwnerProvider` and never accept it from callers.
- v1 binds `SingleLocalOwnerProvider`, which returns the one `UserProfile` row created at first launch.
- An integration test inserts rows for two owners and asserts that no repository method returns the other owner's rows.

### Time representation

- `Instant` → epoch milliseconds (`INTEGER`).
- `LocalDate` → epoch day (`INTEGER`), so range queries and indexes are cheap.
- `TimeZone` → IANA ID (`TEXT`), stored with each timestamped user entry.
- `localDate` is assigned at creation from the entry time, the stored zone and the day-start hour in force, and is user-editable afterwards.

## user.db schema

The tables below list only columns beyond the standard set (`id`, `ownerId`, `createdAt`, `updatedAt`, `deletedAt`, `localRevision`, `originDeviceId`). Category: **U** = user-owned, **M** = mixed catalog, **R** = seeded reference, **I** = local infrastructure (never synced).

### Profile and versioned settings

| Table | Cat. | Columns | Keys and indexes |
| --- | --- | --- | --- |
| `UserProfile` | U | `displayName`, `sex` (MALE, FEMALE), `birthDate` (epoch day), `heightM` | one row in v1, created silently at first launch; its `id` is the owner ID and `ownerId` equals `id`; personal fields are null until entered |
| `DayBoundarySetting` | U | `dayStartMinutes` (0–1439), `effectiveFrom` (epoch day) | `(ownerId, effectiveFrom)` unique |
| `EnergyAccountingSetting` | U | `mode` (ADAPTIVE, STATIC\_FORMULA, FORMULA\_PLUS\_EXERCISE, WEARABLE\_TOTAL), `exerciseCreditFraction`, `bmrFormula` (MIFFLIN\_ST\_JEOR, KATCH\_MCARDLE), `activityFactor`, `effectiveFrom` | `(ownerId, effectiveFrom)` unique |
| `MetricTarget` | U | `metricId`, `kind` (MIN, MAX, RANGE), `minValue`, `maxValue`, `formulaType` (FIXED, PER\_KG\_BODY\_WEIGHT, PER\_KG\_LEAN\_MASS, PERCENT\_OF\_ENERGY), `formulaParamsJson`, `effectiveFrom` | `(ownerId, metricId, effectiveFrom)` unique |
| `SourcePriority` | U | `measurementTypeId`, `rank`, `sourceKind` (MANUAL, HEALTH\_CONNECT, IMPORT), `sourcePackage` | `(ownerId, measurementTypeId, rank)` unique |

`MetricTarget` covers nutrient targets, the direct-water goal (`hydration.direct`), sleep and step goals. Evaluating a past day uses the row with the greatest `effectiveFrom` on or before that day. `MetricTarget` gains `period` (DAY, WEEK; default DAY) when weekly targets arrive (M3/M4): a WEEK target is summarized as a tally over a range.

**Home and charts (M2).** `SavedChart` (U): `name`, `scope` (HOME, NUTRITION, TRAINING, BODY), `specJson` (serialized `ChartSpec`), `pinnedToHome`, `homeOrder`; keys `(ownerId, scope)`, `(ownerId, pinnedToHome, homeOrder)`. `HomePinnedMeasurement` (U): `measurementTypeId`, `displayOrder`; keys `(ownerId, measurementTypeId)` unique, `(ownerId, displayOrder)`.

### Nutrition

| Table | Cat. | Columns | Keys and indexes |
| --- | --- | --- | --- |
| `NutrientDefinition` | R | `key` (PK, e.g. `protein`), `displayName`, `unit` (G, MG, UG, IU, KCAL), `category` (MACRO, VITAMIN, MINERAL, LIPID, AMINO\_ACID, OTHER), `usdaNumber`, `healthConnectField`, `displayOrder`, `defaultVisible` | PK `key` |
| `NutrientReferenceIntake` | R | `nutrientKey`, `sex`, `ageMinYears`, `ageMaxYears`, `rda`, `upperLimit` | `(nutrientKey, sex, ageMinYears)` |
| `Food` | U | `lineageId`, `version`, `supersededAt`, `name`, `brand`, `origin` (USER, USDA\_FOUNDATION, USDA\_SR\_LEGACY, USDA\_FNDDS, USDA\_BRANDED, OPEN\_FOOD\_FACTS, IMPORT\_CRONOMETER), `originId`, `datasetVersion`, `barcode`, `isRecipe`, `isSupplement`, `countsTowardHydration`, `recipeYieldGrams`, `note` | `(ownerId, lineageId, version)` unique; `(ownerId, origin, originId)`; `(ownerId, barcode)` |
| `FoodNutrient` | U (child) | `foodId`, `nutrientKey`, `amountPer100g` | PK `(foodId, nutrientKey)`; **absent row = unknown, never zero** |
| `ServingSize` | U (child) | `foodId`, `label`, `grams`, `isDefault`, `displayOrder` | `(foodId, displayOrder)` |
| `RecipeIngredient` | U (child) | `recipeFoodId`, `displayOrder`, `ingredientFoodId`, `grams` | `(recipeFoodId, displayOrder)` |
| `MealSlot` | U | `name`, `displayOrder`, `defaultMinutesAfterDayStart` | `(ownerId, displayOrder)` |
| `DiaryEntry` | U | `localDate`, `loggedAt`, `timeZone`, `mealSlotId`, `status` (PLANNED, CONSUMED), `foodId` (null for quick-add), `amountGrams`, `enteredValue`, `enteredUnit`, `quickEnergyKcal`, `quickProteinG`, `quickCarbohydrateG`, `quickFatG`, `note` | `(ownerId, localDate)`; `(ownerId, foodId)` |
| `HydrationEntry` | U | `localDate`, `loggedAt`, `timeZone`, `volumeMl`, `enteredValue`, `enteredUnit`, `beverageType` | `(ownerId, localDate)` |
| `BulkOperationLog` | U | `operationType` (REBIND\_FOOD\_VERSION, …), `paramsJson`, `affectedIdsJson`, `previousValuesJson`, `performedAt`, `undoneAt` | `(ownerId, performedAt)` |

**Food versions.** `Food`, `FoodNutrient`, `ServingSize` and `RecipeIngredient` form one immutable aggregate per version. Editing inserts a new version with the same `lineageId` and `version + 1` and stamps `supersededAt` on the previous one. A recipe's `FoodNutrient` rows are computed from its ingredients when the version is created, so later edits to an ingredient never change a past recipe.

**Rebind.** `RebindDiaryEntriesToLatestVersion(lineageId, scope)` with scope ALL, ON\_OR\_AFTER(date) or SELECTED(ids) updates `DiaryEntry.foodId` in one stamped transaction and writes a `BulkOperationLog` row holding the previous `foodId` values, which makes it undoable.

### Training

| Table | Cat. | Columns | Keys and indexes |
| --- | --- | --- | --- |
| `MovementPattern` | M | `externalKey`, `name` (e.g. horizontal press, hinge, squat) | `externalKey` unique |
| `ExerciseDefinition` | M | `externalKey`, `name`, `movementPatternId`, `equipment` (BARBELL, DUMBBELL, MACHINE, CABLE, BODYWEIGHT, KETTLEBELL, BAND, OTHER), `trackingType` (WEIGHT\_REPS, BODYWEIGHT\_REPS, ASSISTED, DURATION, DISTANCE\_DURATION), `isUnilateral`, `instructions` | `externalKey` unique where not null; `(ownerId, name)` |
| `ExerciseMuscle` | M (child) | `exerciseId`, `muscle` (enum of \~20 groups), `role` (PRIMARY, SECONDARY) | PK `(exerciseId, muscle)` |
| `ExerciseOverride` | U | `exerciseId`, `nameOverride`, `equipmentOverride`, `musclesOverrideJson`, `instructionsOverride`, `hidden` | `(ownerId, exerciseId)` unique |
| `RoutineTemplate` | U | `name`, `folder`, `displayOrder`, `programId` (null in v1), `note` | `(ownerId, folder, displayOrder)` |
| `TemplateExercise` | U | `templateId`, `displayOrder`, `exerciseId`, `supersetGroup`, `restSeconds`, `progressionRuleJson` (null in v1), `note` | `(templateId, displayOrder)` |
| `TemplateSet` | U | `templateExerciseId`, `displayOrder`, `setType` (WARMUP, WORKING, DROP, FAILURE), `targetRepsMin`, `targetRepsMax`, `targetWeightKg`, `targetRir`, `targetDurationSeconds` | `(templateExerciseId, displayOrder)` |
| `WorkoutSession` | U | `localDate`, `startedAt`, `endedAt`, `timeZone`, `templateId`, `status` (SCHEDULED, IN\_PROGRESS, COMPLETED, SKIPPED), `name`, `note` | `(ownerId, localDate)`; `(ownerId, status)` |
| `WorkoutExercise` | U | `sessionId`, `displayOrder`, `exerciseId`, `supersetGroup`, `note` | `(sessionId, displayOrder)`; `(exerciseId)` |
| `WorkoutSet` | U | `workoutExerciseId`, `displayOrder`, `setType`, `weightKg`, `enteredWeightValue`, `enteredWeightUnit`, `reps`, `rir`, `durationSeconds`, `distanceM`, `completed`, `completedAt`, `note` | `(workoutExerciseId, displayOrder)` |

Every set is written to Room the moment it is entered, so process death mid-workout loses nothing. The rest timer is in-memory state owned by a foreground service.

### Biometrics, sleep and days

| Table | Cat. | Columns | Keys and indexes |
| --- | --- | --- | --- |
| `MeasurementType` | M | `key`, `displayName`, `canonicalUnit`, `category` (BODY, VITAL, ACTIVITY, LAB, CUSTOM), `dailyAggregation` (SUM, MEAN, LAST, MIN, MAX), `hasSecondaryValue`, `referenceRangeLow`, `referenceRangeHigh`, `decimals`, `healthConnectRecordType` | `key` unique where `ownerId` is null (enforced by built-ins using UUIDv5 IDs from `measurement-type:<key>`; built-ins have `localRevision` 0 and null `originDeviceId`); `(ownerId, key)` unique |
| `Measurement` | U | `measurementTypeId`, `value`, `secondaryValue` (e.g. diastolic), `startTime`, `endTime`, `timeZone`, `localDate`, `source` (MANUAL, HEALTH\_CONNECT, IMPORT), `sourcePackage`, `externalId`, `enteredValue`, `enteredUnit`, `note` | `(ownerId, measurementTypeId, startTime)`; `(ownerId, measurementTypeId, localDate)`; `(ownerId, source, externalId)` unique |
| `SleepSession` | U | `startTime`, `endTime`, `timeZone`, `wakeDate`, `source`, `sourcePackage`, `externalId`, `note` | `(ownerId, wakeDate)`; `(ownerId, source, externalId)` unique |
| `SleepStage` | U (child) | `sessionId`, `stage` (AWAKE, LIGHT, DEEP, REM, SLEEPING, OUT\_OF\_BED, UNKNOWN), `startTime`, `endTime` | `(sessionId, startTime)` |
| `DayLog` | U | `localDate`, `completeness` (UNKNOWN, COMPLETE, PARTIAL, UNTRACKED), `note` | `(ownerId, localDate)` unique |
| `ProjectionPlan` | U | `name`, `status` (ACTIVE, ARCHIVED), `modelVersion`, `startDate`, `startWeightKg`, `startBodyFatFraction`, `targetWeightKg`, `durationDays`, `activityFactor`, `adaptiveThermogenesisCoefficient`, `intakeFloorKcal`, `minBodyFatFraction`, `maxLeanMassLossKg` | `(ownerId, status)` |

Plans store inputs only. Projections are deterministic and recomputed on demand; `modelVersion` keeps old plans reproducible.

### Infrastructure (never synced)

| Table | Columns | Purpose |
| --- | --- | --- |
| `SyncSequence` | `id` (always 1), `value` | Global monotonic revision counter |
| `SyncCursor` | `remoteId`, `lastPushedRevision`, `lastPulledCursor` | One row per future remote; empty in v1 |
| `HealthConnectSyncState` | `recordType`, `changesToken`, `lastSyncedAt` | Incremental Health Connect import position |
| `DatasetVersion` | `datasetKey`, `version`, `seededAt` | Which exercise and nutrient seeds have been applied |

### Deferred tables (v2)

`Program`, `ProgramBlock`, `ProgressPhoto`, FTS4 virtual tables over food names and notes. Existing tables already carry the nullable columns these will attach to (`RoutineTemplate.programId`, `TemplateExercise.progressionRuleJson`).

## reference.db and copy-on-first-use

`reference.db` is a read-only search catalog built offline from USDA FoodData Central and shipped in the APK assets. Diary entries never point into it.

### Contents

| Table | Columns | Notes |
| --- | --- | --- |
| `RefFood` | `fdcId` (PK), `dataType` (FOUNDATION, SR\_LEGACY, FNDDS), `description`, `foodCategory`, `publicationDate` | Foundation, SR Legacy and FNDDS only; Branded is looked up over the network |
| `RefFoodNutrient` | `fdcId`, `nutrientKey`, `amountPer100g` | Mapped to the app's stable nutrient keys at build time; absent = unknown |
| `RefServingSize` | `fdcId`, `label`, `grams` | From USDA food portions |
| `RefFoodFts` | FTS4 over `description`, `foodCategory` | Prefix and token search |
| `RefMetadata` | `datasetVersion`, `builtAt`, `sourceReleases` | Compared with `DatasetVersion` at startup |

The database is opened with `createFromAsset("reference.db")`. A new bundled `datasetVersion` replaces the file wholesale; there are no migrations.

### Build pipeline

A small JVM tool in the repository (`tools/reference-builder`, run manually, not part of the app build) downloads the USDA CSV releases, maps nutrient numbers to stable keys, drops nutrients the app does not track, writes `reference.db`, runs `VACUUM`, and records the dataset version. The output is committed to `:core:reference` assets (Git LFS if it exceeds GitHub's per-file limit).

### Copy-on-first-use

1. The user searches; results merge `RefFoodFts` hits with `user.db` foods (custom foods and previously logged foods rank first).
2. The user logs a reference food. `:core:data` computes `id = UUIDv5("usda:<fdcId>")` and checks `user.db`.
3. Not present: copy `RefFood`, `RefFoodNutrient` and `RefServingSize` into `Food` (version 1, `origin` from `dataType`, `originId = fdcId`, `datasetVersion`), `FoodNutrient` and `ServingSize` inside one stamped transaction.
4. The `DiaryEntry` references the `user.db` copy.
5. A later dataset release with different values does not change history. The food detail screen offers "Update to latest USDA values", which creates version 2 in the lineage and optionally runs the rebind operation.

Branded and barcode lookups (USDA FDC API, Open Food Facts) follow the same path: the network result is copied into `user.db` on first log and cached thereafter.

### Why this design

- All nutrient-total joins happen inside `user.db` in SQL.
- History is immutable across dataset updates.
- Backups contain every food actually eaten (hundreds of rows) and exclude the multi-megabyte catalog.
- Future sync carries only foods the user has used.

## Health Connect integration

Health Connect is the only wearable data path: the Fitbit app (now Google Health) writes Versa 4 data into it, and GetFit copies that data into `user.db`, read-only.

**Why not the cloud APIs.** The legacy Fitbit Web API is turned off on 2026-10-30 and Google is not onboarding new projects to its replacement ([source](https://developers.google.com/health/reference/rest/v4beta/Subscription)). Garmin's Connect Developer Program stopped accepting new applicants in spring 2026 ([source](https://the5krunner.com/2026/09/14/garmin-developer-api-access-paused/)).

### Manifest requirements

- `<uses-permission>` for each record type read (below), plus `READ_HEALTH_DATA_HISTORY` for data older than the default window and `READ_HEALTH_DATA_IN_BACKGROUND` for WorkManager imports.
- An `activity-alias` with action `android.intent.action.VIEW_PERMISSION_USAGE` and category `android.intent.category.HEALTH_PERMISSIONS`, pointing at a static privacy-rationale screen. Without it, Health Connect will not show the permission dialog.

### Record types read in v1

| Health Connect record | Maps to | Notes |
| --- | --- | --- |
| `WeightRecord` | `Measurement` (`body.weight`) |  |
| `BodyFatRecord` | `Measurement` (`body.fat_fraction`) |  |
| `HeartRateRecord` | `Measurement` (`vital.heart_rate`) | Samples aggregated to daily min/mean/max on import to bound row count |
| `RestingHeartRateRecord` | `Measurement` (`vital.resting_heart_rate`) |  |
| `HeartRateVariabilityRmssdRecord` | `Measurement` (`vital.hrv_rmssd`) | Availability from the Versa 4 to be confirmed with the inspector |
| `OxygenSaturationRecord` | `Measurement` (`vital.spo2`) |  |
| `RespiratoryRateRecord` | `Measurement` (`vital.respiratory_rate`) |  |
| `SleepSessionRecord` | `SleepSession` + `SleepStage` | Attributed to wake date |
| `StepsRecord` | `Measurement` (`activity.steps`) | Daily totals via the aggregate API to avoid overlap double-counting |
| `ActiveCaloriesBurnedRecord`, `TotalCaloriesBurnedRecord` | `Measurement` (`activity.active_energy`, `activity.total_energy`) | Displayed; drive the budget only in FORMULA\_PLUS\_EXERCISE or WEARABLE\_TOTAL mode |
| `ExerciseSessionRecord` | `Measurement` (`activity.exercise_minutes`) | Cardio context only; strength sessions are logged natively |

### Import flow

1. First run after permission: read each record type over the history window in pages; store `externalId = metadata.id`, `sourcePackage = metadata.dataOrigin.packageName`.
2. Obtain a changes token per record type and save it in `HealthConnectSyncState`.
3. A periodic `HealthConnectImportWorker` (WorkManager, every few hours, plus on app foreground) calls `getChanges(token)`: upserts by `(source, externalId)`, applies deletions as tombstones, saves the new token.
4. An expired token (Health Connect discards old tokens) triggers a bounded re-read of recent history, deduplicated by `externalId`.
5. If write-back is added later, every read filters out records whose origin is `com.jrussell.fitness`.

### Deduplication and source priority

- Identical records are prevented by the unique `(ownerId, source, externalId)` index.
- Overlapping records from different origins (e.g. weight from the Fitbit app and a manual entry) are all stored. The metric layer resolves them at query time: manual entries win, then the highest-ranked `SourcePriority` row for that measurement type, then the most recent `updatedAt`.
- Summable activity metrics (steps, energy) use Health Connect's aggregate API, which already deduplicates across origins.

### Inspector screen (first Health Connect deliverable)

A screen in `:feature:body` listing, for every readable record type: permission status, record count over the last 30 days, earliest and latest timestamps, and the distinct origin package names. It answers exactly what the Versa 4 provides and remains a permanent debugging tool.

## Metric registry

Every chartable number in the app is a metric published through one contract, so any chart can plot, overlay, transform or tabulate any metric without per-chart code.

### Contract (`:core:domain`)

```kotlin
@JvmInline
value class MetricId(val value: String)          // "nutrition.nutrient.protein", "body.weight"

enum class Aggregation { SUM, MEAN, LAST, MIN, MAX }
enum class Bucket { DAY, WEEK, MONTH }

data class MetricDescriptor(
    val id: MetricId,
    val displayNameResource: Int,
    val canonicalUnit: UnitOfMeasure,
    val dailyAggregation: Aggregation,
    val category: MetricCategory,
    val higherIsBetter: Boolean?
)

data class DataPoint(
    val date: LocalDate,
    val value: Double,
    val coverage: Double?      // fraction of the value backed by known data; null when not applicable
)

interface MetricSeriesProvider {
    fun descriptors(): List<MetricDescriptor>
    fun series(metricId: MetricId, range: LocalDateRange, bucket: Bucket): Flow<List<DataPoint>>
}

interface MetricRegistry {
    fun allDescriptors(): List<MetricDescriptor>
    fun series(metricId: MetricId, range: LocalDateRange, bucket: Bucket): Flow<List<DataPoint>>
}
```

Providers are contributed with Hilt multibinding (`@IntoSet`) from `:core:data`; the registry routes each `MetricId` to its provider.

### Metric inventory (v1)

| Provider | Metric IDs | Aggregation |
| --- | --- | --- |
| Nutrition | `nutrition.energy`, `nutrition.nutrient.<key>` for every `NutrientDefinition`, `nutrition.macro_ratio.<protein\|carbohydrate\|fat>`, `nutrition.water_from_food` | SUM per day (coverage-aware) |
| Hydration | `hydration.direct`, `hydration.total` | SUM |
| Training | `training.volume_load`, `training.volume_load.muscle.<muscle>`, `training.hard_sets.muscle.<muscle>`, `training.e1rm.exercise.<id>`, `training.session_duration`, `training.sessions` | SUM, except e1RM (MAX) |
| Body | `body.<measurement type key>` for every `MeasurementType`, `body.trend_weight`, `body.lean_mass`, `body.fat_mass` | per type |
| Sleep | `sleep.duration`, `sleep.stage.<stage>`, `sleep.efficiency` | per wake date |
| Energy | `energy.expenditure.formula`, `energy.expenditure.adaptive`, `energy.expenditure.wearable`, `energy.balance` | MEAN |
| Adherence | `adherence.target.<metricId>` (fraction of days within target), `adherence.days_complete` | MEAN |
| Projection | `projection.<planId>.weight`, `.fat_mass`, `.lean_mass`, `.expenditure`, `.intake` | LAST |

### Rules

- Only days marked COMPLETE contribute to nutrition aggregates used by adaptive expenditure; the explorer can show all days with incomplete ones flagged.
- Unknown nutrient values lower `coverage` and are never treated as zero.
- `body.lean_mass` and `body.fat_mass` derive from weight × body-fat fraction on days where both exist (source priority applied first).
- e1RM uses the current formula setting and only sets with reps ≤ 10.

### Transforms (pure functions over `List<DataPoint>`)

- Exponential moving average; rolling mean (n days); week-over-week delta; cumulative sum.
- Normalization to 0–1 or to percent of target.
- Scatter pairing of two metrics by date, with optional lag (e.g. sleep on day *d* versus training volume on day *d + 1*).

### Charts

The UI never calls a charting library directly. A `ChartSpec` (series list, transforms, axes, overlays, reference lines from targets) is rendered by a single component in `:core:ui` that wraps Vico and always offers a semantic summary and a "View as table" toggle.

The chart builder (decision #38) edits a `ChartSpec` in one of three views: **overlay, scaled** (each series mapped to its own range so shapes line up, real values in a readout), **overlay, real units** (at most two units on two axes) and **compare two** (one dot per day with an optional one-day shift, or the pair over time, with a plain-language correlation strength that never claims cause). Cross-domain charts are built and pinned from Home; single-domain charts live in their tab's Trends view. Saved charts never store a date range: they follow the screen showing them, and Today draws the last 7 days.

## Energy accounting

The app computes every expenditure estimate it can and always shows them side by side; the active mode only decides which one drives the daily energy budget.

### Modes

| Mode | Budget expenditure | Wearable calories |
| --- | --- | --- |
| ADAPTIVE | Adaptive estimate from intake and trend weight; falls back to STATIC\_FORMULA until calibrated | Displayed only |
| STATIC\_FORMULA | BMR × activity factor | Displayed only |
| FORMULA\_PLUS\_EXERCISE | BMR × sedentary factor (1.2) + exercise calories × `exerciseCreditFraction` | Active calories credited at the chosen fraction |
| WEARABLE\_TOTAL | Wearable total energy expenditure for the day | Drives the budget |

The user's initial configuration is STATIC\_FORMULA, switching to ADAPTIVE once calibration is available. Mode changes are rows in `EnergyAccountingSetting` with an `effectiveFrom` date, so past days keep the mode that was in force.

### Basal metabolic rate

Mifflin–St Jeor (weight W in kg, height H in cm, age A in years; s = +5 for male, −161 for female):

```latex
\mathrm{BMR} = 10W + 6.25H - 5A + s
```

Katch–McArdle, used when body-fat data exists (lean mass L in kg):

```latex
\mathrm{BMR} = 370 + 21.6L
```

### Adaptive expenditure estimate

Over a trailing window of n days (default 21), with mean logged intake Ī over COMPLETE days and trend-weight change Δw from the first to the last day of the window:

```latex
\hat{E} = \bar{I} - \frac{\Delta E_{\mathrm{stored}}}{n}, \qquad \Delta E_{\mathrm{stored}} = \frac{\Delta w}{\frac{1-p}{\rho_F} + \frac{p}{\rho_L}}
```

where ρF ≈ 9,440 kcal/kg, ρL ≈ 1,816 kcal/kg, and p is the Forbes lean-partition fraction at the window's mean fat mass (defined in the projection section). Without body-fat data, p uses the population default for the user's sex and BMI.

**Calibration gate.** The estimate is published only when the window has at least 14 COMPLETE days and at least 8 weigh-ins. Otherwise the metric reports "calibrating" and ADAPTIVE mode uses STATIC\_FORMULA.

**Trend weight** is an exponential moving average whose smoothing factor adjusts for gaps between weigh-ins (α = 0.1 per day by default, Δd = days since the previous weigh-in):

```latex
T_t = T_{t-\Delta d} + \alpha_{\mathrm{eff}}\,(w_t - T_{t-\Delta d}), \qquad \alpha_{\mathrm{eff}} = 1 - (1-\alpha)^{\Delta d}
```

### Daily budget

Budget = expenditure from the active mode + goal adjustment. The goal adjustment comes from the active `ProjectionPlan` (required intake minus current expenditure) or from a fixed energy target in `MetricTarget`. Home's Energy card shows: budget, consumed, remaining, and a compact comparison of the formula, adaptive and wearable estimates.

## Projection engine

The engine answers "what daily intake reaches weight X in N weeks, is it feasible, and what happens to fat mass, lean mass and expenditure along the way." It is a pure-Kotlin module operating on day indices and `Double` values, with no date or Android types.

### Inputs and outputs

| Inputs | Outputs |
| --- | --- |
| Sex, age, height | Baseline BMR and TDEE |
| Start weight, start body-fat fraction (or lean mass) | Required constant daily intake |
| Activity factor (or a calibrated value) | Daily series: weight, fat mass, lean mass, body-fat %, BMR, expenditure, energy balance, cumulative lean loss |
| Target weight, duration in days | Feasibility verdict, with the first violating day and the rule violated |
| Constraints: intake floor, minimum body-fat fraction, maximum lean loss, adaptive-thermogenesis coefficient | `modelVersion` |

### Model v1: two compartments with Forbes partitioning

State on day t: fat mass F and lean (fat-free) mass L, in kg. Constants: ρF = 9,440 kcal/kg, ρL = 1,816 kcal/kg.

Expenditure uses Katch–McArdle scaled by the activity factor, reduced by an adaptive-thermogenesis term proportional to the deficit (β ≈ 0.1 by default; E₀ = baseline expenditure; I = intake):

```latex
E(t) = \mathrm{PAL}\,(370 + 21.6\,L(t)) - \beta\,(E_0 - I)
```

The Forbes partition fraction sends a share p of the energy imbalance to lean tissue. With C = 10.4 kg × ρL/ρF ≈ 2.0 kg, this reproduces Forbes' dL/dF = 10.4/F:

```latex
p(t) = \frac{C}{C + F(t)}
```

Daily update (forward Euler, one-day step), with ΔE = I − E(t):

```latex
F(t+1) = F(t) + \frac{(1-p)\,\Delta E}{\rho_F}, \qquad L(t+1) = L(t) + \frac{p\,\Delta E}{\rho_L}
```

Metabolic adaptation emerges from two terms: BMR falls as L falls, and the β term lowers expenditure in proportion to the deficit.

### Worked example (day 1)

Male, 90 kg, 20% body fat (F = 18 kg, L = 72 kg), PAL 1.5, β 0.1, intake 2,300 kcal/day.

| Quantity | Calculation | Value |
| --- | --- | --- |
| BMR | 370 + 21.6 × 72 | 1,925.2 kcal |
| Baseline expenditure E₀ | 1.5 × 1,925.2 | 2,887.8 kcal |
| Day-1 expenditure | 2,887.8 − 0.1 × (2,887.8 − 2,300) | 2,829.0 kcal |
| Energy balance ΔE | 2,300 − 2,829.0 | −529.0 kcal |
| Partition p | 2.0 / (2.0 + 18) | 0.10 |
| Fat-mass change | 0.9 × −529.0 / 9,440 | −0.0504 kg |
| Lean-mass change | 0.1 × −529.0 / 1,816 | −0.0291 kg |
| Weight change | sum | −0.0796 kg (≈ −0.56 kg/week) |

The first golden-value test reproduces this table to four decimal places; multi-week fixtures are computed in a spreadsheet and committed with the tests.

### Solving for the required intake

Final weight W(T) = F(T) + L(T) increases monotonically with constant intake I, so the required intake is found by bisection over \[0, 2 × E₀\] until |W(T) − target| < 0.01 kg (about 30 iterations of a T-day simulation; negligible cost).

### Feasibility rules

Each rule yields a specific, explainable verdict and the first day it fails.

| Rule | Condition for failure | Message |
| --- | --- | --- |
| Physically impossible | Required I < 0 | Even total fasting does not reach the target in time |
| Below intake floor | Required I < `intakeFloorKcal` | Required intake is below your configured minimum |
| Fat-mobilization limit | On any day, \|ΔE\| > 68 kcal × F(t) (Alpert's ≈ 31 kcal/lb of fat per day) | Deficit exceeds what fat stores can supply; excess would come from lean tissue |
| Body-fat floor | Final F / W < `minBodyFatFraction` | Target is below your minimum body-fat percentage |
| Lean-loss tolerance | L(0) − L(T) > `maxLeanMassLossKg` | Projected lean-mass loss exceeds your tolerance |

### Adaptive calibration (v2)

With at least 3–4 weeks of COMPLETE days and weigh-ins, the simulator is driven by actual logged intake, and PAL is fitted by minimizing Σ(Wsim(d) − T(d))² against trend weight T. The fit is one-dimensional (golden-section search over PAL ∈ \[1.1, 2.5\]). The calibrated PAL replaces the population activity factor in new projections.

### Known limitations

- No glycogen/water transient: real first-week weight loss exceeds the projection (planned for v2).
- Forbes partitioning ignores resistance training, which spares lean mass; a v2 `leanPartitionMultiplier` parameter can model it.
- Constant intake only; variable-intake plans (diet breaks, refeeds) are v2.

## Design system and accessibility

The visual language is flat, dense and numeric: neutral grayscale surfaces, one accent hue, hairline dividers, tabular figures, and color that always carries a redundant encoding.

### Theme structure (`:core:designsystem`)

- **Theme mode:** System (default), Light, Dark, and an optional true-black Dark variant. Stored in DataStore; resolved at the root of `setContent`.
- **Dynamic color:** off by default (optional toggle later), so chart colors stay predictable.
- **Layer 1, Material `ColorScheme`:** surfaces, text, primary accent, error, outline. Consumed automatically by Material components.
- **Layer 2, `DataColors`:** app-specific semantic tokens provided through `LocalDataColors`.

```kotlin
@Immutable
data class DataColors(
    val seriesPalette: List<Color>,
    val macroProtein: Color,
    val macroCarbohydrate: Color,
    val macroFat: Color,
    val targetMet: Color,
    val targetUnder: Color,
    val targetOver: Color,
    val deltaPositive: Color,
    val deltaNegative: Color,
    val chartGridLine: Color,
    val chartAxisLabel: Color,
    val projectionLine: Color,
    val actualLine: Color
)
```

**Rule:** no composable outside `:core:designsystem` uses a `Color(...)` literal; detekt enforces it.

### Palette matrix

Each palette has a separately tuned light and dark instance (6 `DataColors` instances in total).

| Palette | Purpose | Basis |
| --- | --- | --- |
| Default | Brand look | Neutral grays + violet accent (`#6941C6` light, `#C3B1FF` dark) |
| Color-vision-safe | Protanopia, deuteranopia, tritanopia | Okabe–Ito set; black entry replaced in dark mode |
| High contrast | Low vision | Maximum luminance separation |

A unit test computes the WCAG contrast ratio of every foreground/background token pair in all six instances and requires ≥ 4.5:1 for text and ≥ 3:1 for graphical elements.

### Visual conventions

| Element | Convention |
| --- | --- |
| Typeface | Inter (decision #31), bundled; `fontFeatureSettings = "tnum"` on all numeric text |
| Elevation | `tonalElevation = 0.dp` everywhere; separation by hairline dividers |
| Corners | 2–4 dp |
| Density | Compact visual rows; every interactive element keeps a ≥ 48 dp touch target via `minimumInteractiveComponentSize()` |
| Edge-to-edge | Mandatory since API 36; insets handled by `Scaffold` |

### Redundant encoding (never color alone)

| Element | Second encoding |
| --- | --- |
| Multiple chart series | Dash pattern, marker shape, direct end-of-line labels |
| Over/under target | Icon (▲ ▼ ✓) and signed text ("+12 g") |
| Macro bars | Text labels or segment patterns |
| Projection vs actual | Dashed (projected) vs solid (observed) |

### Interaction patterns

- **Multi-select:** long-press any list row (`Modifier.combinedClickable`, haptic feedback). Selection is `Set<Uuid>` in `UiState`. A non-empty selection swaps the top bar for a contextual action bar: delete, copy to date, move to meal, create recipe from selection. `BackHandler` clears it.
- **Navigation:** four bottom tabs — Home, Nutrition, Training, Body — with Settings in the top bar. Home is read-only; each work tab has Log / Trends views (decision #38).

### TalkBack and text scaling

- Diary and set rows merge semantics into one announcement ("Chicken breast, 200 grams, 330 kilocalories, lunch").
- Long-press actions are mirrored as `customActions`; selection uses `stateDescription`; selection-count changes are announced.
- Every chart exposes a generated summary ("Body weight, 30 days, 84.2 to 82.9 kilograms, decreasing 0.3 kilograms per week") and a "View as table" toggle, built once into the `:core:ui` chart component.
- Unit-bearing values have explicit content descriptions so units are spoken in full.
- All text uses `sp`; layouts are checked at 200% font scale. Dense tables reflow into stacked rows above a font-scale threshold.
- Animations are gated on the system animator scale through `LocalReducedMotion`.
- Every design-system component carries `@PreviewLightDark` and `@PreviewFontScale` previews.

## Backup, export and the sync seam

With no sync in v1, the phone holds the only copy of the data, so three backup layers ship in the first usable release.

### Backup layers

| Layer | Mechanism | Contents | Trigger |
| --- | --- | --- | --- |
| Android Auto Backup | `dataExtractionRules` to Google Drive | `user.db`, DataStore; excludes `reference.db` and crash logs | System-scheduled |
| Scheduled export | `ScheduledExportWorker` writes `VACUUM INTO` copies to a folder chosen once via the Storage Access Framework (e.g. a Drive-synced folder) | Consistent SQLite snapshot, timestamped; keeps the last N | Daily, while charging |
| Manual export / import | Settings → Export | Versioned JSON (`ExportBundle` schema v1) or SQLite file | On demand |

Auto Backup has a per-app size quota; keeping `reference.db` out is what keeps the app within it. Restore from a SQLite snapshot replaces `user.db` after validating its schema version; restore from JSON merges by `id` and `updatedAt`.

### JSON export format

- Top level: `formatVersion`, `exportedAt`, `appVersion`, `ownerId`, then one array per table, including tombstoned rows.
- Defined with kotlinx.serialization data classes in `:core:importers`, so the same format serves backup, the future server's initial upload, and test fixtures.

### Sync seam (built now, implemented later)

```kotlin
interface SyncGateway {
    suspend fun push(changes: List<SyncableChange>): PushResult
    suspend fun pull(sinceRemoteCursor: String?): PullResult
}

class NoOpSyncGateway @Inject constructor() : SyncGateway {
    override suspend fun push(changes: List<SyncableChange>): PushResult {
        return PushResult.Skipped(reason = "Sync disabled")
    }
    override suspend fun pull(sinceRemoteCursor: String?): PullResult {
        return PullResult.Empty
    }
}
```

- `SyncWorker` runs periodically and calls the gateway; with `NoOpSyncGateway` bound, it does nothing.
- A future remote starts with `SyncCursor.lastPushedRevision = 0`, so its first push is every row with `localRevision > 0`, tombstones included — a complete initial upload with no special code.
- Conflict policy for single-user, multi-device sync: last writer wins per row on `(updatedAt, originDeviceId)`.
- A future Spring Boot backend implements the same contract over HTTP, partitioning every table by `ownerId`. Account deletion is one bounded delete per table by `ownerId`.

## Testing strategy

Everything that protects stored data or computes numbers the user acts on is tested on the JVM and runs in CI on every push.

| Layer | Framework | What is tested | Where it runs |
| --- | --- | --- | --- |
| Pure domain (`:core:model`, `:core:domain`) | JUnit 5, AssertK | Quantity conversions, target resolution by effective date, energy-mode selection, e1RM formulas, source-priority resolution | CI |
| Projection (`:core:projection`) | JUnit 5, AssertK | Golden values (worked example to 4 decimal places; multi-week spreadsheet fixtures), solver convergence, every feasibility rule, trend-weight smoothing with gaps | CI |
| Importers (`:core:importers`) | JUnit 5, AssertK | Strong and Cronometer CSV fixtures (including malformed rows), JSON export round-trip | CI |
| Database (`:core:database`) | JUnit 4, Robolectric, Room | DAO queries, owner isolation, tombstone filtering, revision stamping, every migration via `MigrationTestHelper` against committed schema JSON | CI |
| Data (`:core:data`) | JUnit 4, Robolectric, Turbine | Repository flows, copy-on-first-use, rebind and undo, Health Connect mapping and dedup with multi-origin fixtures | CI |
| ViewModels (`:feature:*`) | JUnit 4, Turbine, fakes from `:core:testing` | `UiState` transitions, multi-select, validation | CI |
| Design system | JUnit 5 | WCAG contrast ratios for all 6 palette instances | CI |
| Architecture | Konsist | Module and layer rules | CI |
| UI semantics | Compose UI test | Content descriptions, custom actions, state descriptions; accessibility checks | Device, before releases |

**Conventions**

- Hand-written fakes in `:core:testing` (one per repository interface) instead of a mocking framework.
- Test dispatchers injected through the dispatcher qualifiers; no `runBlocking` against real dispatchers.
- Fixture builders with sensible defaults (`aFood()`, `aWorkoutSet()`), so tests state only what matters.
- Before every release build: Accessibility Scanner pass on each screen, and a restore-from-backup drill on the debug app.

## v1 milestone plan

Version 1 is reached when the release build replaces Cronometer and Strong for daily use. The milestones run in order; each ends with a concrete exit check.

1. **M0 — Skeleton.** Private repo, `build-logic` convention plugins, version catalog, every module created empty, Hilt, Navigation 3 with five empty tabs, light/dark theme stub, crash-log handler, CI workflow.
   - Exit: debug and release builds install side by side on the S25; CI is green.
2. **M1 — Foundations.** Design-system tokens, palette matrix and contrast tests, bundled typeface; `user.db` with standard columns, `SyncSequence`, `CurrentOwnerProvider`, `UserProfile` and versioned settings; metric-registry contract; chart component with table toggle; `NoOpSyncGateway`; all three backup layers; manual body-weight entry; four-tab navigation with Home (range control and Body card) and Body Log / Trends (M1c).
   - Exit: enter weights → see trend-weight chart and table → export, wipe the debug app, restore, identical data.
3. **M2 — Body and Health Connect.** Permission flow and rationale screen, inspector, import worker for weight, body fat, heart rate, resting heart rate, HRV, SpO2, sleep, steps and energy; source priority; Body tab screens; `MeasurementType` editor (including lab markers).
   - Exit: inspector shows what the Versa 4 writes; a week of Fitbit sleep and heart data appears without duplicates.
4. **M3 — Training.** Exercise dataset seed and overrides, custom exercises, routine templates, live workout with per-set persistence, rest-timer foreground service, previous performance, PRs and e1RM, Strong CSV import.
   - Exit: a full gym session logged in GetFit; complete Strong history imported with exercises mapped.
5. **M4 — Nutrition.** Reference-database builder and `reference.db`; search with copy-on-first-use; diary with meal slots, planned/consumed status, multi-select actions and quick-add; recipes; food versioning with undoable rebind; versioned targets; coverage-aware totals; hydration and its goal; `DayLog`; Cronometer CSV import.
   - Exit: a day logged in both apps matches Cronometer's energy and macros within a stated tolerance; Cronometer history imported.
6. **M5 — Energy and projection.** BMR formulas, the four energy modes with side-by-side estimates, daily budget on Home, projection plan screen in Body, simulator, solver and feasibility verdicts, plan-versus-actual charts.
   - Exit: the worked example reproduces in the UI; a 20-week plan shows trajectories and a correct verdict for an infeasible target.
7. **M6 — Daily-driver release.** Polish and accessibility pass over Home and the chart builder (built from M1c onward, decision #38), accessibility pass at 200% font scale with TalkBack, R8 release build signed with the release key.
   - Exit: the release build is installed as the daily app; Cronometer and Strong subscriptions can be canceled.

## Risks and deferred items

The largest risk is scope: an all-in-one app never reaching daily use. The milestone exits above are the defense; anything not listed in M0–M6 waits.

### Risks

| Risk | Impact | Mitigation |
| --- | --- | --- |
| Scope creep across three domains | v1 never ships | Fixed M0–M6 scope; everything else goes to the v2 list below |
| Data loss on the only device | Entire history lost | Three backup layers in M1; restore drill before each release |
| Lost release keystore | Cannot update without uninstalling (data wiped) | Keystore backed up outside the repo at creation |
| Migration bug corrupts `user.db` | Partial or total data loss | Explicit migrations only, committed schema JSON, `MigrationTestHelper` tests, snapshot export before each release install |
| Versa 4 writes fewer Health Connect types than expected | Missing HRV or other metrics | Inspector in M2 confirms actual coverage before dependent features are built |
| Health Connect or Google Health behavior changes | Import breaks | Import isolated in `:core:healthconnect`; inspector exposes failures |
| Branded foods lack micronutrients | Understated totals | Unknown ≠ zero; coverage shown with every total |
| Learning curve (Kotlin, Compose, Android lifecycle) | Slow early milestones | M0–M1 deliberately small; pure-Kotlin modules use familiar JVM testing |
| Forbes model overstates lean loss for lifters | Pessimistic projections | Documented limitation; v2 `leanPartitionMultiplier` |

### Deferred to v2 and later

- Adaptive PAL calibration in the projection engine (the adaptive expenditure metric itself ships in M5).
- Progression rules and programs (`Program → Block → Week`); columns already reserved.
- Barcode scanning (CameraX + ML Kit) and Open Food Facts lookup.
- Home-screen widgets (Jetpack Glance) for water and quick-add.
- Full-text search over foods and notes (FTS4).
- Progress photos.
- Glycogen/water transient and variable-intake plans in the projection model.
- Health Connect write-back.
- Sync backend (Spring Boot), accounts, multiple users.
- Kotlin Multiplatform extraction and an iOS client.
- Play Store publication: privacy policy, data-safety form, Health Connect declaration, account deletion, Open Food Facts attribution, open-source licenses screen.

### Open questions

- Exercise dataset license to be verified before bundling (M3).
- Exact Health Connect record types written by the Versa 4 (answered by the M2 inspector).
