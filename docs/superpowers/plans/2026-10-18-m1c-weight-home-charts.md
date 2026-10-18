# M1c — Weight, Home and Charts Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Switch GetFit to the approved four-tab layout (Home, Nutrition, Training, Body). Give Body a working weight log and a trend-weight chart with a table view, built on the metric registry and the shared chart component, and give Home its date-range control and Body card.

**Architecture:**
- **Metric registry:** the contract lives in `:core:domain` (`MetricId`, `MetricDescriptor`, `DataPoint`, `MetricSeriesProvider`, `MetricRegistry`) together with pure transforms: daily bucketing, gap-adjusted trend weight, and range change. `:core:data` implements it with a `BodyMetricProvider` (`body.weight`, `body.trend_weight`) contributed through Hilt multibinding.
- **Chart component:** one composable in `:core:ui` wraps Vico. It always offers a spoken summary and "View as table".
- **Feature wiring:** `:feature:body` (Log / Trends) and `:feature:home` (renamed from `:feature:today`) consume only `:core:domain` interfaces. `:feature:trends` is deleted.

**Tech Stack:** Kotlin 2.4.21, Compose BOM 2026.09.00 (Material 3 `DatePicker`, `SegmentedButton`), Vico 2.x (`compose-m3`), kotlinx-datetime 0.8.0, Hilt 2.60.1 multibinding, Robolectric 4.17 + Compose UI test, Turbine.

**Spec:**
- `docs/superpowers/specs/2026-10-17-navigation-home-design.md`, the approved design. This plan implements its "M1c" slice and its "Spec amendments".
- `docs/initial_design.md`, sections "Metric registry", "Charts", "Energy accounting → Trend weight", "TalkBack and text scaling", "Redundant encoding" and "Code conventions".

This is plan 3 of 4 for M1 (M1a → M1b → **M1c** → M1d). `v0.1.0` comes after M1d.

## Global Constraints

- **Branch:** `m1c-weight-home-charts`, cut from `master` (`a3d2999` or later), fast-forward merged when green. The repo has a GitHub remote; push only when the owner asks.
- **SDK levels:** `minSdk` 34, `targetSdk` 37, `compileSdk` 37. Gradle runs on JDK 25 (CI uses Temurin 25).
- **Kotlin style:** explicit (explicit types, block bodies, named classes), with KDoc that explains why. **American spelling everywhere** (color, gray, behavior).
- **Strings:** "Every user-visible string is a resource, including plurals. Numbers are formatted with locale-aware formatters. Display units are a presentation concern only."
- **Canonical units:** weight is stored in kilograms at full precision, plus `enteredValue` / `enteredUnit` as typed. `KILOGRAMS_PER_POUND = 0.45359237`.
- **"Derived values are never stored":** trend weight is computed at query time.
- **Trend weight:** `T_t = T_{t−Δd} + α_eff·(w_t − T_{t−Δd})` with `α_eff = 1 − (1−α)^Δd`, `α = 0.1` per day, and Δd = days since the previous weigh-in.
- **Charts:**
  - "The UI never calls a charting library directly."
  - Every chart offers a generated summary ("Body weight, 30 days, 84.2 to 82.9 kilograms, decreasing 0.3 kilograms per week") and a "View as table" toggle.
  - Series are never told apart by color alone (dash, marker or direct label).
- **Dependency rules:**
  - features depend only on `:core:domain`, `:core:model`, `:core:ui` and `:core:designsystem`;
  - ViewModels take only `:core:domain` interfaces (enforced by Konsist);
  - no feature depends on another; cross-feature navigation goes through `:app` callbacks.
- **Design-doc decisions:**
  - Home is read-only and opens on Today;
  - range presets are Today, 7 days, 30 days and "From…";
  - charts on Today show the last 7 days;
  - Body range figures use CHANGE (start → now trend, rate per week);
  - Home cards for domains with no data are hidden.
- **Robolectric:** each new module with Robolectric tests gets `src/test/resources/robolectric.properties` with `sdk=36`.

## Calls made in this plan

1. **Weight unit preference:** `MassUnit { POUND, KILOGRAM }` lives in `UserPreferences` (DataStore, key `mass_unit`), with Settings → Units → Weight offering Pounds / Kilograms. When the key is absent, the default comes from the device locale: `POUND` for country US, LR or MM, otherwise `KILOGRAM`.
2. **A chosen date wins over the day-start rule.** `ManualMeasurement` gains `localDateOverride: LocalDate?` (default null). When the user picks a date in the entry dialog, the entry is filed under that date even if its time falls before the day start. Leaving the defaults ("now") passes null, and the day-start rule applies.
3. **One value per day:** the daily weight is the LAST reading of each local date, by `startTime`, matching `MeasurementType.dailyAggregation = LAST`. Trend weight has a value only on days with a weigh-in, and is computed over **all** history, then sliced to the range, so a range's first value is never a cold start.
4. **Valid weights** are 20–400 kg (≈ 44–882 lb) after conversion. Input is parsed locale-aware (`82,4` in a German locale). An empty, non-numeric, zero, negative or out-of-range value shows a field error and saves nothing.
5. **No editing yet.** M1c offers add and delete (with a confirmation dialog). Editing an entry and undo arrive with the first list that needs bulk actions (M3/M4).
6. **Time is behind a domain interface.** `WallClock { now(); timeZone() }` lives in `:core:domain`, with `SystemWallClock` in `:core:data` bound in `:app` and `FakeWallClock` in `:core:testing`. ViewModels may take only domain interfaces, and today's date depends on the zone.
7. **Range selection is shared domain logic.** `DateRangeSelection` (Today, LastDays(n), From(date)) and its resolution live in `:core:domain`. The `RangeChips` composable lives in `:core:ui`, so Home and Body share both.
8. **Metric display names** are string resources in `:core:data` (`MetricDescriptor.displayNameResource`), as the spec's contract says.
9. **Bucketing:** `Bucket.WEEK` and `Bucket.MONTH` are implemented through the descriptor's `dailyAggregation` (LAST, MEAN, SUM, MIN, MAX), as a pure function. M1c's charts use `DAY`.
10. **Launch-window background stays deferred.** Making it match High contrast needs a synchronous preference read at cold start, which the design rules out. The cost is a brief background tone shift on cold start in High contrast only.
11. **Dependency Analysis keeps unused dependencies at "warn".** Nutrition and Training are still empty, so their standard dependencies are unused.

## Review Focus

