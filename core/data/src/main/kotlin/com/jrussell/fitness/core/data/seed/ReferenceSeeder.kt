package com.jrussell.fitness.core.data.seed

import androidx.room.withTransaction
import com.jrussell.fitness.core.database.GetFitDatabase
import com.jrussell.fitness.core.database.entity.DatasetVersionEntity
import com.jrussell.fitness.core.database.entity.MeasurementTypeEntity
import com.jrussell.fitness.core.domain.IdGenerator
import com.jrussell.fitness.core.model.DailyAggregation
import com.jrussell.fitness.core.model.MeasurementCategory
import com.jrussell.fitness.core.model.MeasurementTypeKeys
import com.jrussell.fitness.core.model.StandardUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Seeds the built-in reference rows into `user.db`, once per dataset version.
 *
 * Built-in measurement types get UUIDv5 IDs (identical on every device), no owner,
 * `localRevision = 0` (so a first sync push, which sends `localRevision > 0`, skips them) and no
 * origin device. Raise [MEASUREMENT_TYPES_VERSION] and add rows to [builtInMeasurementTypes] to
 * ship more; existing rows are never overwritten.
 *
 * Repositories that depend on reference rows call [seed] before using them, so a screen opened
 * before the startup bootstrap finishes still finds them. After the first success it returns
 * immediately.
 */
@Singleton
class ReferenceSeeder @Inject constructor(
    private val database: GetFitDatabase,
    private val idGenerator: IdGenerator,
    private val clock: Clock,
) {
    private val mutex: Mutex = Mutex()
    private var seeded: Boolean = false

    suspend fun seed() {
        mutex.withLock {
            if (!seeded) {
                seedOnce()
                seeded = true
            }
        }
    }

    private suspend fun seedOnce() {
        database.withTransaction {
            val applied: Int = database.datasetVersionDao().version(MEASUREMENT_TYPES_KEY) ?: 0
            if (applied < MEASUREMENT_TYPES_VERSION) {
                val now: Instant = clock.now()
                database.measurementTypeDao().insertAll(builtInMeasurementTypes(now))
                database.datasetVersionDao().upsert(
                    DatasetVersionEntity(MEASUREMENT_TYPES_KEY, MEASUREMENT_TYPES_VERSION, now),
                )
            }
        }
    }

    private fun builtInMeasurementTypes(now: Instant): List<MeasurementTypeEntity> {
        return listOf(
            MeasurementTypeEntity(
                id = idGenerator.nameBasedId("measurement-type:${MeasurementTypeKeys.WEIGHT}"),
                ownerId = null,
                key = MeasurementTypeKeys.WEIGHT,
                displayName = "Weight",
                canonicalUnit = StandardUnit.KILOGRAM,
                category = MeasurementCategory.BODY,
                dailyAggregation = DailyAggregation.LAST,
                hasSecondaryValue = false,
                referenceRangeLow = null,
                referenceRangeHigh = null,
                decimals = 1,
                healthConnectRecordType = "WeightRecord",
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
                localRevision = 0,
                originDeviceId = null,
            ),
        )
    }

    private companion object {
        const val MEASUREMENT_TYPES_KEY: String = "measurement_types"
        const val MEASUREMENT_TYPES_VERSION: Int = 1
    }
}
