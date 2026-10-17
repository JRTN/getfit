# M1b — Data Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Stand up `user.db` and the data layer every later milestone writes through: typed UUID identifiers, revision-stamped transactional writes, owner scoping with a single local profile, a versioned day-boundary setting, measurement tables with a built-in weight type, injected dispatchers, the sync seam (`NoOpSyncGateway`), and the static-analysis rules that guard them.

**Architecture:** `:core:database` owns the Room schema (entities, DAOs, converters, committed schema JSON) and nothing else. `:core:data` owns everything that makes a write correct: `StampedWriter` (one transaction = sequence increment + clock + device ID), `SingleLocalOwnerProvider` (the one `UserProfile`, created on first use), the reference seeder, and the repository implementations. `:core:domain` declares the repository interfaces, `Outcome`/`DomainError`, `IdGenerator` and the pure `localDateFor` rule. `:app` is the only module that binds and provides. M1b has no UI; its exit is a green suite plus an on-device boot that creates `user.db`, the profile and the device ID.

**Tech Stack:** Room 2.8.5 (KSP), kotlinx-datetime 0.8.0, `kotlin.uuid.Uuid` / `kotlin.time.Clock` / `kotlin.time.Instant` (Kotlin 2.4.21 stdlib), Hilt 2.60.1, Robolectric 4.17, Turbine 1.2.1, kotlinx-coroutines-test 1.11.0, detekt 2.0.0-alpha.6 API, Konsist 0.18.1.

**Spec:** `docs/initial_design.md`, sections "Code conventions" (1, 5, 6, 7), "Data architecture", "user.db schema" (Profile and versioned settings; Biometrics; Infrastructure), "Backup, export and the sync seam" (Sync seam), "Build infrastructure" (custom static-analysis rules) and "Testing strategy". This is plan 2 of 4 for M1 (M1a design system → **M1b data foundation** → M1c weight, metrics and charts → M1d backup).

## Global Constraints

- Branch `m1b-data-foundation`, cut from `master` at `502cb34`, merged back when green (decision 36). No tag (`v0.1.0` comes after M1d). Do not push.
- `minSdk` 34, `targetSdk` 37, `compileSdk` 37.
- Kotlin explicit style: explicit types, block bodies, named classes. KDoc on every convention plugin and build-logic helper.
- "Every entity ID is a value class over `kotlin.uuid.Uuid` … New rows use UUIDv7 (time-ordered); reference foods use UUIDv5."
- "Room `@Entity` classes live only in `:core:database` and `:core:reference`. Domain models in `:core:model` are plain data classes with no annotations. `:core:data` maps between them."
- "Repositories expose reads as `Flow<T>` and writes as `suspend` functions. Every write runs inside `database.withTransaction { }` together with its revision stamp."
- "Coroutine dispatchers are injected through qualifiers (`@IoDispatcher`, `@DefaultDispatcher`); no code references `Dispatchers.IO` directly."
- "Expected failures are values of a sealed type in `:core:domain`; exceptions are reserved for programming errors."
- "DAOs take `ownerId` as a parameter on every query; repositories obtain it from the injected `CurrentOwnerProvider` and never accept it from callers."
- "`Instant` → epoch milliseconds (`INTEGER`). `LocalDate` → epoch day (`INTEGER`). `TimeZone` → IANA ID (`TEXT`)."
- "`user.db` … Migrated: Yes, explicit migrations only." `fallbackToDestructiveMigration` is forbidden and detekt enforces it (this plan adds the rule).
- Tombstones: "DAOs filter `deletedAt IS NULL`."
- `:feature:*` never sees storage; `:core:data` is the only module depending on `:core:database`/`:core:datastore`; `:app` is the only binder.
- Each module with Robolectric tests needs `src/test/resources/robolectric.properties` containing `sdk=36`.
- Run Gradle with `JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"`; in Git Bash set `MSYS_NO_PATHCONV=1` for `adb shell` paths.

## Calls made in this plan

1. **Only the tables M1 uses are created now:** `UserProfile`, `DayBoundarySetting`, `MeasurementType`, `Measurement`, `SyncSequence`, `SyncCursor`, `DatasetVersion`. `EnergyAccountingSetting` (M5), `MetricTarget` (M4), `SourcePriority` and `HealthConnectSyncState` (M2) arrive with their milestones. Schema version stays 1 and may change freely until `v0.1.0`, because no real data exists before then. M1d pins the v1 identity hash when it tags `v0.1.0`; after that, every schema change is a numbered migration with a `MigrationTestHelper` test. Cost if wrong: none now; later tables each need one `CREATE TABLE` migration.
2. **`UserProfile` fields `displayName`, `sex`, `birthDate` and `heightM` are nullable.** The row is created silently at first launch, before the user has entered anything. M5 asks for them before computing BMR. Cost if wrong: a later NOT NULL migration.
3. **The profile row's `id` is the owner ID,** and its `ownerId` column equals its `id`.
4. **The device ID lives in a file in `noBackupFilesDir`, not in `user.db` or DataStore.** Restoring a backup on a new phone, or after reinstalling, therefore gives that install a new device ID, so two devices never share one sync tie-breaker.
5. **Built-in `MeasurementType` rows** use deterministic UUIDv5 IDs (`nameBasedId("measurement-type:<key>")`), so they are identical on every device. They have `ownerId = null`, `localRevision = 0` (excluded from a first push, which sends `localRevision > 0`) and `originDeviceId = null`. M1 seeds only `weight`; M2 adds more by bumping the dataset version.
6. **The default day start is 0 minutes (midnight)** when no `DayBoundarySetting` row is in force.
7. **Day assignment rule:** take the entry's wall-clock time in its zone. If the time of day (in minutes) is ≥ `dayStartMinutes`, the entry's local date is the calendar date; otherwise it is the day before. The day-start value used is the one in force on the calendar date. This rule never does zone arithmetic, so DST gaps and overlaps cannot shift a date.
8. **`Measurement` gets an extra index `(ownerId, measurementTypeId, localDate)`** beside the spec's `(ownerId, measurementTypeId, startTime)`, because charts and tables query by local date.
9. **No SQLite foreign keys.** Sync applies rows in arbitrary order and tombstones keep parents alive. Referential integrity is the repositories' job.
10. **`LocalProfileDao` and the infrastructure DAOs are the only owner-less DAOs.** They live in package `core.database.infrastructure`. A Konsist rule requires every `@Query` in package `core.database.dao` to take `ownerId`.
11. **The repositories use `withTransaction` directly in `:core:data`,** with no transaction-runner abstraction. Room types stay out of every module except `:core:database` and `:core:data`.
12. **`SyncWorker` is deferred.** With `NoOpSyncGateway` bound it would be a periodic no-op. It arrives with the first real gateway, and WorkManager arrives in M1d for the export worker.
13. **The schema check in M1b compares the committed JSON's `identityHash` with the compiled database's.** `MigrationTestHelper` arrives with migration 1→2.
14. **The `getfit` detekt config moves to its own file,** `config/detekt/getfit.yml`, which every module except `:detekt-rules` loads. This removes `excludes: ['getfit.*']`, so config validation is active everywhere. (It closes M1a deferred minor M5.)
15. **No repository fakes in `:core:testing` yet.** M1c writes the first ViewModels that need `FakeMeasurementRepository` and `FakeDayBoundaryRepository`, so they come with them. M1b adds only `FakeClock`.

