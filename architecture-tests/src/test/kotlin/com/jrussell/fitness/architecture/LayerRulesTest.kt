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
    private val featureNames: List<String> = listOf("home", "nutrition", "training", "body", "settings")

    /**
     * Rule 1: pure-Kotlin modules use only multiplatform APIs (Kotlin stdlib, kotlinx libraries),
     * so they can become Kotlin Multiplatform without rewrites. Bans Android and JVM-only packages
     * (`java.*`, `javax.*`, …) both as imports and as fully qualified references in code.
     */
    @Test
    fun pureModules_importOnlyMultiplatformApis() {
        pureModuleFiles().assertFalse(strict = true) { file: KoFileDeclaration ->
            file.imports.any { import: KoImportDeclaration -> isPlatformSpecific(import.name) }
        }
    }

    /** Rule 1, continued: no fully qualified platform references slip past the import check. */
    @Test
    fun pureModules_referenceNoPlatformPackagesInCode() {
        pureModuleFiles().assertFalse(strict = true) { file: KoFileDeclaration ->
            codeLines(file).any { line: String -> FULLY_QUALIFIED_PLATFORM_REFERENCE.containsMatchIn(line) }
        }
    }

    private fun pureModuleFiles(): List<KoFileDeclaration> {
        return Konsist.scopeFromProduction()
            .files
            .filter { file: KoFileDeclaration -> normalizedModuleName(file) in pureModules }
    }

    /** The file's lines minus imports, package line and comments (KDoc may mention `java.time`). */
    private fun codeLines(file: KoFileDeclaration): List<String> {
        return file.text.lines()
            .map { line: String -> line.trim() }
            .filterNot { line: String -> NON_CODE_PREFIXES.any { prefix: String -> line.startsWith(prefix) } }
            .map { line: String -> line.substringBefore("//") }
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

    private fun isPlatformSpecific(name: String): Boolean {
        return PLATFORM_PACKAGES.any { prefix: String -> name.startsWith("$prefix.") }
    }

    private companion object {
        /** Packages that exist only on the JVM or Android, never in Kotlin Multiplatform common code. */
        val PLATFORM_PACKAGES: List<String> = listOf("java", "javax", "jdk", "sun", "android", "androidx", "dalvik")

        /** Line starts that mark imports, the package line and comments (KDoc may name `java.time`). */
        val NON_CODE_PREFIXES: List<String> = listOf("import ", "package ", "//", "/*", "*")

        /** A platform package used inline, e.g. `java.util.UUID.randomUUID()`. */
        val FULLY_QUALIFIED_PLATFORM_REFERENCE: Regex =
            Regex("""(?<![\w.])(${PLATFORM_PACKAGES.joinToString("|")})\.[a-z]""")
    }
}
