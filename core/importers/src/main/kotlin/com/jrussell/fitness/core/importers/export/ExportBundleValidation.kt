package com.jrussell.fitness.core.importers.export

import com.jrussell.fitness.core.model.DailyAggregation
import com.jrussell.fitness.core.model.MeasurementCategory
import com.jrussell.fitness.core.model.MeasurementSource
import com.jrussell.fitness.core.model.Sex
import com.jrussell.fitness.core.model.displayUnitOf
import com.jrussell.fitness.core.model.measurementUnitOf
import kotlin.uuid.Uuid
import kotlinx.datetime.TimeZone

/** Why a row in an otherwise readable bundle can't be restored. */
sealed interface BundleProblem {
    /** A day start outside 0..1439 minutes. */
    data class DayStartOutOfRange(val id: Uuid) : BundleProblem

    /** A time-zone ID this device doesn't know. */
    data class UnknownTimeZone(val id: Uuid, val zone: String) : BundleProblem

    /** A measurement of a type that is neither built in nor in the bundle. */
    data class UnknownMeasurementType(val id: Uuid, val typeId: Uuid) : BundleProblem

    /** A value or entered value that is NaN or infinite. */
    data class NonFiniteValue(val id: Uuid) : BundleProblem

    /** An enum name or unit code in [field] that no version of GetFit writes. */
    data class UnknownName(val field: String, val value: String) : BundleProblem
}

private val VALID_DAY_START_MINUTES: IntRange = 0..1439

/**
 * Every problem in [bundle] that would stop its rows from being stored, or empty if it can be
 * restored. [builtInTypeIds] are the measurement types every install seeds; a measurement may
 * refer to one of those or to a type in the bundle.
 */
fun validateExportBundle(bundle: ExportBundle, builtInTypeIds: Set<Uuid>): List<BundleProblem> {
    val knownTypes: Set<Uuid> = builtInTypeIds + bundle.measurementTypes.map { type: MeasurementTypeRecord -> type.id }
    return bundle.profiles.flatMap(::profileProblems) +
        bundle.dayBoundarySettings.flatMap(::dayBoundaryProblems) +
        bundle.measurementTypes.flatMap(::typeProblems) +
        bundle.measurements.flatMap { measurement: MeasurementRecord -> measurementProblems(measurement, knownTypes) }
}

private fun profileProblems(profile: ProfileRecord): List<BundleProblem> {
    return listOfNotNull(profile.sex?.let { sex: String -> unknownName("sex", sex, Sex.entries.map { it.name }) })
}

private fun dayBoundaryProblems(setting: DayBoundaryRecord): List<BundleProblem> {
    return if (setting.dayStartMinutes in VALID_DAY_START_MINUTES) {
        emptyList()
    } else {
        listOf(BundleProblem.DayStartOutOfRange(setting.id))
    }
}

private fun typeProblems(type: MeasurementTypeRecord): List<BundleProblem> {
    return listOfNotNull(
        BundleProblem.UnknownName("canonicalUnit", type.canonicalUnit)
            .takeIf { measurementUnitOf(type.canonicalUnit) == null },
        unknownName("category", type.category, MeasurementCategory.entries.map { it.name }),
        unknownName("dailyAggregation", type.dailyAggregation, DailyAggregation.entries.map { it.name }),
    )
}

private fun measurementProblems(measurement: MeasurementRecord, knownTypes: Set<Uuid>): List<BundleProblem> {
    val finite: Boolean = measurement.value.isFinite() &&
        measurement.secondaryValue?.isFinite() != false &&
        measurement.enteredValue?.isFinite() != false
    return listOfNotNull(
        BundleProblem.UnknownMeasurementType(measurement.id, measurement.measurementTypeId)
            .takeIf { measurement.measurementTypeId !in knownTypes },
        BundleProblem.NonFiniteValue(measurement.id).takeIf { !finite },
        BundleProblem.UnknownTimeZone(measurement.id, measurement.timeZone)
            .takeIf { runCatching { TimeZone.of(measurement.timeZone) }.isFailure },
        unknownName("source", measurement.source, MeasurementSource.entries.map { it.name }),
        measurement.enteredUnit?.let { unit: String ->
            BundleProblem.UnknownName("enteredUnit", unit).takeIf { displayUnitOf(unit) == null }
        },
    )
}

private fun unknownName(field: String, value: String, known: List<String>): BundleProblem? {
    return BundleProblem.UnknownName(field, value).takeIf { value !in known }
}