## Review Focus

1. **A failed write leaves nothing behind.** If a stamped write throws halfway, neither its rows nor its revision increment persist. Pinned in Task 6 (`write_blockThrows_rollsBackRowsAndRevision`).
2. **Exactly one profile, ever.** Concurrent first calls, a second provider instance, and an existing profile (e.g. from a restored `user.db`) all resolve to the same single profile. Pinned in Task 6 (`currentOwnerId_concurrentFirstCalls_createOneProfile`, `currentOwnerId_newInstance_reusesExistingProfile`).
3. **Early-morning entries land on the right day.** An entry at 01:30 with a 04:00 day start belongs to the previous day, including across a DST change. Pinned in Task 1 (`localDateFor_*`) and Task 7 (`addManual_beforeDayStart_assignsPreviousLocalDate`).
4. **Bad numbers never reach the database.** A NaN or infinite value (e.g. from a failed parse in M1c) is rejected with `DomainError.Validation`. Pinned in Task 7 (`addManual_nonFiniteValue_returnsValidation`).
5. **Another owner's row is invisible and untouchable.** Reads never return it, and deleting by its ID returns `NotFound` and leaves it intact. Pinned in Task 7 (`observe_neverReturnsOtherOwnersRows`, `delete_otherOwnersRow_returnsNotFoundAndLeavesItIntact`).

## File Structure

```
core/model/.../core/model/ids/{OwnerId,DeviceId,MeasurementId,MeasurementTypeId}.kt   value classes over Uuid
core/model/.../core/model/{Sex,MeasurementSource,MeasurementCategory,DailyAggregation}.kt
core/model/.../core/model/{Measurement,MeasurementTypeKeys}.kt
core/domain/.../core/domain/{Outcome,DomainError,IdGenerator,DayAssignment}.kt
core/domain/.../core/domain/{DayBoundaryRepository,MeasurementRepository,ManualMeasurement}.kt
core/database/.../core/database/GetFitDatabase.kt, DatabaseFactory.kt, Converters.kt
core/database/.../core/database/entity/*Entity.kt, SyncMetadata.kt
core/database/.../core/database/dao/{DayBoundarySettingDao,MeasurementTypeDao,MeasurementDao}.kt     owner-scoped
core/database/.../core/database/infrastructure/{SyncSequenceDao,DatasetVersionDao,LocalProfileDao}.kt
core/database/schemas/com.jrussell.fitness.core.database.GetFitDatabase/1.json          committed
core/data/.../core/data/di/CoroutineQualifiers.kt         @IoDispatcher @DefaultDispatcher @ApplicationScope
core/data/.../core/data/id/UuidIdGenerator.kt
core/data/.../core/data/write/{StampedWriter,WriteStamp,DeviceIdProvider,FileDeviceIdProvider}.kt
core/data/.../core/data/owner/{CurrentOwnerProvider,SingleLocalOwnerProvider}.kt
core/data/.../core/data/seed/{ReferenceSeeder,DataBootstrap}.kt
core/data/.../core/data/repository/{DefaultDayBoundaryRepository,DefaultMeasurementRepository,MeasurementMapper}.kt
core/sync/.../core/sync/{SyncGateway,SyncableChange,PushResult,PullResult,NoOpSyncGateway}.kt
core/testing/.../core/testing/FakeClock.kt
app/.../di/{CoroutinesModule,DatabaseModule,DataModule,SyncModule}.kt; GetFitApplication.kt (bootstrap)
detekt-rules/.../{NoDirectDispatchers,NoDestructiveMigration}.kt; NoColorLiteralOutsideDesignSystem.kt (gaps)
config/detekt/{detekt.yml,getfit.yml}
architecture-tests/.../DaoOwnerScopingRulesTest.kt
```

---

### Task 1: Typed IDs, `Outcome`, `IdGenerator` and the day-assignment rule

**Files:**
- Modify: `core/model/build.gradle.kts` (`api(libs.kotlinx.datetime)`)
- Create: `core/model/.../ids/{OwnerId,DeviceId,MeasurementId,MeasurementTypeId}.kt`, `core/domain/.../{Outcome,DomainError,IdGenerator,DayAssignment}.kt`, `core/data/.../id/UuidIdGenerator.kt`
- Test: `core/domain/src/test/.../DayAssignmentTest.kt` (JUnit 5), `core/data/src/test/.../id/UuidIdGeneratorTest.kt` (JUnit 4)

**Interfaces:**
- Produces:
  - `@JvmInline value class OwnerId(val value: Uuid)`, and the same shape for `DeviceId`, `MeasurementId` and `MeasurementTypeId`.
  - `sealed interface Outcome<out T> { data class Success<T>(val value: T); data class Failure(val error: DomainError) : Outcome<Nothing> }`
  - `sealed interface DomainError` with `Validation(field: String, reason: String)`, `PermissionDenied(permission: String)`, `ImportParse(line: Int, reason: String)` and `NotFound(entity: String, id: String)`. These match the spec exactly.
  - `interface IdGenerator { fun newTimeOrderedId(): Uuid; fun nameBasedId(name: String): Uuid }`
  - `fun localDateFor(instant: Instant, timeZone: TimeZone, dayStartMinutes: Int): LocalDate`. It throws `IllegalArgumentException` unless `dayStartMinutes` is in `0..1439`. The rule is call 7.
  - `class UuidIdGenerator @Inject constructor(private val clock: Clock) : IdGenerator` and `internal fun uuidV5(namespace: Uuid, name: String): Uuid`.
  - `UuidIdGenerator.APP_NAMESPACE = Uuid.parse("3f6b2c1e-8a4d-4f7a-9c2e-5b1d7e9a0c44")`. Its KDoc must say "never change: copied reference foods and built-in rows derive their IDs from it".

