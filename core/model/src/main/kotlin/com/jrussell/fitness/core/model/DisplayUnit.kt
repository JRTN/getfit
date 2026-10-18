package com.jrussell.fitness.core.model

/**
 * The unit a value was typed in, as recorded in `Measurement.enteredUnit` ("181.7 lb"). Its
 * [code] is stored, so it is permanent once shipped; the list grows as entry screens need more.
 */
enum class DisplayUnit(val code: String, val dimension: Dimension?) {
    POUND("lb", Dimension.MASS),
    KILOGRAM("kg", Dimension.MASS),
    CENTIMETER("cm", Dimension.LENGTH),
    INCH("in", Dimension.LENGTH),
    PERCENT("%", Dimension.RATIO),
}

/** The display unit stored as [code], or `null` if the code is unknown. */
fun displayUnitOf(code: String): DisplayUnit? {
    return DisplayUnit.entries.firstOrNull { unit: DisplayUnit -> unit.code == code }
}

/** The display unit a weight typed in this unit is recorded with. */
fun MassUnit.displayUnit(): DisplayUnit {
    return when (this) {
        MassUnit.POUND -> DisplayUnit.POUND
        MassUnit.KILOGRAM -> DisplayUnit.KILOGRAM
    }
}
