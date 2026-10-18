package com.jrussell.fitness.core.database.dao

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.jrussell.fitness.core.database.FIXTURE_DEVICE
import com.jrussell.fitness.core.database.FIXTURE_NOW
import com.jrussell.fitness.core.database.GetFitDatabase
import com.jrussell.fitness.core.database.aMeasurement
import com.jrussell.fitness.core.database.entity.MeasurementEntity
import com.jrussell.fitness.core.database.inMemoryGetFitDatabase
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MeasurementDaoTest {
    private val database: GetFitDatabase = inMemoryGetFitDatabase()
    private val dao: MeasurementDao = database.measurementDao()
    private val owner: Uuid = Uuid.random()
    private val weight: Uuid = Uuid.random()

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun observe_filtersOwnerTypeRangeAndTombstones() = runTest {
        val match: MeasurementEntity = aMeasurement(owner, weight, LocalDate(2026, 10, 10))
        dao.insert(match)
        dao.insert(aMeasurement(Uuid.random(), weight, LocalDate(2026, 10, 10)))
        dao.insert(aMeasurement(owner, Uuid.random(), LocalDate(2026, 10, 10)))
        dao.insert(aMeasurement(owner, weight, LocalDate(2026, 9, 30)))
        dao.insert(aMeasurement(owner, weight, LocalDate(2026, 10, 11), deletedAt = FIXTURE_NOW))

        val result: List<MeasurementEntity> =
            dao.observeByTypeAndDates(owner, weight, LocalDate(2026, 10, 1), LocalDate(2026, 10, 31)).first()

        assertThat(result).containsExactly(match)
    }

    @Test
    fun tombstone_otherOwner_changesNothing() = runTest {
        val otherOwner: Uuid = Uuid.random()
        val theirs: MeasurementEntity = aMeasurement(otherOwner, weight, LocalDate(2026, 10, 10))
        dao.insert(theirs)

        val changed: Int = dao.tombstone(owner, theirs.id, FIXTURE_NOW, revision = 9, deviceId = FIXTURE_DEVICE)

        assertThat(changed).isEqualTo(0)
        assertThat(dao.findById(otherOwner, theirs.id)).isNotNull()
        assertThat(dao.findById(owner, theirs.id)).isNull()
    }

    @Test
    fun exportAll_includesTombstones() = runTest {
        val live: MeasurementEntity = aMeasurement(owner, weight, LocalDate(2026, 10, 10))
        val deleted: MeasurementEntity = aMeasurement(owner, weight, LocalDate(2026, 10, 11), deletedAt = FIXTURE_NOW)
        dao.insert(live)
        dao.insert(deleted)
        dao.insert(aMeasurement(Uuid.random(), weight, LocalDate(2026, 10, 10)))

        assertThat(dao.exportAll(owner)).containsExactlyInAnyOrder(live, deleted)
    }

    @Test
    fun findAnyById_findsTombstoned() = runTest {
        val deleted: MeasurementEntity = aMeasurement(owner, weight, LocalDate(2026, 10, 11), deletedAt = FIXTURE_NOW)
        dao.insert(deleted)

        assertThat(dao.findAnyById(owner, deleted.id)).isEqualTo(deleted)
        assertThat(dao.findAnyById(Uuid.random(), deleted.id)).isNull()
    }

    @Test
    fun countLive_skipsTombstones() = runTest {
        dao.insert(aMeasurement(owner, weight, LocalDate(2026, 10, 10)))
        dao.insert(aMeasurement(owner, weight, LocalDate(2026, 10, 11), deletedAt = FIXTURE_NOW))
        dao.insert(aMeasurement(Uuid.random(), weight, LocalDate(2026, 10, 10)))

        assertThat(dao.countLive(owner)).isEqualTo(1)
    }

    @Test
    fun update_replacesRow() = runTest {
        val row: MeasurementEntity = aMeasurement(owner, weight, LocalDate(2026, 10, 10))
        dao.insert(row)

        dao.update(row.copy(value = 80.0))

        assertThat(dao.findAnyById(owner, row.id)?.value).isEqualTo(80.0)
    }
}