1. **Typed input in any form.** `82,4` in a comma-decimal locale saves 82.4. An empty field, `abc`, `0`, `-5` or `9999` shows a field error and saves nothing. Pinned in Task 7 (`weightInput_*` tests).
2. **Backfilled entries land on the chosen day.** An entry dated Oct 10 at 01:30 with a 04:00 day start is filed under Oct 10, not Oct 9. Pinned in Task 2 (`addManual_localDateOverride_winsOverDayStart`) and Task 7 (`onSaveWeight_pickedDate_passesOverride`).
3. **Switching units never changes stored data.** 181.7 lb stores 82.418… kg, and is shown as 181.7 lb in pounds and 82.4 kg in kilograms. Pinned in Task 3 (`mass_poundsRoundTrip`) and Task 7 (`history_rendersInPreferredUnit`).
4. **Sparse and messy history.**
   - Several weigh-ins on one day use the last one.
   - A 5-day gap uses `α_eff = 1 − 0.9⁵`.
   - A single weigh-in gives a single-point chart and a "not enough data" change figure.
   - No weigh-ins at all gives empty states, not crashes.

   Pinned in Task 3 (`trendWeight_*`, `rangeChange_*`), Task 4 (`series_sameDay_usesLast`) and Tasks 6–7 (empty-state tests).
5. **A future "From…" date.** A start date after today resolves to today (a one-day range), never an inverted range. Pinned in Task 3 (`dateRangeSelection_futureStart_clampsToToday`).

## File Structure

```
docs/initial_design.md                                   spec amendments (Task 1)
feature/home/  (renamed from feature/today)              HomeKey, HomeScreen, HomeViewModel, HomeEntry
feature/trends/                                          deleted
feature/body/.../{BodyScreen,BodyViewModel,BodyUiState,WeightEntryDialog,WeightInput,BodyEntry}.kt
feature/settings/.../{SettingsScreen,SettingsViewModel}.kt   + Units section, null-until-loaded
core/model/.../{Mass,MassUnit,UserPreferences}.kt
core/domain/.../time/WallClock.kt, .../range/DateRangeSelection.kt
core/domain/.../metric/{MetricId,MetricDescriptor,DataPoint,Aggregation,Bucket,UnitOfMeasure,MetricCategory,
                        MetricSeriesProvider,MetricRegistry,Bucketing,TrendWeight,RangeChange}.kt
core/data/.../metric/{DefaultMetricRegistry,BodyMetricProvider}.kt, .../time/SystemWallClock.kt
core/data/src/main/res/values/strings.xml                metric display names
core/ui/.../chart/{MetricChart,ChartSeries,ChartSummary,ChartTable}.kt, .../range/RangeChips.kt
core/testing/.../{FakeWallClock,FakeMeasurementRepository,FakeDayBoundaryRepository,FakeMetricRegistry}.kt
app/.../di/{MetricsModule,DataModule(+WallClock)}.kt, navigation/TopLevelDestination.kt, ui/GetFitApp.kt
core/designsystem/src/main/res/drawable/ic_home{,_selected}.xml
docs/developer/*                                         guide update (Task 8)
```

---

### Task 1: Four tabs, Home module, spec amendments, shell minors

**Files:**
- **Move:** `feature/today/` → `feature/home/`. The package becomes `com.jrussell.fitness.feature.home`; `TodayKey`/`TodayScreen`/`todayEntry` become `HomeKey`/`HomeScreen`/`homeEntry`; the string `feature_today_title` becomes `feature_home_title` = "Home".
- **Delete:** `feature/trends/`; `ic_today*.xml` and `ic_trends*.xml` (with their `GetFitIcons` entries).
- **Create:** `ic_home.xml` and `ic_home_selected.xml`, the Material Symbols Outlined 24 px "home" drawables (outlined, and fill 1), with `android:tint` removed as in M1a.
- **Modify:**
  - `settings.gradle.kts`, `app/build.gradle.kts`;
  - `TopLevelDestination.kt` (HOME, NUTRITION, TRAINING, BODY);
  - `MainViewModel.kt` (start key `HomeKey`);
  - `GetFitApp.kt` (entry provider; `homeEntry(onOpenBody = { navigation.switchTo(BodyKey) })`);
  - `GetFitIcons.kt` and `GetFitIconsTest`;
  - `LayerRulesTest.featureNames`;
  - `MainActivity.kt` (drop the empty `onDispose { }` by using `SideEffect` for the system-bar style);
  - the M1a explicit-type slips: give `MIN_ROW_HEIGHT`, the Compose test-rule `val`s and the untyped lambda parameters in `:feature:settings` and `:app` explicit types;
  - `docs/initial_design.md`.
- **Test:** `app/src/test/.../TopLevelBackStackTest.kt` (add 1), `TopLevelDestinationTest.kt` (new).

**Interfaces:**
- Produces:
  - `@Serializable data object HomeKey : NavKey`;
  - `fun EntryProviderScope<NavKey>.homeEntry(onOpenBody: () -> Unit)`;
  - `TopLevelDestination.entries == [HOME, NUTRITION, TRAINING, BODY]`;
  - `GetFitIcons.Home` / `HomeSelected`.

- [ ] **Step 1:** Run `git checkout -b m1c-weight-home-charts`.
- [ ] **Step 2: Write the tests.**
  - `TopLevelDestinationTest.tabs_areHomeNutritionTrainingBody_inOrder` asserts the entries' keys equal `[HomeKey, NutritionKey, TrainingKey, BodyKey]`.
  - `TopLevelBackStackTest.push_closesOverlay`:
    1. `openOverlay("settings")`, then `push("detail")`;
    2. assert `overlay == null` and `backStack.last() == "detail"`.

    This closes an M1a leftover.
- [ ] **Step 3:** Run `./gradlew :app:testDebugUnitTest`. Expected: `TopLevelDestinationTest` fails to compile (`HomeKey` unresolved). `push_closesOverlay` passes, which documents existing behavior; record that in the ledger.
- [ ] **Step 4:** Do the move, the deletions and the wiring. Search for every remaining `today`/`Today`/`trends`/`Trends` reference outside `docs/superpowers/` and remove it (`git grep -n -i "feature.today\|TodayKey\|trends"`).
- [ ] **Step 5: Amend `docs/initial_design.md`** as listed under the design doc's "Spec amendments on approval":
  - decision #30, and new decision #38 ("Navigation and Home: four tabs, read-only Home with range presets and the cross-domain chart builder, Log/Trends(/Plan) work tabs; see `docs/superpowers/specs/2026-10-17-navigation-home-design.md`");
  - the module inventory;
  - "Interaction patterns → Navigation";
  - "Charts";
  - `MetricTarget.period`, plus the `SavedChart` and `HomePinnedMeasurement` tables marked "(M2)";
  - "Daily budget" ("Home's Energy card shows");
  - milestones M1c and M6;
  - the Visual conventions typeface row, which becomes "Inter (decision #31), bundled; …" (an M1a leftover).
- [ ] **Step 6:** Run `./gradlew check assembleDebug`. Expected: green, with `TopLevelDestinationTest` passing.
- [ ] **Step 7: Emulator check.** Four tabs show; the app opens on Home; back from Body returns to Home; the Settings overlay still works.
- [ ] **Step 8:** Commit: `feat: four-tab navigation (Home, Nutrition, Training, Body); :feature:today becomes :feature:home; remove :feature:trends; spec amendments`.

### Task 2: Data-layer additions and M1b leftovers

**Files:**
- **Modify:**
  - `core/domain/.../ManualMeasurement.kt` (`localDateOverride`);
  - `core/data/.../repository/DefaultMeasurementRepository.kt`;
  - `DefaultDayBoundaryRepository.kt`.
