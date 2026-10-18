# 4. Data layer

GetFit is local-first: the phone holds the only copy of the data. This chapter explains how that data is stored so it is safe today and ready to sync tomorrow.

## Three stores

| Store | Module | Holds | Status |
| --- | --- | --- | --- |
| `user.db` (Room/SQLite) | [`:core:database`](../../core/database/) | Everything the user owns, plus seeded reference rows | Schema v1 |
| Preferences DataStore | [`:core:datastore`](../../core/datastore/) | UI-only preferences: theme mode, palette, weight unit, and each screen's remembered date range | In use |
| `reference.db` (read-only) | `:core:reference` | Bundled USDA food catalog, replaced wholesale with each dataset version | M4 |

**Rule of thumb:** a setting that changes how *past* data is evaluated (day start, energy mode, targets) is a **versioned row in `user.db`** with an effective date, never a DataStore preference. Changing it then never re-dates history, and it travels with backups and sync.

## Layering inside the data layer

```
:core:domain    MeasurementRepository (interface)          ← features depend on this
                    ▲ implements
:core:data      DefaultMeasurementRepository
                    │ uses StampedWriter, CurrentOwnerProvider, ReferenceSeeder, IdGenerator
:core:database  MeasurementDao, MeasurementEntity          ← Room types never leave :core:data
```

- **Entities** (`@Entity` classes, e.g. [`MeasurementEntity`](../../core/database/src/main/kotlin/com/jrussell/fitness/core/database/entity/MeasurementEntity.kt)) exist only in `:core:database`. They use raw `Uuid` IDs and carry the sync columns.
- **Domain models** (e.g. [`Measurement`](../../core/model/src/main/kotlin/com/jrussell/fitness/core/model/Measurement.kt)) are plain data classes in `:core:model`, with typed IDs and no annotations. They have no owner or sync columns.
- **Mappers** in `:core:data` ([`MeasurementMapper`](../../core/data/src/main/kotlin/com/jrussell/fitness/core/data/repository/MeasurementMapper.kt)) translate between the two.
- **Reads** are `Flow`s and **writes** are `suspend` functions returning [`Outcome`](../../core/domain/src/main/kotlin/com/jrussell/fitness/core/domain/Outcome.kt).

## Standard columns and `SyncMetadata`

Every user-owned table carries the same columns, so that sync can be added later with no migration:

| Column | Meaning |
| --- | --- |
| `id` | Primary key: a UUIDv7 (time-ordered) for new rows, or a UUIDv5 for deterministic rows |
| `ownerId` | The profile that owns the row; leads every composite index |
| `createdAt`, `updatedAt` | Epoch milliseconds; `updatedAt` is the future last-writer-wins key |
| `deletedAt` | Tombstone. Non-null means deleted. Rows are never physically deleted, so a deletion can sync |
| `localRevision` | The global revision of the transaction that last wrote the row |
| `originDeviceId` | The install that made the last write; the sync tie-breaker |

The five sync columns are one embedded class, [`SyncMetadata`](../../core/database/src/main/kotlin/com/jrussell/fitness/core/database/entity/SyncMetadata.kt).

## Revision stamping: `StampedWriter`

[`StampedWriter`](../../core/data/src/main/kotlin/com/jrussell/fitness/core/data/write/StampedWriter.kt) is the only way repositories write:

```kotlin
stampedWriter.write { stamp: WriteStamp ->
    dao.insert(entity.copy(sync = stamp.toSyncMetadata()))
}
```

Each call is one `withTransaction`:
1. The block runs inside the transaction.
2. [`SyncSequenceDao.incrementAndGet()`](../../core/database/src/main/kotlin/com/jrussell/fitness/core/database/infrastructure/SyncSequenceDao.kt) bumps the single-row global counter.
3. The block receives a [`WriteStamp`](../../core/data/src/main/kotlin/com/jrussell/fitness/core/data/write/WriteStamp.kt) holding revision, now and device ID.
4. If the block throws, everything rolls back, including the revision.

