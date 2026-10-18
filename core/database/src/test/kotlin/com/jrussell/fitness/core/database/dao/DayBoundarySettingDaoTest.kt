package com.jrussell.fitness.core.database.dao

import android.database.sqlite.SQLiteConstraintException
import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.jrussell.fitness.core.database.FIXTURE_NOW
import com.jrussell.fitness.core.database.GetFitDatabase
import com.jrussell.fitness.core.database.aDayBoundarySetting
import com.jrussell.fitness.core.database.entity.DayBoundarySettingEntity
import com.jrussell.fitness.core.database.inMemoryGetFitDatabase
import kotlin.uuid.Uuid
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DayBoundarySettingDaoTest {
    private val database: GetFitDatabase = inMemoryGetFitDatabase()
    private val dao: DayBoundarySettingDao = database.dayBoundarySettingDao()
    private val owner: Uuid = Uuid.random()

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun findInForce_picksLatestOnOrBeforeDay() = runTest {
        dao.insert(aDayBoundarySetting(owner, LocalDate(2026, 1, 1), 240))
        dao.insert(aDayBoundarySetting(owner, LocalDate(2026, 6, 1), 300))

        assertThat(dao.findInForce(owner, LocalDate(2026, 5, 31))?.dayStartMinutes).isEqualTo(240)
        assertThat(dao.findInForce(owner, LocalDate(2026, 6, 1))?.dayStartMinutes).isEqualTo(300)
        assertThat(dao.findInForce(owner, LocalDate(2025, 12, 31))).isNull()
    }

    @Test
    fun findInForce_ignoresOtherOwnerAndTombstones() = runTest {
        dao.insert(aDayBoundarySetting(owner, LocalDate(2026, 1, 1), 240))
        dao.insert(aDayBoundarySetting(owner, LocalDate(2026, 2, 1), 300, deletedAt = FIXTURE_NOW))
        dao.insert(aDayBoundarySetting(Uuid.random(), LocalDate(2026, 3, 1), 60))

        assertThat(dao.findInForce(owner, LocalDate(2026, 4, 1))?.dayStartMinutes).isEqualTo(240)
    }

    @Test
    fun insert_duplicateEffectiveFrom_throwsConstraintViolation() = runTest {
        dao.insert(aDayBoundarySetting(owner, LocalDate(2026, 1, 1), 240))

        val result: Result<Unit> = runCatching { dao.insert(aDayBoundarySetting(owner, LocalDate(2026, 1, 1), 300)) }

        assertThat(result.exceptionOrNull()).isNotNull().isInstanceOf<SQLiteConstraintException>()
    }

    @Test
    fun exportAll_includesTombstones() = runTest {
        val live: DayBoundarySettingEntity = aDayBoundarySetting(owner, LocalDate(2026, 10, 1), 240)
        val deleted: DayBoundarySettingEntity =
            aDayBoundarySetting(owner, LocalDate(2026, 10, 5), 300, deletedAt = FIXTURE_NOW)
        dao.insert(live)
        dao.insert(deleted)

        assertThat(dao.exportAll(owner)).containsExactlyInAnyOrder(live, deleted)
        assertThat(dao.findAnyById(owner, deleted.id)).isEqualTo(deleted)
    }

    @Test
    fun findAnyByEffectiveFrom_findsTombstoned() = runTest {
        val deleted: DayBoundarySettingEntity =
            aDayBoundarySetting(owner, LocalDate(2026, 10, 5), 300, deletedAt = FIXTURE_NOW)
        dao.insert(deleted)

        assertThat(dao.findAnyByEffectiveFrom(owner, LocalDate(2026, 10, 5))).isEqualTo(deleted)
        assertThat(dao.findAnyByEffectiveFrom(Uuid.random(), LocalDate(2026, 10, 5))).isNull()
    }
}
