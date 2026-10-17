package com.jrussell.fitness.core.database.dao

import android.database.Cursor
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.jrussell.fitness.core.database.GetFitDatabase
import com.jrussell.fitness.core.database.aMeasurementType
import com.jrussell.fitness.core.database.entity.MeasurementTypeEntity
import com.jrussell.fitness.core.database.inMemoryGetFitDatabase
import kotlin.uuid.Uuid
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MeasurementTypeDaoTest {
    private val database: GetFitDatabase = inMemoryGetFitDatabase()
    private val dao: MeasurementTypeDao = database.measurementTypeDao()
    private val owner: Uuid = Uuid.random()

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun findByKey_returnsBuiltIn_andOwnersOverrideFirst() = runTest {
        val builtIn: MeasurementTypeEntity = aMeasurementType(ownerId = null, key = "weight")
        dao.insertAll(listOf(builtIn))
        assertThat(dao.findByKey(owner, "weight")).isEqualTo(builtIn)

        val own: MeasurementTypeEntity = aMeasurementType(ownerId = owner, key = "weight")
        dao.insertAll(listOf(own))

        assertThat(dao.findByKey(owner, "weight")).isEqualTo(own)
    }

    @Test
    fun findByKey_ignoresOtherOwnersCustomType() = runTest {
        dao.insertAll(listOf(aMeasurementType(ownerId = Uuid.random(), key = "waist")))

        assertThat(dao.findByKey(owner, "waist")).isNull()
    }

    @Test
    fun insertAll_duplicateId_isIgnored() = runTest {
        val builtIn: MeasurementTypeEntity = aMeasurementType(ownerId = null, key = "weight")

        dao.insertAll(listOf(builtIn))
        dao.insertAll(listOf(builtIn.copy(displayName = "Changed")))

        val count: Int = database.openHelper.readableDatabase.query("SELECT COUNT(*) FROM MeasurementType")
            .use { cursor: Cursor ->
                cursor.moveToFirst()
                cursor.getInt(0)
            }
        assertThat(count).isEqualTo(1)
        assertThat(dao.findByKey(owner, "weight")).isEqualTo(builtIn)
    }
}