Why a global counter? A future sync remote remembers the highest revision it has received. Its next push is "every row with `localRevision` greater than that", tombstones included. A brand-new remote starts at 0 and receives everything, with no special code.

## Owner scoping

- [`CurrentOwnerProvider`](../../core/data/src/main/kotlin/com/jrussell/fitness/core/data/owner/CurrentOwnerProvider.kt) answers "who owns the data being read or written". Repositories call it; callers never pass an owner.
- In v1 it is [`SingleLocalOwnerProvider`](../../core/data/src/main/kotlin/com/jrussell/fitness/core/data/owner/SingleLocalOwnerProvider.kt). It finds the single `UserProfile` row, or creates one on first use; the profile's `id` *is* the owner ID. Concurrent first calls create exactly one profile (a `Mutex`, plus a re-check inside the transaction).
- Every query in [`core/database/.../dao/`](../../core/database/src/main/kotlin/com/jrussell/fitness/core/database/dao/) takes `ownerId`. The Konsist rule [`DaoOwnerScopingRulesTest`](../../architecture-tests/src/test/kotlin/com/jrussell/fitness/architecture/DaoOwnerScopingRulesTest.kt) enforces this.
- The few queries that cannot be owner-scoped live in [`core/database/.../infrastructure/`](../../core/database/src/main/kotlin/com/jrussell/fitness/core/database/infrastructure/):
  - the revision counter;
  - dataset versions;
  - [`LocalProfileDao`](../../core/database/src/main/kotlin/com/jrussell/fitness/core/database/infrastructure/LocalProfileDao.kt), whose job is to *discover* the owner.

The two-owner tests in [`DefaultMeasurementRepositoryTest`](../../core/data/src/test/kotlin/com/jrussell/fitness/core/data/repository/DefaultMeasurementRepositoryTest.kt) insert another owner's rows and prove that reads never see them and deletes cannot touch them.

## Identifiers

- Every ID is a value class over `kotlin.uuid.Uuid`: [`OwnerId`](../../core/model/src/main/kotlin/com/jrussell/fitness/core/model/ids/OwnerId.kt), `MeasurementId`, `MeasurementTypeId`, `DeviceId`. Passing a measurement ID where an owner ID is expected does not compile.
- [`UuidIdGenerator`](../../core/data/src/main/kotlin/com/jrussell/fitness/core/data/id/UuidIdGenerator.kt) makes two kinds:
  - **UUIDv7** (`newTimeOrderedId`) for new rows. They are time-ordered, so they index well and sort by creation. The timestamp comes from the injected `Clock`, so tests are deterministic.
  - **UUIDv5** (`nameBasedId`): the same name always gives the same ID on every device. Built-in rows (`measurement-type:weight`) and, later, copied reference foods use it, so two devices agree on their IDs.
- `UuidIdGenerator.APP_NAMESPACE` must never change: every name-based ID is derived from it.

## Time

| Kotlin type | Stored as | Converter |
| --- | --- | --- |
| `kotlin.time.Instant` | INTEGER epoch milliseconds | [`Converters`](../../core/database/src/main/kotlin/com/jrussell/fitness/core/database/Converters.kt) |
| `kotlinx.datetime.LocalDate` | INTEGER epoch day (cheap range queries) | same |
| `kotlinx.datetime.TimeZone` | TEXT IANA ID | same |
| `Uuid` | TEXT | same |

Every timestamped user entry stores its own time zone, so "which day was this?" survives travel.

### "Now" and "today": `WallClock`

ViewModels never call `Clock.System` or `TimeZone.currentSystemDefault()`. They take [`WallClock`](../../core/domain/src/main/kotlin/com/jrussell/fitness/core/domain/time/WallClock.kt) (bound to `SystemWallClock` in `:core:data`), which gives the current instant *and* the zone, so tests control both with `FakeWallClock`. [`observeToday(clock, dayBoundary)`](../../core/domain/src/main/kotlin/com/jrussell/fitness/core/domain/Today.kt) turns that into "today after the day start" as a `Flow`, re-checked every minute, so Home and Body move to the new day while open.

