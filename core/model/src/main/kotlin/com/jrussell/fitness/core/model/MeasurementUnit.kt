package com.jrussell.fitness.core.model

/** What kind of quantity a unit measures; it decides which display-unit setting applies. */
enum class Dimension {
    MASS,
    LENGTH,
    RATIO,
    ENERGY,
    DURATION,
    HEART_RATE,
    PRESSURE,
    COUNT,
}

/**
 * The unit a measurement type stores its values in (`MeasurementType.canonicalUnit`). Its [code]
 * is what `user.db` and the JSON export hold, so a code is permanent: it never changes spelling
 * or meaning once shipped. [dimension] is `null` for a [CustomUnit], which has no conversions.
 */
sealed interface MeasurementUnit {
    val code: String
    val dimension: Dimension?
}

/**
 * The built-in stored units. Their meanings are fixed for every measurement type, now and later
 * (decided 2026-10-18, before the schema was pinned): masses in kilograms, lengths in meters,
 * body fat and every other percentage as a fraction from 0 to 1, durations in seconds. The list
 * grows as milestones need more units; existing codes never change.
 */
enum class StandardUnit(override val code: String, override val dimension: Dimension) : MeasurementUnit {
    KILOGRAM("kg", Dimension.MASS),
    METER("m", Dimension.LENGTH),
    FRACTION("fraction", Dimension.RATIO),
    KILOCALORIE("kcal", Dimension.ENERGY),
    SECOND("s", Dimension.DURATION),
    BEATS_PER_MINUTE("bpm", Dimension.HEART_RATE),
    MILLIMETER_OF_MERCURY("mmHg", Dimension.PRESSURE),
    COUNT("count", Dimension.COUNT),
}

/** A user-defined lab marker's unit (e.g. "mg/dL"): shown as typed, never converted. */
data class CustomUnit(val label: String) : MeasurementUnit {
    override val code: String get() = CUSTOM_PREFIX + label
    override val dimension: Dimension? get() = null
}

private const val CUSTOM_PREFIX: String = "custom:"

/** The unit stored as [code], or `null` if it is not a known code or a non-blank `custom:` label. */
fun measurementUnitOf(code: String): MeasurementUnit? {
    return if (code.startsWith(CUSTOM_PREFIX)) {
        code.removePrefix(CUSTOM_PREFIX).takeIf { label: String -> label.isNotBlank() }?.let(::CustomUnit)
    } else {
        StandardUnit.entries.firstOrNull { unit: StandardUnit -> unit.code == code }
    }
}
