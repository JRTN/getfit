# 3. Runtime tour

This chapter follows the running app through four journeys. Open each linked file as you go; the steps are in the order the code runs.

## Journey 1: process start to first frame

### 1. `GetFitApplication.onCreate`

[`app/.../GetFitApplication.kt`](../../app/src/main/kotlin/com/jrussell/fitness/GetFitApplication.kt)

- `@HiltAndroidApp` makes Hilt generate the application-level dependency graph (the `SingletonComponent`). Everything `@Singleton` lives as long as the process.
- It installs [`CrashLogHandler`](../../core/data/src/main/kotlin/com/jrussell/fitness/core/data/crash/CrashLogHandler.kt), so any later crash is written to `files/crash-logs/` before Android's default handler ends the process.
- It launches [`DataBootstrap.run()`](../../core/data/src/main/kotlin/com/jrussell/fitness/core/data/seed/DataBootstrap.kt) on the `@ApplicationScope` coroutine scope (from [`CoroutinesModule`](../../app/src/main/kotlin/com/jrussell/fitness/di/CoroutinesModule.kt)). It does not wait for it. The bootstrap does two idempotent things:
  1. [`ReferenceSeeder.seed()`](../../core/data/src/main/kotlin/com/jrussell/fitness/core/data/seed/ReferenceSeeder.kt) inserts built-in rows (today, the Weight measurement type) if this database has not had them yet.
  2. [`SingleLocalOwnerProvider.currentOwnerId()`](../../core/data/src/main/kotlin/com/jrussell/fitness/core/data/owner/SingleLocalOwnerProvider.kt) finds the local profile, or creates it on the very first launch.

Because the UI does not wait for the bootstrap, any repository that needs seeded rows calls `ReferenceSeeder.seed()` itself first. After the first success the seeder returns immediately. This is why a screen opened in the first milliseconds still finds the Weight type.

The database itself opens lazily on first use. [`DatabaseModule`](../../app/src/main/kotlin/com/jrussell/fitness/di/DatabaseModule.kt) provides it once per process via [`createGetFitDatabase`](../../core/database/src/main/kotlin/com/jrussell/fitness/core/database/DatabaseFactory.kt).

### 2. `MainActivity.onCreate`

[`app/.../MainActivity.kt`](../../app/src/main/kotlin/com/jrussell/fitness/MainActivity.kt)

- `@AndroidEntryPoint` lets Hilt inject the activity. It gets [`MainViewModel`](../../app/src/main/kotlin/com/jrussell/fitness/MainViewModel.kt) through `by viewModels()`.
- `MainViewModel` holds two things:
  - **`navigation`**, a [`TopLevelBackStack`](../../app/src/main/kotlin/com/jrussell/fitness/navigation/TopLevelBackStack.kt) starting on Today;
  - **`userPreferences`**, a `StateFlow<UserPreferences?>` that is `null` until DataStore has been read.
- `setContent` draws nothing until the preferences arrive (a few milliseconds). Until then the window background shows. This prevents a flash of the wrong theme.
- Once preferences are loaded:
  - `applyApplicationNightMode` tells Android the app's own light/dark choice through `UiModeManager.setApplicationNightMode`. Android remembers it, so the next cold start's launch window already matches.
  - `enableEdgeToEdge` with a `SystemBarStyle` makes the status-bar and navigation-bar icons follow the app's theme rather than the system's.
  - [`GetFitTheme`](../../core/designsystem/src/main/kotlin/com/jrussell/fitness/core/designsystem/theme/Theme.kt) wraps everything with the chosen palette.

### 3. `GetFitApp`: the shell

[`app/.../ui/GetFitApp.kt`](../../app/src/main/kotlin/com/jrussell/fitness/ui/GetFitApp.kt)

- A `Scaffold` with a top bar (the title and a Settings button), a bottom bar with five tabs, and Navigation 3's `NavDisplay` in between.
- `NavDisplay` renders the last key of `navigation.backStack`. Its `entryProvider` maps keys to screens by calling each feature's entry function: `todayEntry()`, `nutritionEntry()`, … `settingsEntry()`. This is the one place `:app` knows about every feature.
- Two entry decorators give each screen saved UI state and its own `ViewModelStore`. A screen's ViewModel lives exactly as long as the screen is on the back stack.

## Journey 2: navigating

All navigation state is in [`TopLevelBackStack`](../../app/src/main/kotlin/com/jrussell/fitness/navigation/TopLevelBackStack.kt), a small, fully unit-tested class ([`TopLevelBackStackTest`](../../app/src/test/kotlin/com/jrussell/fitness/navigation/TopLevelBackStackTest.kt)).

- **Tabs** come from [`TopLevelDestination`](../../app/src/main/kotlin/com/jrussell/fitness/navigation/TopLevelDestination.kt): five tabs, each with an outlined icon and a filled icon for the selected state. Each tab keeps its own history.
- **`switchTo(tab)`** shows a tab and restores where you were in it.
- **Settings is an overlay**, not a tab: `openOverlay(SettingsKey)` shows it over whichever tab is current. It can never be stacked twice, and switching tabs closes it.
- **Back** (`removeLast`) closes the overlay first, then pops the current tab, then returns to Today. On Today's root it returns `false`, and the activity calls `moveTaskToBack(true)`: the app goes to the background but stays alive, like the system default.

Try it on the emulator: open Settings, switch tabs, and press back from Body. Then read `removeLast`.

## Journey 3: changing the palette

Follow a tap on Settings → Appearance → "High contrast" all the way to the repaint.

