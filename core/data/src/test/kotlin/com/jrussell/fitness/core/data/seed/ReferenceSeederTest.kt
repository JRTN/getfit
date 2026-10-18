package com.jrussell.fitness.core.data.seed

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.jrussell.fitness.core.data.TEST_NOW
import com.jrussell.fitness.core.data.count
import com.jrussell.fitness.core.data.id.UuidIdGenerator
import com.jrussell.fitness.core.data.inMemoryGetFitDatabase
import com.jrussell.fitness.core.database.GetFitDatabase
import com.jrussell.fitness.core.database.entity.MeasurementTypeEntity
import com.jrussell.fitness.core.model.MeasurementTypeKeys
import com.jrussell.fitness.core.testing.FakeClock
import kotlin.uuid.Uuid
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReferenceSeederTest {
    private val database: GetFitDatabase = inMemoryGetFitDatabase()
    private val clock: FakeClock = FakeClock(TEST_NOW)
    private val idGenerator: UuidIdGenerator = UuidIdGenerator(clock)
    private val seeder: ReferenceSeeder = ReferenceSeeder(database, idGenerator, clock)

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun seed_insertsBuiltInWeightType() = runTest {
        seeder.seed()

        val weight: MeasurementTypeEntity? =
            database.measurementTypeDao().findByKey(Uuid.random(), MeasurementTypeKeys.WEIGHT)
        assertThat(weight).isNotNull()
        assertThat(weight?.id).isEqualTo(idGenerator.nameBasedId("measurement-type:weight"))
        assertThat(weight?.ownerId).isNull()
        assertThat(weight?.localRevision).isEqualTo(0L)
        assertThat(weight?.canonicalUnit?.code).isEqualTo("kg")
    }

    @Test
    fun seed_twice_keepsOneRowAndRecordsVersion() = runTest {
        seeder.seed()
        seeder.seed()

        assertThat(database.count("SELECT COUNT(*) FROM MeasurementType")).isEqualTo(1)
        assertThat(database.datasetVersionDao().version("measurement_types")).isEqualTo(1)
    }
}