- **Create:** `core/domain/.../time/WallClock.kt`, `core/data/.../time/SystemWallClock.kt`, and in `core/testing/`: `FakeWallClock.kt`, `FakeMeasurementRepository.kt`, `FakeDayBoundaryRepository.kt`.
- **Test:**
  - `DefaultMeasurementRepositoryTest` (add 3);
  - `DefaultDayBoundaryRepositoryTest` (add 1);
  - `DayAssignmentTest` (add 1);
  - `core/testing/src/test/.../FakeMeasurementRepositoryTest.kt` (JUnit 5).

**Interfaces:**
- Produces:
  - `ManualMeasurement(…, localDateOverride: LocalDate? = null)`;
  - `interface WallClock { fun now(): Instant; fun timeZone(): TimeZone }`;
  - `class SystemWallClock @Inject constructor(clock: Clock) : WallClock`, where the zone is `TimeZone.currentSystemDefault()`, read on every call;
  - `FakeWallClock(var now: Instant, var zone: TimeZone)`;
  - `FakeMeasurementRepository`: an in-memory `MeasurementRepository`. It mirrors the real validation (non-finite value or enteredValue → `Validation`; unknown key → `NotFound`, with `"weight"` known by default), exposes `val measurements: List<Measurement>`, and `observeMeasurements` emits on every change;
  - `FakeDayBoundaryRepository(dayStartMinutes: Int = 0)`.

- [ ] **Step 1: Write the tests.**
  - `addManual_localDateOverride_winsOverDayStart`: day start 240; taken at 2026-10-10T01:30 Toronto; override `LocalDate(2026,10,10)`. The stored `localDate` is Oct 10.
  - `addManual_nonFiniteEnteredValue_returnsValidation` (an M1b leftover): `enteredValue = Double.NaN` gives `Validation("enteredValue", …)` and nothing is stored.
  - `addManual_dayStartChangedOnCalendarDate_usesSettingInForceThatDay` (an M1b leftover): start 0 from 2026-01-01 and 240 from 2026-10-17. Taken at 2026-10-17T02:00 → Oct 16; taken at 2026-10-16T02:00 → Oct 16.
  - `observeDayStartMinutes_identicalWrites_doNotReEmit` (an M1b leftover): Turbine shows one emission of 240 after `setDayStart(240, d)` twice.
  - `DayAssignmentTest.localDateFor_fallBackRepeatedHour_bothBeforeStartArePreviousDay` (an M1b leftover): America/Toronto on 2026-11-01. Both 01:30 instants (EDT and EST) with start 240 → 2026-10-31.
  - `FakeMeasurementRepositoryTest`: add then observe returns it; NaN is rejected.
- [ ] **Step 2:** Run `./gradlew :core:data:testDebugUnitTest :core:domain:test :core:testing:test`. Expected: compile failures, then (after stubbing) the new assertions fail.
- [ ] **Step 3: Implement.**
  - The override: when non-null it is the `localDate`, skipping the day-start lookup.
  - `enteredValue` validation.
  - `.distinctUntilChanged()` on `observeDayStartMinutes`.
  - The `WallClock` types and the fakes.
  - Add `testImplementation(libs.turbine)` to `:core:testing` only if its test needs it.
- [ ] **Step 4:** Run the same command. Expected: all pass. The fall-back test may pass immediately, since it pins existing behavior; if so, ledger it.
- [ ] **Step 5:** Commit: `feat(data): chosen-date override, WallClock, repository fakes; validate enteredValue; distinct day-start emissions`.

### Task 3: Units, range selection and the pure metric transforms

**Files:**
- **Create:**
  - `core/model/.../{Mass,MassUnit}.kt`;
  - `core/domain/.../range/DateRangeSelection.kt`;
  - `core/domain/.../metric/{MetricId,MetricDescriptor,DataPoint,Aggregation,Bucket,UnitOfMeasure,MetricCategory,MetricSeriesProvider,MetricRegistry,Bucketing,TrendWeight,RangeChange}.kt`.
- **Test** (JUnit 5): `core/model/src/test/.../MassTest.kt`, `core/domain/src/test/.../range/DateRangeSelectionTest.kt`, `.../metric/{BucketingTest,TrendWeightTest,RangeChangeTest}.kt`.

**Interfaces:**
- Produces:
  - `@JvmInline value class Mass private constructor(val kilograms: Double)`, with `ofKilograms`, `ofPounds`, `toPounds()`, `in(unit: MassUnit): Double` and `KILOGRAMS_PER_POUND = 0.45359237`;
  - `enum class MassUnit { POUND, KILOGRAM }`.
  - `sealed interface DateRangeSelection { data object Today; data class LastDays(val days: Int); data class From(val start: LocalDate) }`, with:
    - `fun DateRangeSelection.cardRange(today: LocalDate): LocalDateRange`: Today → today..today; LastDays(n) → today−(n−1)..today; From → min(start, today)..today;
    - `fun DateRangeSelection.chartRange(today: LocalDate): LocalDateRange`: as `cardRange`, except Today → LastDays(7).
    - Use `kotlinx.datetime.LocalDateRange` if 0.8.0 provides it; otherwise define `data class LocalDateRange(val start: LocalDate, val endInclusive: LocalDate)` in `core.domain.range` and ledger the choice.
  - The spec's contract, verbatim:
    - `MetricId(value: String)`;
    - `enum Aggregation { SUM, MEAN, LAST, MIN, MAX }`;
    - `enum Bucket { DAY, WEEK, MONTH }`;
    - `MetricDescriptor(id, displayNameResource: Int, canonicalUnit: UnitOfMeasure, dailyAggregation: Aggregation, category: MetricCategory, higherIsBetter: Boolean?)`;
    - `DataPoint(date: LocalDate, value: Double, coverage: Double?)`;
    - `interface MetricSeriesProvider { fun descriptors(): List<MetricDescriptor>; fun series(metricId: MetricId, range: LocalDateRange, bucket: Bucket): Flow<List<DataPoint>> }`;
    - `interface MetricRegistry { fun allDescriptors(): List<MetricDescriptor>; fun series(metricId: MetricId, range: LocalDateRange, bucket: Bucket): Flow<List<DataPoint>> }`.
    - M1c adds `fun descriptor(metricId: MetricId): MetricDescriptor?` to `MetricRegistry`.
  - `enum UnitOfMeasure { KILOGRAM }` (grows per milestone) and `enum MetricCategory { NUTRITION, HYDRATION, TRAINING, BODY, SLEEP, ENERGY, ADHERENCE, PROJECTION }`.
  - `object MetricIds { val BODY_WEIGHT = MetricId("body.weight"); val BODY_TREND_WEIGHT = MetricId("body.trend_weight") }`.
  - `fun bucketize(daily: List<DataPoint>, bucket: Bucket, aggregation: Aggregation): List<DataPoint>`. A WEEK bucket is ISO Monday–Sunday and is dated its Monday; a MONTH bucket is dated the 1st; the coverage is the mean of the non-null coverages, or null.
  - `fun trendWeight(daily: List<DataPoint>, alpha: Double = 0.1): List<DataPoint>`. The input is one point per date, ascending; the output has one point per input date; the first trend equals the first weight.
  - `data class RangeChange(val start: Double, val end: Double, val perWeek: Double)` and `fun rangeChange(trend: List<DataPoint>, range: LocalDateRange): RangeChange?`:
    - the start is the last trend point on or before `range.start`, else the first point in the range;
    - the end is the last point on or before `range.endInclusive`;
    - `perWeek = (end − start) / days(startDate, endDate) × 7`;
    - it returns null when there are fewer than 2 distinct dates or a span of 0 days.

