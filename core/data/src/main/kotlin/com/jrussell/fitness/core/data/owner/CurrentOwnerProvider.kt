package com.jrussell.fitness.core.data.owner

import com.jrussell.fitness.core.model.ids.OwnerId

/**
 * Who owns the rows a repository reads and writes. Repositories get the owner here and never
 * accept it from callers, so a feature cannot ask for another owner's data.
 */
interface CurrentOwnerProvider {
    suspend fun currentOwnerId(): OwnerId
}