- [ ] **Step 1:** Run `git checkout -b m1b-data-foundation`.
- [ ] **Step 2: Write `DayAssignmentTest`.** Every case uses zone `America/Toronto`.

```kotlin
@Test fun localDateFor_beforeDayStart_isPreviousDay() { assertThat(localDateFor(at("2026-10-17T01:30"), toronto, 240)).isEqualTo(LocalDate(2026, 10, 16)) }
@Test fun localDateFor_atDayStart_isSameDay() { assertThat(localDateFor(at("2026-10-17T04:00"), toronto, 240)).isEqualTo(LocalDate(2026, 10, 17)) }
@Test fun localDateFor_midnightStart_isCalendarDate() { assertThat(localDateFor(at("2026-10-17T23:59"), toronto, 0)).isEqualTo(LocalDate(2026, 10, 17)) }
@Test fun localDateFor_afterSpringForward_usesWallClock() { assertThat(localDateFor(at("2026-03-08T03:15"), toronto, 150)).isEqualTo(LocalDate(2026, 3, 8)) }
@Test fun localDateFor_zoneDecidesDate() { val instant = Instant.parse("2026-10-17T02:00:00Z"); assertThat(localDateFor(instant, TimeZone.UTC, 0)).isEqualTo(LocalDate(2026, 10, 17)); assertThat(localDateFor(instant, toronto, 0)).isEqualTo(LocalDate(2026, 10, 16)) }
@Test fun localDateFor_outOfRangeStart_throws() { assertFailure { localDateFor(at("2026-10-17T12:00"), toronto, 1440) }.isInstanceOf<IllegalArgumentException>() }
// at(local) = LocalDateTime.parse(local).toInstant(toronto)
```

- [ ] **Step 3: Write `UuidIdGeneratorTest`.** It uses a fixed `Clock` built as `object : Clock { override fun now() = instant }`; `FakeClock` arrives in Task 6.
  - `uuidV5_matchesRfc9562Vector`: `uuidV5(Uuid.parse("6ba7b810-9dad-11d1-80b4-00c04fd430c8"), "www.example.com").toString()` equals `"2ed6657d-e927-568b-95e1-2665a8aea6a2"`.
  - `nameBasedId_isStableAndNameSensitive`: the same name gives the same ID twice, and `"a"` and `"b"` give different IDs.
  - `newTimeOrderedId_hasVersion7AndRfcVariant`: the version nibble is 7 and the top two variant bits are `10`. Read both from `toLongs { msb, lsb -> }`.
  - `newTimeOrderedId_embedsClockMillis`: `msb ushr 16` equals `clock.now().toEpochMilliseconds()` for `Instant.parse("2026-10-17T12:00:00Z")`.
  - `newTimeOrderedId_laterClockSortsLater`: IDs made at t and t + 1 ms compare ascending by `toString()`.
- [ ] **Step 4:** Run `./gradlew :core:domain:test :core:data:testDebugUnitTest`. Expected: compilation FAILS (unresolved `localDateFor`, `UuidIdGenerator`, `uuidV5`).
- [ ] **Step 5: Implement.**
  - The ID value classes, `Outcome`, `DomainError` and `IdGenerator`.
  - `localDateFor`, following call 7, using `instant.toLocalDateTime(timeZone)`.
  - `newTimeOrderedId()` returns `Uuid.generateV7NonMonotonicAt(clock.now())`, with a file-local `@OptIn(ExperimentalUuidApi::class)`.
  - `uuidV5` uses SHA-1 (`java.security.MessageDigest`) over the namespace bytes followed by the UTF-8 bytes of the name. Take the first 16 bytes, set the version nibble to 5 and the variant to `10`.
  - `nameBasedId(name)` returns `uuidV5(APP_NAMESPACE, name)`.
- [ ] **Step 6:** Run the same command. Expected: 6 + 5 tests PASS.
- [ ] **Step 7:** Commit: `feat: typed UUID ids, Outcome/DomainError, UUIDv7/v5 generator, day-start date assignment`.

### Task 2: Injected dispatchers and clock; DataStore hardening

**Files:**
- Create: `core/data/.../di/CoroutineQualifiers.kt`, `app/.../di/CoroutinesModule.kt`
- Modify: `core/datastore/.../UserPreferencesDataStore.kt` (`scope` becomes required, and the `Dispatchers` import is removed), `core/datastore/.../UserPreferencesDataSource.kt`, `app/.../di/PreferencesModule.kt`
- Test: `core/datastore/src/test/.../UserPreferencesDataSourceTest.kt` (add 1)

**Interfaces:**
- Produces:
  - `@Qualifier @Retention(AnnotationRetention.BINARY) annotation class IoDispatcher`, plus `DefaultDispatcher` and `ApplicationScope` with the same shape. They are in `com.jrussell.fitness.core.data.di`.
  - `CoroutinesModule` (`@InstallIn(SingletonComponent)`) provides:
    - `@IoDispatcher CoroutineDispatcher = Dispatchers.IO`;
    - `@DefaultDispatcher CoroutineDispatcher = Dispatchers.Default`;
    - `@ApplicationScope @Singleton CoroutineScope = CoroutineScope(SupervisorJob() + defaultDispatcher)`;
    - `Clock = Clock.System`;
    - a binding of `IdGenerator` to `UuidIdGenerator`.
  - `fun createUserPreferencesDataStore(scope: CoroutineScope, produceFile: () -> File): DataStore<Preferences>`. `scope` no longer has a default.

- [ ] **Step 1: Write the failing test `userPreferences_wrongTypedStoredValue_fallsBackToDefault`.** Write `intPreferencesKey("theme_mode")` = `2` and `stringPreferencesKey("color_palette")` = `"HIGH_CONTRAST"` straight into the DataStore. The first emission must equal `UserPreferences(UserPreferences.DEFAULT.themeMode, ColorPalette.HIGH_CONTRAST)`, which means the theme falls back and the palette survives.
- [ ] **Step 2:** Run `./gradlew :core:datastore:testDebugUnitTest`. Expected: FAIL with `ClassCastException`.
- [ ] **Step 3:** In `UserPreferencesDataSource`, read each value as `preferences.asMap()[key] as? String`, which treats a wrong type the same as absent. Update the class KDoc's list to mention it.
- [ ] **Step 4:** Make `scope` required in `createUserPreferencesDataStore`. Add `CoroutineQualifiers.kt` and `CoroutinesModule`. `PreferencesModule.provideUserPreferencesDataStore` takes `@IoDispatcher ioDispatcher: CoroutineDispatcher` and passes `CoroutineScope(ioDispatcher + SupervisorJob())`.
- [ ] **Step 5:** Run `./gradlew :core:datastore:testDebugUnitTest :app:assembleDebug`. Expected: 8/8 PASS, and the build succeeds (Hilt graph resolves).
- [ ] **Step 6:** Commit: `feat: injected dispatcher/clock qualifiers; wrong-typed preference falls back to default`.

