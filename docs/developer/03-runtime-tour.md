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

After the bootstrap, [`BackupStartup`](../../core/data/src/main/kotlin/com/jrussell/fitness/core/data/backup/BackupStartup.kt) keeps the daily backup scheduled, and asks for a catch-up snapshot if the last one is stale. Both steps run through [`runGuarded`](../../app/src/main/kotlin/com/jrussell/fitness/BootstrapGuard.kt): a failure is written to the log (`files/logs/`) instead of crashing, because a crash here would repeat on every launch. The application also gives WorkManager the Hilt worker factory (`Configuration.Provider`), which is why the manifest removes WorkManager's default initializer.

The database itself opens lazily on first use. [`DatabaseModule`](../../app/src/main/kotlin/com/jrussell/fitness/di/DatabaseModule.kt) provides it once per process via [`createGetFitDatabase`](../../core/database/src/main/kotlin/com/jrussell/fitness/core/database/DatabaseFactory.kt). Before that, it installs any restore staged before the last restart (see Journey 5).

### 2. `MainActivity.onCreate`

[`app/.../MainActivity.kt`](../../app/src/main/kotlin/com/jrussell/fitness/MainActivity.kt)

- `@AndroidEntryPoint` lets Hilt inject the activity. It gets [`MainViewModel`](../../app/src/main/kotlin/com/jrussell/fitness/MainViewModel.kt) through `by viewModels()`.
- `MainViewModel` holds two things:
  - **`navigation`**, a [`TopLevelBackStack`](../../app/src/main/kotlin/com/jrussell/fitness/navigation/TopLevelBackStack.kt) starting on Home, with one stack per tab;
  - **`userPreferences`**, a `StateFlow<UserPreferences?>` that is `null` until DataStore has been read.
- `setContent` draws nothing until the preferences arrive (a few milliseconds). Until then the window background shows. This prevents a flash of the wrong theme.
- Once preferences are loaded:
  - `applyApplicationNightMode` tells Android the app's own light/dark choice through `UiModeManager.setApplicationNightMode`. Android remembers it, so the next cold start's launch window already matches.
  - `enableEdgeToEdge` with a `SystemBarStyle` makes the status-bar and navigation-bar icons follow the app's theme rather than the system's.
  - [`GetFitTheme`](../../core/designsystem/src/main/kotlin/com/jrussell/fitness/core/designsystem/theme/Theme.kt) wraps everything with the chosen palette.

### 3. `GetFitApp`: the shell

[`app/.../ui/GetFitApp.kt`](../../app/src/main/kotlin/com/jrussell/fitness/ui/GetFitApp.kt)

- A `Scaffold` with a top bar (the title and a Settings button), a bottom bar with four tabs (Home, Nutrition, Training, Body), and [`TabNavDisplay`](../../app/src/main/kotlin/com/jrussell/fitness/ui/TabNavDisplay.kt) in between.
- Its `entryProvider` maps keys to screens by calling each feature's entry function: `homeEntry()`, `nutritionEntry()`, … `settingsEntry()`. This is the one place `:app` knows about every feature, and where cross-feature callbacks are wired (Home's Weight card calls `navigation.switchTo(BodyKey)`).
- `TabNavDisplay` follows Navigation 3's "multiple back stacks" pattern. Each tab's stack (and the Settings overlay) gets its own two entry decorators, for saved UI state and a `ViewModelStore` per screen, and all of them stay in composition; only the visible entries go to `NavDisplay`. So a hidden tab keeps its state (Body stays on Trends while you look at Home), and a screen's state and ViewModel are dropped only when it is popped off its own stack. [`TabNavDisplayTest`](../../app/src/test/kotlin/com/jrussell/fitness/ui/TabNavDisplayTest.kt) pins this.

## Journey 2: navigating

All navigation state is in [`TopLevelBackStack`](../../app/src/main/kotlin/com/jrussell/fitness/navigation/TopLevelBackStack.kt), a small, fully unit-tested class ([`TopLevelBackStackTest`](../../app/src/test/kotlin/com/jrussell/fitness/navigation/TopLevelBackStackTest.kt)).

