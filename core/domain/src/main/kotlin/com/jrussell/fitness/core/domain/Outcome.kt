package com.jrussell.fitness.core.domain

/**
 * The result of an operation that can fail in an expected way (invalid input, missing row).
 * Exceptions stay reserved for programming errors.
 */
sealed interface Outcome<out T> {
    /** The operation succeeded and produced [value]. */
    data class Success<T>(val value: T) : Outcome<T>

    /** The operation failed for the reason in [error]; nothing was changed. */
    data class Failure(val error: DomainError) : Outcome<Nothing>
}
