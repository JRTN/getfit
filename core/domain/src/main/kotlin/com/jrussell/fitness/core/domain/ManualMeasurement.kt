package com.jrussell.fitness.core.domain

import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

/**
 * A reading the user types in.
 *
 * @property typeKey the measurement type, e.g. `MeasurementTypeKeys.WEIGHT`.
 * @property value the reading in the type's canonical unit (kilograms for weight).
 * @property takenAt when it was taken; [timeZone] is where, which decides its day.
 * @property enteredValue the number as typed, before unit conversion (e.g. 181.7).
 * @property enteredUnit the unit as typed (e.g. `"lb"`).
 * @property localDateOverride the day to file the reading under when the user picked a date; null applies
 *   the day-start rule to [takenAt]. A picked date always wins, even for a time before the day start.
 */
data class ManualMeasurement(
    val typeKey: String,
    val value: Double,
    val takenAt: Instant,
    val timeZone: TimeZone,
    val enteredValue: Double?,
    val enteredUnit: String?,
    val note: String?,
    val localDateOverride: LocalDate? = null,
)
