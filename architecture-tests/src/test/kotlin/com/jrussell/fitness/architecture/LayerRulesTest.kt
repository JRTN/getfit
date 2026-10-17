package com.jrussell.fitness.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import com.lemonappdev.konsist.api.declaration.KoImportDeclaration
import com.lemonappdev.konsist.api.verify.assertFalse
import org.junit.jupiter.api.Test

/**
 * Enforces the module dependency rules from the design doc ("Module architecture") by scanning
 * the source code. Gradle already blocks most violations structurally; these catch the rest.
 */
class LayerRulesTest {
    private val pureModules: List<String> = listOf("core/model", "core/domain", "core/projection", "core/importers")
    private val featureNames: List<String> = listOf("today", "nutrition", "training", "body", "trends", "settings")

    /** Rule 1: pure-Kotlin modules stay free of Android so they can become Kotlin Multiplatform. */
    @Test
    fun pureModules_importNoAndroid() {
        Konsist.scopeFromProduction()
            .files
            .filter { file: KoFileDeclaration -> normalizedModuleName(file) in pureModules }
            .assertFalse(strict = true) { file: KoFileDeclaration ->
                file.imports.any { import: KoImportDeclaration -> isAndroidImport(import.name) }
            }
    }

    /** Rule 3: no feature module references another feature's package. */
    @Test
    fun featureModules_doNotReferenceOtherFeatures() {
        featureNames.forEach { feature: String ->
            val otherFeaturePackages: List<String> = featureNames
                .filter { other: String -> other != feature }
                .map { other: String -> "com.jrussell.fitness.feature.$other" }
            Konsist.scopeFromModule("feature/$feature")
                .files
                .assertFalse(strict = true) { file: KoFileDeclaration ->
                    file.imports.any { import: KoImportDeclaration ->
                        otherFeaturePackages.any { prefix: String -> import.name.startsWith(prefix) }
                    }
                }
        }
    }

    /** Konsist reports module names with the OS path separator (`core\model` on Windows). */
    private fun normalizedModuleName(file: KoFileDeclaration): String {
        return file.moduleName.replace('\\', '/')
    }

    private fun isAndroidImport(name: String): Boolean {
        return name.startsWith("android.") || name.startsWith("androidx.")
    }
}
