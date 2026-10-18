package com.jrussell.fitness.core.testing

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isInstanceOf
import com.jrussell.fitness.core.domain.ManualMeasurement
import com.jrussell.fitness.core.domain.Outcome
import com.jrussell.fitness.core.model.Measurement
import com.jrussell.fitness.core.model.MeasurementTypeKeys
import com.jrussell.fitness.core.model.ids.MeasurementId
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Test

class FakeMeasurementRepositoryTest {
    private val repository: FakeMeasurementRepository = FakeMeasurementRepository()
    private val day: LocalDate = LocalDate(2026, 10, 17)

    @Test
    fun addThenObserve_returnsIt() = runTest {
        repository.addManual(weight(82.4))

        val observed: List<Measurement> = repository.observeMeasurements(MeasurementTypeKeys.WEIGHT, day, day).first()

        assertThat(observed).hasSize(1)
    }

    @Test
    fun nonFiniteValue_isRejected() = runTest {
        val outcome: Outcome<MeasurementId> = repository.addManual(weight(Double.NaN))

        assertThat(outcome).isInstanceOf<Outcome.Failure>()
        assertThat(repository.measurements).isEmpty()
    }

    private fun weight(value: Double): ManualMeasurement {
        return ManualMeasurement(
            typeKey = MeasurementTypeKeys.WEIGHT,
            value = value,
            takenAt = Instant.parse("2026-10-17T12:00:00Z"),
            timeZone = TimeZone.UTC,
            enteredValue = null,
            enteredUnit = null,
            note = null,
        )
    }
}
