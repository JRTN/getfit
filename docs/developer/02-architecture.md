# 2. Architecture

GetFit is a single-activity Jetpack Compose app split into about 23 Gradle modules. The split is deliberately fine-grained: each module has one job, and Gradle refuses to compile code that crosses a boundary it should not cross.

## The five layers

```
 :app                         the only module that wires implementations to interfaces
   │
 :feature:home  :feature:nutrition  :feature:training  :feature:body  :feature:settings
   │   (screens + ViewModels; see only the layer below, never storage, never each other)
   │
 :core:designsystem  :core:ui                      Compose UI building blocks
   │
 :core:data ──implements──▶ :core:domain           repositories, stamping, owner, seeding
   │  uses                    ▲
 :core:database  :core:datastore  :core:sync  (:core:reference  :core:healthconnect  :core:backup)
   │
 :core:model  :core:domain  (:core:projection  :core:importers)     pure Kotlin, no Android
```

Dependencies point downward. The one upward arrow is the important one: `:core:data` **implements** the repository interfaces declared in `:core:domain`. Features depend on those interfaces, so a ViewModel never learns that Room or DataStore exists.

## The dependency rules

From the spec ("Module architecture"):

1. **Pure-Kotlin modules** (`:core:model`, `:core:domain`, `:core:projection`, `:core:importers`) depend on no Android module and use only multiplatform APIs: the Kotlin standard library and kotlinx libraries, never `java.*` or `javax.*`. That keeps them portable to Kotlin Multiplatform later.
2. **`:feature:*`** depends only on `:core:domain`, `:core:model`, `:core:ui` and `:core:designsystem`. ViewModels receive repository *interfaces* through Hilt.
3. **No feature depends on another feature.** Cross-feature navigation goes through callbacks supplied by `:app`.
4. **`:core:data`** is the only module (besides `:app`) that depends on the storage modules (`:core:database`, `:core:datastore`, and later `:core:reference`, `:core:healthconnect`, `:core:backup`).
5. **`:app`** depends on everything and is the only module that binds implementations to interfaces (Hilt `@Binds` / `@Provides`).

### How each rule is enforced

| Rule | Enforced by |
| --- | --- |
| Features see only domain/model/ui/designsystem | [`AndroidFeatureConventionPlugin`](../../build-logic/convention/src/main/kotlin/AndroidFeatureConventionPlugin.kt) adds exactly those four dependencies; a feature build file adds nothing |
| Pure modules use only multiplatform APIs (no `java.*`, `javax.*`, `android.*`, `androidx.*`, … imports or inline references) | [`LayerRulesTest.pureModules_importOnlyMultiplatformApis` and `pureModules_referenceNoPlatformPackagesInCode`](../../architecture-tests/src/test/kotlin/com/jrussell/fitness/architecture/LayerRulesTest.kt) (Konsist) |
| No feature references another | [`LayerRulesTest.featureModules_doNotReferenceOtherFeatures`](../../architecture-tests/src/test/kotlin/com/jrussell/fitness/architecture/LayerRulesTest.kt) |
| ViewModels take only domain interfaces | [`ViewModelDependencyRulesTest`](../../architecture-tests/src/test/kotlin/com/jrussell/fitness/architecture/ViewModelDependencyRulesTest.kt) |
| Owner-scoped DAO queries take `ownerId` | [`DaoOwnerScopingRulesTest`](../../architecture-tests/src/test/kotlin/com/jrussell/fitness/architecture/DaoOwnerScopingRulesTest.kt) |
| Dependencies are declared correctly | Dependency Analysis plugin (`./gradlew buildHealth`) |

The Konsist rules run with `strict = true`: a rule that matches no code fails instead of passing silently, so a renamed package cannot quietly switch a rule off.

## Module inventory

**Status** says what exists today. Modules marked *empty* are registered with their convention plugin so the module graph is complete, but they have no code yet.

### App and features

