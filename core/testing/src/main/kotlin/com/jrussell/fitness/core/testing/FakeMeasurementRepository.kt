package com.jrussell.fitness.core.testing

import com.jrussell.fitness.core.domain.DomainError
import com.jrussell.fitness.core.domain.ManualMeasurement
import com.jrussell.fitness.core.domain.MeasurementRepository
import com.jrussell.fitness.core.domain.Outcome
import com.jrussell.fitness.core.domain.localDateFor
import com.jrussell.fitness.core.model.Measurement
import com.jrussell.fitness.core.model.MeasurementSource
import com.jrussell.fitness.core.model.MeasurementTypeKeys
import com.jrussell.fitness.core.model.ids.MeasurementId
import com.jrussell.fitness.core.model.ids.MeasurementTypeId
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate

/**
 * In-memory [MeasurementRepository] for ViewModel tests. It mirrors the real repository's rules:
 * non-finite values are rejected, unknown type keys are not found, a picked date
 * ([ManualMeasurement.localDateOverride]) wins, and otherwise the day starts at [dayStartMinutes].
 *
 * @param knownTypeKeys the type keys that exist; the built-in weight type by default.
 */
class FakeMeasurementRepository(
    knownTypeKeys: Set<String> = setOf(MeasurementTypeKeys.WEIGHT),
    private val dayStartMinutes: Int = 0,
) : MeasurementRepository {
    private val typeIds: Map<String, MeasurementTypeId> =
        knownTypeKeys.associateWith { MeasurementTypeId(Uuid.random()) }
    private val state: MutableStateFlow<List<Pair<String, Measurement>>> = MutableStateFlow(emptyList())

    /** Every live reading, in insertion order, for assertions. */
    val measurements: List<Measurement>
        get() {
            return state.value.map { (_, measurement: Measurement) -> measurement }
        }

    override fun observeMeasurements(typeKey: String, from: LocalDate, to: LocalDate): Flow<List<Measurement>> {
        return state.map { rows: List<Pair<String, Measurement>> ->
            rows.filter { (key: String, measurement: Measurement) ->
                key == typeKey && measurement.localDate in from..to
            }
                .map { (_, measurement: Measurement) -> measurement }
                .sortedBy { measurement: Measurement -> measurement.startTime }
        }
    }

    override suspend fun addManual(entry: ManualMeasurement): Outcome<MeasurementId> {
        val typeId: MeasurementTypeId? = typeIds[entry.typeKey]
        return when {
            !entry.value.isFinite() -> Outcome.Failure(DomainError.Validation("value", "must be a finite number"))
            entry.enteredValue?.isFinite() == false ->
                Outcome.Failure(DomainError.Validation("enteredValue", "must be a finite number"))
            typeId == null -> Outcome.Failure(DomainError.NotFound("MeasurementType", entry.typeKey))
            else -> Outcome.Success(insert(entry, typeId))
        }
    }

    override suspend fun delete(id: MeasurementId): Outcome<Unit> {
        val exists: Boolean = state.value.any { (_, measurement: Measurement) -> measurement.id == id }
        if (!exists) {
            return Outcome.Failure(DomainError.NotFound("Measurement", id.value.toString()))
        }
        state.update { rows: List<Pair<String, Measurement>> ->
            rows.filterNot { (_, measurement: Measurement) -> measurement.id == id }
        }
        return Outcome.Success(Unit)
    }

    private fun insert(entry: ManualMeasurement, typeId: MeasurementTypeId): MeasurementId {
        val id: MeasurementId = MeasurementId(Uuid.random())
        val measurement: Measurement = Measurement(
            id = id,
            typeId = typeId,
            value = entry.value,
            secondaryValue = null,
            startTime = entry.takenAt,
            endTime = entry.takenAt,
            timeZone = entry.timeZone,
            localDate = entry.localDateOverride ?: localDateFor(entry.takenAt, entry.timeZone, dayStartMinutes),
            source = MeasurementSource.MANUAL,
            enteredValue = entry.enteredValue,
            enteredUnit = entry.enteredUnit,
            note = entry.note,
        )
        state.update { rows: List<Pair<String, Measurement>> -> rows + (entry.typeKey to measurement) }
        return id
    }
}
