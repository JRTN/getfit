package com.jrussell.fitness.core.data.seed

import com.jrussell.fitness.core.data.owner.CurrentOwnerProvider
import javax.inject.Inject

/**
 * First-launch (and every-launch) setup of `user.db`: seed the built-in reference rows, then make
 * sure the local profile exists. Both steps are idempotent; `:app` runs this at startup.
 */
class DataBootstrap @Inject constructor(
    private val seeder: ReferenceSeeder,
    private val ownerProvider: CurrentOwnerProvider,
) {
    suspend fun run() {
        seeder.seed()
        ownerProvider.currentOwnerId()
    }
}