- **Tabs** come from [`TopLevelDestination`](../../app/src/main/kotlin/com/jrussell/fitness/navigation/TopLevelDestination.kt): four tabs, each with an outlined icon and a filled icon for the selected state. Each tab keeps its own history.
- **`switchTo(tab)`** shows a tab and restores where you were in it.
- **Settings is an overlay**, not a tab: `openOverlay(SettingsKey)` shows it over whichever tab is current. It can never be stacked twice, and switching tabs closes it.
- **Back** (`removeLast`) closes the overlay first, then pops the current tab, then returns to Home. On Home's root there is nothing left to pop, so Navigation 3 does not intercept back at all and Android's default runs. On Android 12 and later (the app needs 14) that moves the app to the background without finishing it. `MainActivity`'s `onExit = { moveTaskToBack(true) }` is a fallback for the case where back *is* intercepted at the root and `removeLast` returns `false`; Navigation 3 1.2.0 never does that.

Try it on the emulator: open Settings, switch tabs, and press back from Body. Then read `removeLast`.

## Journey 3: changing the palette

Follow a tap on Settings → Appearance → "High contrast" all the way to the repaint.

1. [`SettingsScreen`](../../feature/settings/src/main/kotlin/com/jrussell/fitness/feature/settings/SettingsScreen.kt) calls `viewModel.onPaletteSelected(ColorPalette.HIGH_CONTRAST)`.
2. [`SettingsViewModel`](../../feature/settings/src/main/kotlin/com/jrussell/fitness/feature/settings/SettingsViewModel.kt) depends only on the interface [`UserPreferencesRepository`](../../core/domain/src/main/kotlin/com/jrussell/fitness/core/domain/UserPreferencesRepository.kt) from `:core:domain`, and calls `setPalette`.
3. Hilt has bound that interface (in [`PreferencesModule`](../../app/src/main/kotlin/com/jrussell/fitness/di/PreferencesModule.kt)) to [`DefaultUserPreferencesRepository`](../../core/data/src/main/kotlin/com/jrussell/fitness/core/data/preferences/DefaultUserPreferencesRepository.kt) in `:core:data`.
4. That delegates to [`UserPreferencesDataSource`](../../core/datastore/src/main/kotlin/com/jrussell/fitness/core/datastore/UserPreferencesDataSource.kt), which writes the enum's name into a Preferences DataStore file.
5. DataStore emits the new preferences on its `Flow`. `MainViewModel.userPreferences` updates, `MainActivity` recomposes, and `GetFitTheme` supplies the high-contrast color scheme to the whole tree.

Nothing about the DataStore file can stop the app from starting. A corrupt file is replaced with defaults, a read error emits defaults, and an unknown or wrong-typed stored value falls back to that field's default (see the class KDoc and [`UserPreferencesDataSourceTest`](../../core/datastore/src/test/kotlin/com/jrussell/fitness/core/datastore/UserPreferencesDataSourceTest.kt)).

## Journey 4: logging a weight, through to Home and the chart

Follow "Add weight" → 181.7 → Save on the Body tab until the number shows up on Home and in the chart.

**The screen.** [`BodyScreen`](../../feature/body/src/main/kotlin/com/jrussell/fitness/feature/body/BodyScreen.kt) shows Log or Trends. On Log, [`WeightEntryDialog`](../../feature/body/src/main/kotlin/com/jrussell/fitness/feature/body/WeightEntryDialog.kt) collects the text, plus a date and time only if the user changes them (nothing after today can be picked), and calls `BodyViewModel.onSaveWeight(text, pickedDate, pickedTime, locale)`.

**Parsing.** [`parseWeightInput`](../../feature/body/src/main/kotlin/com/jrussell/fitness/feature/body/WeightInput.kt) reads the text in the user's unit and locale ("181.7", or "82,4" in Germany), rejects anything that isn't wholly a number, and checks 20–400 kg. A bad value leaves the dialog open with a field error that TalkBack announces.

**Saving.** [`BodyViewModel`](../../feature/body/src/main/kotlin/com/jrussell/fitness/feature/body/BodyViewModel.kt) builds a [`ManualMeasurement`](../../core/domain/src/main/kotlin/com/jrussell/fitness/core/domain/ManualMeasurement.kt) and hands it to [`DefaultMeasurementRepository.addManual`](../../core/data/src/main/kotlin/com/jrussell/fitness/core/data/repository/DefaultMeasurementRepository.kt). Read that method alongside this list.