| Module | Responsibility | Status |
| --- | --- | --- |
| [`:app`](../../app/) | `GetFitApplication`, `MainActivity`, the navigation shell, all Hilt modules (`di/`) | Shell with four tabs that keep their state ([`TabNavDisplay`](../../app/src/main/kotlin/com/jrussell/fitness/ui/TabNavDisplay.kt)), Settings overlay, DI wiring |
| [`:feature:home`](../../feature/home/) | Read-only summary of where you are (decision #38); later the cross-domain chart builder | Range chips (remembered) and the Weight card: latest weight and its trend per week |
| [`:feature:nutrition`](../../feature/nutrition/) | Diary, food search, recipes, targets, hydration | Placeholder screen |
| [`:feature:training`](../../feature/training/) | Live workout, templates, exercise library | Placeholder screen |
| [`:feature:body`](../../feature/body/) | Measurements, sleep, Health Connect inspector, projection plans (M5) | Weight: Log (add, history, delete) and Trends (chart), sharing one remembered range |
| [`:feature:settings`](../../feature/settings/) | Settings | Appearance (theme mode, palette) and weight unit |

Each feature exposes the same three files: a Navigation 3 key (`HomeKey`), a screen composable (`HomeScreen`), and an entry function (`homeEntry()`) that registers the screen with the app's navigation. See [`HomeEntry.kt`](../../feature/home/src/main/kotlin/com/jrussell/fitness/feature/home/HomeEntry.kt). Each work tab (Nutrition, Training, Body) has **Log** and **Trends** views; Body gains a **Plan** view in M5. There is no Trends tab and no Today tab: Home and each tab's Trends view replaced them (decision #38).

### Core: UI

| Module | Responsibility | Status |
| --- | --- | --- |
| [`:core:designsystem`](../../core/designsystem/) | Palettes, contrast math, typography (Inter), shapes, icons, `GetFitTheme` | Complete for M1 |
| [`:core:ui`](../../core/ui/) | Shared composites: chart component, data table, multi-select scaffold | [`MetricChart`](../../core/ui/src/main/kotlin/com/jrussell/fitness/core/ui/chart/MetricChart.kt) (Vico, with dotted best-fit trend, spoken summary and table view), [`RangeChips`](../../core/ui/src/main/kotlin/com/jrussell/fitness/core/ui/range/RangeChips.kt), number and date formatting |

### Core: data and storage

| Module | Responsibility | Status |
| --- | --- | --- |
| [`:core:data`](../../core/data/) | Repository implementations, `StampedWriter`, owner provider, device ID, seeding, crash log, metric providers | Measurements, day boundary, preferences, remembered ranges, metric registry (body weight) |
| [`:core:database`](../../core/database/) | `user.db`: Room entities, DAOs, converters, committed schema JSON | Schema v1 (7 tables) |
| [`:core:datastore`](../../core/datastore/) | Preferences DataStore for UI-only settings | Theme mode, palette, weight unit, each screen's date range |
| [`:core:sync`](../../core/sync/) | `SyncGateway` seam and `NoOpSyncGateway` | Seam only |
| [`:core:reference`](../../core/reference/) | Bundled read-only USDA food catalog (`reference.db`) | Empty (M4) |
| [`:core:healthconnect`](../../core/healthconnect/) | Health Connect client and import worker | Empty (M2) |
| [`:core:backup`](../../core/backup/) | Snapshot and JSON export/import | Empty (M1d) |

### Core: pure Kotlin

| Module | Responsibility | Status |
| --- | --- | --- |
| [`:core:model`](../../core/model/) | Domain types: typed IDs, `Measurement`, enums, `UserPreferences` | Growing per milestone |
| [`:core:domain`](../../core/domain/) | Repository interfaces, `Outcome`/`DomainError`, `IdGenerator`, `localDateFor`, `observeToday`, `WallClock`, the metric contract and transforms (best-fit trend, trend weight), date ranges | Growing per milestone |
| [`:core:projection`](../../core/projection/) | Weight projection simulator and solver | Empty (M5) |
| [`:core:importers`](../../core/importers/) | Strong and Cronometer CSV parsers, JSON export format | Empty (M1d/M3/M4) |

### Support modules (not part of the app)

| Module | Responsibility |
| --- | --- |
| [`:core:testing`](../../core/testing/) | Test helpers shared across modules: `FakeClock`, `FakeWallClock`, `MainDispatcherRule`, hand-written repository and registry fakes |
| [`:architecture-tests`](../../architecture-tests/) | Konsist rules that read every module's source |
| [`:detekt-rules`](../../detekt-rules/) | GetFit's custom detekt rules (rule set `getfit`) |
| [`build-logic`](../../build-logic/) | Convention plugins: the build configuration every module shares |

## Why so many modules?

- **Boundaries are compiled, not remembered.** A feature cannot import a DAO because the DAO's module is not on its classpath.
- **Builds stay fast.** Gradle rebuilds only the modules a change touches, and the configuration cache and build cache are on.
- **Domain logic stays portable.** The pure-Kotlin modules could move to Kotlin Multiplatform (e.g. for a future server or desktop) without rewrites.
- **Each module reads like a chapter.** A newcomer can understand `:core:sync` without reading `:feature:training`.

The cost is ceremony: every module needs a build file. The convention plugins reduce that to one or two lines (see [Build and quality](06-build-and-quality.md)).

Next: [Runtime tour](03-runtime-tour.md).
