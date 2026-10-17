package com.jrussell.fitness.core.data.repository

import com.jrussell.fitness.core.database.entity.MeasurementEntity
import com.jrussell.fitness.core.model.Measurement
import com.jrussell.fitness.core.model.ids.MeasurementId
import com.jrussell.fitness.core.model.ids.MeasurementTypeId

/** The domain view of a stored row: typed IDs, no owner or sync columns. */
internal fun MeasurementEntity.toDomain(): Measurement {
    return Measurement(
        id = MeasurementId(id),
        typeId = MeasurementTypeId(measurementTypeId),
        value = value,
        secondaryValue = secondaryValue,
        startTime = startTime,
        endTime = endTime,
        timeZone = timeZone,
        localDate = localDate,
        source = source,
        enteredValue = enteredValue,
        enteredUnit = enteredUnit,
        note = note,
    )
}
