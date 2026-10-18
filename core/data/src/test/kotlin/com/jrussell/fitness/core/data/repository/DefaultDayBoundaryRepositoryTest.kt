package com.jrussell.fitness.core.data.repository

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isNotNull
import com.jrussell.fitness.core.data.TEST_NOW
import com.jrussell.fitness.core.data.count
import com.jrussell.fitness.core.database.entity.DayBoundarySettingEntity
import com.jrussell.fitness.core.domain.DomainError
import com.jrussell.fitness.core.domain.Outcome
import kotlin.time.Duration.Companion.hours
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DefaultDayBoundaryRepositoryTest {
    private val harness: RepositoryTestHarness = RepositoryTestHarness()
    private val repository: DefaultDayBoundaryRepository = harness.dayBoundaryRepository

    @After
    fun close() {
        harness.close()
    }

    @Test
    fun observe_noRows_isMidnight() = runTest {
        assertThat(repository.observeDayStartMinutes(LocalDate(2026, 10, 17)).first()).isEqualTo(0)
    }

    @Test
    fun observe_usesLatestEffectiveOnOrBefore() = runTest {
        repository.setDayStart(240, LocalDate(2026, 1, 1))
        repository.setDayStart(300, LocalDate(2026, 6, 1))

        assertThat(repository.observeDayStartMinutes(LocalDate(2026, 5, 31)).first()).isEqualTo(240)
        assertThat(repository.observeDayStartMinutes(LocalDate(2026, 6, 1)).first()).isEqualTo(300)
        assertThat(repository.observeDayStartMinutes(LocalDate(2025, 12, 31)).first()).isEqualTo(0)
    }

    @Test
    fun setDayStart_sameEffectiveDate_updatesInsteadOfDuplicating() = runTest {
        val day: LocalDate = LocalDate(2026, 1, 1)
        repository.setDayStart(240, day)
        val firstRevision: Long = currentRow(day).sync.localRevision
        harness.clock.advanceBy(1.hours)

        val outcome: Outcome<Unit> = repository.setDayStart(300, day)

        assertThat(outcome).isEqualTo(Outcome.Success(Unit))
        assertThat(harness.database.count("SELECT COUNT(*) FROM DayBoundarySetting")).isEqualTo(1)
        val row: DayBoundarySettingEntity = currentRow(day)
        assertThat(row.dayStartMinutes).isEqualTo(300)
        assertThat(row.sync.createdAt).isEqualTo(TEST_NOW)
        assertThat(row.sync.updatedAt).isEqualTo(TEST_NOW + 1.hours)
        assertThat(row.sync.localRevision).isGreaterThan(firstRevision)
    }

    @Test
    fun setDayStart_outOfRange_returnsValidation() = runTest {
        listOf(-1, 1440).forEach { minutes: Int ->
            val outcome: Outcome<Unit> = repository.setDayStart(minutes, LocalDate(2026, 1, 1))

            val error: DomainError? = (outcome as? Outcome.Failure)?.error
            assertThat((error as? DomainError.Validation)?.field).isEqualTo("dayStartMinutes")
        }
        assertThat(harness.database.count("SELECT COUNT(*) FROM DayBoundarySetting")).isEqualTo(0)
    }

    @Test
    fun observe_otherOwnersSetting_isIgnored() = runTest {
        harness.database.dayBoundarySettingDao().insert(aDayBoundaryFor(Uuid.random(), LocalDate(2026, 1, 1), 240))

        assertThat(repository.observeDayStartMinutes(LocalDate(2026, 10, 17)).first()).isEqualTo(0)
    }

    @Test
    fun observeDayStartMinutes_identicalWrites_doNotReEmit() = runTest {
        val day: LocalDate = LocalDate(2026, 1, 1)

        repository.observeDayStartMinutes(LocalDate(2026, 10, 17)).test {
            assertThat(awaitItem()).isEqualTo(0)
            repository.setDayStart(240, day)
            assertThat(awaitItem()).isEqualTo(240)
            repository.setDayStart(240, day)
            // A later, different value must be the very next emission: no repeated 240 in between.
            repository.setDayStart(300, day)
            assertThat(awaitItem()).isEqualTo(300)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private suspend fun currentRow(day: LocalDate): DayBoundarySettingEntity {
        val owner: Uuid = harness.ownerProvider.currentOwnerId().value
        val row: DayBoundarySettingEntity? = harness.database.dayBoundarySettingDao().findByEffectiveFrom(owner, day)
        assertThat(row).isNotNull()
        return row!!
    }
}
