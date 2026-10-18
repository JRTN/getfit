package com.jrussell.fitness.core.data.repository

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isInstanceOf
import com.jrussell.fitness.core.data.TEST_NOW
import com.jrussell.fitness.core.database.entity.MeasurementEntity
import com.jrussell.fitness.core.database.entity.MeasurementTypeEntity
import com.jrussell.fitness.core.domain.ManualMeasurement
import com.jrussell.fitness.core.domain.Outcome
import com.jrussell.fitness.core.model.DailyAggregation
import com.jrussell.fitness.core.model.Measurement
import com.jrussell.fitness.core.model.MeasurementCategory
import com.jrussell.fitness.core.model.MeasurementTypeKeys
import com.jrussell.fitness.core.model.StandardUnit
import com.jrussell.fitness.core.model.ids.MeasurementId
import kotlin.uuid.Uuid
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The repository used before (or while) the startup bootstrap runs: nothing here seeds first.
 * Pins the final-review finding that a type missing at collection time froze the flow forever.
 */
@RunWith(RobolectricTestRunner::class)
class MeasurementRepositoryBootstrapTest {
    private val harness: RepositoryTestHarness = RepositoryTestHarness()
    private val repository: DefaultMeasurementRepository = harness.measurementRepository
    private val day: LocalDate = LocalDate(2026, 10, 17)

    @After
    fun close() {
        harness.close()
    }

    @Test
    fun addManual_beforeBootstrap_stillFindsBuiltInType() = runTest {
        val outcome: Outcome<MeasurementId> = repository.addManual(
            ManualMeasurement(MeasurementTypeKeys.WEIGHT, 82.4, TEST_NOW, TimeZone.UTC, null, null, null),
        )

        assertThat(outcome).isInstanceOf<Outcome.Success<MeasurementId>>()
    }

    @Test
    fun observe_typeCreatedAfterCollectionStarts_emitsItsReadings() = runTest {
        val owner: Uuid = harness.ownerProvider.currentOwnerId().value

        repository.observeMeasurements("waist", day, day).test {
            assertThat(awaitItem()).isEmpty()

            val waist: MeasurementTypeEntity = waistTypeFor(owner)
            harness.database.measurementTypeDao().insertAll(listOf(waist))
            val reading: MeasurementEntity = aMeasurementFor(owner, waist.id, day)
            harness.database.measurementDao().insert(reading)

            assertThat(expectMostRecentReadings(this)).containsExactly(MeasurementId(reading.id))
            cancelAndIgnoreRemainingEvents()
        }
    }

    private suspend fun expectMostRecentReadings(
        turbine: app.cash.turbine.ReceiveTurbine<List<Measurement>>,
    ): List<MeasurementId> {
        var latest: List<Measurement> = turbine.awaitItem()
        while (latest.isEmpty()) {
            latest = turbine.awaitItem()
        }
        return latest.map { measurement: Measurement -> measurement.id }
    }

    private fun waistTypeFor(owner: Uuid): MeasurementTypeEntity {
        return MeasurementTypeEntity(
            id = Uuid.random(),
            ownerId = owner,
            key = "waist",
            displayName = "Waist",
            canonicalUnit = StandardUnit.METER,
            category = MeasurementCategory.BODY,
            dailyAggregation = DailyAggregation.LAST,
            hasSecondaryValue = false,
            referenceRangeLow = null,
            referenceRangeHigh = null,
            decimals = 2,
            healthConnectRecordType = null,
            createdAt = TEST_NOW,
            updatedAt = TEST_NOW,
            deletedAt = null,
            localRevision = 1,
            originDeviceId = owner,
        )
    }
}