1. [`SettingsScreen`](../../feature/settings/src/main/kotlin/com/jrussell/fitness/feature/settings/SettingsScreen.kt) calls `viewModel.onPaletteSelected(ColorPalette.HIGH_CONTRAST)`.
2. [`SettingsViewModel`](../../feature/settings/src/main/kotlin/com/jrussell/fitness/feature/settings/SettingsViewModel.kt) depends only on the interface [`UserPreferencesRepository`](../../core/domain/src/main/kotlin/com/jrussell/fitness/core/domain/UserPreferencesRepository.kt) from `:core:domain`, and calls `setPalette`.
3. Hilt has bound that interface (in [`PreferencesModule`](../../app/src/main/kotlin/com/jrussell/fitness/di/PreferencesModule.kt)) to [`DefaultUserPreferencesRepository`](../../core/data/src/main/kotlin/com/jrussell/fitness/core/data/preferences/DefaultUserPreferencesRepository.kt) in `:core:data`.
4. That delegates to [`UserPreferencesDataSource`](../../core/datastore/src/main/kotlin/com/jrussell/fitness/core/datastore/UserPreferencesDataSource.kt), which writes the enum's name into a Preferences DataStore file.
5. DataStore emits the new preferences on its `Flow`. `MainViewModel.userPreferences` updates, `MainActivity` recomposes, and `GetFitTheme` supplies the high-contrast color scheme to the whole tree.

Nothing about the DataStore file can stop the app from starting. A corrupt file is replaced with defaults, a read error emits defaults, and an unknown or wrong-typed stored value falls back to that field's default (see the class KDoc and [`UserPreferencesDataSourceTest`](../../core/datastore/src/test/kotlin/com/jrussell/fitness/core/datastore/UserPreferencesDataSourceTest.kt)).

## Journey 4: recording a weight (data layer only, for now)

There is no Weight screen yet (it arrives in M1c), but the whole write path exists and is tested. Read [`DefaultMeasurementRepository.addManual`](../../core/data/src/main/kotlin/com/jrussell/fitness/core/data/repository/DefaultMeasurementRepository.kt) alongside this list.

1. A caller passes a [`ManualMeasurement`](../../core/domain/src/main/kotlin/com/jrussell/fitness/core/domain/ManualMeasurement.kt): type key `"weight"`, value in kilograms, when and in which time zone it was taken, and what the user actually typed (e.g. 181.7 lb).
2. `referenceSeeder.seed()` makes sure the built-in Weight type exists.
3. `ownerProvider.currentOwnerId()` gives the owner. **Repositories never accept an owner from callers**, so no screen can ask for someone else's data.
4. The value is validated. NaN or infinity returns `Outcome.Failure(DomainError.Validation("value", …))`, and an unknown type key returns `NotFound`. Expected failures are values, not exceptions.
5. The entry's **local date** is computed with [`localDateFor`](../../core/domain/src/main/kotlin/com/jrussell/fitness/core/domain/DayAssignment.kt), using the day-start setting in force on that calendar date. With a 04:00 day start, a 01:30 weigh-in counts toward the previous day.
6. [`StampedWriter.write { stamp -> … }`](../../core/data/src/main/kotlin/com/jrussell/fitness/core/data/write/StampedWriter.kt) opens one Room transaction. Inside it:
   - the global revision counter goes up by one;
   - the block receives a [`WriteStamp`](../../core/data/src/main/kotlin/com/jrussell/fitness/core/data/write/WriteStamp.kt) (revision, now, device ID);
   - the row is inserted with those values in its sync columns.

   If anything throws, the row and the revision increment roll back together.
7. The new row's ID (a time-ordered UUIDv7) comes back as `Outcome.Success(MeasurementId(…))`.

Reading is the mirror image. `observeMeasurements("weight", from, to)` returns a `Flow` that follows the measurement type reactively, then switches to Room's live query for that type and date range. It maps each row through [`MeasurementMapper`](../../core/data/src/main/kotlin/com/jrussell/fitness/core/data/repository/MeasurementMapper.kt) into the domain [`Measurement`](../../core/model/src/main/kotlin/com/jrussell/fitness/core/model/Measurement.kt). Any later insert or delete makes the flow emit again.

To watch this happen, run [`DefaultMeasurementRepositoryTest`](../../core/data/src/test/kotlin/com/jrussell/fitness/core/data/repository/DefaultMeasurementRepositoryTest.kt) in Android Studio with a breakpoint in `StampedWriter.write`.

## Where the wiring lives

Every binding is in `:app`'s [`di/`](../../app/src/main/kotlin/com/jrussell/fitness/di/) package:

| Module | Provides or binds |
| --- | --- |
| [`CoroutinesModule`](../../app/src/main/kotlin/com/jrussell/fitness/di/CoroutinesModule.kt) | `@IoDispatcher`, `@DefaultDispatcher`, `@ApplicationScope`, `Clock`, `IdGenerator` |
| [`DatabaseModule`](../../app/src/main/kotlin/com/jrussell/fitness/di/DatabaseModule.kt) | `GetFitDatabase` (`user.db`) |
| [`DataModule`](../../app/src/main/kotlin/com/jrussell/fitness/di/DataModule.kt) | `CurrentOwnerProvider`, `DeviceIdProvider`, `DayBoundaryRepository`, `MeasurementRepository` |
| [`PreferencesModule`](../../app/src/main/kotlin/com/jrussell/fitness/di/PreferencesModule.kt) | The preferences DataStore and `UserPreferencesRepository` |
| [`SyncModule`](../../app/src/main/kotlin/com/jrussell/fitness/di/SyncModule.kt) | `SyncGateway` → `NoOpSyncGateway` |

Classes with an `@Inject constructor` (`StampedWriter`, `ReferenceSeeder`, the repositories) need no entry. Hilt builds them from their constructors. Modules only exist for interfaces, qualifiers and things built by factories.

Next: [Data layer](04-data-layer.md).
