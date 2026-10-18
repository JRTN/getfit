package com.jrussell.fitness.core.testing

import com.jrussell.fitness.core.domain.DayBoundaryRepository
import com.jrussell.fitness.core.domain.DomainError
import com.jrussell.fitness.core.domain.Outcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalDate

/**
 * In-memory [DayBoundaryRepository] with one day start for all dates (enough for ViewModel tests;
 * versioning by effective date is covered by the real repository's tests).
 */
class FakeDayBoundaryRepository(dayStartMinutes: Int = 0) : DayBoundaryRepository {
    private val state: MutableStateFlow<Int> = MutableStateFlow(dayStartMinutes)

    override fun observeDayStartMinutes(on: LocalDate): Flow<Int> {
        return state
    }

    override suspend fun setDayStart(dayStartMinutes: Int, effectiveFrom: LocalDate): Outcome<Unit> {
        if (dayStartMinutes !in VALID_MINUTES) {
            return Outcome.Failure(DomainError.Validation("dayStartMinutes", "must be between 0 and 1439"))
        }
        state.value = dayStartMinutes
        return Outcome.Success(Unit)
    }

    private companion object {
        val VALID_MINUTES: IntRange = 0..1439
    }
}