1. The entry carries type key `"weight"`, the value in kilograms, when and in which time zone it was taken (now, or the picked moment but never later than now), what the user actually typed (181.7 lb) and, when a date was picked, `localDateOverride`.
2. `referenceSeeder.seed()` makes sure the built-in Weight type exists.
3. `ownerProvider.currentOwnerId()` gives the owner. **Repositories never accept an owner from callers**, so no screen can ask for someone else's data.
4. The value is validated. NaN or infinity returns `Outcome.Failure(DomainError.Validation("value", …))`, and an unknown type key returns `NotFound`. Expected failures are values, not exceptions.
5. The entry's **local date** is the picked date if there is one, so a backfilled entry lands on the day the user chose. Otherwise [`localDateFor`](../../core/domain/src/main/kotlin/com/jrussell/fitness/core/domain/DayAssignment.kt) computes it from the day-start setting in force on that calendar date. With a 04:00 day start, a 01:30 weigh-in counts toward the previous day.
6. [`StampedWriter.write { stamp -> … }`](../../core/data/src/main/kotlin/com/jrussell/fitness/core/data/write/StampedWriter.kt) opens one Room transaction. Inside it:
   - the global revision counter goes up by one;
   - the block receives a [`WriteStamp`](../../core/data/src/main/kotlin/com/jrussell/fitness/core/data/write/WriteStamp.kt) (revision, now, device ID);
   - the row is inserted with those values in its sync columns.

   If anything throws, the row and the revision increment roll back together.
7. The new row's ID (a time-ordered UUIDv7) comes back as `Outcome.Success(MeasurementId(…))`.

Reading is the mirror image. `observeMeasurements("weight", from, to)` returns a `Flow` that follows the measurement type reactively, then switches to Room's live query for that type and date range. It maps each row through [`MeasurementMapper`](../../core/data/src/main/kotlin/com/jrussell/fitness/core/data/repository/MeasurementMapper.kt) into the domain [`Measurement`](../../core/model/src/main/kotlin/com/jrussell/fitness/core/model/Measurement.kt). Any later insert or delete makes the flow emit again.

To watch this happen, run [`DefaultMeasurementRepositoryTest`](../../core/data/src/test/kotlin/com/jrussell/fitness/core/data/repository/DefaultMeasurementRepositoryTest.kt) in Android Studio with a breakpoint in `StampedWriter.write`.

**Back to the screens.** Nothing tells Home or Body that a weight was added; they are already watching.

- [`BodyMetricProvider`](../../core/data/src/main/kotlin/com/jrussell/fitness/core/data/metric/BodyMetricProvider.kt) turns that live query into the `body.weight` series: one point per day, the day's last reading. Screens ask for it through [`MetricRegistry`](../../core/domain/src/main/kotlin/com/jrussell/fitness/core/domain/metric/MetricContract.kt), never the repository, so a future chart builder can ask for any metric the same way. [`BodyMetricProviderLiveTest`](../../core/data/src/test/kotlin/com/jrussell/fitness/core/data/metric/BodyMetricProviderLiveTest.kt) proves a save reaches the series while it is being watched.
- `BodyViewModel` combines that series with the tab's remembered range ([`DateRangeRepository`](../../core/domain/src/main/kotlin/com/jrussell/fitness/core/domain/range/DateRangeRepository.kt), shared by Log and Trends) and the user's unit; everything shown is converted from kilograms here. The latest-weight card's rate is the slope of [`linearTrend`](../../core/domain/src/main/kotlin/com/jrussell/fitness/core/domain/metric/Transforms.kt) over the range, labeled "since <range start>".
- [`HomeViewModel`](../../feature/home/src/main/kotlin/com/jrussell/fitness/feature/home/HomeViewModel.kt) does the same over Home's own remembered range for the Weight card.
- On Trends, [`MetricChart`](../../core/ui/src/main/kotlin/com/jrussell/fitness/core/ui/chart/MetricChart.kt) gets only the daily weights. It draws them as the solid main line, adds the dotted best-fit trend itself, and builds the spoken summary and the table.
- "Today" comes from [`observeToday`](../../core/domain/src/main/kotlin/com/jrussell/fitness/core/domain/Today.kt), which re-reads the [`WallClock`](../../core/domain/src/main/kotlin/com/jrussell/fitness/core/domain/time/WallClock.kt) every minute, so a screen left open past the day start moves to the new day.

## Journey 5: a restore, end to end

The user opens Settings › Backup, taps **Restore from a file…** and picks `getfit-backup-2026-10-18.db`.