- [ ] **Step 1: Write the tests.**
  - **`MassTest`:**
    - `mass_poundsRoundTrip`: `Mass.ofPounds(181.7).kilograms` ≈ 82.41824 (±1e-5), and `.toPounds()` ≈ 181.7 (±1e-9);
    - `mass_inUnit` covers both units.
  - **`DateRangeSelectionTest`** (today = 2026-10-17):
    - Today: card 17..17, chart 11..17;
    - LastDays(30): card 2026-09-18..17;
    - `dateRangeSelection_futureStart_clampsToToday`: From(2026-10-20) → 17..17;
    - From(2026-10-01): card 01..17.
  - **`BucketingTest`:**
    - `bucketize_day_isIdentity`;
    - `bucketize_week_last_usesLastDayOfIsoWeek`: points on Mon 12th and Thu 15th → one point dated 2026-10-12 with Thursday's value;
    - `bucketize_month_mean`.
  - **`TrendWeightTest`:**
    - `trendWeight_firstPoint_equalsWeight`;
    - `trendWeight_consecutiveDays_usesAlpha`: weights 100, 101 on consecutive days → 100.0, 100.1;
    - `trendWeight_gap_usesEffectiveAlpha`: 100 on day 0, 105 on day 5 → 100 + (1 − 0.9⁵)·5 = 102.04755 (±1e-5);
    - `trendWeight_empty_isEmpty`.
  - **`RangeChangeTest`:**
    - `rangeChange_usesPointBeforeRangeStart`;
    - `rangeChange_perWeek`: 100.0 on Oct 1 and 99.0 on Oct 15 → −0.5;
    - `rangeChange_singlePoint_isNull`;
    - `rangeChange_noPoints_isNull`.
- [ ] **Step 2:** Run `./gradlew :core:model:test :core:domain:test`. Expected: compilation fails.
- [ ] **Step 3:** Implement everything listed under Produces. Trend weight follows the Global Constraints formula exactly.
- [ ] **Step 4:** Run the same command. Expected: all pass.
- [ ] **Step 5:** Commit: `feat(domain): metric registry contract, bucketing, gap-adjusted trend weight, range change, Mass and date-range selection`.

### Task 4: Metric registry implementation and the body provider

**Files:**
- **Create:**
  - `core/data/.../metric/{DefaultMetricRegistry,BodyMetricProvider}.kt`;
  - `core/data/src/main/res/values/strings.xml` (`metric_body_weight` = "Weight", `metric_body_trend_weight` = "Trend weight");
  - `core/testing/.../FakeMetricRegistry.kt`;
  - `app/.../di/MetricsModule.kt`.
- **Modify:** `app/.../di/DataModule.kt` (bind `WallClock` → `SystemWallClock`).
- **Test:** `core/data/src/test/.../metric/{BodyMetricProviderTest,DefaultMetricRegistryTest}.kt`.

**Interfaces:**
- Consumes: Task 3's contract and transforms; M1b's `MeasurementRepository`.
- Produces:
  - `class BodyMetricProvider @Inject constructor(measurements: MeasurementRepository) : MetricSeriesProvider`. Its descriptors are:
    - `body.weight`: KILOGRAM, LAST, BODY, `higherIsBetter = null`;
    - `body.trend_weight`: the same.

    For `body.weight` it observes weights over the requested range and keeps the LAST reading per `localDate` by `startTime`. For `body.trend_weight` it observes from `LocalDate.fromEpochDays(0)` to `range.endInclusive`, computes `trendWeight` and slices it to the range. Then it calls `bucketize`.
  - `class DefaultMetricRegistry @Inject constructor(providers: Set<@JvmSuppressWildcards MetricSeriesProvider>) : MetricRegistry`. It routes by descriptor ID; an unknown ID emits `emptyList()`; two providers claiming one ID throw `IllegalStateException` at construction (a programming error).
  - `MetricsModule` contributes `@Binds @IntoSet BodyMetricProvider` and binds `MetricRegistry`.
  - `FakeMetricRegistry(descriptors, series: MutableMap<MetricId, MutableStateFlow<List<DataPoint>>>)`.

- [ ] **Step 1: Write the tests** (Robolectric with `RepositoryTestHarness`, or `FakeMeasurementRepository` where no Room is needed):
  - `series_sameDay_usesLast`: 08:00 → 82.0 and 20:00 → 83.0 on one day give one point of 83.0;
  - `trend_rangeStartsMidHistory_isWarm`: weights from Oct 1 to 15 with a range of Oct 10–15 → the first trend point equals the full-history trend on Oct 10;
  - `series_emptyHistory_isEmpty`;
  - `registry_unknownMetric_emitsEmpty`;
  - `registry_duplicateIds_throws`.
- [ ] **Step 2:** Run `./gradlew :core:data:testDebugUnitTest`. Expected: FAIL (unresolved).
- [ ] **Step 3:** Implement the provider, the registry, the fake, `MetricsModule` and the `WallClock` binding.
- [ ] **Step 4:** Run `./gradlew :core:data:testDebugUnitTest :app:assembleDebug`. Expected: tests pass and the Hilt graph builds.
- [ ] **Step 5:** Commit: `feat(data): metric registry with body weight and trend weight providers`.

### Task 5: Chart component and range chips in `:core:ui`

**Files:**
- **Modify:**
  - `gradle/libs.versions.toml`: add `vico`, the latest stable 2.x release at execution time (≥ 2 weeks old); ledger the version. Add `vico-compose-m3 = { group = "com.patrykandpatrick.vico", name = "compose-m3" }`.
  - `core/ui/build.gradle.kts`: depend on vico, `:core:domain` and `:core:designsystem`.
- **Create:**
  - `core/ui/.../chart/{ChartSeries,ChartSummary,MetricChart,ChartTable}.kt`;
  - `core/ui/.../range/RangeChips.kt`;
  - `core/ui/src/main/res/values/strings.xml`;
  - `core/ui/src/test/resources/robolectric.properties`.
- **Test:** `core/ui/src/test/.../chart/{ChartSummaryTest,MetricChartSemanticsTest}.kt`, `.../range/RangeChipsTest.kt`.

