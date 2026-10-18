package com.jrussell.fitness.core.domain

import com.jrussell.fitness.core.model.Measurement
import com.jrussell.fitness.core.model.ids.MeasurementId
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/** The current user's measurements (weight, and later body fat, heart rate, lab markers). */
interface MeasurementRepository {
    /**
     * Readings of [typeKey] whose day falls in [from]..[to] (inclusive), oldest first. Emits an
     * empty list for an unknown type key, and again whenever the readings change.
     */
    fun observeMeasurements(typeKey: String, from: LocalDate, to: LocalDate): Flow<List<Measurement>>

    /**
     * Records a reading the user typed in. Fails with [DomainError.Validation] for a value that
     * (or an entered value) that is not a finite number, and [DomainError.NotFound] for an unknown
     * type key. A [ManualMeasurement.localDateOverride] files the reading under that date.
     */
    suspend fun addManual(entry: ManualMeasurement): Outcome<MeasurementId>

    /** Deletes a reading. Fails with [DomainError.NotFound] if it does not exist or is already deleted. */
    suspend fun delete(id: MeasurementId): Outcome<Unit>
}
