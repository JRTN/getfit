package com.jrussell.fitness.core.data.repository

import com.jrussell.fitness.core.data.FixedDeviceIdProvider
import com.jrussell.fitness.core.data.TEST_NOW
import com.jrussell.fitness.core.data.id.UuidIdGenerator
import com.jrussell.fitness.core.data.inMemoryGetFitDatabase
import com.jrussell.fitness.core.data.owner.SingleLocalOwnerProvider
import com.jrussell.fitness.core.data.seed.ReferenceSeeder
import com.jrussell.fitness.core.data.write.StampedWriter
import com.jrussell.fitness.core.database.GetFitDatabase
import com.jrussell.fitness.core.database.entity.DayBoundarySettingEntity
import com.jrussell.fitness.core.database.entity.MeasurementEntity
import com.jrussell.fitness.core.database.entity.SyncMetadata
import com.jrussell.fitness.core.model.MeasurementSource
import com.jrussell.fitness.core.testing.FakeClock
import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

/** The real data-layer graph over an in-memory database, as `:app` wires it. */
class RepositoryTestHarness {
    val database: GetFitDatabase = inMemoryGetFitDatabase()
    val clock: FakeClock = FakeClock(TEST_NOW)
    val idGenerator: UuidIdGenerator = UuidIdGenerator(clock)
    val stampedWriter: StampedWriter = StampedWriter(database, clock, FixedDeviceIdProvider())
    val ownerProvider: SingleLocalOwnerProvider = SingleLocalOwnerProvider(database, stampedWriter, idGenerator)
    val seeder: ReferenceSeeder = ReferenceSeeder(database, idGenerator, clock)

    val dayBoundaryRepository: DefaultDayBoundaryRepository =
        DefaultDayBoundaryRepository(database, ownerProvider, stampedWriter, idGenerator)
    val measurementRepository: DefaultMeasurementRepository =
        DefaultMeasurementRepository(database, ownerProvider, stampedWriter, idGenerator, seeder)

    fun close() {
        database.close()
    }
}

private val OTHER_DEVICE: Uuid = Uuid.parse("0c44e9a1-5b1d-4f7a-9c2e-8a4d3f6b2c1e")

private fun otherOwnersSync(): SyncMetadata {
    return SyncMetadata(TEST_NOW, TEST_NOW, null, 1, OTHER_DEVICE)
}

/** A day-boundary row belonging to [ownerId], written straight through the DAO. */
fun aDayBoundaryFor(ownerId: Uuid, effectiveFrom: LocalDate, dayStartMinutes: Int): DayBoundarySettingEntity {
    return DayBoundarySettingEntity(Uuid.random(), ownerId, dayStartMinutes, effectiveFrom, otherOwnersSync())
}

/** A manual measurement belonging to [ownerId], written straight through the DAO. */
fun aMeasurementFor(ownerId: Uuid, typeId: Uuid, localDate: LocalDate, at: Instant = TEST_NOW): MeasurementEntity {
    return MeasurementEntity(
        id = Uuid.random(),
        ownerId = ownerId,
        measurementTypeId = typeId,
        value = 70.0,
        secondaryValue = null,
        startTime = at,
        endTime = at,
        timeZone = TimeZone.UTC,
        localDate = localDate,
        source = MeasurementSource.MANUAL,
        sourcePackage = null,
        externalId = null,
        enteredValue = null,
        enteredUnit = null,
        note = null,
        sync = otherOwnersSync(),
    )
}
