package com.jrussell.fitness.core.model

/** Where a measurement came from. Manual entry always wins over the other sources. */
enum class MeasurementSource {
    MANUAL,
    HEALTH_CONNECT,
    IMPORT,
}

/** How measurement types are grouped in the Body tab and the type editor. */
enum class MeasurementCategory {
    BODY,
    VITAL,
    ACTIVITY,
    LAB,
    CUSTOM,
}

/** How several readings of one type on the same day combine into that day's value. */
enum class DailyAggregation {
    SUM,
    MEAN,
    LAST,
    MIN,
    MAX,
}
