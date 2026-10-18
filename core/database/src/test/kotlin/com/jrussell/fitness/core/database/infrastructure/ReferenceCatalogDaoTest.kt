package com.jrussell.fitness.core.database.infrastructure

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
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
class ReferenceCatalogDaoTest {
    private val database: GetFitDatabase = inMemoryGetFitDatabase()
    private val dao: ReferenceCatalogDao = database.referenceCatalogDao()

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun builtInIds_listsOnlyOwnerless() = runTest {
        val builtIn: MeasurementTypeEntity = aMeasurementType(ownerId = null, key = "weight")
        database.measurementTypeDao().insertAll(listOf(builtIn, aMeasurementType(ownerId = Uuid.random(), key = "x")))

        assertThat(dao.builtInMeasurementTypeIds()).containsExactly(builtIn.id)
    }

    @Test
    fun upsertBuiltIns_insertsThenReplacesByID() = runTest {
        val builtIn: MeasurementTypeEntity = aMeasurementType(ownerId = null, key = "weight")
        dao.upsertBuiltIns(listOf(builtIn))

        dao.upsertBuiltIns(listOf(builtIn.copy(decimals = 2)))

        assertThat(database.measurementTypeDao().findAnyById(Uuid.random(), builtIn.id)?.decimals).isEqualTo(2)
    }
}
