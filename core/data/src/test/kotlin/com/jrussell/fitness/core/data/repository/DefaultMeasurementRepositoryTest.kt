package com.jrussell.fitness.core.data.repository

import android.database.Cursor
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.jrussell.fitness.core.data.count
import com.jrussell.fitness.core.database.entity.MeasurementEntity
import com.jrussell.fitness.core.domain.DomainError
import com.jrussell.fitness.core.domain.ManualMeasurement
import com.jrussell.fitness.core.domain.Outcome
import com.jrussell.fitness.core.model.Measurement
import com.jrussell.fitness.core.model.MeasurementSource
import com.jrussell.fitness.core.model.MeasurementTypeKeys
import com.jrussell.fitness.core.model.ids.MeasurementId
import com.jrussell.fitness.core.model.ids.MeasurementTypeId
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DefaultMeasurementRepositoryTest {
    private val harness: RepositoryTestHarness = RepositoryTestHarness()
    private val repository: DefaultMeasurementRepository = harness.measurementRepository
    private val toronto: TimeZone = TimeZone.of("America/Toronto")
    private val october: ClosedRange<LocalDate> = LocalDate(2026, 10, 1)..LocalDate(2026, 10, 31)
    private val weightTypeId: Uuid = harness.idGenerator.nameBasedId("measurement-type:weight")

    @Before
    fun seed() = runTest {
        harness.seeder.seed()
    }

    @After
    fun close() {
        harness.close()
    }

    @Test
    fun addManual_thenObserve_returnsIt() = runTest {
        val takenAt: Instant = at("2026-10-17T08:00")

        val outcome: Outcome<MeasurementId> =
            repository.addManual(weight(82.4, takenAt).copy(enteredValue = 181.7, enteredUnit = "lb"))

        val id: MeasurementId = (outcome as Outcome.Success).value
        assertThat(observeOctober()).containsExactly(
            Measurement(
                id = id,
                typeId = MeasurementTypeId(weightTypeId),
                value = 82.4,
                secondaryValue = null,
                startTime = takenAt,
                endTime = takenAt,
                timeZone = toronto,
                localDate = LocalDate(2026, 10, 17),
                source = MeasurementSource.MANUAL,
                enteredValue = 181.7,
                enteredUnit = "lb",
                note = null,
            ),
        )
    }

    @Test
    fun addManual_beforeDayStart_assignsPreviousLocalDate() = runTest {
        harness.dayBoundaryRepository.setDayStart(240, LocalDate(2026, 1, 1))

        repository.addManual(weight(82.4, at("2026-10-17T01:30")))

        assertThat(observeOctober().single().localDate).isEqualTo(LocalDate(2026, 10, 16))
    }

    @Test
    fun addManual_unknownType_returnsNotFound() = runTest {
        val waist: ManualMeasurement = weight(80.0, at("2026-10-17T08:00")).copy(typeKey = "waist")

        val outcome: Outcome<MeasurementId> = repository.addManual(waist)

        assertThat(outcome).isEqualTo(Outcome.Failure(DomainError.NotFound("MeasurementType", "waist")))
    }

    @Test
    fun addManual_nonFiniteValue_returnsValidation() = runTest {
        listOf(Double.NaN, Double.POSITIVE_INFINITY).forEach { value: Double ->
            val outcome: Outcome<MeasurementId> = repository.addManual(weight(value, at("2026-10-17T08:00")))

            val error: DomainError? = (outcome as? Outcome.Failure)?.error
            assertThat((error as? DomainError.Validation)?.field).isEqualTo("value")
        }
        assertThat(harness.database.count("SELECT COUNT(*) FROM Measurement")).isEqualTo(0)
    }

    @Test
    fun delete_tombstonesAndHidesRow() = runTest {
        val id: MeasurementId = (repository.addManual(weight(82.4, at("2026-10-17T08:00"))) as Outcome.Success).value
        val addedRevision: Long = rawRevision()
        harness.clock.advanceBy(1.hours)

        val outcome: Outcome<Unit> = repository.delete(id)

        assertThat(outcome).isEqualTo(Outcome.Success(Unit))
        assertThat(observeOctober()).isEmpty()
        val owner: Uuid = harness.ownerProvider.currentOwnerId().value
        assertThat(harness.database.measurementDao().findById(owner, id.value)).isNull()
        assertThat(rawDeletedAt()).isEqualTo(harness.clock.now().toEpochMilliseconds())
        assertThat(rawRevision()).isGreaterThan(addedRevision)
    }

    @Test
    fun delete_otherOwnersRow_returnsNotFoundAndLeavesItIntact() = runTest {
        val otherOwner: Uuid = Uuid.random()
        val theirs: MeasurementEntity = aMeasurementFor(otherOwner, weightTypeId, LocalDate(2026, 10, 17))
        harness.database.measurementDao().insert(theirs)

        val outcome: Outcome<Unit> = repository.delete(MeasurementId(theirs.id))

        assertThat(outcome).isEqualTo(Outcome.Failure(DomainError.NotFound("Measurement", theirs.id.toString())))
        assertThat(harness.database.measurementDao().findById(otherOwner, theirs.id)).isNotNull()
    }

    @Test
    fun observe_neverReturnsOtherOwnersRows() = runTest {
        harness.database.measurementDao().insert(aMeasurementFor(Uuid.random(), weightTypeId, LocalDate(2026, 10, 17)))
        val mine: MeasurementId = (repository.addManual(weight(82.4, at("2026-10-17T08:00"))) as Outcome.Success).value

        assertThat(observeOctober().map { measurement: Measurement -> measurement.id }).containsExactly(mine)
    }

    @Test
    fun observe_excludesDatesOutsideRange() = runTest {
        repository.addManual(weight(82.4, at("2026-10-17T08:00")))

        assertThat(observeWeight(LocalDate(2026, 10, 18), LocalDate(2026, 10, 31))).isEmpty()
        assertThat(observeWeight(LocalDate(2026, 10, 1), LocalDate(2026, 10, 16))).isEmpty()
    }

    private suspend fun observeWeight(from: LocalDate, to: LocalDate): List<Measurement> {
        return repository.observeMeasurements(MeasurementTypeKeys.WEIGHT, from, to).first()
    }

    private fun at(local: String): Instant {
        return LocalDateTime.parse(local).toInstant(toronto)
    }

    private fun weight(value: Double, takenAt: Instant): ManualMeasurement {
        return ManualMeasurement(
            typeKey = MeasurementTypeKeys.WEIGHT,
            value = value,
            takenAt = takenAt,
            timeZone = toronto,
            enteredValue = null,
            enteredUnit = null,
            note = null,
        )
    }

    private suspend fun observeOctober(): List<Measurement> {
        return repository.observeMeasurements(MeasurementTypeKeys.WEIGHT, october.start, october.endInclusive).first()
    }

    private fun rawRevision(): Long {
        return harness.database.openHelper.readableDatabase.query("SELECT localRevision FROM Measurement")
            .use { cursor: Cursor ->
                cursor.moveToFirst()
                cursor.getLong(0)
            }
    }

    private fun rawDeletedAt(): Long? {
        return harness.database.openHelper.readableDatabase.query("SELECT deletedAt FROM Measurement")
            .use { cursor: Cursor ->
                cursor.moveToFirst()
                if (cursor.isNull(0)) null else cursor.getLong(0)
            }
    }
}
