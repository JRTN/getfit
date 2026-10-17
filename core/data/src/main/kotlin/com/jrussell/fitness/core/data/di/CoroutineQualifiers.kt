package com.jrussell.fitness.core.data.di

import javax.inject.Qualifier

/**
 * The dispatcher for blocking I/O (files, DataStore, Room queries). Inject it instead of naming
 * `Dispatchers.IO`, so tests can substitute a test dispatcher. `:app` provides it.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/** The dispatcher for CPU-bound work. Inject it instead of naming `Dispatchers.Default`. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

/**
 * A process-lifetime `CoroutineScope` for work that must outlive any screen, such as first-launch
 * bootstrap. Its `SupervisorJob` keeps one failed child from cancelling the others.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
