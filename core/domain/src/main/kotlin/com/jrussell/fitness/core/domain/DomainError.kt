package com.jrussell.fitness.core.domain

/** Why an [Outcome] failed. Each case carries enough detail for the UI to explain it. */
sealed interface DomainError {
    /** [field] was rejected because of [reason] (e.g. out of range, not a finite number). */
    data class Validation(val field: String, val reason: String) : DomainError

    /** The operation needs [permission], which the user has not granted. */
    data class PermissionDenied(val permission: String) : DomainError

    /** An imported file could not be read at [line] because of [reason]. */
    data class ImportParse(val line: Int, val reason: String) : DomainError

    /** No visible [entity] with [id] exists for the current owner. */
    data class NotFound(val entity: String, val id: String) : DomainError
}
