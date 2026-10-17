# 7. Conventions and testing

## Kotlin style: explicit

The codebase is written in an explicit style (spec decision #1). It reads more like Java than idiomatic-terse Kotlin, on purpose: every line says what it is, so it can be read without an IDE.

```kotlin
// Yes
val revision: Long = database.syncSequenceDao().incrementAndGet()
override fun now(): Instant {
    return now
}

// No
val revision = database.syncSequenceDao().incrementAndGet()
override fun now() = now
```

- **Explicit types** on properties, locals and lambda parameters (`{ row: MeasurementEntity -> row.toDomain() }`).
- **Block bodies** with `return`, not expression bodies. ktlint's expression-body rule is disabled in [`.editorconfig`](../../.editorconfig).
- **Named classes** over anonymous objects and long lambdas.
- **Formatting** is ktlint's Android Studio style, with a maximum line length of 120. `./gradlew ktlintFormat` fixes most findings.

## Comments: KDoc that explains why

- Every public class and function has KDoc. Good KDoc says what the thing is for and why it is shaped this way. [`StampedWriter`](../../core/data/src/main/kotlin/com/jrussell/fitness/core/data/write/StampedWriter.kt), [`FileDeviceIdProvider`](../../core/data/src/main/kotlin/com/jrussell/fitness/core/data/write/FileDeviceIdProvider.kt) and every convention plugin are examples.
- Inline comments are for non-obvious reasons ("resolved before the transaction: the first call reads a file, which must not hold the database lock"), not narration.

## Patterns that apply everywhere

| Convention | Rule | Where to see it |
| --- | --- | --- |
| Typed IDs | Every entity ID is a value class over `Uuid` | [`core/model/.../ids/`](../../core/model/src/main/kotlin/com/jrussell/fitness/core/model/ids/) |
| Expected failures are values | Return `Outcome.Failure(DomainError.…)`; throw only for programming errors (e.g. `require` on an impossible argument) | [`Outcome`](../../core/domain/src/main/kotlin/com/jrussell/fitness/core/domain/Outcome.kt), [`DomainError`](../../core/domain/src/main/kotlin/com/jrussell/fitness/core/domain/DomainError.kt) |
| Reads and writes | Repositories expose `Flow<T>` reads and `suspend` writes | [`MeasurementRepository`](../../core/domain/src/main/kotlin/com/jrussell/fitness/core/domain/MeasurementRepository.kt) |
| Injected dispatchers | Never name `Dispatchers.IO`; inject `@IoDispatcher` / `@DefaultDispatcher` (detekt enforces it) | [`CoroutineQualifiers`](../../core/data/src/main/kotlin/com/jrussell/fitness/core/data/di/CoroutineQualifiers.kt) |
| Injected clock | Use `kotlin.time.Clock` from DI, never `Clock.System` in logic, so time is controllable in tests | [`UuidIdGenerator`](../../core/data/src/main/kotlin/com/jrussell/fitness/core/data/id/UuidIdGenerator.kt) |
| Derived values are never stored | Trend weight, e1RM, daily totals and similar are computed at query time from raw rows, so changing a formula recomputes history | Spec, convention 11 |
| Canonical units | Store kilograms (or kcal, ml, seconds, metres) at full precision, plus what the user typed (`enteredValue`, `enteredUnit`); display units are presentation only | [`ManualMeasurement`](../../core/domain/src/main/kotlin/com/jrussell/fitness/core/domain/ManualMeasurement.kt) |
| Strings | Every user-visible string is a resource; numbers use locale-aware formatting | `res/values/strings.xml` in each module |
| No colour literals | Colours come from the theme (detekt enforces it) | [Design system](05-design-system.md) |

## Testing

### The stack

| Where | Framework | Why |
| --- | --- | --- |
| Pure-Kotlin modules (`:core:model`, `:core:domain`) | JUnit 5 + AssertK | Plain JVM; JUnit 5 is the modern default |
| Android modules | JUnit 4 + Robolectric + AssertK | Robolectric runs Android APIs (Room, Context, Compose) on the JVM; it is built on JUnit 4 |
| Flows | Turbine | Readable assertions on emissions (`awaitItem()`) |
| Coroutines | kotlinx-coroutines-test | `runTest`, virtual time, `StandardTestDispatcher` |
| Compose semantics | Compose UI test (v2 rule) under Robolectric | Checks roles and states, e.g. that radio buttons read as radio buttons |

### Principles

- **Test first.** Each behaviour starts as a failing test; the code is written to pass it. A test that passes before the code exists is testing nothing. Bug fixes start with a test that reproduces the bug (see [`MeasurementRepositoryBootstrapTest`](../../core/data/src/test/kotlin/com/jrussell/fitness/core/data/repository/MeasurementRepositoryBootstrapTest.kt), written to reproduce a review finding).
- **Real code over mocks.** There is no mocking framework.
  - Database code is tested against a real in-memory Room database.
  - Collaborators are real classes, or hand-written fakes in [`:core:testing`](../../core/testing/): `FakeClock`, `FakeUserPreferencesRepository`, `MainDispatcherRule`.
  - Each repository interface gets one fake when the first ViewModel needs it.
- **Test names** read `subject_condition_expectedResult`, for example `addManual_beforeDayStart_assignsPreviousLocalDate`.
- **One behaviour per test,** asserting outcomes rather than calls.

### Recipes

**A DAO test.** Robolectric with an in-memory database; see [`MeasurementDaoTest`](../../core/database/src/test/kotlin/com/jrussell/fitness/core/database/dao/MeasurementDaoTest.kt) and its [`Fixtures.kt`](../../core/database/src/test/kotlin/com/jrussell/fitness/core/database/Fixtures.kt).

```kotlin
@RunWith(RobolectricTestRunner::class)
class MeasurementDaoTest {
    private val database: GetFitDatabase = inMemoryGetFitDatabase()
    @After fun closeDatabase() { database.close() }
    @Test fun observe_filtersOwnerTypeRangeAndTombstones() = runTest { … }
}
```

**A repository test.** [`RepositoryTestHarness`](../../core/data/src/test/kotlin/com/jrussell/fitness/core/data/repository/RepositoryTestHarness.kt) builds the real data graph (`StampedWriter`, owner provider, seeder, repositories) over an in-memory database with a `FakeClock`, exactly as `:app` wires it.

**A ViewModel test.** Use a fake repository plus `MainDispatcherRule`; see [`SettingsViewModelTest`](../../feature/settings/src/test/kotlin/com/jrussell/fitness/feature/settings/SettingsViewModelTest.kt).

### Gotchas

- New module with Robolectric tests: add `src/test/resources/robolectric.properties` with `sdk=36`.
- Room `@Query` flows re-run on any write to their table. Use `first()` for a one-shot read and Turbine for "emits again when…".
- `assertFailure { }` takes a non-suspending lambda. Inside `runTest`, use `runCatching { … }.exceptionOrNull()`.

Next: [Development workflow](08-development-workflow.md).