### Task 3: detekt — dispatcher and migration rules, color-rule gaps, scoped config

**Files:**
- Create: `detekt-rules/.../{NoDirectDispatchers,NoDestructiveMigration}.kt` and their tests, plus `config/detekt/getfit.yml`
- Modify: `detekt-rules/.../NoColorLiteralOutsideDesignSystem.kt` and its test, `GetFitRuleSetProvider.kt`, `config/detekt/detekt.yml` (delete the `config: excludes` block and the `getfit:` section), `build-logic/.../QualityConventionPlugin.kt` (load `getfit.yml` in every module except `:detekt-rules`, and update its KDoc)

**Interfaces:**
- Produces: the detekt rules `NoDirectDispatchers` and `NoDestructiveMigration`, both in rule set `getfit` and active in `getfit.yml`.
  - **`NoDirectDispatchers`** reports `Dispatchers.IO`, `.Default`, `.Main` and `.Unconfined`, whether written as `Dispatchers` or `kotlinx.coroutines.Dispatchers`. It allows them in package `com.jrussell.fitness.di`. Calls such as `Dispatchers.setMain(...)` are allowed.
  - **`NoDestructiveMigration`** reports any call whose name starts with `fallbackToDestructiveMigration`, everywhere.

- [ ] **Step 1: Write the tests** (JUnit 5, `rule.lint(code)`):
  - `NoDirectDispatchersTest`:
    - `dispatchersIo_inFeaturePackage_isReported` (expects 1);
    - `dispatchersDefault_inDataPackage_isReported`;
    - `fullyQualifiedDispatchersIo_isReported`;
    - `dispatchers_inAppDiPackage_isAllowed`;
    - `setMain_isAllowed`.
  - `NoDestructiveMigrationTest`:
    - `fallbackToDestructiveMigration_isReported` (`builder.fallbackToDestructiveMigration(dropAllTables = true)`);
    - `onDowngradeVariant_isReported`;
    - `addMigrations_isAllowed`.
  - Add to `NoColorLiteralOutsideDesignSystemTest`. Each of these is in package `…feature.today` and expects 1 finding:
    - `namedColorConstant_isReported` (`Color.Red`);
    - `hslCall_isReported` (`Color.hsl(270f, 0.5f, 0.5f)`);
    - `androidParseColor_isReported` (`android.graphics.Color.parseColor("#6941C6")`);
    - `importAlias_isReported` (`import androidx.compose.ui.graphics.Color as Tint`).
  - The four existing tests must still pass. In particular, `Color.Unspecified`, `Color.Transparent` and `android.graphics.Color.TRANSPARENT` stay allowed.
- [ ] **Step 2:** Run `./gradlew :detekt-rules:test`. Expected: FAIL (2 classes unresolved; the 4 new color tests report 0).
- [ ] **Step 3: Implement.**
  - Extend the color rule with `visitDotQualifiedExpression`:
    - a receiver of `Color` or `androidx.compose.ui.graphics.Color` is reported unless the selector is `Unspecified` or `Transparent`;
    - a receiver of `android.graphics.Color` is reported unless the selector is `TRANSPARENT`.
  - Add `visitImportDirective` for aliased imports of either `Color` class.
  - A single `Color(...)` must still produce exactly one finding.
  - Register both new rules in the provider.