**Interfaces:**
- Consumes: `DataPoint`, `DateRangeSelection` and `LocalDateRange` (Task 3); `GetFitTheme.dataColors`.
- Produces:
  - `data class ChartSeries(val label: String, val unitSymbol: String, val unitSpoken: String, val points: List<DataPoint>, val style: SeriesStyle)`. The values are already in display units.
  - `enum SeriesStyle { LINE, POINTS }`.
  - `data class ChartSummaryData(val spanDays: Int, val first: Double, val last: Double, val perWeek: Double?)` and `fun chartSummaryData(points: List<DataPoint>, range: LocalDateRange): ChartSummaryData?` (null when there are no points).
  - `@Composable fun MetricChart(title: String, series: List<ChartSeries>, range: LocalDateRange, formatValue: (Double) -> String, modifier: Modifier = Modifier)`. It:
    - renders with Vico: `LINE` as a solid line, `POINTS` as unconnected markers;
    - colors series from `seriesPalette` in order, with a direct end label on LINE series for redundant encoding;
    - sets the chart's `contentDescription` to the spoken summary of the **last** series (the trend). The format is the spec's: "Weight, 30 days, 212.4 to 209.9 pounds, decreasing 1.2 pounds per week". The words are resources; "increasing", "decreasing" and "steady" are plurals-aware where needed; `perWeek` within ±0.05 counts as steady;
    - has a "View as table" / "View as chart" `TextButton` that toggles `ChartTable`, with one row per date (newest first) and one column per series;
    - shows a "No data in this range" message when every series is empty.
  - `@Composable fun RangeChips(selection: DateRangeSelection, onSelect: (DateRangeSelection) -> Unit, today: LocalDate)`. It has chips for Today, 7 days, 30 days and "From…". "From…" opens a Material 3 `DatePickerDialog` whose `selectableDates` excludes dates after `today`. Each chip has `Role.RadioButton` semantics with its selected state.

- [ ] **Step 1: Write the tests.**
  - `ChartSummaryTest` (JUnit 4):
    - `summary_thirtyDays_firstLastPerWeek`;
    - `summary_noPoints_isNull`;
    - `summary_singlePoint_hasNullPerWeek`.
  - `MetricChartSemanticsTest` (Robolectric + Compose v2 rule):
    - `chart_exposesSpokenSummary`: the node with the summary content description contains "Weight, 30 days";
    - `viewAsTable_togglesTableRows`: clicking "View as table" shows row text for the latest date and value; clicking "View as chart" hides it;
    - `emptySeries_showsNoDataMessage`.
  - `RangeChipsTest`: `chips_exposeRadioRoleAndSelection`, and clicking "7 days" calls `onSelect(LastDays(7))`.
- [ ] **Step 2:** Run `./gradlew :core:ui:testDebugUnitTest`. Expected: FAIL (unresolved).
- [ ] **Step 3:** Implement. Vico usage stays inside `MetricChart.kt`; no other file imports Vico. Add a `@Preview` for light/dark and font scale on `MetricChart`.
- [ ] **Step 4:** Run `./gradlew :core:ui:testDebugUnitTest :core:ui:check buildHealth`. Expected: green. Apply any `api`/`implementation` advice.
- [ ] **Step 5:** Commit: `feat(ui): MetricChart (Vico) with spoken summary and table view; shared RangeChips`.

### Task 6: Units setting, Settings first-frame fix, Home with the Body card

**Files:**
- **Modify:**
  - `core/model/.../UserPreferences.kt` (`massUnit: MassUnit`, with `DEFAULT.massUnit = KILOGRAM`);
  - `core/domain/.../UserPreferencesRepository.kt` (`setMassUnit`);
  - `core/datastore/.../UserPreferencesDataSource.kt` (key `mass_unit`, plus a `defaultMassUnit: MassUnit` constructor parameter used when the key is absent or unreadable);
  - `core/data/.../DefaultUserPreferencesRepository.kt`;
  - `core/testing/.../FakeUserPreferencesRepository.kt`;
  - `app/.../di/PreferencesModule.kt` (the locale default: `POUND` if `Locale.getDefault().country` is US, LR or MM);
  - `feature/settings/.../{SettingsViewModel,SettingsScreen}.kt` and the strings;
  - `feature/home/.../{HomeScreen,HomeEntry}.kt`.
- **Create:** `feature/home/.../{HomeViewModel,HomeUiState}.kt` and `feature/home/src/test/resources/robolectric.properties`.
- **Test:**
  - `UserPreferencesDataSourceTest` (add 2);
  - `SettingsViewModelTest` (add 2);
  - `SettingsScreenSemanticsTest` (add 1);
  - `feature/home/src/test/.../{HomeViewModelTest,HomeScreenSemanticsTest}.kt`.

**Interfaces:**
- Produces:
  - `UserPreferences(themeMode, palette, massUnit)` and `suspend fun setMassUnit(unit: MassUnit)`.
  - `SettingsViewModel.uiState: StateFlow<UserPreferences?>`: null until loaded, with nothing rendered until then. This fixes the M1a first-frame flash.
  - `@HiltViewModel class HomeViewModel @Inject constructor(metrics: MetricRegistry, measurements: MeasurementRepository, preferences: UserPreferencesRepository, dayBoundary: DayBoundaryRepository, clock: WallClock)` with `uiState: StateFlow<HomeUiState>` and `onRangeSelected(selection)`.
  - `HomeUiState`: today, the selection, and `body: BodyCard?` (null when there are no weigh-ins, which hides the card). `BodyCard` is either:
    - `Today(latest, latestDate, trend)`, or
    - `Range(change: RangeChange?)`, where null renders "Not enough data yet".
  - `homeEntry(onOpenBody)`: tapping the Body card calls `onOpenBody`.

- [ ] **Step 1: Write the tests.**
  - `UserPreferencesDataSourceTest`:
    - `massUnit_absent_usesInjectedDefault` (`POUND` injected → POUND);
    - `setMassUnit_persists`.
  - `SettingsViewModelTest`:
    - `uiState_beforeLoad_isNull`, using a never-emitting fake flow;
    - `onMassUnitSelected_writesRepository`.
  - `SettingsScreenSemanticsTest.units_exposeRadioRoleAndSelection`.
  - `HomeViewModelTest`:
    - `noWeighIns_hidesBodyCard`;
    - `today_showsLatestAndTrend`;
    - `rangeThirtyDays_showsChange`;
    - `rangeWithSingleWeighIn_showsNotEnoughData`;
    - `values_followPreferredUnit`.
  - `HomeScreenSemanticsTest`:
    - `rangeChips_present`;
    - `bodyCard_click_callsOnOpenBody`;
    - `bodyCard_mergesSemantics`: one announcement, e.g. "Weight 212.4 pounds, trend 213.1 pounds".
- [ ] **Step 2:** Run `./gradlew :core:datastore:testDebugUnitTest :feature:settings:testDebugUnitTest :feature:home:testDebugUnitTest`. Expected: FAIL.
- [ ] **Step 3: Implement.**
  - Settings gets a "Units" section with the radio group "Weight: Pounds, Kilograms".
  - Home shows `RangeChips`, then the Body card. Today mode shows "212.4 lb, trend 213.1 lb". Range mode shows "215.0 → 213.1 lb, −1.1 lb/week".
  - A read-only empty state ("Your summary appears here as you log") shows when no card is visible.