### Day assignment

People log after midnight that belongs to "yesterday". The user picks a **day start** (e.g. 04:00), and [`localDateFor(instant, zone, dayStartMinutes)`](../../core/domain/src/main/kotlin/com/jrussell/fitness/core/domain/DayAssignment.kt) decides the day:
- if the wall-clock time is at or after the day start, the entry belongs to that calendar date;
- otherwise it belongs to the day before.

The rule compares wall-clock minutes only, with no zone arithmetic, so DST changes can never shift a date. [`DayAssignmentTest`](../../core/domain/src/test/kotlin/com/jrussell/fitness/core/domain/DayAssignmentTest.kt) covers the boundary, a midnight start, spring-forward, and the zone deciding the date.

**A chosen date wins.** When the user backfills an entry and picks its date, [`ManualMeasurement.localDateOverride`](../../core/domain/src/main/kotlin/com/jrussell/fitness/core/domain/ManualMeasurement.kt) files it under that date regardless of the day-start rule: "Oct 10 at 01:30" with a 04:00 start lands on Oct 10, because that is the day the user meant. Entries made "now" leave it `null` and follow the rule.

### Versioned settings

The day start is versioned: [`DayBoundarySettingEntity`](../../core/database/src/main/kotlin/com/jrussell/fitness/core/database/entity/DayBoundarySettingEntity.kt) rows each have an `effectiveFrom` date. Evaluating any day uses the row with the greatest `effectiveFrom` on or before it. With no row, the day starts at midnight (`DEFAULT_DAY_START_MINUTES = 0`). Setting the same date twice updates that row rather than inserting a second one. See [`DefaultDayBoundaryRepository`](../../core/data/src/main/kotlin/com/jrussell/fitness/core/data/repository/DefaultDayBoundaryRepository.kt). Energy settings and targets will follow the same pattern.

## Metrics: the registry and its providers

Charts and summary cards never read repositories directly. They ask a [`MetricRegistry`](../../core/domain/src/main/kotlin/com/jrussell/fitness/core/domain/metric/MetricContract.kt) for a metric by ID, over a date range, in day/week/month buckets, and get a `Flow<List<DataPoint>>` back (values in canonical units, one point per bucket). This is the seam M2's cross-domain chart builder will use to put any metric next to any other.

- [`DefaultMetricRegistry`](../../core/data/src/main/kotlin/com/jrussell/fitness/core/data/metric/DefaultMetricRegistry.kt) routes each request to the [`MetricSeriesProvider`](../../core/domain/src/main/kotlin/com/jrussell/fitness/core/domain/metric/MetricContract.kt) that declares that metric. Providers are collected into a set by Hilt ([`MetricsModule`](../../app/src/main/kotlin/com/jrussell/fitness/di/MetricsModule.kt)); adding a domain means adding a provider, not editing the registry.
- [`BodyMetricProvider`](../../core/data/src/main/kotlin/com/jrussell/fitness/core/data/metric/BodyMetricProvider.kt) publishes `body.weight` (each day's last reading) and `body.trend_weight` (the gap-adjusted moving average of decision #28, kept for M5's expenditure estimate and not shown to users). Both are built from the live measurement query, so a new weigh-in reaches every watching screen.
- Pure math lives in [`Transforms.kt`](../../core/domain/src/main/kotlin/com/jrussell/fitness/core/domain/metric/Transforms.kt): bucketing, `trendWeight`, and `linearTrend`, the least-squares line every chart draws as its dotted trend and whose slope is every "per week" rate shown. `rangeChange` gives a card's first and last values in a range and that slope (0 with fewer than two dates).
- Derived values are never stored (convention 11): change the formula and all history follows.

## Remembered date ranges

