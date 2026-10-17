package com.jrussell.fitness.core.model

import com.jrussell.fitness.core.model.ids.MeasurementId
import com.jrussell.fitness.core.model.ids.MeasurementTypeId
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

/**
 * One reading of a measurement type, in the type's canonical unit (kilograms for weight).
 * [enteredValue] and [enteredUnit] keep what the user typed, e.g. 181.7 lb. [localDate] is the
 * day the reading counts toward, after the day-start rule.
 */
data class Measurement(
    val id: MeasurementId,
    val typeId: MeasurementTypeId,
    val value: Double,
    val secondaryValue: Double?,
    val startTime: Instant,
    val endTime: Instant,
    val timeZone: TimeZone,
    val localDate: LocalDate,
    val source: MeasurementSource,
    val enteredValue: Double?,
    val enteredUnit: String?,
    val note: String?,
)