- [ ] **Step 4:** Run the Step 2 command plus `:architecture-tests:test`. Expected: green.
- [ ] **Step 5:** Commit: `feat: weight unit preference, Home with range control and Body card; Settings renders only once loaded`.

### Task 7: Body tab: Log and Trends

**Files:**
- **Create:** in `feature/body/.../`: `BodyViewModel.kt`, `BodyUiState.kt`, `WeightInput.kt`, `WeightEntryDialog.kt`, `BodyLogContent.kt`, `BodyTrendsContent.kt`; plus `feature/body/src/test/resources/robolectric.properties`.
- **Modify:** `BodyScreen.kt`, `BodyEntry.kt` and `feature/body/src/main/res/values/strings.xml`.
- **Test:** `feature/body/src/test/.../{WeightInputTest,BodyViewModelTest,BodyScreenSemanticsTest}.kt`.

**Interfaces:**
- Consumes:
  - `MeasurementRepository` and `ManualMeasurement(localDateOverride)`;
  - `MetricRegistry`, `MetricIds`, `trendWeight`/`rangeChange` (via the registry);
  - `UserPreferencesRepository.userPreferences.massUnit` (from Task 6);
  - `DayBoundaryRepository`, `WallClock`, `DateRangeSelection`, `MetricChart`, `RangeChips`.
- Produces:
  - `fun parseWeightInput(text: String, unit: MassUnit, locale: Locale): WeightInputResult`, where `sealed interface WeightInputResult { data class Valid(val mass: Mass, val entered: Double); data class Invalid(val reason: WeightInputError) }` and `enum WeightInputError { EMPTY, NOT_A_NUMBER, OUT_OF_RANGE }`. Parsing uses `NumberFormat.getInstance(locale)` with full-string consumption; valid weights are 20–400 kg.
  - `@HiltViewModel class BodyViewModel @Inject constructor(measurements: MeasurementRepository, metrics: MetricRegistry, preferences: UserPreferencesRepository, dayBoundary: DayBoundaryRepository, clock: WallClock)`, exposing:
    - `uiState: StateFlow<BodyUiState>`, holding: today's date, the mass unit, the latest weight and its date, the history (newest first, at most 90 days), the trends selection, chart series in display units, and the entry dialog state;
    - `onSaveWeight(text: String, pickedDate: LocalDate?, pickedTime: LocalTime?)`, which passes `localDateOverride = pickedDate` only when the user changed the date;
    - `onDelete(id: MeasurementId)`;
    - `onRangeSelected(selection: DateRangeSelection)`.
  - `BodyScreen`: a `SingleChoiceSegmentedButtonRow` with Log / Trends. The selection is saved with `rememberSaveable`, so it survives tab switches and recreation.

- [ ] **Step 1: Write the tests.**
  - **`WeightInputTest`:**
    - `weightInput_usDecimal_parses` ("181.7", POUND, US → 82.418 kg, entered 181.7);
    - `weightInput_germanComma_parses` ("82,4", KILOGRAM, GERMANY → 82.4);
    - `weightInput_empty_isEmpty`;
    - `weightInput_letters_isNotANumber`;
    - `weightInput_zero_isOutOfRange`;
    - `weightInput_negative_isOutOfRange`;
    - `weightInput_huge_isOutOfRange` ("9999");
    - `weightInput_trailingGarbage_isNotANumber` ("82.4kg").
  - **`BodyViewModelTest`**, using the fakes and `MainDispatcherRule`, with `FakeWallClock` at 2026-10-17T12:00Z in Toronto:
    - `onSaveWeight_valid_addsKilogramsWithEnteredUnit`;
    - `onSaveWeight_invalid_setsFieldErrorAndSavesNothing`;
    - `onSaveWeight_pickedDate_passesOverride`;
    - `onSaveWeight_defaultNow_passesNoOverride`;
    - `history_rendersInPreferredUnit`: 82.418 kg shows as 181.7 lb, then switching the preference to KILOGRAM shows 82.4 kg, with the stored value unchanged;
    - `trends_todaySelection_chartsLastSevenDays`;
    - `emptyHistory_hasNoLatestAndEmptyChart`.
  - **`BodyScreenSemanticsTest`:**
    - `logTrends_exposeSelection`;
    - `addWeight_dialogFieldError_isAnnounced`: the error text is in the field's `error` semantics.
