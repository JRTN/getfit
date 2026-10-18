package com.jrussell.fitness.core.model

/** The unit a mass is shown and typed in. Storage is always kilograms; this is presentation only. */
enum class MassUnit {
    POUND,
    KILOGRAM,
}

/**
 * A mass, stored canonically in kilograms at full precision (code convention 2). Convert only
 * at the edges: typing in pounds becomes kilograms here, and showing pounds converts back, so
 * switching the display unit never changes stored data.
 */
@JvmInline
value class Mass private constructor(val kilograms: Double) {
    /** This mass in pounds. */
    fun toPounds(): Double {
        return kilograms / KILOGRAMS_PER_POUND
    }

    /** This mass in [unit]. */
    fun inUnit(unit: MassUnit): Double {
        return when (unit) {
            MassUnit.KILOGRAM -> kilograms
            MassUnit.POUND -> toPounds()
        }
    }

    companion object {
        /** The international avoirdupois pound, exactly. */
        const val KILOGRAMS_PER_POUND: Double = 0.45359237

        fun ofKilograms(value: Double): Mass {
            return Mass(value)
        }

        fun ofPounds(value: Double): Mass {
            return Mass(value * KILOGRAMS_PER_POUND)
        }

        /** A mass of [value] in [unit]. */
        fun of(value: Double, unit: MassUnit): Mass {
            return when (unit) {
                MassUnit.KILOGRAM -> ofKilograms(value)
                MassUnit.POUND -> ofPounds(value)
            }
        }
    }
}