Home and Body each remember their range choice across restarts, separately. Home offers All, Today, 7 days, 30 days and From…; Body has no Today, since its latest-weight card already shows the newest weigh-in (`RangeScreen.offersToday`). [`DateRangeRepository`](../../core/domain/src/main/kotlin/com/jrussell/fitness/core/domain/range/DateRangeRepository.kt) stores it as stable text (`"ALL"`, `"LAST_DAYS:30"`, `"FROM:2026-06-01"`) under `range_<screen>` in the preferences DataStore ([`RangeSelectionDataSource`](../../core/datastore/src/main/kotlin/com/jrussell/fitness/core/datastore/RangeSelectionDataSource.kt)). Unreadable text, or a choice the screen doesn't offer (an old Today on Body), falls back to the screen's default (Home: Today, Body: All), so a bad value can never break a screen.

## Built-in reference rows and seeding

- `MeasurementType` is a *mixed catalog*: built-in rows have no owner; user-defined rows belong to an owner.
- Built-ins get deterministic UUIDv5 IDs, `localRevision = 0` (a first sync push, which sends `> 0`, skips them) and no origin device.
- [`ReferenceSeeder`](../../core/data/src/main/kotlin/com/jrussell/fitness/core/data/seed/ReferenceSeeder.kt) inserts them once per dataset version, recorded in the `DatasetVersion` table. To ship more built-ins, raise the version constant and add rows; existing rows are never overwritten.

## The device ID

[`FileDeviceIdProvider`](../../core/data/src/main/kotlin/com/jrussell/fitness/core/data/write/FileDeviceIdProvider.kt) keeps a random UUID in `no_backup/device_id`. Android never backs that folder up. A backup restored onto a new phone, or after a reinstall, therefore gets a fresh device ID, and two devices never share a sync tie-breaker.

## The sync seam

[`SyncGateway`](../../core/sync/src/main/kotlin/com/jrussell/fitness/core/sync/SyncGateway.kt) defines `push(changes)` and `pull(sinceCursor)`. v1 binds [`NoOpSyncGateway`](../../core/sync/src/main/kotlin/com/jrussell/fitness/core/sync/NoOpSyncGateway.kt), which skips pushes and returns nothing on pulls. A future server (the spec suggests Spring Boot) implements the same interface over HTTP.

Everything sync needs is already being recorded on every row: UUID keys, tombstones, revisions and origin devices. The empty `SyncCursor` table is where each remote's progress will go.

## Schema and migrations

- The schema is exported to [`core/database/schemas/`](../../core/database/schemas/) on every build and committed. [`SchemaJsonTest`](../../core/database/src/test/kotlin/com/jrussell/fitness/core/database/SchemaJsonTest.kt) checks that the committed JSON matches the compiled database.
- **Version 1 can still change** until `v0.1.0` is tagged at the end of M1, because no real data exists before then.
- From `v0.1.0` on:
  - every schema change is a numbered `Migration`, tested with Room's `MigrationTestHelper`;
  - `fallbackToDestructiveMigration` is forbidden, and a custom detekt rule fails the build if anyone writes it.

## Checklist: adding a user-owned table

1. **Entity** in `core/database/.../entity/`: `id`, `ownerId`, your columns, `@Embedded val sync: SyncMetadata`, and indexes led by `ownerId`. Add it to `GetFitDatabase.entities`.
2. **DAO** in `core/database/.../dao/`: every `@Query` takes `ownerId` (Konsist will remind you), and reads filter `deletedAt IS NULL`. Deletes are tombstone `UPDATE`s, never `DELETE`.
3. **Domain model** in `:core:model` (typed ID, no annotations) and a **repository interface** in `:core:domain` (Flow reads, `suspend` writes returning `Outcome`).
4. **Repository implementation** in `:core:data`: get the owner from `CurrentOwnerProvider`, write only through `StampedWriter`, and map entities with a mapper.
5. **Binding** in `:app`'s `DataModule`.
6. **Tests:** DAO tests (in-memory Room under Robolectric) and repository tests including a two-owner case.
7. **Schema:** commit the regenerated JSON (and, after `v0.1.0`, a migration plus its test).

Next: [Design system](05-design-system.md).
