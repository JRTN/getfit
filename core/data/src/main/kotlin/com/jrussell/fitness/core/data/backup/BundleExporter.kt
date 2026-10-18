package com.jrussell.fitness.core.data.backup

import com.jrussell.fitness.core.data.owner.CurrentOwnerProvider
import com.jrussell.fitness.core.database.GetFitDatabase
import com.jrussell.fitness.core.importers.export.EXPORT_FORMAT_VERSION
import com.jrussell.fitness.core.importers.export.ExportBundle
import com.jrussell.fitness.core.model.ids.OwnerId
import javax.inject.Inject
import kotlin.time.Clock

/** The installed app's version name, as recorded in exports (`:app` reads it from the package). */
data class AppVersion(val name: String)

/**
 * Builds the JSON export of the current owner's data: their profile, day-start settings,
 * user-defined measurement types and measurements, tombstones included. Built-in types are left
 * out (every install seeds them with the same IDs), as are device-local tables.
 */
class BundleExporter @Inject constructor(
    private val database: GetFitDatabase,
    private val ownerProvider: CurrentOwnerProvider,
    private val clock: Clock,
    private val appVersion: AppVersion,
) {
    suspend fun export(): ExportBundle {
        val owner: OwnerId = ownerProvider.currentOwnerId()
        return ExportBundle(
            formatVersion = EXPORT_FORMAT_VERSION,
            exportedAt = clock.now(),
            appVersion = appVersion.name,
            ownerId = owner.value,
            profiles = listOfNotNull(database.localProfileDao().findLocalProfile()?.toRecord()),
            dayBoundarySettings = database.dayBoundarySettingDao().exportAll(owner.value).map { it.toRecord() },
            measurementTypes = database.measurementTypeDao().exportOwned(owner.value).map { it.toRecord() },
            measurements = database.measurementDao().exportAll(owner.value).map { it.toRecord() },
        )
    }
}