1. **Picking.** [`BackupSettings`](../../feature/settings/src/main/kotlin/com/jrussell/fitness/feature/settings/BackupSettings.kt) launches the system picker (`OpenDocument`) and hands the chosen content URI to [`BackupViewModel.onRestoreFileChosen`](../../feature/settings/src/main/kotlin/com/jrussell/fitness/feature/settings/BackupViewModel.kt). One action runs at a time, so a double tap is ignored.
2. **Inspecting.** [`DefaultBackupRepository.inspect`](../../core/data/src/main/kotlin/com/jrussell/fitness/core/data/backup/DefaultBackupRepository.kt) copies the document to the cache and reads its first 16 bytes:
   - A SQLite header means a database copy. [`inspectSnapshot`](../../core/backup/src/main/kotlin/com/jrussell/fitness/core/backup/snapshot/SnapshotCheck.kt) opens it read-only and checks:
     - `quick_check`;
     - the schema version against `USER_DATABASE_IDENTITY_HASHES`;
     - the rows: day starts, time zones and unit codes.
   - Anything else is decoded as a JSON export and validated.
   - Either way, nothing has changed yet.
3. **Confirming.** The ViewModel shows the preview: "Replace all data? Everything on this phone is replaced with this backup (4 entries, last changed Oct 18). GetFit will restart." For a JSON file it shows "Restore from this file?" instead.
4. **Replacing.** On confirm, the repository writes a safety copy of the current data with `VACUUM INTO` (`files/restore/before-restore.db`), then [`PendingRestore.stage`](../../core/backup/src/main/kotlin/com/jrussell/fitness/core/backup/snapshot/PendingRestore.kt) copies the file to `staged.db.tmp`, flushes it, and renames it to `staged.db`. The result is `RestoreResult.RestartRequired`.
5. **Restarting.** The ViewModel calls [`AppRestarter`](../../app/src/main/kotlin/com/jrussell/fitness/restart/ProcessAppRestarter.kt). It starts [`RestartActivity`](../../app/src/main/kotlin/com/jrussell/fitness/restart/RestartActivity.kt) in the separate `:restart` process, which kills the main process, launches the app again, and exits.
6. **Installing.** In the new process, the first injection of the database runs `DatabaseModule`. It calls `installPendingRestore`, which deletes the old `-wal`/`-shm` files and moves `staged.db` over `user.db`, and only then opens Room. The log records "staged a database restore" and "installed the staged database".

A JSON file takes a different path at step 4: [`BundleMerger`](../../core/data/src/main/kotlin/com/jrussell/fitness/core/data/backup/BundleMerger.kt) merges it in one stamped transaction, Room's flows emit, and Body and Home update with no restart. Settings shows "Restored: 3 added, 1 updated".

## Where the wiring lives

Every binding is in `:app`'s [`di/`](../../app/src/main/kotlin/com/jrussell/fitness/di/) package:

| Module | Provides or binds |
| --- | --- |
| [`CoroutinesModule`](../../app/src/main/kotlin/com/jrussell/fitness/di/CoroutinesModule.kt) | `@IoDispatcher`, `@DefaultDispatcher`, `@ApplicationScope`, `Clock`, `IdGenerator` |
| [`BackupModule`](../../app/src/main/kotlin/com/jrussell/fitness/di/BackupModule.kt) | `BackupRepository` and the worker's `SnapshotJob` (both `DefaultBackupRepository`), `BackupScheduler`, `DocumentStore`, `AppRestarter`, `BackupFiles`, `BackupSettingsDataSource`, `AppVersion`, `Logger` (`FileLogger`) |
| [`DatabaseModule`](../../app/src/main/kotlin/com/jrussell/fitness/di/DatabaseModule.kt) | `GetFitDatabase` (`user.db`), after installing a staged restore |
| [`DataModule`](../../app/src/main/kotlin/com/jrussell/fitness/di/DataModule.kt) | `CurrentOwnerProvider`, `DeviceIdProvider`, `DayBoundaryRepository`, `MeasurementRepository`, `WallClock` |
| [`MetricsModule`](../../app/src/main/kotlin/com/jrussell/fitness/di/MetricsModule.kt) | `MetricRegistry`, and each `MetricSeriesProvider` into a set (`BodyMetricProvider` so far) |
| [`PreferencesModule`](../../app/src/main/kotlin/com/jrussell/fitness/di/PreferencesModule.kt) | The preferences DataStore, `UserPreferencesRepository` and `DateRangeRepository` |
| [`SyncModule`](../../app/src/main/kotlin/com/jrussell/fitness/di/SyncModule.kt) | `SyncGateway` → `NoOpSyncGateway` |

Classes with an `@Inject constructor` (`StampedWriter`, `ReferenceSeeder`, the repositories) need no entry. Hilt builds them from their constructors. Modules only exist for interfaces, qualifiers and things built by factories.

Next: [Data layer](04-data-layer.md).
