package com.jrussell.fitness.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType

/*
 * Helpers that let convention plugins read `gradle/libs.versions.toml`.
 *
 * Module build scripts get a generated, type-safe `libs` accessor (`libs.hilt.android`), but
 * precompiled convention plugin classes do not. These helpers look entries up by their alias
 * string instead, so the catalog remains the single source of versions for build-logic too.
 */

/** The project's `libs` version catalog, i.e. the contents of `gradle/libs.versions.toml`. */
val Project.libs: VersionCatalog
    get() {
        return extensions.getByType<VersionCatalogsExtension>().named("libs")
    }

/**
 * Looks up a `[libraries]` entry by its alias, e.g. `library("junit5-jupiter")`.
 *
 * @throws NoSuchElementException if the alias is not in the catalog — a build-logic bug that
 *   should fail the build immediately.
 */
fun VersionCatalog.library(alias: String): Provider<MinimalExternalModuleDependency> {
    return findLibrary(alias).get()
}

/**
 * Returns the plugin ID for a `[plugins]` entry by its alias, e.g. `pluginId("kotlin-jvm")`
 * returns `"org.jetbrains.kotlin.jvm"`. Pass the result to `pluginManager.apply(...)`.
 *
 * @throws NoSuchElementException if the alias is not in the catalog.
 */
fun VersionCatalog.pluginId(alias: String): String {
    return findPlugin(alias).get().get().pluginId
}
