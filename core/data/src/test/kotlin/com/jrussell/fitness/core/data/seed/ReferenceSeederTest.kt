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
import com.jrussell.fitness.core.model.DailyAggregation
import com.jrussell.fitness.core.model.MeasurementCategory
import com.jrussell.fitness.core.model.MeasurementTypeKeys
import com.jrussell.fitness.core.model.StandardUnit
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

    @Test
    fun raisedVersion_updatesChangedBuiltIn_keepsOwnedRows() = runTest {
        val owner: Uuid = Uuid.random()
        val builtInId: Uuid = idGenerator.nameBasedId("measurement-type:weight")
        val v1: MeasurementTypeEntity = aBuiltIn(builtInId, decimals = 1)
        seedMeasurementTypes(database, listOf(v1), version = 1, seededAt = TEST_NOW)
        val owned: MeasurementTypeEntity = aBuiltIn(
            Uuid.random(),
            decimals = 3,
        ).copy(ownerId = owner, localRevision = 7)
        database.measurementTypeDao().insert(owned)

        seedMeasurementTypes(database, listOf(v1.copy(decimals = 2)), version = 2, seededAt = TEST_NOW)

        val updated: MeasurementTypeEntity? = database.measurementTypeDao().findAnyById(owner, builtInId)
        assertThat(updated?.decimals).isEqualTo(2)
        assertThat(updated?.localRevision).isEqualTo(0L)
        assertThat(database.measurementTypeDao().findAnyById(owner, owned.id)).isEqualTo(owned)
        assertThat(database.datasetVersionDao().version("measurement_types")).isEqualTo(2)
    }

    @Test
    fun sameVersion_doesNotRewriteBuiltIns() = runTest {
        val builtInId: Uuid = idGenerator.nameBasedId("measurement-type:weight")
        seedMeasurementTypes(database, listOf(aBuiltIn(builtInId, decimals = 1)), version = 1, seededAt = TEST_NOW)

        seedMeasurementTypes(database, listOf(aBuiltIn(builtInId, decimals = 2)), version = 1, seededAt = TEST_NOW)

        assertThat(database.measurementTypeDao().findAnyById(Uuid.random(), builtInId)?.decimals).isEqualTo(1)
    }

    private fun aBuiltIn(id: Uuid, decimals: Int): MeasurementTypeEntity {
        return MeasurementTypeEntity(
            id = id,
            ownerId = null,
            key = MeasurementTypeKeys.WEIGHT,
            displayName = "Weight",
            canonicalUnit = StandardUnit.KILOGRAM,
            category = MeasurementCategory.BODY,
            dailyAggregation = DailyAggregation.LAST,
            hasSecondaryValue = false,
            referenceRangeLow = null,
            referenceRangeHigh = null,
            decimals = decimals,
            healthConnectRecordType = "WeightRecord",
            createdAt = TEST_NOW,
            updatedAt = TEST_NOW,
            deletedAt = null,
            localRevision = 0,
            originDeviceId = null,
        )
    }
}