- [ ] **Step 2:** Run `./gradlew :feature:body:testDebugUnitTest`. Expected: FAIL.
- [ ] **Step 3: Implement.**
  - **Log view:**
    - a latest-weight card ("212.4 lb · today", or the date if it isn't today);
    - "+ Add weight", which opens `WeightEntryDialog`: a numeric field in the preferred unit, plus date and time rows that default to now and open Material 3 date and time pickers. Future dates are not selectable.
    - the history list, where each row has a delete action with a confirmation dialog and a `customActions` entry for TalkBack.
  - **Trends view:** `RangeChips`, then `MetricChart` with two series:
    - weigh-ins as `POINTS`, labeled "Weight";
    - trend as `LINE`, labeled "Trend".
  - Values are formatted with a locale-aware formatter to 1 decimal place.
- [ ] **Step 4:** Run `./gradlew :feature:body:testDebugUnitTest :architecture-tests:test`. Expected: green. The Konsist ViewModel rule passes because every constructor parameter is a `:core:domain` interface.
- [ ] **Step 5:** Commit: `feat(body): weight log (add, history, delete) and trend-weight chart with table view`.

### Task 8: Final verification, docs, review and merge

- [ ] **Step 1:** Run `./gradlew check buildHealth assembleDebug assembleRelease`. Expected: green.
- [ ] **Step 2: Emulator walkthrough** (cleared debug data).
  - **Weights:** enter three weights on different days, including one backdated with a time before the day start, and one in kg after switching units. Check that:
    - the Log list, the latest card and Home's Body card agree;
    - the Trends chart shows dots and a trend line;
    - "View as table" lists the rows;
    - the 7-day and 30-day ranges update Home;
    - delete asks for confirmation.
  - **TalkBack pass** (an M1a leftover): enable it with `adb shell settings put secure enabled_accessibility_services com.google.android.marvin.talkback/com.google.android.marvin.talkback.TalkBackService` if the image has TalkBack. Check that:
    - the tabs, range chips, Body card, chart summary and delete action are announced sensibly;
    - text holds up at 200% font (`adb shell settings put system font_scale 2.0`).

    If the image has no TalkBack, ledger it and leave the check for the owner's device.
- [ ] **Step 3: Developer guide** (per the docs-per-merge rule): update `docs/developer/` for everything this plan adds or removes:
  - the module inventory (`:feature:home`, no `:feature:trends`, `:core:ui` charts);
  - the runtime tour, with a new journey: "logging a weight" through to Home and the chart;
  - the data layer (registry, providers, `WallClock`, chosen-date override);
  - the design system (chart redundant encoding);
  - Vico in `stack.md`, with the version.

  Also update the README roadmap (M1c done) and the developer guide's status line.
- [ ] **Step 4:** Final whole-branch review: a fresh reviewer on the most capable model, given this plan's Review Focus and the ledger's rulings. Fix Critical and Important findings test-first in one pass; ledger the minors.
- [ ] **Step 5:** Fast-forward merge into `master` (no tag). Delete the branch and the workspace, and update memory `m1-progress`. Offer to push.

## Calls made during execution

Every ruling the executor made, in order, from the execution ledger (each ends with what it costs if wrong). Later rulings supersede earlier ones where the owner changed direction: the displayed trend, the trend-line toggle and the date ranges were each revised after trying them on the emulator (see the spec amendments dated 2026-10-18).

- Task 1: Ruling: homeEntry() stays parameterless in Task 1; Task 6 adds onOpenBody with the Body card — the placeholder Home has nothing to tap, and an unused parameter would trip detekt — cost if wrong: none (Task 6 changes one signature and one call site).
- Task 1: Ruling: system-bar style uses LaunchedEffect(dark) instead of the plan's SideEffect — re-applies only when the resolved theme flips rather than on every recomposition; still removes the empty onDispose — cost if wrong: none.
- Task 1: Ruling: GetFitIconsTest's directional-icon list now holds only Back (the Trends chart icons were the other directional icons and are deleted) — cost if wrong: none.
- Task 2: Ruling: observeDayStartMinutes_identicalWrites_doNotReEmit cannot fail without distinctUntilChanged — Room coalesces back-to-back invalidations into one re-query, so the duplicate never reaches a test deterministically; strengthened to "the next emission after a duplicate write is the new value" and kept distinctUntilChanged as defense-in-depth — cost if wrong: one redundant recomposition per identical write.
- Task 2: Ruling: FakeMeasurementRepository and FakeDayBoundaryRepository are simplified (one day start for all dates; fixed random type ids per key) — ViewModel tests need behavior, not versioning; the real repositories' tests cover versioning — cost if wrong: a VM test needing versioned day starts would need a richer fake.
- Task 3: Ruling: Mass conversion is named inUnit(unit), not the plan's in(unit) — `in` is a Kotlin keyword; also added Mass.of(value, unit) for parsing — cost if wrong: a rename.
- Task 3: Ruling: the plan's expected 181.7 lb = 82.41824 kg was an arithmetic slip; the correct value is 82.417733629 kg (181.7 × 0.45359237), and the test now asserts that. Task 7's tests that cite "82.418 kg" use the corrected figure; its display assertions (82.4 kg, 181.7 lb) are unaffected — cost if wrong: none.
- Task 3: Ruling: used kotlinx.datetime.LocalDateRange (exists in 0.8.0) rather than a custom range type; the metric contract and range types live in two files (metric/MetricContract.kt, metric/Transforms.kt) instead of one file per type — cost if wrong: a file split.
- Task 3: Ruling: rangeChange restructured with a single return for detekt ReturnCount — behavior unchanged, covered by RangeChangeTest — cost if wrong: none.
- Task 3a: Ruling: :core:testing is not covered — it is JVM test-fixture code (JUnit 4 rules) rather than a portable domain module, matching the spec's list of pure modules — cost if wrong: test fixtures would need a separate portable variant when tests go multiplatform.
- Task 4: Ruling: FakeMetricRegistry filters stored points to the requested range and ignores buckets — ViewModel tests use DAY — cost if wrong: a test using WEEK buckets would need the fake to bucketize.
- Task 5: Ruling: Vico 3.3.1 instead of the plan's 2.x — 3.x is the current stable line on Maven Central (3.3.1 published 2026-08-28, more than two weeks old); API checked against its sources jar — cost if wrong: an older major would mean a different API, no user-visible change.
- Task 5: Ruling: series are told apart by a legend row (dot or line sample + label) plus shape (POINTS = muted unconnected dots in colorScheme.outline, LINE = solid line in seriesPalette order) instead of Vico direct end labels — satisfies "never color alone" with less custom drawing; outline already passes the 3:1 graphic contrast test in every palette — cost if wrong: a later polish could add end labels.
- Task 5: Ruling: the chart uses Vico's static-model overload (CartesianChartHost(chart, model)) with scroll and zoom disabled and Zoom.Content, x = epoch day spanning the whole range, y fixed to the data min/max plus padding (max(0.5, 10%)) instead of starting at zero — whole range fits the width and weight changes are visible — cost if wrong: none.
- Task 5: Ruling: VicoChart split into rememberSeriesLines / rememberAxisRange / chartModel for detekt LongMethod — behavior unchanged, covered by MetricChartSemanticsTest — cost if wrong: none.
- Task 6: Ruling: HomeViewModel takes MetricRegistry, UserPreferencesRepository, DayBoundaryRepository, WallClock — not MeasurementRepository as the plan listed; the registry already gives the latest daily weigh-in and the trend, so the extra dependency would be unused — cost if wrong: one constructor parameter.
- Task 6: Ruling: UserPreferences.massUnit and UserPreferencesDataSource.defaultMassUnit carry defaults (KILOGRAM) so existing call sites and tests keep compiling; the locale default (POUND for US/LR/MM) is applied in :app's PreferencesModule.customaryMassUnit — cost if wrong: none.
- Task 6: Ruling: shared formatting (formatOneDecimal, formatSignedOneDecimal with a true minus sign, massUnitSymbol/massUnitSpoken) lives in :core:ui/format so Home and Body (Task 7) use the same rules — cost if wrong: none.
- Task 6: Ruling: Home's "today" is computed when the screen subscribes (no midnight rollover while it stays open); HomeUiState is null until loaded (same pattern as Settings) so no empty-state flash — cost if wrong: Home left open across the day start shows yesterday until reopened.
- Task 7: Ruling: BodyViewModel.uiState is StateFlow<BodyUiState?> (null until loaded), not the brief's non-null type — same no-empty-flash pattern as Home and Settings — cost if wrong: none.
- Task 7: Ruling: onSaveWeight takes a trailing locale parameter (default Locale.getDefault(); the dialog passes the configuration locale) — Locale can't be a ViewModel constructor dependency under the Konsist rule, and tests need a fixed locale — cost if wrong: one parameter.
- Task 7: Ruling: parseWeightInput turns grouping off — with grouping, German "80.5" parses as 805 (a valid 365 kg in pounds); now it is NOT_A_NUMBER. Added weightInput_germanDot_isNotANumber and weightInput_surroundingSpaces_parse — cost if wrong: a German user typing a dot sees an error instead of a save.
- Task 7: Ruling: dialog open/closed and its field error live in the ViewModel (onAddWeight, onDismissEntry, onEntryEdited clears the error on edit), so the dialog closes only after a successful save; typed text and picked date/time stay in rememberSaveable — cost if wrong: none.
- Task 7: Ruling: a picked time in the future is saved as now (onSaveWeight_futureTimeToday_clampsToNow); a time with no picked date uses today's calendar date — cost if wrong: a deliberately future-dated weigh-in can't be entered.
- Task 7: Ruling: "latest" comes from the registry's daily series over all history (not the 90-day list), so a weigh-in older than 90 days still shows; history sorts by day then time, newest first; chart series are the all-history registry series sliced to chartRange — cost if wrong: none.
- Task 7: Ruling: added BodyActions (a class of callbacks) so BodyContent stays under detekt's parameter limit; added onDelete_removesTheReading and historyRow_offersDeleteAsCustomAction beyond the brief — cost if wrong: none.
- Task 7: Ruling: RangeChips' private StartDatePicker became the public PastDatePickerDialog(today, onPicked, onDismiss, initial) in :core:ui, reused by the entry dialog — cost if wrong: none (RangeChipsTest still green).
- Task 7: Ruling: added a Delete icon (Material Symbols Outlined) to GetFitIcons for the history rows — cost if wrong: none.
- Task 7: Ruling: chart fixes found on the emulator, in Task 5's MetricChart — y-axis uses VerticalAxis.ItemPlacer.count(5) (the default placer showed one label on a narrow range) and 24dp unscalable layer padding at both ends (the last date label was cut to "O…" and today's dot clipped) — visual-only, verified by screenshot, no unit test — cost if wrong: none.
- Task 7: Ruling: Log/Trends survives rotation but NOT a tab switch, contrary to the brief — TopLevelBackStack removes the other tab's entries from NavDisplay's back stack, so Navigation 3's saveable-state and ViewModel decorators discard them (Home's range selection resets the same way). The fix is app-level (per-tab entry decorators, Nav3 multiple-back-stacks recipe), not Body's; raised to the owner instead of expanding Task 7 — cost if wrong: switching tabs resets each tab's view and range.
- Post-Task 7 (user feedback): Ruling: displayed "trend" is the least-squares rate per week; chart = solid weigh-in line with dots + dotted best-fit line; EMA trend weight kept internal for M5 — user directed; spec amended 2026-10-18 — cost if wrong: UI copy and chart series only
- Post-Task 7: Ruling: no-rate case shows 0.0 lb/week (user's call); Body card rate window 30 days; Home Today rate uses last 7 days (chartRange) — Today's one-day range has no slope — cost if wrong: one constant
- Post-Task 7: Ruling: Home range card with no weigh-in inside the range shows latest → latest, 0.0 — keeps the card instead of hiding it — cost if wrong: one fallback
- Post-Task 7: Ruling: "today" re-checked every minute via observeToday (core:domain), shared by Home and Body — screens left open past the day start stayed on yesterday — cost if wrong: a 1-minute ticker per subscribed screen
- Post-Task 7: Ruling: MetricChart getXStep = 1 day — Vico inferred a 7-day step from weekly weigh-ins and dropped all but one date label — cost if wrong: none known
- Post-Task 7 (round 2): Ruling: MetricChart derives a dotted best-fit trend for every series itself (ChartSeries has no style any more); screens pass values only — user: "true for all graphs" — cost if wrong: one parameter
- Post-Task 7: Ruling: one global "Trend lines" switch in Settings › Charts covers all built-in charts (not one per tab) — only Body has charts today; per-tab switches can split it later — cost if wrong: a few rows
- Post-Task 7: Ruling: trend uses its series' color (dotted); legend "Trend" for one series, "<label> trend" for several — cost if wrong: copy and color only
- Post-Task 7: Ruling: TopLevelBackStack overlay became a stack (pushOverlay) for Settings sub-pages — cost if wrong: small
- Post-Task 7 (round 3): Ruling: trend toggle is per chart, next to "View as table", in rememberSaveable (not persisted; on by default) — user moved it from Settings; Settings › Charts, showTrendLines preference and overlay stack reverted — cost if wrong: persisting it needs a per-chart key
- Post-Task 7 (round 4): Ruling: DateRangeSelection.All added (range starts at the screen's earliest data); Body defaults to All, Home to Today — user asked for an easy way back to "all" — cost if wrong: one default
- Post-Task 7: Ruling: Body Log and Trends share one range, which filters the history list and the card rate; latest value stays the newest weigh-in overall — user: filter the log by date to start fresh — cost if wrong: small
- Post-Task 7: Ruling: ranges persisted per screen via DateRangeRepository (DataStore key range_<screen>, text codec in core:domain, unreadable → default) — user: separate saved ranges — cost if wrong: small
- Post-Task 7: Ruling: "since" date = range start (All → first weigh-in), short format with year only when not this year — cost if wrong: copy only
- Post-Task 7: Ruling: RangeChips use FlowRow (wrap) instead of horizontal scroll — five chips plus a long From label were cut off — cost if wrong: layout only
- Task 8: Ruling: tab state fix pulled into Task 8 (user approved) — TabNavDisplay decorates each tab stack and the overlay separately (Navigation 3 multiple-back-stacks pattern); TopLevelBackStack keeps a stack for every tab (no removal on back) — cost if wrong: contained in :app
- Task 8: Ruling: app module gets Robolectric Compose test deps and isIncludeAndroidResources (application convention) to test the shell — cost if wrong: test-only
- Task 8: Ruling: Home card spoken order is "Weight X pounds, <day>, <rate> since <date>" — the trailing date read as part of "since" — cost if wrong: copy only
- Task 8: Ruling: table view shows no legend and no visible summary sentence (user: legend and "voice description" out of place); the summary remains the chart's content description — cost if wrong: copy only
- Task 8: Ruling: "backdated with a time before the day start" not walked on the emulator — no day-start UI until a later milestone and the default start is midnight; covered by addManual_localDateOverride_winsOverDayStart and onSaveWeight_pickedDate_passesOverride — cost if wrong: none beyond the tests
- Task 8: Ruling: TalkBack is installed and was enabled: it runs with the app and focus lands on controls, but adb cannot capture speech or drive focus; wording checked through the semantics tree (dumps + semantics tests). Listening pass left for the owner's device — cost if wrong: a wording issue found later
- Final: Ruling: Important #1 (Today on Body's Log lists 7 days of history) stands — the owner approved "Today works as on charts: the last 7 days" for the shared Body range; one range keeps history, rate and chart consistent — cost if wrong: switch history to cardRange (one line)
- Final: Ruling: declined-to-judge items (stone unit, locale-derived default unit not persisted, edit/undo, legend Row at many series, shared ViewModel key across stacks) — all outside M1c scope or latent with today's keys — cost if wrong: later polish

### Deferred minors (final review)

- a picked time without a picked date uses the calendar date, not the logical day (latent until a day-start UI exists)
- a failed save gives no feedback (dialog stays open silently)
- table rows read values without a unit in TalkBack
- Home Weight card has no onClickLabel ("Open Body")
- ChartTable composes all rows eagerly (All over years of data)
- delete confirmation not kept across rotation
- MetricChart formatting nit; trendLine computed twice per recomposition; TopLevelBackStack.backStack now unused by the UI