- [ ] **Step 4:** Run `./gradlew :detekt-rules:test`. Expected: all PASS (4 existing + 4 new color tests + 8 new).
- [ ] **Step 5:** Move the `getfit:` section into `getfit.yml`, activating all three rules. Remove `excludes` and wire `QualityConventionPlugin`. Add a typo key (`NoColorLiteral:`) to `getfit.yml` and run `./gradlew :feature:settings:detekt` (or the module's detekt task that `check` runs). Expected: FAILS on config validation. Remove the typo.
- [ ] **Step 6:** Run `./gradlew check`. Expected: PASS. Task 2 already removed the only direct dispatcher use. Any new finding is a real violation: fix the code, don't suppress it.
- [ ] **Step 7:** Commit: `build: detekt rules for direct dispatchers and destructive migration; close color-rule gaps; validate getfit config everywhere`.

### Task 4: `user.db` infrastructure — converters, sync tables, profile, database factory

**Files:**
- Modify: `core/database/build.gradle.kts` (depend on `:core:model`; add kotlinx-datetime), `build-logic/.../AndroidRoomConventionPlugin.kt` (KDoc: the schema JSON is checked by a test)
- Create:
  - In `core/database/.../`: `Converters.kt`, `entity/SyncMetadata.kt`, `entity/UserProfileEntity.kt`, `entity/SyncSequenceEntity.kt`, `entity/SyncCursorEntity.kt`, `entity/DatasetVersionEntity.kt`, `infrastructure/{SyncSequenceDao,DatasetVersionDao,LocalProfileDao}.kt`, `GetFitDatabase.kt` and `DatabaseFactory.kt`;
  - `core/database/src/test/resources/robolectric.properties`;
  - `schemas/.../1.json`, which Room generates and is committed.
- Test: `core/database/src/test/.../{ConvertersTest,SyncSequenceDaoTest,LocalProfileDaoTest,SchemaJsonTest}.kt`

**Interfaces:**
- Produces:
  - `data class SyncMetadata(createdAt: Instant, updatedAt: Instant, deletedAt: Instant?, localRevision: Long, originDeviceId: Uuid)`, used with `@Embedded` in every user-owned entity.
  - `@Entity(tableName = "UserProfile") data class UserProfileEntity`:
    - `@PrimaryKey id: Uuid`, `ownerId: Uuid`;
    - `displayName: String?`, `sex: Sex?`, `birthDate: LocalDate?`, `heightM: Double?`;
    - `@Embedded sync: SyncMetadata`.
    - `Sex { MALE, FEMALE }` goes in `:core:model`.
  - Other entities:
    - `SyncSequenceEntity(@PrimaryKey id: Int, value: Long)`;
    - `SyncCursorEntity(@PrimaryKey remoteId: String, lastPushedRevision: Long, lastPulledCursor: String?)`, which has no DAO (call 12);
    - `DatasetVersionEntity(@PrimaryKey datasetKey: String, version: Int, seededAt: Instant)`.
  - `SyncSequenceDao.incrementAndGet(): Long`, a `@Transaction` that inserts `(1, 0)` with IGNORE, adds one and returns the value.
  - `DatasetVersionDao`: `version(datasetKey: String): Int?` and `upsert(entity: DatasetVersionEntity)`.
  - `LocalProfileDao`: `findLocalProfile(): UserProfileEntity?`, which takes the oldest by `createdAt` with `LIMIT 1` and ignores tombstones, and `insert(entity: UserProfileEntity)`.
  - `GetFitDatabase : RoomDatabase` with `version = 1` and `exportSchema = true`. Its DAO accessors are `syncSequenceDao()`, `datasetVersionDao()` and `localProfileDao()`.
  - `const val USER_DATABASE_NAME = "user.db"` and `fun createGetFitDatabase(context: Context, queryContext: CoroutineContext): GetFitDatabase`. The factory uses `Room.databaseBuilder(...).setQueryCoroutineContext(queryContext)`, with no fallback and no migrations yet.
  - `Converters` converts:
    - `Uuid` ↔ `String`;
    - `Instant` ↔ `Long` (epoch ms);
    - `LocalDate` ↔ `Int` (epoch day, via `toEpochDays().toInt()` / `fromEpochDays`);
    - `TimeZone` ↔ `String` (IANA ID).

- [ ] **Step 1: Write the tests.** DAO tests use `Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), GetFitDatabase::class.java).allowMainThreadQueries().build()` under `@RunWith(RobolectricTestRunner::class)`.
  - `ConvertersTest`: round trips for:
    - `Uuid`;
    - `Instant.fromEpochMilliseconds(1_760_000_000_123)`;
    - `LocalDate(1969, 12, 31)`, which is epoch day −1;
    - `TimeZone.of("America/Toronto")`.
  - `SyncSequenceDaoTest.incrementAndGet_startsAtOneAndIncrements`: returns 1, then 2, then 3.
  - `LocalProfileDaoTest`:
    - `findLocalProfile_empty_isNull`;
    - `findLocalProfile_returnsInsertedProfile`, which also checks that the nullable fields stay null.
  - `SchemaJsonTest.committedSchema_matchesCompiledDatabase`:
    - read `File("schemas/com.jrussell.fitness.core.database.GetFitDatabase/1.json")`, relative to the module directory;
    - extract `"identityHash"` with kotlinx-serialization-json or a regex;
    - compare it with `SELECT identity_hash FROM room_master_table` on an opened in-memory database.
    - If kotlinx-serialization-json is used, add it as `testImplementation`.
- [ ] **Step 2:** Run `./gradlew :core:database:testDebugUnitTest`. Expected: compilation FAILS.
- [ ] **Step 3:** Implement the converters, entities, DAOs, database and factory.
- [ ] **Step 4:** Run the same command. Expected: 4 + 1 + 2 + 1 tests PASS, and `schemas/…/1.json` exists.
- [ ] **Step 5:** Commit, including `schemas/`: `feat(database): user.db v1 infrastructure — converters, SyncSequence, SyncCursor, DatasetVersion, UserProfile`.

### Task 5: Settings and measurement tables, owner-scoped DAOs, Konsist DAO rule

**Files:**
- Create: `core/database/.../entity/{DayBoundarySettingEntity,MeasurementTypeEntity,MeasurementEntity}.kt`, `dao/{DayBoundarySettingDao,MeasurementTypeDao,MeasurementDao}.kt`, `core/model/.../{MeasurementSource,MeasurementCategory,DailyAggregation}.kt`, `architecture-tests/.../DaoOwnerScopingRulesTest.kt`
- Modify: `GetFitDatabase.kt` (adds the entities and DAO accessors; still version 1) and `schemas/…/1.json` (regenerated)
- Test: `core/database/src/test/.../{DayBoundarySettingDaoTest,MeasurementDaoTest,MeasurementTypeDaoTest}.kt`

**Interfaces:**
- Consumes: `SyncMetadata` and `Converters` from Task 4.
- Produces:
  - `DayBoundarySettingEntity(id, ownerId, dayStartMinutes: Int, effectiveFrom: LocalDate, @Embedded sync)` has a unique index on `(ownerId, effectiveFrom)`.
  - `MeasurementTypeEntity` is mixed-catalog, so its columns are explicit rather than `SyncMetadata`:
    - `@PrimaryKey id: Uuid`, `ownerId: Uuid?`;
    - `key`, `displayName`, `canonicalUnit: String`;
    - `category: MeasurementCategory` (BODY, VITAL, ACTIVITY, LAB, CUSTOM);
    - `dailyAggregation: DailyAggregation` (SUM, MEAN, LAST, MIN, MAX);
    - `hasSecondaryValue: Boolean`, `referenceRangeLow: Double?`, `referenceRangeHigh: Double?`, `decimals: Int`, `healthConnectRecordType: String?`;
    - `createdAt`, `updatedAt`, `deletedAt?`, `localRevision: Long`, `originDeviceId: Uuid?`.
    - It has a unique index on `(ownerId, key)`.
  - `MeasurementEntity`:
    - `id`, `ownerId`, `measurementTypeId: Uuid`;
    - `value: Double`, `secondaryValue: Double?`;
    - `startTime: Instant`, `endTime: Instant`, `timeZone: TimeZone`, `localDate: LocalDate`;
    - `source: MeasurementSource` (MANUAL, HEALTH_CONNECT, IMPORT), `sourcePackage: String?`, `externalId: String?`;
    - `enteredValue: Double?`, `enteredUnit: String?`, `note: String?`;
    - `@Embedded sync`.
    - Indexes: `(ownerId, measurementTypeId, startTime)`, `(ownerId, measurementTypeId, localDate)`, and unique `(ownerId, source, externalId)`.
  - `DayBoundarySettingDao`:
    - `observeInForce(ownerId: Uuid, day: LocalDate): Flow<DayBoundarySettingEntity?>` and `findInForce(ownerId, day): DayBoundarySettingEntity?`. Both select `effectiveFrom <= :day AND deletedAt IS NULL ORDER BY effectiveFrom DESC LIMIT 1`.
    - `findByEffectiveFrom(ownerId, effectiveFrom): DayBoundarySettingEntity?`
    - `insert(entity)` and `update(entity)`.
  - `MeasurementTypeDao`:
    - `findByKey(ownerId: Uuid, key: String): MeasurementTypeEntity?` matches `(ownerId = :ownerId OR ownerId IS NULL)` and `deletedAt IS NULL`. It prefers the owner's row (`ORDER BY ownerId IS NULL`).
    - `@Insert(onConflict = IGNORE) insertAll(entities: List<MeasurementTypeEntity>)`
  - `MeasurementDao`:
    - `insert(entity)`.
    - `observeByTypeAndDates(ownerId, measurementTypeId: Uuid, from: LocalDate, to: LocalDate): Flow<List<MeasurementEntity>>` is inclusive and ordered by `startTime`.
    - `findById(ownerId, id): MeasurementEntity?`
    - `tombstone(ownerId, id, deletedAt: Instant, revision: Long, deviceId: Uuid): Int` sets `deletedAt` and `updatedAt` to `:deletedAt` and stamps the revision and device ID, `WHERE ownerId AND id AND deletedAt IS NULL`. It returns the number of rows changed.
  - All owner-scoped read queries filter `deletedAt IS NULL`.

- [ ] **Step 1: Write the DAO tests** (in-memory database, as in Task 4):
  - `DayBoundarySettingDaoTest`:
    - `findInForce_picksLatestOnOrBeforeDay`: rows for 2026-01-01 (240) and 2026-06-01 (300) give 240 on 2026-05-31, 300 on 2026-06-01 and null on 2025-12-31;
    - `findInForce_ignoresOtherOwnerAndTombstones`;
    - `insert_duplicateEffectiveFrom_throwsConstraintViolation` (`SQLiteConstraintException`).
  - `MeasurementDaoTest`:
    - `observe_filtersOwnerTypeRangeAndTombstones`: one row each for another owner, another type, out of range and tombstoned, plus one match, gives exactly the match;
    - `tombstone_otherOwner_changesNothing`, which returns 0.
  - `MeasurementTypeDaoTest`:
    - `findByKey_returnsBuiltIn_andOwnersOverrideFirst`;
    - `findByKey_ignoresOtherOwnersCustomType`;
    - `insertAll_duplicateId_isIgnored`.
- [ ] **Step 2:** Run `./gradlew :core:database:testDebugUnitTest`. Expected: compilation FAILS.
- [ ] **Step 3:** Implement the enums, entities and DAOs, and register them in `GetFitDatabase`.
- [ ] **Step 4:** Run the same command. Expected: all PASS, including `SchemaJsonTest` against the regenerated `1.json`.
- [ ] **Step 5: Write `DaoOwnerScopingRulesTest.ownerScopedDaos_takeOwnerIdOnEveryQuery`.** It runs Konsist over the files in package `com.jrussell.fitness.core.database.dao..` (main source set). Every function annotated `@Query` must have a parameter named `ownerId`. Use `assertTrue(strict = true)`. To prove it bites, temporarily rename `ownerId` in `MeasurementDao.findById` and run `./gradlew :architecture-tests:test`. Expected: FAIL naming `findById`. Revert the rename and run again. Expected: PASS.
- [ ] **Step 6:** Commit: `feat(database): day-boundary setting, measurement type and measurement tables with owner-scoped DAOs`.

### Task 6: Stamped writes, device ID, owner provider, reference seeding

**Files:**
- Modify: `core/data/build.gradle.kts`. The test dependencies are `projects.core.testing`, robolectric, turbine and kotlinx-coroutines-test. The main dependencies are kotlinx-datetime and room-ktx (for `withTransaction`).
- Create:
  - `core/data/.../write/{WriteStamp,StampedWriter,DeviceIdProvider,FileDeviceIdProvider}.kt`, `owner/{CurrentOwnerProvider,SingleLocalOwnerProvider}.kt` and `seed/{ReferenceSeeder,DataBootstrap}.kt`;
  - `core/model/.../MeasurementTypeKeys.kt` (`const val WEIGHT = "weight"`);
  - `core/testing/.../FakeClock.kt`;
  - `core/data/src/test/resources/robolectric.properties`.
- Test: `core/data/src/test/.../{write/StampedWriterTest,write/FileDeviceIdProviderTest,owner/SingleLocalOwnerProviderTest,seed/ReferenceSeederTest}.kt`

**Interfaces:**
- Consumes: Task 1's `IdGenerator`/`UuidIdGenerator`; Task 2's `@IoDispatcher`; Tasks 4 and 5's DAOs and `GetFitDatabase`.
- Produces:
  - `data class WriteStamp(val revision: Long, val now: Instant, val deviceId: DeviceId)` with `fun toSyncMetadata(createdAt: Instant = now): SyncMetadata`.
  - `class StampedWriter @Inject constructor(database: GetFitDatabase, clock: Clock, deviceIdProvider: DeviceIdProvider)`, with `suspend fun <T> write(block: suspend (WriteStamp) -> T): T`. It resolves the device ID before the transaction. Inside `database.withTransaction` it calls `syncSequenceDao().incrementAndGet()`, reads `clock.now()` and runs `block`.
  - `interface DeviceIdProvider { suspend fun deviceId(): DeviceId }`
  - `class FileDeviceIdProvider(directory: File, ioDispatcher: CoroutineDispatcher) : DeviceIdProvider`. It reads or creates `directory/device_id` holding `Uuid.random()`. Unparseable content is replaced, and the result is cached in memory after the first read.
  - `interface CurrentOwnerProvider { suspend fun currentOwnerId(): OwnerId }`
  - `@Singleton class SingleLocalOwnerProvider @Inject constructor(database, stampedWriter, idGenerator) : CurrentOwnerProvider`. Under a `Mutex` it returns the cached ID or `findLocalProfile()`. Otherwise, in one stamped write, it inserts a profile with `id = ownerId = newTimeOrderedId()` and all optional fields null.
  - `class ReferenceSeeder @Inject constructor(database, idGenerator, clock)`, with `suspend fun seed()`. If `DatasetVersionDao.version("measurement_types")` is below `MEASUREMENT_TYPES_VERSION = 1`, it does both of these in one transaction:
    - `insertAll(builtIns)`;
    - `upsert(DatasetVersionEntity("measurement_types", 1, now))`.
  - Built-in `weight`:
    - `id = nameBasedId("measurement-type:weight")`, `ownerId = null`;
    - `displayName = "Weight"`, `canonicalUnit = "kg"`;
    - `category = BODY`, `dailyAggregation = LAST`;
    - `hasSecondaryValue = false`, `decimals = 1`, `healthConnectRecordType = "WeightRecord"`;
    - `localRevision = 0`, `originDeviceId = null`, and `createdAt = updatedAt = now`.
  - `class DataBootstrap @Inject constructor(seeder: ReferenceSeeder, ownerProvider: CurrentOwnerProvider)`, with `suspend fun run()` (seed, then resolve the owner).
  - `class FakeClock(var now: Instant) : Clock`, with `fun advanceBy(duration: Duration)`.

- [ ] **Step 1: Write the tests.** Use a Robolectric in-memory database, `StandardTestDispatcher`/`runTest`, `FakeClock(Instant.parse("2026-10-17T12:00:00Z"))` and a fixed `DeviceIdProvider` lambda object.
  - `StampedWriterTest`:
    - `write_stampsIncreasingRevisions` gives 1, then 2;
    - `write_passesClockTimeAndDeviceId`;
    - `write_blockThrows_rollsBackRowsAndRevision`: the block inserts a `UserProfileEntity` and then throws. Afterwards `findLocalProfile()` is null, and the next write receives revision 1.
  - `FileDeviceIdProviderTest`:
    - `deviceId_isStableAcrossInstances`, using a `TemporaryFolder`;
    - `deviceId_corruptFile_isReplacedWithValidId`, where the file holds `"not-a-uuid"` and is rewritten with the new ID.
  - `SingleLocalOwnerProviderTest`:
    - `currentOwnerId_firstCall_createsProfileOwnedByItself`: the profile has `id == ownerId == returned value` and `sync.localRevision == 1`;
    - `currentOwnerId_secondCall_returnsSameId`;
    - `currentOwnerId_newInstance_reusesExistingProfile`;
    - `currentOwnerId_concurrentFirstCalls_createOneProfile`: ten `async` calls produce one distinct ID, and `SELECT COUNT(*) FROM UserProfile` equals 1.
  - `ReferenceSeederTest`:
    - `seed_insertsBuiltInWeightType` checks the ID, `ownerId == null` and `localRevision == 0`;
    - `seed_twice_keepsOneRowAndRecordsVersion`.
- [ ] **Step 2:** Run `./gradlew :core:data:testDebugUnitTest`. Expected: compilation FAILS.
- [ ] **Step 3:** Implement everything listed under Produces.
- [ ] **Step 4:** Run the same command. Expected: all new tests PASS, and `CrashLogHandlerTest` still passes.
- [ ] **Step 5:** Commit: `feat(data): revision-stamped writes, per-install device id, single local owner, measurement-type seed`.

### Task 7: Day-boundary and measurement repositories

**Files:**
- Create: `core/model/.../Measurement.kt`, `core/domain/.../{DayBoundaryRepository,MeasurementRepository,ManualMeasurement}.kt` and `core/data/.../repository/{DefaultDayBoundaryRepository,DefaultMeasurementRepository,MeasurementMapper}.kt`
- Modify: `architecture-tests/.../{LayerRulesTest,ViewModelDependencyRulesTest}.kt`, where every `strict = false` becomes `strict = true` (M1a deferred minor M6)
- Test: `core/data/src/test/.../repository/{DefaultDayBoundaryRepositoryTest,DefaultMeasurementRepositoryTest}.kt`

**Interfaces:**
- Consumes: everything Task 6 produces, plus `localDateFor` and `Outcome` from Task 1.
- Produces:
  - In `:core:model`, `data class Measurement` has these fields:
    - `id: MeasurementId`, `typeId: MeasurementTypeId`;
    - `value: Double`, `secondaryValue: Double?`;
    - `startTime: Instant`, `endTime: Instant`, `timeZone: TimeZone`, `localDate: LocalDate`;
    - `source: MeasurementSource`;
    - `enteredValue: Double?`, `enteredUnit: String?`, `note: String?`.
  - `data class ManualMeasurement(typeKey: String, value: Double, takenAt: Instant, timeZone: TimeZone, enteredValue: Double?, enteredUnit: String?, note: String?)`
  - `interface DayBoundaryRepository`:
    - `fun observeDayStartMinutes(on: LocalDate): Flow<Int>`, which falls back to `DEFAULT_DAY_START_MINUTES = 0`;
    - `suspend fun setDayStart(dayStartMinutes: Int, effectiveFrom: LocalDate): Outcome<Unit>`.
  - `interface MeasurementRepository`:
    - `fun observeMeasurements(typeKey: String, from: LocalDate, to: LocalDate): Flow<List<Measurement>>`;
    - `suspend fun addManual(entry: ManualMeasurement): Outcome<MeasurementId>`;
    - `suspend fun delete(id: MeasurementId): Outcome<Unit>`.
  - Failures:
    - a `dayStartMinutes` outside 0..1439 gives `Validation("dayStartMinutes", …)`;
    - a non-finite value gives `Validation("value", …)`;
    - an unknown type key gives `NotFound("MeasurementType", key)`;
    - a missing, tombstoned or foreign ID gives `NotFound("Measurement", id)`;
    - observing an unknown type key emits an empty list.
  - `setDayStart` with an existing `effectiveFrom` updates that row's minutes and stamp, keeps its `id` and `createdAt`, and never inserts a duplicate.
  - `addManual` stores `source = MANUAL` and `endTime = takenAt`. It computes `localDate` with call 7, using the setting in force on `takenAt.toLocalDateTime(timeZone).date`.

- [ ] **Step 1: Write the tests.** They use real DAOs on an in-memory database, the real `StampedWriter`, `SingleLocalOwnerProvider` and `ReferenceSeeder` (seeded in `@Before`), and `FakeClock`. "Another owner" rows are inserted straight through the DAOs with a fresh UUID owner.
  - `DefaultDayBoundaryRepositoryTest`:
    - `observe_noRows_isMidnight`;
    - `observe_usesLatestEffectiveOnOrBefore` (the Task 5 dates, through the repository);
    - `setDayStart_sameEffectiveDate_updatesInsteadOfDuplicating`: one row, the new value, the same `createdAt` and a higher `localRevision`;
    - `setDayStart_outOfRange_returnsValidation`, for both −1 and 1440;
    - `observe_otherOwnersSetting_isIgnored`.
  - `DefaultMeasurementRepositoryTest`, where Toronto is the zone:
    - `addManual_thenObserve_returnsIt`: value 82.4, `enteredValue` 181.7, `enteredUnit` "lb", source MANUAL and the expected `localDate`;
    - `addManual_beforeDayStart_assignsPreviousLocalDate`: day start 240 from 2026-01-01, taken at 2026-10-17T01:30 local, gives 2026-10-16;
    - `addManual_unknownType_returnsNotFound`;
    - `addManual_nonFiniteValue_returnsValidation`, for NaN and `Double.POSITIVE_INFINITY`, with nothing inserted;
    - `delete_tombstonesAndHidesRow`: `observe` is empty afterwards; `MeasurementDao.findById` is null; the raw row has `deletedAt == clock.now()` and a higher `localRevision`;
    - `delete_otherOwnersRow_returnsNotFoundAndLeavesItIntact`;
    - `observe_neverReturnsOtherOwnersRows`, the spec's two-owner integration test;
    - `observe_excludesDatesOutsideRange`.
- [ ] **Step 2:** Run `./gradlew :core:data:testDebugUnitTest`. Expected: compilation FAILS.
- [ ] **Step 3:** Implement the interfaces, the mapper (entity ↔ domain, wrapping IDs) and both repositories. Reads use `flow { emitAll(dao.observe…(ownerProvider.currentOwnerId().value, …)) }` followed by a `map`. Writes go through `StampedWriter`.
- [ ] **Step 4:** Run the same command. Expected: all PASS.
- [ ] **Step 5:** Switch the Konsist rules to `strict = true` and run `./gradlew :architecture-tests:test`. Expected: PASS. A rule that now fails because it matches nothing has the wrong selector: fix the selector, don't relax the rule.
- [ ] **Step 6:** Commit: `feat(data): day-boundary and measurement repositories with owner scoping and tombstones`.

### Task 8: Sync seam, app wiring and first-launch bootstrap

**Files:**
- Modify: `core/sync/build.gradle.kts` (apply `app.hilt`), `app/build.gradle.kts` (adds `:core:database`), `app/.../GetFitApplication.kt`, `docs/initial_design.md`
- Create: `core/sync/.../{SyncGateway,SyncableChange,PushResult,PullResult,NoOpSyncGateway}.kt`, `app/.../di/{DatabaseModule,DataModule,SyncModule}.kt`
- Test: `core/sync/src/test/.../NoOpSyncGatewayTest.kt`

**Interfaces:**
- Produces:
  - `interface SyncGateway { suspend fun push(changes: List<SyncableChange>): PushResult; suspend fun pull(sinceRemoteCursor: String?): PullResult }`
  - `data class SyncableChange(val table: String, val rowId: Uuid, val localRevision: Long, val updatedAtEpochMillis: Long, val originDeviceId: Uuid, val deleted: Boolean, val payloadJson: String)`
  - `sealed interface PushResult { data class Accepted(val highestRevision: Long); data class Skipped(val reason: String); data class Failed(val reason: String) }`
  - `sealed interface PullResult { data object Empty; data class Changes(val changes: List<SyncableChange>, val nextCursor: String) }`
  - `class NoOpSyncGateway @Inject constructor() : SyncGateway`. Its `push` returns `PushResult.Skipped(reason = "Sync disabled")` and its `pull` returns `PullResult.Empty`.
  - Module bindings:
    - `DatabaseModule` provides the `@Singleton GetFitDatabase` via `createGetFitDatabase(context, ioDispatcher)` and each DAO.
    - `DataModule`:
      - binds `CurrentOwnerProvider` to `SingleLocalOwnerProvider`;
      - provides a `@Singleton DeviceIdProvider` as `FileDeviceIdProvider(context.noBackupFilesDir, ioDispatcher)`;
      - binds both repositories.
    - `SyncModule` binds `SyncGateway` to `NoOpSyncGateway`.
  - `GetFitApplication.onCreate` launches `dataBootstrap.run()` in `@ApplicationScope`.

- [ ] **Step 1:** Write `NoOpSyncGatewayTest`: `push_isSkippedWithReason` (`Skipped("Sync disabled")`) and `pull_isEmpty`. Add `robolectric.properties` only if a test needs Robolectric; these don't.
- [ ] **Step 2:** Run `./gradlew :core:sync:testDebugUnitTest`. Expected: FAIL (unresolved).
- [ ] **Step 3:** Implement the gateway types and all three app modules, plus the bootstrap launch.
- [ ] **Step 4:** Run `./gradlew check buildHealth assembleDebug assembleRelease`. Expected: all green. Apply any DAGP advice, e.g. `api` vs `implementation` for Room or kotlinx-datetime on `:core:database`.
- [ ] **Step 5: On-device check (emulator-5554).**
  1. `adb shell pm clear com.jrussell.fitness.debug`, then install and launch the debug build.
  2. `adb shell run-as com.jrussell.fitness.debug ls databases no_backup`. Expected: `user.db` (plus `-wal`/`-shm`) and `device_id`.
  3. Read `device_id` with `run-as … cat no_backup/device_id`, force-stop, relaunch and read it again. Expected: unchanged.
  4. `adb logcat -d | grep -E "FATAL|AndroidRuntime"`. Expected: nothing from the app. The crash log stays empty.
  5. The UI is unchanged from M1a.
- [ ] **Step 6: Update `docs/initial_design.md`** under "user.db schema":
  - `UserProfile`: "fields null until entered (row created at first launch)";
  - `Measurement` keys: add `(ownerId, measurementTypeId, localDate)`;
  - `MeasurementType`: "built-ins use UUIDv5 IDs, `localRevision` 0";
  - one sentence on the device ID living in `noBackupFilesDir`.
- [ ] **Step 7:** Commit: `feat: NoOpSyncGateway, user.db wiring and first-launch bootstrap (profile, device id, seed)`.

### Task 9: Final review and merge

- [ ] **Step 1:** Run `./gradlew check buildHealth assembleDebug assembleRelease` on the branch tip. Expected: green.
- [ ] **Step 2:** Final whole-branch review (fresh reviewer, most capable model), with this plan's Review Focus and the ledger's rulings. Fix the Critical and Important findings RED→GREEN in one pass, and ledger the minors.
- [ ] **Step 3:** Fast-forward merge `m1b-data-foundation` into `master` (no tag). Delete the branch and the plan workspace, and update memory `m1-progress` with what M1b closed and what it deferred.
